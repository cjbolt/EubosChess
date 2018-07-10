package eubos.search;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IGenerateMoveList;
import eubos.position.IPositionAccessors;

public class MoveSearcher extends Thread {
	
	private EubosEngineMain eubosEngine;
	private IChangePosition pm;
	private IPositionAccessors pos;
	private MiniMaxMoveGenerator mg;
	
	public MoveSearcher( EubosEngineMain eubos, IChangePosition inputPm, IGenerateMoveList mlgen, IPositionAccessors pos, int searchDepth ) {
		eubosEngine = eubos;
		pm = inputPm;
		this.pos = pos;
		mg = new MiniMaxMoveGenerator( eubosEngine, pm, mlgen, pos, searchDepth );
	}
	
	public void halt() {
		mg.terminateFindMove();
	}
	
	public void run() {
		try {
			GenericMove selectedMove = mg.findMove();
			pm.performMove(selectedMove);
			eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( selectedMove, null ));
		} catch( NoLegalMoveException e ) {
			System.out.println( "Eubos has run out of legal moves for side " + pos.getOnMove().toString() );
		} catch(InvalidPieceException e ) {
			System.out.println( 
					"Serious error: Eubos can't find a piece on the board whilst searching findMove(), at "
							+ e.getAtPosition().toString() );
		}
	}
}
