package eubos.search.searchers;

import java.util.ArrayList;
import java.util.List;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.score.ReferenceScore;
import eubos.search.DrawChecker;
import eubos.search.NoLegalMoveException;
import eubos.search.Score;
import eubos.search.SearchResult;
import eubos.search.generators.MiniMaxMoveGenerator;
import eubos.search.transposition.FixedSizeTranspositionTable;
import eubos.search.transposition.ITransposition;

public class MultithreadedIterativeMoveSearcher extends IterativeMoveSearcher {
	
	private static final int STAGGERED_START_TIME_FOR_THREADS = 25;
	private static final boolean ALTERNATIVE_MOVE_LIST_ORDERING_IN_WORKER_THREADS = true;
	
	protected IterativeMoveSearchStopper stopper;
	protected int threads = 0;
	private long rootPositionHash = 0;
	private FixedSizeTranspositionTable tt;
	
	protected List<MultithreadedSearchWorkerThread> workers;
	protected List<MiniMaxMoveGenerator> moveGenerators;
	
	public MultithreadedIterativeMoveSearcher(EubosEngineMain eubos, 
			FixedSizeTranspositionTable hashMap, 
			String fen,  
			DrawChecker dc, 
			long time,
			long increment,
			int threads,
			ReferenceScore refScore) {
		super(eubos, hashMap, fen, dc, time, increment, refScore);
		this.setName("MultithreadedIterativeMoveSearcher");
		this.threads = threads;
		this.tt = hashMap;
		rootPositionHash = mg.pos.getHash();
		workers = new ArrayList<MultithreadedSearchWorkerThread>(threads);
		createMoveGenerators(hashMap, fen, dc, threads);
		stopper = new IterativeMoveSearchStopper();
	}

	private void createMoveGenerators(FixedSizeTranspositionTable hashMap, String fen, DrawChecker dc, int threads) {
		moveGenerators = new ArrayList<MiniMaxMoveGenerator>(threads);
		// The first move generator shall be that constructed by the abstract MoveSearcher
		moveGenerators.add(mg);
		// Create subsequent move generators using cloned DrawCheckers
		for (int i=1; i < threads; i++) {
			MiniMaxMoveGenerator thisMg = new MiniMaxMoveGenerator(hashMap, fen, new DrawChecker(dc.getState()), sr, refScore);
			moveGenerators.add(thisMg);
		}
		// Set move ordering scheme to use, if in operation
		if (ALTERNATIVE_MOVE_LIST_ORDERING_IN_WORKER_THREADS) {
			int i=0;
			for (MiniMaxMoveGenerator thisMg : moveGenerators) {
				int useOrderingScheme = (i%4)+1;
				thisMg.alternativeMoveListOrdering(useOrderingScheme);
				EubosEngineMain.logger.info(String.format("MoveGenerator %d using ordering scheme %d", i, useOrderingScheme));
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
		EubosEngineMain.logger.info(String.format("Halting all workers"));
		for (MultithreadedSearchWorkerThread worker : workers) {
			worker.halt();
		}		
	}

	@Override
	public void run() {
		enableSearchMetricsReporter(true);
		boolean isSearchCompleted = false;
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
		do {
			try {
				synchronized (this) {
					if (isAtLeastOneWorkerStillAlive()) {
						// Conditional, because if a mate is found from the hash table, the worker(s) might return before we hit the wait
						wait();
						EubosEngineMain.logger.info("MultithreadedIterativeMoveSearcher got notified.");
					} else {
						EubosEngineMain.logger.info("MultithreadedIterativeMoveSearcher dropthrough, already finished!");
						isSearchCompleted = true;
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				EubosEngineMain.logger.info("MultithreadedIterativeMoveSearcher got an InterruptedException.");
			}
		} while (!isSearchCompleted && isAtLeastOneWorkerStillAlive());
		enableSearchMetricsReporter(false);
		stopper.end();
		terminateSearchMetricsReporter();
		sendBestMove();
	}

	private void sendBestMove() {
		GenericMove bestMove;
		ITransposition trans = tt.getTransposition(this.rootPositionHash);
		if (trans != null && trans.getType() == Score.exact) {
			EubosEngineMain.logger.info(String.format("best is trans=%s", trans.report()));
			if (Score.isMate(trans.getScore())) {
				// it is possible that we didn't send a uci info pv message, so update the last score
				refScore.updateLastScore(trans);
			}
			bestMove = Move.toGenericMove(trans.getBestMove());
		} else {
			EubosEngineMain.logger.warning("Can't find bestMove in Transposition Table");
			bestMove = workers.get(0).result.bestMove;
		}
		eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( bestMove, null ));
	}
	
	private boolean isAtLeastOneWorkerStillAlive() {
		boolean isAtLeastOneWorkerStillAlive = false;
		for (MultithreadedSearchWorkerThread worker : workers) {
			if (!worker.halted) {
				isAtLeastOneWorkerStillAlive = true;
				EubosEngineMain.logger.info(String.format("Worker still active %s", worker.getName()));
				break;
			}
		}
		if (!isAtLeastOneWorkerStillAlive) {
			EubosEngineMain.logger.info("All workers halted, stopping MultithreadedIterativeMoveSearcher");
		}
		return isAtLeastOneWorkerStillAlive;
	}

	class MultithreadedSearchWorkerThread extends Thread {
		
		private AbstractMoveSearcher main;
		private MiniMaxMoveGenerator myMg;
		public SearchResult result;
		boolean halted = false;
		
		public MultithreadedSearchWorkerThread( MiniMaxMoveGenerator moveGen, AbstractMoveSearcher main ) {
			this.myMg = moveGen;
			this.main = main;
		}
		
		public void run() {
			byte currentDepth = 1;
			List<Integer> pc = null;
			result = new SearchResult(null, false);
		
			while (!searchStopped || !halted) {
				try {
					result = myMg.findMove(currentDepth, pc, sr);
				} catch( NoLegalMoveException e ) {
					EubosEngineMain.logger.info(
							String.format("out of legal moves"));
					searchStopped = true;
				} catch(InvalidPieceException e ) {
					EubosEngineMain.logger.info(
							String.format("can't find piece at %s", e.getAtPosition()));
					searchStopped = true;
				}
				if (result != null && result.foundMate && !analyse) {
					EubosEngineMain.logger.info(String.format("%s found mate", this));
					break;
				}				
				if (stopper.extraTime && !searchStopped) {
					// don't start a new iteration, we only allow time to complete the current ply
					searchStopped = true;
					if (DEBUG_LOGGING) {
						EubosEngineMain.logger.info(String.format(
							"%s findMove stopped, not time for a new iteration, ran for %d ms", this, stopper.timeRanFor));
					}
				}
				pc = myMg.pc.toPvList(0);
				currentDepth++;
				if (currentDepth == Byte.MAX_VALUE) {
					break;
				}				
			}
			// The result can be read by reading the result member of this object or by reading the shared transposition table
			halted = true;
			EubosEngineMain.logger.info(String.format("Worker %s halted, notifying", this.getName()));
			synchronized(main) {
				main.notify();
			}
			myMg.sda.close();
		}
		
		public void halt() {
			myMg.terminateFindMove(); 
			halted = true;
		}	
	}
}
