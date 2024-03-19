package eubos.search.searchers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.position.PositionManager;
import eubos.score.PawnEvalHashTable;
import eubos.score.ReferenceScore;
import eubos.search.DrawChecker;

import eubos.search.SearchResult;
import eubos.search.generators.MiniMaxMoveGenerator;
import eubos.search.transposition.FixedSizeTranspositionTable;

public class MultithreadedIterativeMoveSearcher extends IterativeMoveSearcher {
	
	private static final int STAGGERED_START_TIME_FOR_THREADS = 0;
	private static final boolean ALTERNATIVE_MOVE_LIST_ORDERING_IN_WORKER_THREADS = true;
	
	protected IterativeMoveSearchStopper stopper;
	protected int threads = 0;
	
	protected List<MultithreadedSearchWorkerThread> workers;
	protected List<MiniMaxMoveGenerator> moveGenerators;
	
	public MultithreadedIterativeMoveSearcher(EubosEngineMain eubos, 
			FixedSizeTranspositionTable hashMap,
			PawnEvalHashTable pawnHash,
			String fen,  
			DrawChecker dc, 
			long time,
			long increment,
			int threads,
			ReferenceScore refScore,
			int move_overhead) {
		super(eubos, hashMap, pawnHash, fen, dc, time, increment, refScore, move_overhead);
		this.setName("MultithreadedIterativeMoveSearcher");
		this.threads = threads;
		workers = new ArrayList<MultithreadedSearchWorkerThread>(threads);
		createMoveGenerators(hashMap, pawnHash, fen, dc, threads);
		stopper = new IterativeMoveSearchStopper();
	}

	private void createMoveGenerators(FixedSizeTranspositionTable hashMap, PawnEvalHashTable pawnHash, String fen, DrawChecker dc, int threads) {
		moveGenerators = new ArrayList<MiniMaxMoveGenerator>(threads);
		// The first move generator shall be that constructed by the abstract MoveSearcher, this one shall be accessed by the stopper thread
		moveGenerators.add(mg);
		// Create subsequent move generators using cloned DrawCheckers and distinct PositionManagers
		for (int i=1; i < threads; i++) {
			DrawChecker cloned_dc = new DrawChecker(dc);
			PositionManager pm = new PositionManager(fen, cloned_dc, pawnHash);
			MiniMaxMoveGenerator thisMg = new MiniMaxMoveGenerator(hashMap, pm, sr, refScore.getReference());
			moveGenerators.add(thisMg);
		}
		// Set move ordering scheme to use, if in operation
		if (ALTERNATIVE_MOVE_LIST_ORDERING_IN_WORKER_THREADS) {
			int i=0;
			for (MiniMaxMoveGenerator thisMg : moveGenerators) {
				int useOrderingScheme = (i%4)+1;
				thisMg.alternativeMoveListOrdering(useOrderingScheme);
				if (EubosEngineMain.ENABLE_LOGGING) {
					EubosEngineMain.logger.fine(String.format("MoveGenerator %d using ordering scheme %d", i, useOrderingScheme));
				}
				i++;
			}
		}
	}
	
	@Override
	public void halt() {
		searchStopped = true;
		haltAllWorkers();
	}
	
	private void haltAllWorkers() {
		if (EubosEngineMain.ENABLE_LOGGING) {
			EubosEngineMain.logger.info("Halting all workers");
		}
		for (MultithreadedSearchWorkerThread worker : workers) {
			worker.halt();
		}		
	}
	
