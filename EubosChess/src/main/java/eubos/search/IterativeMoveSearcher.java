package eubos.search;

import java.sql.Timestamp;
import java.util.List;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IEvaluate;
import eubos.position.IGenerateMoveList;
import eubos.position.IPositionAccessors;

public class IterativeMoveSearcher extends AbstractMoveSearcher {
	
	long gameTimeRemaining;
	private static final int AVG_MOVES_PER_GAME = 40;
	boolean searchStopped = false;

	public IterativeMoveSearcher(EubosEngineMain eubos, 
			FixedSizeTranspositionTable hashMap, 
			IChangePosition inputPm, 
			IGenerateMoveList mlgen, 
			IPositionAccessors pos, 
			long time,
			IEvaluate pe ) {
		super(eubos,inputPm,pos, new MiniMaxMoveGenerator( eubos, hashMap, inputPm, mlgen, pos, pe ));
		gameTimeRemaining = time;
		this.setName("IterativeMoveSearcher");
	}
	
	@Override
	public void halt() {
		mg.terminateFindMove();
		searchStopped = true; 
	}
	
	@Override
	public void run() {
		byte currentDepth = 1;
		SearchResult res = new SearchResult(null, false);
		List<GenericMove> pc = null;
		long timeQuota = calculateSearchTimeAllocation();
		Timestamp msTargetEndTime = new Timestamp(System.currentTimeMillis() + timeQuota);
		IterativeMoveSearchStopper stopper = new IterativeMoveSearchStopper(msTargetEndTime);
		stopper.start();
		while (!searchStopped) {
			try {
				res = mg.findMove(currentDepth, pc);
			} catch( NoLegalMoveException e ) {
				System.out.println( "Eubos has run out of legal moves for side " + pos.getOnMove().toString() );
			} catch(InvalidPieceException e ) {
				System.out.println( 
						"Serious error: Eubos can't find a piece on the board whilst searching findMove(), at "
								+ e.getAtPosition().toString() );
			}
			if (res != null && res.foundMate)
				break;
			pc = mg.pc.toPvList();
			currentDepth++;
		}
		stopper.end();
		eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( res.bestMove, null ));
	}

	private long calculateSearchTimeAllocation() {
		int moveHypothesis = (AVG_MOVES_PER_GAME - pos.getMoveNumber());
		int movesRemaining = (moveHypothesis > 10) ? moveHypothesis : 10;
		long msPerMove = gameTimeRemaining/movesRemaining;
		return msPerMove;
	}
	
	class IterativeMoveSearchStopper extends Thread {
		
		private static final int CHECK_RATE_MS = 100;
		private Timestamp msTargetEndTime;
		private boolean stopperActive = false;
		
		IterativeMoveSearchStopper( Timestamp endTime ) {
			this.msTargetEndTime = endTime;
		}
		
		public void run() {
			stopperActive = true;
			do {
				Timestamp msCurrTime = new Timestamp(System.currentTimeMillis());
				if (msCurrTime.after(msTargetEndTime)) {
					mg.terminateFindMove();
					searchStopped = true;
					stopperActive = false;
				}
				try {
					synchronized (this) {
						this.wait(CHECK_RATE_MS);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} while (stopperActive);
		}
		
		public void end() {
			stopperActive = false;
			synchronized (this) {
				this.notify();
			}
		}
	}
}
