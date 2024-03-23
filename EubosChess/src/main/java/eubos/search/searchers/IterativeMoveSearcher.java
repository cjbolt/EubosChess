package eubos.search.searchers;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.score.PawnEvalHashTable;
import eubos.score.ReferenceScore;
import eubos.score.ReferenceScore.Reference;
import eubos.search.DrawChecker;

import eubos.search.Score;
import eubos.search.SearchResult;
import eubos.search.transposition.FixedSizeTranspositionTable;

public class IterativeMoveSearcher extends AbstractMoveSearcher {
	
	public static final int AVG_MOVES_PER_GAME = 60;
	long gameTimeRemaining;
	int moveNumber = 0;
	boolean analyse = false;
	int move_overhead = 10;
	
	volatile boolean searchStopped = false;
	public static final boolean DEBUG_LOGGING = true;
	public static final boolean EXPLICIT_GARBAGE_COLLECTION = false;
	private int [] scoreHistory;
	private int [] deltaHistory;

	public IterativeMoveSearcher(EubosEngineMain eubos, 
			FixedSizeTranspositionTable hashMap,
			PawnEvalHashTable pawnHash,
			String fen,  
			DrawChecker dc, 
			long time,
			long increment,
			ReferenceScore refScore,
			int moveOverhead) {
		super(eubos, fen, dc, hashMap, refScore, pawnHash);
		this.move_overhead = moveOverhead;
		this.setName("IterativeMoveSearcher");
		scoreHistory = new int [EubosEngineMain.SEARCH_DEPTH_IN_PLY+1];
		deltaHistory = new int [EubosEngineMain.SEARCH_DEPTH_IN_PLY+1];
		if (time == Long.MAX_VALUE) {
			analyse = true;
			gameTimeRemaining = time;
		} else {
			setGameTimeRemaining(time, increment);
		}
		if (EubosEngineMain.ENABLE_LOGGING) {
			EubosEngineMain.logger.info(
					String.format("Starting search gameTimeRemaining=%d", gameTimeRemaining));
		}
	}

	private void setGameTimeRemaining(long time, long increment) {
		// We use the Lichess hypothesis about increments and game time
		moveNumber = mg.pos.getMoveNumber();
		long incrementTime = increment * Math.max((AVG_MOVES_PER_GAME - moveNumber), 0);
		incrementTime = Math.min(Math.max(time-5000, 0), incrementTime); // Cater for short on time
		gameTimeRemaining = time + incrementTime;
	}
	
	@Override
	public void halt() {
		mg.terminateFindMove();
		searchStopped = true; 
	}
	
