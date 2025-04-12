package eubos.search.searchers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import eubos.main.EubosEngineMain;
import eubos.position.PositionManager;
import eubos.search.DrawChecker;
import eubos.search.ReferenceScore;
import eubos.search.Score;
import eubos.search.SearchResult;
import eubos.search.generators.MiniMaxMoveGenerator;
import eubos.search.transposition.FixedSizeTranspositionTable;

public class MultithreadedIterativeMoveSearcher extends IterativeMoveSearcher {
	
	private static final boolean ALTERNATIVE_MOVE_LIST_ORDERING_IN_WORKER_THREADS = true;
	
	protected int threads = 0;
	
	protected List<MultithreadedSearchWorkerThread> workers;
	protected List<MiniMaxMoveGenerator> moveGenerators;
	
	public MultithreadedIterativeMoveSearcher(EubosEngineMain eubos, 
			FixedSizeTranspositionTable hashMap,
			String fen,  
			DrawChecker dc, 
			long time,
			long increment,
			int threads,
			ReferenceScore refScore,
			int move_overhead) {
		super(eubos, hashMap, fen, dc, time, increment, refScore, move_overhead);
		this.setName("MultithreadedIterativeMoveSearcher");
		this.threads = threads;
		workers = new ArrayList<MultithreadedSearchWorkerThread>(threads);
		createMoveGenerators(hashMap, fen, dc, threads);
	}

	private void createMoveGenerators(FixedSizeTranspositionTable hashMap, String fen, DrawChecker dc, int threads) {
		moveGenerators = new ArrayList<MiniMaxMoveGenerator>(threads);
		// The first move generator shall be that constructed by the abstract MoveSearcher, this one shall be accessed by the stopper thread
		moveGenerators.add(mg);
		// Create subsequent move generators using cloned DrawCheckers and distinct PositionManagers
		for (int i=1; i < threads; i++) {
			DrawChecker cloned_dc = new DrawChecker(dc);
			PositionManager pm = new PositionManager(fen, cloned_dc);
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
		startSearch();
		
		// Create workers and let them run
		for (int i=0; i < threads; i++) {
			MultithreadedSearchWorkerThread worker = new MultithreadedSearchWorkerThread(moveGenerators.get(i), this);
			workers.add(worker);
			worker.start();
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

		try {		
			SearchResult res = getFavouredWorkerResult();
			stopSearch(res);
			eubosEngine.sendBestMoveCommand(res);
		} catch (Exception e) {
			EubosEngineMain.handleFatalError(e, "Exception during sendBestMoveCommand", moveGenerators.get(0).pos);
		}
	}
	
	private SearchResult getFavouredWorkerResult() throws Exception {
		SearchResult result = null;
		int ply = 1000; // set to large value so we back-up the best mate score
		boolean anyFoundMate = false;
		for (MultithreadedSearchWorkerThread worker : workers) {
			eubosEngine.sendInfoString(worker.result.report(worker.myMg.pos.getTheBoard()));
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
			int best_score = Score.PROVISIONAL_ALPHA;
			for (MultithreadedSearchWorkerThread worker : workers) {
				// Find deepest trusted PV
				if (worker.result.trusted) {
					if (worker.result.depth > ply || (worker.result.depth == ply && worker.result.score > best_score)) {
						ply = worker.result.depth;
						best_score = worker.result.score;
						result = worker.result;
					}
				}
			}
			if (ply == 0) {
				// If no trusted PVs, find deepest
				for (MultithreadedSearchWorkerThread worker : workers) {
					if (worker.result.depth > ply || (worker.result.depth == ply && worker.result.score > best_score)) {
						ply = worker.result.depth;
						best_score = worker.result.score;
						result = worker.result;
					}
				}
			}
		}
		if (result == null) {
			StringBuilder sb = new StringBuilder();
			sb.append("Invalid worker exception: ");
			int num = 0;
			for (MultithreadedSearchWorkerThread worker : workers) {
				++num;
				sb.append(String.format("worker %d %s\n", num, worker.result.report(this.mg.pos.getTheBoard())));
			}
			throw new Exception(sb.toString());
		}
		return result;
	}

	class MultithreadedSearchWorkerThread extends Thread {
		
		private AbstractMoveSearcher main;
		private MiniMaxMoveGenerator myMg;
		public SearchResult result;
		private volatile boolean halted = false;
		final AtomicBoolean finished = new AtomicBoolean(false);
		
		public MultithreadedSearchWorkerThread(MiniMaxMoveGenerator moveGen, AbstractMoveSearcher main) {
			this.myMg = moveGen;
			this.main = main;
			result = new SearchResult();
			this.setName(String.format("MultithreadedSearchWorkerThread=%d",this.getId()));
		}
		
		private void stopWorker() {
			halted = true;
			myMg.sda.close();
			if (EubosEngineMain.ENABLE_LOGGING) {
				EubosEngineMain.logger.fine(String.format("Worker %s halted, notifying", getName()));
			}
			synchronized(main) {
				finished.set(true);
				main.notify();
			}
		}
		
		public void run() {
			byte currentDepth = 1;
		
			while (!searchStopped && !halted && currentDepth < EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
				result = myMg.findMove(currentDepth, sr);
				updateScoreHistory(result, currentDepth);
				halted = checkForImmediateHalt(result);
				if (!searchStopped) {
					handleTimeManagement(result, currentDepth);
					currentDepth++;
				}			
			}
			
			// Note: The search result can be attained by directly reading the result member of this object
			// or alternatively, indirectly, by reading the shared transposition table.
			stopWorker();
		}
		
		public void halt() {
			myMg.terminateFindMove(); 
			halted = true;
		}	
	}
}
