package eubos.search.searchers;

import java.util.List;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;

import eubos.board.InvalidPieceException;
import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.position.PositionManager;
import eubos.search.DrawChecker;
import eubos.search.KillerList;
import eubos.search.NoLegalMoveException;
import eubos.search.SearchDebugAgent;
import eubos.search.SearchResult;
import eubos.search.transposition.FixedSizeTranspositionTable;

public class IterativeMoveSearcher extends AbstractMoveSearcher {
	
	public static final int AVG_MOVES_PER_GAME = 60;
	long gameTimeRemaining;
	int moveNumber = 0;
	
	boolean searchStopped = false;
	public static final boolean DEBUG_LOGGING = true;
	public static final boolean EXPLICIT_GARBAGE_COLLECTION = false;

	public IterativeMoveSearcher(EubosEngineMain eubos, 
			FixedSizeTranspositionTable hashMap, 
			String fen,  
			DrawChecker dc, 
			long time,
			long increment) {
		super(eubos, fen, dc, hashMap);
		EubosEngineMain.logger.info(
				String.format("Starting initialScore=%d gameTimeRemaining=%d", initialScore, time));
		// We use the lichess hypothesis about increments and game time
		moveNumber = mg.pos.getMoveNumber();
		long incrementTime = increment * Math.max((AVG_MOVES_PER_GAME - moveNumber), 0);
		incrementTime = Math.min(Math.max(time-5000, 0), incrementTime); // Cater for short on time
		gameTimeRemaining = time + incrementTime;
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
		IterativeMoveSearchStopper stopper = new IterativeMoveSearchStopper();
		stopper.start();
		while (!searchStopped) {
			try {
				res = mg.findMove(currentDepth, pc);
			} catch( NoLegalMoveException e ) {
				EubosEngineMain.logger.info(
						String.format("IterativeMoveSearcher out of legal moves"));
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
			if (stopper.extraTime && !searchStopped) {
				// don't start a new iteration, we only allow time to complete the current ply
				searchStopped = true;
				if (DEBUG_LOGGING) {
					EubosEngineMain.logger.info(String.format(
							"findMove stopped, not time for a new iteration, ran for %d ms", stopper.timeRanFor));
				}
			}
			pc = mg.pc.toPvList(0);
			currentDepth++;
			if (currentDepth == Byte.MAX_VALUE) {
				break;
			}
		}
		EubosEngineMain.logger.info(
			String.format("IterativeMoveSearcher ended best=%s gameTimeRemaining=%d", res.bestMove, gameTimeRemaining));
		stopper.end();
		eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( res.bestMove, null ));
		mg.terminateSearchMetricsReporter();
		SearchDebugAgent.close();
		if (EXPLICIT_GARBAGE_COLLECTION) {
			if (gameTimeRemaining > 60000)
				System.gc();
		}
	}

	class IterativeMoveSearchStopper extends Thread {
		
		private boolean stopperActive = false;
		boolean extraTime = false;
		private int checkPoint = 0;
		long timeRanFor = 0;
		long timeIntoWait = 0;
		long timeOutOfWait = 0;
		
		public IterativeMoveSearchStopper() {
			this.setName("IterativeMoveSearchStopper");
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		}
		
		public void run() {
			stopperActive = true;
			boolean hasWaitedOnce = false;
			do {
				long timeQuantaForCheckPoint = calculateSearchTimeQuanta();
				if (hasWaitedOnce) {
					evaluateSearchProgressAtCheckpoint();
					if (DEBUG_LOGGING) {
						EubosEngineMain.logger.info(String.format(
								"checkPoint=%d searchStopped=%s ranFor=%d ", checkPoint, searchStopped, timeRanFor));
					}
				}
				if (stopperActive) {
					// Handle sleeping and account for failure to wake up in a timely fashion
					long duration = sleepAndReportDuration(timeQuantaForCheckPoint);
					gameTimeRemaining -= duration;
					timeRanFor += duration;
					if (duration > 3*timeQuantaForCheckPoint) {
						EubosEngineMain.logger.info(String.format(
								"Problem with waking stopper, quitting! checkPoint=%d ranFor=%d timeQuanta=%d duration=%d",
								checkPoint, timeRanFor, timeQuantaForCheckPoint, duration));
						stopMoveSearcher();
					}
				}
				hasWaitedOnce = true;
			} while (stopperActive);
		}
		
		public void end() {
			stopperActive = false;
			this.interrupt();
		}
		
		private long calculateSearchTimeQuanta() {
			int moveHypothesis = (AVG_MOVES_PER_GAME - moveNumber);
			int movesRemaining = Math.max(moveHypothesis, 10);
			long msPerMove = Math.max((gameTimeRemaining/movesRemaining), 2);
			long timeQuanta = msPerMove/2;
			return timeQuanta;
		}
		
		private void evaluateSearchProgressAtCheckpoint() {
			boolean terminateNow = false;
			
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
				break;
			}
			
			if (terminateNow) { stopMoveSearcher(); } else { checkPoint++; };
		}
		
		private long sleepAndReportDuration(long timeQuanta) {
			timeIntoWait = System.currentTimeMillis();
			try {
				Thread.sleep(timeQuanta);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			timeOutOfWait = System.currentTimeMillis();
			return timeOutOfWait - timeIntoWait;
		}
		
		private void stopMoveSearcher() {
			halt();
			stopperActive = false;
		}
	}
}
