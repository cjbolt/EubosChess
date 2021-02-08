package eubos.search.searchers;

import java.util.List;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;

import eubos.board.InvalidPieceException;
import eubos.main.EubosEngineMain;
import eubos.score.ReferenceScore;
import eubos.score.ReferenceScore.Reference;
import eubos.search.DrawChecker;
import eubos.search.NoLegalMoveException;
import eubos.search.SearchResult;
import eubos.search.transposition.FixedSizeTranspositionTable;

public class IterativeMoveSearcher extends AbstractMoveSearcher {
	
	public static final int AVG_MOVES_PER_GAME = 60;
	long gameTimeRemaining;
	int moveNumber = 0;
	boolean analyse = false;
	
	boolean searchStopped = false;
	public static final boolean DEBUG_LOGGING = true;
	public static final boolean EXPLICIT_GARBAGE_COLLECTION = false;

	public IterativeMoveSearcher(EubosEngineMain eubos, 
			FixedSizeTranspositionTable hashMap, 
			String fen,  
			DrawChecker dc, 
			long time,
			long increment,
			ReferenceScore refScore) {
		super(eubos, fen, dc, hashMap, refScore);
		this.setName("IterativeMoveSearcher");
		if (time == Long.MAX_VALUE) {
			analyse = true;
			gameTimeRemaining = time;
		} else {
			setGameTimeRemaining(time, increment);
		}
		EubosEngineMain.logger.info(
				String.format("Starting search gameTimeRemaining=%d", gameTimeRemaining));
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
		SearchResult res = new SearchResult(null, false);
		List<Integer> pc = null;
		enableSearchMetricsReporter(true);
		IterativeMoveSearchStopper stopper = new IterativeMoveSearchStopper();
		stopper.start();
		while (!searchStopped) {
			try {
				res = mg.findMove(currentDepth, pc, sr);
			} catch( NoLegalMoveException e ) {
				EubosEngineMain.logger.info(
						String.format("IterativeMoveSearcher out of legal moves"));
				searchStopped = true;
			} catch(InvalidPieceException e ) {
				EubosEngineMain.logger.info(
						String.format("IterativeMoveSearcher can't find piece at %s", e.getAtPosition()));
				searchStopped = true;
			}
			if (res != null && res.foundMate && !analyse) {
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
		enableSearchMetricsReporter(false);
		eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( res.bestMove, null ));
		terminateSearchMetricsReporter();
		mg.sda.close();
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
		
		protected long calculateSearchTimeQuanta() {
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
			byte currDepth = (byte)mg.sm.getDepth();
			Reference ref = refScore.getReference();
			switch (checkPoint) {
			case 1:
				if (currentScore >= ref.score && currDepth >= ref.depth) {
					terminateNow = true;
				}
				extraTime = true;
				break;
			case 3:
				if ((currentScore >= ref.score - 300) && (currDepth >= ref.depth))
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

	public void closeSearchDebugAgent() {
		mg.sda.close();
	}
}
