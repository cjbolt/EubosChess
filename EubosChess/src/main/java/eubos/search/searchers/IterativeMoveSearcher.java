package eubos.search.searchers;

import java.sql.Timestamp;
import java.util.List;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.score.IEvaluate;
import eubos.search.NoLegalMoveException;
import eubos.search.SearchDebugAgent;
import eubos.search.SearchResult;
import eubos.search.generators.MiniMaxMoveGenerator;
import eubos.search.transposition.FixedSizeTranspositionTable;

public class IterativeMoveSearcher extends AbstractMoveSearcher {
	
	long gameTimeRemaining;
	short initialScore;
	public static final int AVG_MOVES_PER_GAME = 60;
	boolean searchStopped = false;

	public IterativeMoveSearcher(EubosEngineMain eubos, 
			FixedSizeTranspositionTable hashMap, 
			IChangePosition inputPm,  
			IPositionAccessors pos, 
			long time,
			IEvaluate pe ) {
		super(eubos,inputPm,pos, new MiniMaxMoveGenerator( eubos, hashMap, inputPm, pos, pe ));
		initialScore = pe.evaluatePosition();
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
		IterativeMoveSearchStopper stopper = new IterativeMoveSearchStopper(msTargetEndTime, initialScore);
		stopper.start();
		while (!searchStopped) {
			try {
				res = mg.findMove(currentDepth, pc);
			} catch( NoLegalMoveException e ) {
				System.err.println(
						String.format("Eubos has run out of legal moves for side %s", pos.getOnMove()));
				searchStopped = true;
			} catch(InvalidPieceException e ) {
				System.err.println(
						String.format("Eubos can't find piece searching findMove(), at %s", e.getAtPosition()));
				searchStopped = true;
			}
			if (res != null && res.foundMate)
				break;
			pc = mg.pc.toPvList();
			currentDepth++;
			if (currentDepth == Byte.MAX_VALUE) {
				break;
			}
		}
		stopper.end();
		eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( res.bestMove, null ));
		SearchDebugAgent.close();
	}

	private long calculateSearchTimeAllocation() {
		int moveHypothesis = (AVG_MOVES_PER_GAME - pos.getMoveNumber());
		int movesRemaining = (moveHypothesis > 10) ? moveHypothesis : 10;
		long msPerMove = gameTimeRemaining/movesRemaining;
		return msPerMove/2;
	}
	
	class IterativeMoveSearchStopper extends Thread {
		
		private static final int CHECK_RATE_MS = 100;
		private Timestamp msTargetEndTime;
		private boolean stopperActive = false;
		private int checkPoint = 0;
		private short lastScore = 0;
		
		IterativeMoveSearchStopper( Timestamp endTime, short initialScore ) {
			this.msTargetEndTime = endTime;
			lastScore = initialScore;
		}
		
		public void run() {
			stopperActive = true;
			boolean terminateNow = false;
			do {
				Timestamp msCurrTime = new Timestamp(System.currentTimeMillis());
				if (msCurrTime.after(msTargetEndTime)) {
					/* Consider extending time for Search according to following... */
					short currentScore = mg.sm.getCpScore();
					switch (checkPoint) {
					case 0:
						if (currentScore > (lastScore + 600))
							terminateNow = true;
						break;
					case 1:
						if (currentScore >= (lastScore - 25))
							terminateNow = true;
						break;
					case 3:
						if (currentScore >= (initialScore - 300))
							terminateNow = true;
						break;
					case 6:
						terminateNow = true;
					default:
						break;
					}
					if (terminateNow) {
						mg.terminateFindMove();
						searchStopped = true;
						stopperActive = false;
					} else {
						lastScore = currentScore;
						checkPoint++;
						// update the target time
						long timeQuota = calculateSearchTimeAllocation();
						msTargetEndTime = new Timestamp(System.currentTimeMillis() + timeQuota);
					}
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
