package eubos.search.searchers;

import java.util.List;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;

import eubos.board.InvalidPieceException;
import eubos.main.EubosEngineMain;
import eubos.search.DrawChecker;
import eubos.search.NoLegalMoveException;
import eubos.search.SearchDebugAgent;
import eubos.search.SearchResult;
import eubos.search.generators.MiniMaxMoveGenerator;
import eubos.search.transposition.FixedSizeTranspositionTable;

public class MultithreadedIterativeMoveSearcher extends IterativeMoveSearcher {
	
	protected IterativeMoveSearchStopper stopper;
	protected MiniMaxMoveGenerator mg2;
	
	protected MultithreadedIterativeMoveSearcherWorkerThread worker1;
	protected MultithreadedIterativeMoveSearcherWorkerThread worker2;
	
	public MultithreadedIterativeMoveSearcher(EubosEngineMain eubos, 
			FixedSizeTranspositionTable hashMap, 
			String fen,  
			DrawChecker dc, 
			long time,
			long increment) {
		super(eubos, hashMap, fen, dc, time, increment);
		this.setName("MultithreadedIterativeMoveSearcher");
		mg2 = new MiniMaxMoveGenerator(hashMap, fen, dc, sr);
		stopper = new IterativeMoveSearchStopper();
	}
	
	@Override
	public void halt() {
		worker1.halt();
		worker2.halt();
		searchStopped = true; 
	}
	
	@Override
	public void run() {
		stopper.start();
		// Create workers and let them run
		worker1 = new MultithreadedIterativeMoveSearcherWorkerThread(mg);
		worker2 = new MultithreadedIterativeMoveSearcherWorkerThread(mg2);
		worker1.start();
		worker2.start();
		enableSearchMetricsReporter(true);
		// wait for the workers to check in with their result
		while (!searchStopped) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		// Collate best move from worker threads and send it
		while(worker1.isAlive() && worker2.isAlive()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		enableSearchMetricsReporter(false);
		EubosEngineMain.logger.info(
			String.format("MultithreadedIterativeMoveSearcher worker1 ended best=%s gameTimeRemaining=%d", worker1.result.bestMove, gameTimeRemaining));
		EubosEngineMain.logger.info(
				String.format("MultithreadedIterativeMoveSearcher worker2 ended best=%s gameTimeRemaining=%d", worker2.result.bestMove, gameTimeRemaining));
		stopper.end();
		eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( worker1.result.bestMove, null ));
		terminateSearchMetricsReporter();
		SearchDebugAgent.close();
		if (EXPLICIT_GARBAGE_COLLECTION) {
			if (gameTimeRemaining > 60000)
				System.gc();
		}
	}
	
	class MultithreadedIterativeMoveSearcherWorkerThread extends Thread {
		
		private MiniMaxMoveGenerator myMg;
		public SearchResult result;
		
		public MultithreadedIterativeMoveSearcherWorkerThread( MiniMaxMoveGenerator moveGen ) {
			this.myMg = moveGen;
		}
		
		public void run() {
			
			byte currentDepth = 1;
			List<Integer> pc = null;
			result = new SearchResult(null, false);
			
			EubosEngineMain.logger.info(String.format("MultithreadedIterativeMoveSearcherWorkerThread %s alive", this));
		
			while (!searchStopped) {
				try {
					result = myMg.findMove(currentDepth, pc, sr);
				} catch( NoLegalMoveException e ) {
					EubosEngineMain.logger.info(
							String.format("MultithreadedIterativeMoveSearcherWorkerThread out of legal moves"));
					searchStopped = true;
				} catch(InvalidPieceException e ) {
					EubosEngineMain.logger.info(
							String.format("MultithreadedIterativeMoveSearcherWorkerThread can't find piece at %s", e.getAtPosition()));
					searchStopped = true;
				}
				if (result != null && result.foundMate) {
					EubosEngineMain.logger.info("MultithreadedIterativeMoveSearcherWorkerThread found mate");
					break;
				}				
				if (stopper.extraTime && !searchStopped) {
					// don't start a new iteration, we only allow time to complete the current ply
					searchStopped = true;
					if (DEBUG_LOGGING) {
						EubosEngineMain.logger.info(String.format(
								"MultithreadedIterativeMoveSearcherWorkerThread %s findMove stopped, not time for a new iteration, ran for %d ms", this, stopper.timeRanFor));
					}
				}
				pc = myMg.pc.toPvList(0);
				currentDepth++;
				if (currentDepth == Byte.MAX_VALUE) {
					break;
				}
			}
			
			// Send result to main thread can read result structure
			
		}
		
		public void halt() {
			myMg.terminateFindMove(); 
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
