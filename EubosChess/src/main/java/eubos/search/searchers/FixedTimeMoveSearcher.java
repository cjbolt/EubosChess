package eubos.search.searchers;

import java.sql.Timestamp;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;

import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.score.PawnEvalHashTable;
import eubos.score.ReferenceScore;
import eubos.search.DrawChecker;
import eubos.search.SearchResult;
import eubos.search.transposition.FixedSizeTranspositionTable;


public class FixedTimeMoveSearcher extends AbstractMoveSearcher {

	long moveTime;
	volatile boolean searchStopped = false;

	public FixedTimeMoveSearcher(EubosEngineMain eubos, FixedSizeTranspositionTable hashMap, PawnEvalHashTable pawnHash, String fen, DrawChecker dc, long time) {
		super(eubos, fen, dc, hashMap, new ReferenceScore(hashMap), pawnHash);
		moveTime = time;
		this.setName("FixedTimeMoveSearcher");
	}
	
	@Override
	public void halt() {
		mg.terminateFindMove();
	}
	
	@Override
	public void run() {
		byte currentDepth = 1;
		SearchResult res = new SearchResult(Move.NULL_MOVE, false);
		enableSearchMetricsReporter(true);
		Timestamp msTargetEndTime = new Timestamp(System.currentTimeMillis() + moveTime);
		MoveSearchStopper stopper = new MoveSearchStopper(msTargetEndTime);
		stopper.start();
		while (!searchStopped) {
			res = mg.findMove(currentDepth, sr);
			Timestamp msCurrTime = new Timestamp(System.currentTimeMillis());
			if (msCurrTime.after(msTargetEndTime))
				break;
			if (res != null) {
				if (res.foundMate) {
					EubosEngineMain.logger.info("FixedTimeMoveSearcher found mate");
					searchStopped = true;
				} else if (res.bestMove == Move.NULL_MOVE) {
					EubosEngineMain.logger.info("FixedTimeMoveSearcher out of legal moves");
					searchStopped = true;
				}
			}
			if (!searchStopped) {
				currentDepth++;
				if (currentDepth == EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
					break;
				}
			}
		}
		stopper.end();
		enableSearchMetricsReporter(false);
		eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( Move.toGenericMove(res.bestMove), null ));
		terminateSearchMetricsReporter();
		mg.sda.close();
	}
	
	class MoveSearchStopper extends Thread {
		
		private volatile boolean stopperActive = false;
		long timeRanFor = 0;
		long timeIntoWait = 0;
		long timeOutOfWait = 0;
		Timestamp endTimestamp = null;
		
		public MoveSearchStopper(Timestamp msTargetEndTime) {
			this.setName("IterativeMoveSearchStopper");
			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			endTimestamp = msTargetEndTime;
		}
		
		public void run() {
			stopperActive = true;
			do {
				if (stopperActive) {
					// Handle sleeping and account for failure to wake up in a timely fashion
					sleepAndReportDuration(1000);
					Timestamp msCurrTime = new Timestamp(System.currentTimeMillis());
					if (msCurrTime.after(endTimestamp)) {
						stopMoveSearcher();
						break;
					}
				}
			} while (stopperActive);
		}
		
		public void end() {
			stopperActive = false;
			this.interrupt();
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

