package eubos.search.searchers;

import java.sql.Timestamp;
import java.util.List;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;

import eubos.board.InvalidPieceException;
import eubos.board.Piece.Colour;
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
	
	public static final int AVG_MOVES_PER_GAME = 50;
	long gameTimeRemaining;
	short initialScore;
	boolean searchStopped = false;

	public IterativeMoveSearcher(EubosEngineMain eubos, 
			FixedSizeTranspositionTable hashMap, 
			IChangePosition inputPm,  
			IPositionAccessors pos, 
			long time,
			IEvaluate pe ) {
		super(eubos,inputPm,pos, new MiniMaxMoveGenerator( eubos, hashMap, inputPm, pos, pe ));
		initialScore = pe.evaluatePosition();
		if (Colour.isBlack(pos.getOnMove())) {
			initialScore = (short)-initialScore;
		}
		EubosEngineMain.logger.info("IterativeMoveSearcher initialScore="+initialScore);
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
		List<Integer> pc = null;
		IterativeMoveSearchStopper stopper = new IterativeMoveSearchStopper(initialScore);
		stopper.start();
		while (!searchStopped) {
			try {
				res = mg.findMove(currentDepth, pc);
			} catch( NoLegalMoveException e ) {
				EubosEngineMain.logger.info(
						String.format("IterativeMoveSearcher out of legal moves for %s", pos.getOnMove()));
				searchStopped = true;
			} catch(InvalidPieceException e ) {
				EubosEngineMain.logger.info(
						String.format("IterativeMoveSearcher can't find piece at %s", e.getAtPosition()));
				searchStopped = true;
			}
			if (res != null && res.foundMate) {
				EubosEngineMain.logger.info("IterativeMoveSearcher found mate");
				break;
			}				
			if (stopper.extraTime) {
				// don't start a new iteration, we just allow time to complete the current ply
				searchStopped = true;
			}
			pc = mg.pc.toPvList(0);
			currentDepth++;
			if (currentDepth == Byte.MAX_VALUE) {
				break;
			}
		}
		EubosEngineMain.logger.info(
			String.format("IterativeMoveSearcher ended best=%s, %s", res.bestMove, mg.pc.toStringAt(0)));
		stopper.end();
		eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( res.bestMove, null ));
		mg.terminateSearchMetricsReporter();
		SearchDebugAgent.close();
	}

	class IterativeMoveSearchStopper extends Thread {
		
		private boolean stopperActive = false;
		boolean extraTime = false;
		private int checkPoint = 0;
		
		IterativeMoveSearchStopper(short initialScore) {
		}
		
		public void run() {
			long timeQuanta = 0;
			stopperActive = true;
			boolean hasWaitedOnce = false;
			boolean terminateNow = false;
			do {
				timeQuanta = calculateSearchTimeQuanta();
				if (hasWaitedOnce) {
					/* Consider extending time for Search according to following... */
					short currentScore = mg.sm.getCpScore();
					switch (checkPoint) {
					case 0:
						if (currentScore > (initialScore + 500))
							terminateNow = true;
						break;
					case 1:
						if (currentScore >= (initialScore - 25)) {
							terminateNow = true;
						}
						extraTime = true;
						break;
					case 3:
						if (currentScore >= (initialScore - 300))
							terminateNow = true;
						break;
					case 7:
						terminateNow = true;
					default:
						break;
					}
					if (terminateNow) {
						mg.terminateFindMove();
						searchStopped = true;
						stopperActive = false;
					} else {
						checkPoint++;
					}
				}
				try {
					synchronized (this) {
						this.wait(timeQuanta);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				hasWaitedOnce = true;
			} while (stopperActive);
		}
		
		public void end() {
			stopperActive = false;
			synchronized (this) {
				this.notify();
			}
		}
		
		private long calculateSearchTimeQuanta() {
			int moveHypothesis = (AVG_MOVES_PER_GAME - pos.getMoveNumber());
			int movesRemaining = (moveHypothesis > 10) ? moveHypothesis : 10;
			long msPerMove = gameTimeRemaining/movesRemaining;
			long timeQuanta = msPerMove/2;
			return timeQuanta;
		}
	}
}