	@Override
	public void run() {
		byte currentDepth = 1;
		int currentDelta = 0;
		SearchResult res = new SearchResult();
		enableSearchMetricsReporter(true);
		IterativeMoveSearchStopper stopper = new IterativeMoveSearchStopper();
		stopper.start();
		while (!searchStopped) {
			res = mg.findMove(currentDepth, sr);
			if (res != null) {
				scoreHistory[currentDepth] = res.score;
				if (currentDepth > 1) {
					currentDelta = scoreHistory[currentDepth-1] - res.score;
				}
				deltaHistory[currentDepth] = currentDelta;
				if (res.foundMate && !analyse) {
					EubosEngineMain.logger.info("IterativeMoveSearcher found mate");
					searchStopped = true;
				} else if (res.pv[0] == Move.NULL_MOVE) {
					EubosEngineMain.logger.severe("IterativeMoveSearcher out of legal moves");
					searchStopped = true;
				}
			}
			if (!searchStopped) {
				Reference ref = refScore.getReference();
				boolean canTerminate = res.score >= (ref.score + stopper.checkpointScoreThreshold[stopper.checkPoint]) 
						&& res.depth >= ref.depth;
				if (stopper.extraTime && canTerminate) {
					searchStopped = true;
					if (EubosEngineMain.ENABLE_LOGGING) {
						EubosEngineMain.logger.fine(String.format(
								"findMove stopped, extraTime and (%d >= (%d + %d @checkPoint=%d) AND depth=%d >= %d ref.depth), ran for %d ms", 
								res.score, ref.score, stopper.checkpointScoreThreshold[stopper.checkPoint], 
								stopper.checkPoint, res.depth ,ref.depth, stopper.timeRanFor));
					}
				}
//				if (stopper.extraTime) {
//					int sum = 0;
//					for (int i = Math.max(1, currentDepth-5); i < currentDepth; i++) {
//						sum += deltaHistory[i];
//					}
//					boolean deteriorating = sum > 25; 
//					if (!deteriorating) {
//						if (DEBUG_LOGGING) {
//							EubosEngineMain.logger.finer(String.format(
//									"findMove stopped, not time for a new iteration, ran for %d ms", stopper.timeRanFor));
//						}
//						searchStopped = true;
//					}
//				}
//				if (stopper.extraTime) {				
//					// don't start a new iteration, we were only allowing time to complete the search at the current ply
//					searchStopped = true;
//					if (DEBUG_LOGGING) {
//						EubosEngineMain.logger.finer(String.format(
//								"findMove stopped, not time for a new iteration, ran for %d ms", stopper.timeRanFor));
//					}
//				}
				currentDepth++;
				if (currentDepth == EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
					break;
				}
			}
		}
		EubosEngineMain.logger.info(
			String.format("IterativeMoveSearcher ended best=%s gameTimeRemaining=%d", res.pv[0], gameTimeRemaining));
		stopper.end();
		enableSearchMetricsReporter(false);
		eubosEngine.sendBestMoveCommand(res);
		terminateSearchMetricsReporter();
		mg.sda.close();
		if (EXPLICIT_GARBAGE_COLLECTION) {
			if (gameTimeRemaining > 60000)
				System.gc();
		}
	}

	class IterativeMoveSearchStopper extends Thread {
		
		final int checkpointScoreThreshold[] = {50, 50, 24, 12, 4, 0, -24, -100, -500};
		private volatile boolean stopperActive = false;
		volatile boolean extraTime = false;
		int checkPoint = 0;
		long timeRanFor = 0;
		long timeIntoWait = 0;
		long timeOutOfWait = 0;
		
		public IterativeMoveSearchStopper() {
			this.setName("IterativeMoveSearchStopper");
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		}
		
		public void run() {
			stopperActive = true;
			EubosEngineMain.logger.finer(String.format("IterativeMoveSearchStopper is now running"));
			try {
				do {
					if (checkWhetherToStop()) { 
						stopMoveSearcher(); 
					}
					if (stopperActive) {
						// Handle sleeping and account for failure to wake up in a timely fashion
						long timeQuantaForCheckPoint = calculateSearchTimeQuanta();
						long duration = sleepAndReportDuration(timeQuantaForCheckPoint);
						gameTimeRemaining -= duration;
						timeRanFor += duration;
						handleAnomalySituations(duration, timeQuantaForCheckPoint);
						checkPoint++;
					}
				} while (stopperActive);
			} catch (Exception e) {
				handleException(e);
			}
			EubosEngineMain.logger.finer(String.format("IterativeMoveSearchStopper has now stopped running"));
		}
		
		public void end() {
			stopperActive = false;
			this.interrupt();
		}
		
		protected long calculateSearchTimeQuanta() {
			int moveHypothesis = (AVG_MOVES_PER_GAME - moveNumber);
			int movesRemaining = Math.max(moveHypothesis, 10);
			long msPerMove = Math.max((gameTimeRemaining/movesRemaining), 2);
			msPerMove -= move_overhead;
			long timeQuanta = Math.max((msPerMove/3), 3);
			return timeQuanta;
		}
		