	private boolean alWorkersFinished() {
		for (MultithreadedSearchWorkerThread worker : workers) {
			if (!worker.finished.get()) {
				if (EubosEngineMain.ENABLE_LOGGING) {
					EubosEngineMain.logger.fine(
							String.format("%s not finished.", worker.getName()));
				}
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unused")
	@Override
	public void run() {
		enableSearchMetricsReporter(true);
		stopper.start();
		
		// Create workers and let them run
		for (int i=0; i < threads; i++) {
			MultithreadedSearchWorkerThread worker = new MultithreadedSearchWorkerThread(moveGenerators.get(i), this);
			workers.add(worker);
			worker.start();
			if (STAGGERED_START_TIME_FOR_THREADS > 0 && gameTimeRemaining > STAGGERED_START_TIME_FOR_THREADS*100) {
				try {
					Thread.sleep(STAGGERED_START_TIME_FOR_THREADS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
		// Wait for multithreaded search to complete, all threads must finish
		synchronized (this) {
			try {
				while (!alWorkersFinished()) {
					wait();
					EubosEngineMain.logger.finer("MultithreadedIterativeMoveSearcher got notified.");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				EubosEngineMain.logger.info("MultithreadedIterativeMoveSearcher got an InterruptedException.");
			}
		}
		enableSearchMetricsReporter(false);
		stopper.end();
		terminateSearchMetricsReporter();
		try {
			eubosEngine.sendBestMoveCommand(getFavouredWorkerResult());
		} catch (Exception e) {
			EubosEngineMain.handleFatalError(e, "Exception during sendBestMoveCommand", moveGenerators.get(0).pos);
		}
	}
	
	private SearchResult getFavouredWorkerResult() {
		SearchResult result = null;
		int ply = 1000; // set to large value so we back-up the best mate score
		boolean anyFoundMate = false;
		for (MultithreadedSearchWorkerThread worker : workers) {
			// If there is a mate, give shortest pv to mate
			if (worker.result.foundMate) {
				anyFoundMate = true;
				if (worker.result.depth < ply) {
					ply = worker.result.depth;
					result = worker.result;
				}
			}
		}
		if (!anyFoundMate) {
			// else favour deepest pv
			ply = 0;
			for (MultithreadedSearchWorkerThread worker : workers) {
				// Find deepest trusted PV
				if (worker.result.trusted) {
					if (worker.result.depth > ply) {
						ply = worker.result.depth;
						result = worker.result;
					}
				}
			}
			if (ply == 0) {
				// If no trusted PVs, find deepest
				for (MultithreadedSearchWorkerThread worker : workers) {
					if (worker.result.depth > ply) {
						ply = worker.result.depth;
						result = worker.result;
					}
				}
			}
		}
		return result;
	}

	class MultithreadedSearchWorkerThread extends Thread {
		
		private AbstractMoveSearcher main;
		private MiniMaxMoveGenerator myMg;
		public SearchResult result;
		private volatile boolean halted = false;
		final AtomicBoolean finished = new AtomicBoolean(false);
		
		public MultithreadedSearchWorkerThread( MiniMaxMoveGenerator moveGen, AbstractMoveSearcher main ) {
			this.myMg = moveGen;
			this.main = main;
			this.setName(String.format("MultithreadedSearchWorkerThread=%d",this.getId()));
		}
		
		public void run() {
			byte currentDepth = 1;
			result = new SearchResult(new int [] {Move.NULL_MOVE}, false, 0L, currentDepth, true, 0);
		
			while (!searchStopped && !halted) {
				result = myMg.findMove(currentDepth, sr);
				if (result != null) {
					if (result.foundMate && !analyse) {
						EubosEngineMain.logger.info("IterativeMoveSearcher found mate");
						searchStopped = true;
						halted = true;
					} else if (result.pv == null || result.pv[0] == Move.NULL_MOVE) {
						EubosEngineMain.logger.severe("IterativeMoveSearcher out of legal moves");
						searchStopped = true;
						halted = true;
					}
				}
				if (!searchStopped) {
//					if (stopper.extraTime) {
//						// don't start a new iteration, we were only allowing time to complete the search at the current ply
//						searchStopped = true;
//						if (DEBUG_LOGGING) {
//							if (EubosEngineMain.ENABLE_LOGGING) {
//								EubosEngineMain.logger.fine(String.format(
//										"findMove stopped, not time for a new iteration, ran for %d ms", stopper.timeRanFor));
//							}
//						}
//					}
					currentDepth++;
					if (currentDepth == EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
						break;
					}
				}			
			}
			// The result can be read by reading the result member of this object or by reading the shared transposition table
			halted = true;
			myMg.reportStatistics();
			myMg.sda.close();
			if (EubosEngineMain.ENABLE_LOGGING) {
				EubosEngineMain.logger.fine(String.format("Worker %s halted, notifying", getName()));
			}
			synchronized(main) {
				finished.set(true);
				main.notify();
			}
		}
		
		public void halt() {
			myMg.terminateFindMove(); 
			halted = true;
		}	
	}
}
