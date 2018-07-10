package eubos.search;

import java.sql.Timestamp;
import java.util.LinkedList;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IGenerateMoveList;
import eubos.position.IPositionAccessors;

public class IterativeMoveSearcher extends AbstractMoveSearcher {
	
	long gameTimeRemaining;
	private static final int AVG_MOVES_PER_GAME = 70;

	public IterativeMoveSearcher(EubosEngineMain eubos, IChangePosition inputPm, 
			IGenerateMoveList mlgen, IPositionAccessors pos, long time ) {
		super(eubos,inputPm,pos, new MiniMaxMoveGenerator( eubos, inputPm, mlgen, pos ));
		gameTimeRemaining = time;
	}
	
	@Override
	public void halt() {
		mg.terminateFindMove();
	}
	
	@Override
	public void run() {
		GenericMove selectedMove = null;
		LinkedList<GenericMove> pc = null;
		long timeQuota = calculateSearchTimeAllocation();
		Timestamp msTargetEndTime = new Timestamp(System.currentTimeMillis() + timeQuota);
		for (int depth=1; depth<8; depth++) {
			try {
				// Need to seed the moveList so that the best moves are searched first.
				selectedMove = mg.findMove(depth, pc);
			} catch( NoLegalMoveException e ) {
				System.out.println( "Eubos has run out of legal moves for side " + pos.getOnMove().toString() );
			} catch(InvalidPieceException e ) {
				System.out.println( 
						"Serious error: Eubos can't find a piece on the board whilst searching findMove(), at "
								+ e.getAtPosition().toString() );
			}
			Timestamp msCurrTime = new Timestamp(System.currentTimeMillis());
			if (msCurrTime.after(msTargetEndTime))
				break;
			pc = mg.pc.toPvList();
		}
		eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( selectedMove, null ));
	}

	private long calculateSearchTimeAllocation() {
		int moveHypothesis = (AVG_MOVES_PER_GAME - pos.getMoveNumber());
		int movesRemaining = (moveHypothesis > 0) ? moveHypothesis : 10;
		long msPerMove = gameTimeRemaining/movesRemaining;
		return msPerMove;
	}
}