		private boolean checkWhetherToStop() {
			if (checkPoint == checkpointScoreThreshold.length) return true;
			if (checkPoint == 0) return false;
			
			int threshold = checkpointScoreThreshold[checkPoint];
			boolean canTerminate = false;
			short currentScore;
			byte currDepth;
			boolean hasBackedUpAScore;
			short ref_score;
			byte ref_depth;
			boolean isResearchingAspirationFail = mg.lastAspirationFailed();
			
			if (checkPoint == 2) extraTime = true;
			
			EubosEngineMain.logger.finer("Stopper getting lock for mg.sm");
			synchronized(mg.sm) {
				currentScore = mg.sm.getCpScore();
				currDepth = (byte)mg.sm.getDepth();
				hasBackedUpAScore = mg.sm.isScoreBackedUpFromSearch();			
			}
			EubosEngineMain.logger.finer("Stopper getting lock for ref score");
			Reference ref = refScore.getReference();
			synchronized(ref) {
				ref_score = ref.score;	
				ref_depth = ref.depth;
			}
						
			canTerminate = hasBackedUpAScore 
					&& !isResearchingAspirationFail 
					&& currentScore >= (ref_score + threshold) 
					&& currDepth >= ref_depth;
			
			if (DEBUG_LOGGING) {
				if (EubosEngineMain.ENABLE_LOGGING) {
					EubosEngineMain.logger.info(String.format(
							"canTerminate=%s @ checkPoint=%d" +
							"(hasBackedUp=%s AND !isAspFail=%s AND score=%s more (refScore=%s + thresh=%d) AND depth=%d more ref_depth=%d)"+
							" ranFor=%d",
							canTerminate, checkPoint,hasBackedUpAScore, isResearchingAspirationFail, Score.toString(currentScore),
							Score.toString(ref_score), threshold, currDepth, ref_depth, timeRanFor));
				}
			}
			
			return canTerminate;
		}
		
		private long sleepAndReportDuration(long timeQuanta) {
			timeIntoWait = System.currentTimeMillis();
			if (EubosEngineMain.ENABLE_LOGGING) {
				EubosEngineMain.logger.finer(String.format("IterativeMoveSearchStopper CP=%d into sleep @ %d for %d",
						checkPoint, timeIntoWait, timeQuanta));
			}
			try {
				Thread.sleep(timeQuanta);
			} catch (InterruptedException e) {
				if (EubosEngineMain.ENABLE_LOGGING) {
					EubosEngineMain.logger.info("IterativeMoveSearchStopper interrupted");
				}
				Thread.currentThread().interrupt();
			}
			timeOutOfWait = System.currentTimeMillis();
			if (EubosEngineMain.ENABLE_LOGGING) {
				EubosEngineMain.logger.finer(String.format("IterativeMoveSearchStopper CP=%d out of sleep @ %d",
						checkPoint, timeOutOfWait));
			}
			return timeOutOfWait - timeIntoWait;
		}
		
		private void stopMoveSearcher() {
			halt();
			stopperActive = false;
		}
		
		private void handleException(Exception e) {
			Writer buffer = new StringWriter();
			PrintWriter pw = new PrintWriter(buffer);
			e.printStackTrace(pw);
			String error = String.format("Stopper crashed with: %s\n%s",
					e.getMessage(), buffer.toString());
			System.err.println(error);
			EubosEngineMain.logger.severe(error);
			stopMoveSearcher();
		}
		
		private void handleAnomalySituations(long duration, long timeQuantaForCheckPoint) {
			if (duration > 3*timeQuantaForCheckPoint) {
				EubosEngineMain.logger.severe(String.format(
						"Problem with waking stopper, quitting! checkPoint=%d ranFor=%d timeQuanta=%d duration=%d",
						checkPoint, timeRanFor, timeQuantaForCheckPoint, duration));
				stopMoveSearcher();
			} else if (gameTimeRemaining < 500) {
				/* Because we attempt to exceed the previous reference score depth, in some circumstances
				   (high depth, drawing endgames) it is necessary to quit the search before the ref depth. */
				EubosEngineMain.logger.warning(String.format("Stopping search as gameTimeRemaining=%d < 500ms", gameTimeRemaining));
				stopMoveSearcher();
			}
		}
	}

	public void closeSearchDebugAgent() {
		mg.sda.close();
	}
}
