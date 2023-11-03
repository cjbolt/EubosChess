package eubos.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import com.fluxchess.jcpi.AbstractEngine;
import com.fluxchess.jcpi.commands.EngineAnalyzeCommand;
import com.fluxchess.jcpi.commands.EngineDebugCommand;
import com.fluxchess.jcpi.commands.EngineInitializeRequestCommand;
import com.fluxchess.jcpi.commands.EngineNewGameCommand;
import com.fluxchess.jcpi.commands.EnginePonderHitCommand;
import com.fluxchess.jcpi.commands.EngineReadyRequestCommand;
import com.fluxchess.jcpi.commands.EngineSetOptionCommand;
import com.fluxchess.jcpi.commands.EngineStartCalculatingCommand;
import com.fluxchess.jcpi.commands.EngineStopCalculatingCommand;
import com.fluxchess.jcpi.commands.ProtocolInformationCommand;
import com.fluxchess.jcpi.commands.ProtocolInitializeAnswerCommand;
import com.fluxchess.jcpi.commands.ProtocolReadyAnswerCommand;
import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.models.*;
import com.fluxchess.jcpi.options.Options;
import com.fluxchess.jcpi.options.SpinnerOption;
import com.fluxchess.jcpi.protocols.NoProtocolException;

import eubos.board.Board;

import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.board.SquareAttackEvaluator;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.PositionManager;
import eubos.score.PawnEvalHashTable;
import eubos.score.PositionEvaluator;
import eubos.score.ReferenceScore;
import eubos.search.DrawChecker;
import eubos.search.KillerList;
import eubos.search.PlySearcher;
import eubos.search.PrincipalContinuation;
import eubos.search.Score;
import eubos.search.SearchDebugAgent;
import eubos.search.SearchMetrics;
import eubos.search.SearchResult;
import eubos.search.searchers.AbstractMoveSearcher;
import eubos.search.searchers.FixedDepthMoveSearcher;
import eubos.search.searchers.FixedTimeMoveSearcher;
import eubos.search.searchers.MultithreadedIterativeMoveSearcher;
import eubos.search.transposition.FixedSizeTranspositionTable;
import eubos.search.transposition.Transposition;

import java.text.SimpleDateFormat;
import java.util.logging.*;
import java.util.Set;

public class EubosEngineMain extends AbstractEngine {
	
	static final int EUBOS_MAJOR_VERSION = 2;
	static final int EUBOS_MINOR_VERSION = 24;
	
	public static final byte SEARCH_DEPTH_IN_PLY = Byte.MAX_VALUE;
	public static final int DEFAULT_NUM_SEARCH_THREADS = 1;
	
	public static final boolean ENABLE_LOGGING = false;
	public static final boolean ENABLE_UCI_INFO_SENDING = true;
	public static final boolean ENABLE_UCI_MOVE_NUMBER = false;
	
	public static final boolean ENABLE_ASSERTS = false;
	public static final boolean ENABLE_PERFT = false;
	public static final boolean ENABLE_TEST_SUITES = false;
	
	public static final boolean ENABLE_REPETITION_DETECTION = true;
	public static final boolean ENABLE_TRANSPOSITION_TABLE = true;
	public static final boolean ENABLE_QUIESCENCE_CHECK = true;
	public static final boolean ENABLE_ASPIRATION_WINDOWS = true;
	public static final boolean ENABLE_SKEWED_ASPIRATION_WINDOWS = false;
	public static final boolean ENABLE_LAZY_EVALUATION = true;	
	public static final boolean ENABLE_LATE_MOVE_REDUCTION = true;
	public static final boolean ENABLE_NULL_MOVE_PRUNING = true;
	public static final boolean ENABLE_STAGED_MOVE_GENERATION = true;
	public static final boolean ENABLE_COUNTED_PASSED_PAWN_MASKS = true;
	public static final boolean ENABLE_ITERATIVE_DEEPENING = true;
	public static final boolean ENABLE_FUTILITY_PRUNING = true;
	public static final boolean ENABLE_RAZORING_ON_QUIESCENCE = false;
	public static final boolean ENABLE_FUTILITY_PRUNING_OF_KILLER_MOVES = false;
	public static final boolean ENABLE_PER_MOVE_FUTILITY_PRUNING = true;
	public static final boolean ENABLE_INSTANT_REPLY = false;
	public static final boolean ENABLE_OVERWRITE_TRANS_WITH_SEARCH = false;
	
	public static final boolean ENABLE_PINNED_TO_KING_CHECK_IN_ILLEGAL_DETECTION = true;
	public static final boolean ENABLE_PIECE_LISTS = false;
	
	public static final int MAXIMUM_PLIES_IN_GAME = 250;
	
	// Permanent data structures - static for the duration of a single game
	FixedSizeTranspositionTable hashMap = null;
	PawnEvalHashTable pawnHash = null;
	DrawChecker dc;
	ReferenceScore whiteRefScore;
	ReferenceScore blackRefScore;
	
	// Temporary data structures - created and deleted at each analyse/find move instance
	public PositionManager rootPosition;
	private AbstractMoveSearcher ms;
	String lastFen = null;
	private boolean createdHashTable = false;
	private boolean analysisMode = false;
	
	// Multithreading configuration
	public static int numberOfWorkerThreads;
	public static int numCores;
	public static int defaultNumberOfWorkerThreads;
	static {
		numCores = Runtime.getRuntime().availableProcessors();
		defaultNumberOfWorkerThreads = Math.max(numCores-2, 1);
		numberOfWorkerThreads = defaultNumberOfWorkerThreads;
	}
	
	int move_overhead = 10;
	
	// Hash configuration
	public static final int MIN_HASH_SIZE = 32;
	public static final int MAX_HASH_SIZE = 4*1000;
	public static final long DEFAULT_HASH_SIZE = FixedSizeTranspositionTable.MBYTES_DEFAULT_HASH_SIZE;

	public static long hashSize = DEFAULT_HASH_SIZE;

    public static Logger logger = Logger.getLogger("eubos.main");

	private static FileHandler fh; 
    
	public EubosEngineMain() {
		// Attempt to use an auto-flushing output stream 
		super(new BufferedReader(new InputStreamReader(System.in)), new PrintStream(System.out, true));
	}
	
	public EubosEngineMain( PipedWriter out) throws IOException {
		super(new BufferedReader(new PipedReader(out)), new PrintStream(System.out, true));
		logger.setLevel(Level.INFO);
	}
	
	private void checkToCreateEnginePermanentDataStructures() {
		try {
			if (!createdHashTable) {
				hashMap = new FixedSizeTranspositionTable(hashSize, numberOfWorkerThreads);
			}	
		} catch (OutOfMemoryError oome) {
			long heapFreeSize = Runtime.getRuntime().freeMemory()/1_000_000L;
			logger.severe(String.format("Out of mem %s allocating hashMap=%d MB, trying %d free size",
					oome.getMessage(), hashSize, heapFreeSize));
			hashMap = new FixedSizeTranspositionTable(Math.max(heapFreeSize-1, 0), numberOfWorkerThreads);
        } catch (Exception e) {
        	logger.severe(String.format("Exception occurred allocating hashMap=%d MB: %s",
        			hashSize, e.getMessage()));
        } finally {
        	if (hashMap != null) {
        		createdHashTable = true;
        	}
        }
		dc = new DrawChecker();
		pawnHash = new PawnEvalHashTable();
		whiteRefScore = new ReferenceScore(hashMap);
		blackRefScore = new ReferenceScore(hashMap);
	}

	public void receive(EngineInitializeRequestCommand command) {
		logger.fine("Eubos Initialising");
		ProtocolInitializeAnswerCommand reply = new ProtocolInitializeAnswerCommand(
				String.format("Eubos %d.%d", EUBOS_MAJOR_VERSION, EUBOS_MINOR_VERSION),
				"Chris Bolt");
		reply.addOption(Options.newHashOption((int)DEFAULT_HASH_SIZE, MIN_HASH_SIZE, MAX_HASH_SIZE));
		reply.addOption(new SpinnerOption("Threads", defaultNumberOfWorkerThreads, 1, numCores));
		reply.addOption(new SpinnerOption("Move Overhead", 10, 0, 5000));
		reply.addOption(new SpinnerOption("Lazy Threshold", PositionEvaluator.lazy_eval_threshold_cp, 0, 1000));
		logger.fine(String.format("Cores available=%d", numCores));
		this.getProtocol().send( reply );
	}

	public void receive(EngineSetOptionCommand command) {
		logger.fine(String.format("SetOptionCommand is %s", command.name));
		// If the GUI has configured the hash table size, reinitialise it at the correct size
		if (command.name.startsWith("Hash")) {
			hashSize = Long.parseLong(command.value);
			logger.fine(String.format("MaxHashSizeInMBs=%d", hashSize));
			/* In Heroku Eubos deployments for lichess-bot, we never get a new game UCI command; 
			 * so we need to rebuild the hash table if it was resized from the defaults, force this
			 * by setting the created flag to false. */
			createdHashTable = false;
			checkToCreateEnginePermanentDataStructures();
		}
		if (command.name.startsWith("Threads")) {
			numberOfWorkerThreads = Integer.parseInt(command.value);
			logger.fine(String.format("WorkerThreads=%d", numberOfWorkerThreads));
		}
		if (command.name.startsWith("Move Overhead")) {
			move_overhead = Integer.parseInt(command.value);
			logger.fine(String.format("Move Overhead=%d", move_overhead));
		}
		if (command.name.startsWith("Lazy Threshold")) {
			//PositionEvaluator.lazy_eval_threshold_cp = Integer.parseInt(command.value);
			logger.fine(String.format("Lazy Threshold=%d", PositionEvaluator.lazy_eval_threshold_cp));
		}
	}

	public void receive(EngineDebugCommand command) {
	}

	public void receive(EngineReadyRequestCommand command) {
		this.getProtocol().send( new ProtocolReadyAnswerCommand("") );
	}

	public void receive(EngineNewGameCommand command) {
		logger.fine("New Game");
		checkToCreateEnginePermanentDataStructures();
	}

	public void receive(EngineAnalyzeCommand command) {
		if (ENABLE_LOGGING) {
			logger.fine(String.format("Analysing position: %s with moves %s",
					command.board.toString(), command.moves));
		}
		checkToCreateEnginePermanentDataStructures();
		createPositionFromAnalyseCommand(command);
	}
	
	void createPositionFromAnalyseCommand(EngineAnalyzeCommand command) {
		String uci_fen_string = command.board.toString();
		rootPosition = new PositionManager(uci_fen_string, dc, pawnHash);
		// If there is a move history, apply those moves to ensure correct state in draw checker
		if (!command.moves.isEmpty()) {
			for (GenericMove nextMove : command.moves) {
				int move = Move.toMove(nextMove, rootPosition.getTheBoard());
				rootPosition.performMove(move);
				if (Move.isCapture(move) || Move.isPawnMove(move)) {
					// Pawn moves and captures are irreversible so we can reset the draw checker
					dc.reset(rootPosition.getPlyNumber());
				}
			}
		}
		lastFen = rootPosition.getFen();
		long hashCode = rootPosition.getHash();
		if (ENABLE_LOGGING) {
			logger.info(String.format("positionReceived fen=%s hashCode=%d",
					lastFen, hashCode));
		}
	}

	public void receive(EngineStartCalculatingCommand command) {
		// The move searcher will report the best move found via a callback to this object, 
		// this will occur when the tree search is concluded and the thread completes execution.
		long rootHash = rootPosition.getHash();
		long rootTrans = hashMap.getTransposition(rootHash);
		if (ENABLE_INSTANT_REPLY) {
			if (Score.isMate(Transposition.getScore(rootTrans))) {
				int [] pv = new int[] { Move.valueOfFromTransposition(rootTrans, rootPosition.getTheBoard()) };
				ReferenceScore refScore = Colour.isWhite(rootPosition.getOnMove()) ? whiteRefScore : blackRefScore;
				refScore.updateReference(rootPosition);
				if (ENABLE_LOGGING) {
					logger.info(String.format("EngineStartCalculatingCommand - Mate in transposition %s", 
							Transposition.report(rootTrans, rootPosition.getTheBoard())));
				}
				SearchResult result = new SearchResult(pv, true, rootTrans, Transposition.getDepthSearchedInPly(rootTrans), true, 0);
				sendBestMoveCommand(result);
			}
		} else {
			moveSearcherFactory(command);
			ms.start();
		}
	}
	
	private void moveSearcherFactory(EngineStartCalculatingCommand command) {
		// Update the Reference Score, used by the Search process, for the new root position
		ReferenceScore refScore = Colour.isWhite(rootPosition.getOnMove()) ? whiteRefScore : blackRefScore;
		// Parse clock time data
		boolean clockTimeValid = true;
		long clockTime = 0;
		long clockInc = 0;
		GenericColor side = rootPosition.onMoveIsWhite() ? GenericColor.WHITE : GenericColor.BLACK;
		try {
			clockTime = command.getClock(side);
		} catch (NullPointerException e) {
			logger.warning(String.format("go clock time %d invalid for %c", clockTime, side.toChar()));
			clockTimeValid = false;
		}
		if (clockTimeValid) {
			try {
				clockInc = command.getClockIncrement(side);
			} catch (NullPointerException e) {
				clockTimeValid = true;
				logger.warning("No clock increment");
				clockInc = 0;
			}
		}
		analysisMode = false;
		// Create Move Searcher
		if (clockTimeValid) {
			logger.info("Search move, clock time " + clockTime);
			ms = new MultithreadedIterativeMoveSearcher(this, hashMap, pawnHash, lastFen, dc, clockTime, clockInc,
					numberOfWorkerThreads, refScore, move_overhead);
		}
		else if (command.getMoveTime() != null) {
			logger.info("Search move, fixed time " + command.getMoveTime());
			ms = new FixedTimeMoveSearcher(this, hashMap, pawnHash, lastFen, dc, command.getMoveTime(), refScore);
		} else {
			// Analyse mode
			byte searchDepth = 0;
			if (command.getInfinite()) {
				// We shall use a timed search which, for all intents and purposes, is infinite.
				analysisMode = true;
			} else if (command.getDepth() != null) {
				searchDepth = (byte)((int)command.getDepth());
			}	
			if (searchDepth != 0) {
				logger.info(String.format("Search move, fixed depth %d", searchDepth));
				ms = new FixedDepthMoveSearcher(this, hashMap, lastFen, dc, searchDepth, refScore);
			} else if (analysisMode) {
				logger.info(String.format("Search move, infinite search, threads %d", numberOfWorkerThreads));
				ms = new MultithreadedIterativeMoveSearcher(this, hashMap, pawnHash, lastFen, dc, Long.MAX_VALUE, clockInc,
						numberOfWorkerThreads, refScore, move_overhead);
			} else {
				searchDepth = 8;
				logger.info(String.format("DEFAULT: Search move, fixed depth %d", searchDepth));
				ms = new FixedDepthMoveSearcher(this, hashMap, lastFen, dc, searchDepth, refScore);	
			}
		}
	}

	public void receive(EngineStopCalculatingCommand command) {
		logger.info("UCI Stop command received");
		// Request an early terminate of the move searcher.
		ms.halt();
	}

	public void receive(EnginePonderHitCommand command) {
	}
	
	public void sendInfoCommand(ProtocolInformationCommand infoCommand) {
		if (ENABLE_UCI_INFO_SENDING) {
			this.getProtocol().send(infoCommand);
			if (ENABLE_LOGGING) {
				logInfo(infoCommand);
				validatePv(infoCommand);
			}
		}
	}
	
	private void validatePv(ProtocolInformationCommand command) {
		if (ENABLE_ASSERTS) {
			try {
				if (command.getMoveList() != null) {
					int moves_applied = 0;
					for (GenericMove move : command.getMoveList()) {
						int eubos_move = Move.toMove(move, rootPosition.getTheBoard());
						rootPosition.performMove(eubos_move);
						++moves_applied;
					}
					for (int i=0; i<moves_applied; i++) {
						rootPosition.unperformMove();
					}
				}
			} catch (AssertionError e) {
				Writer buffer = new StringWriter();
				PrintWriter pw = new PrintWriter(buffer);
				e.printStackTrace(pw);
				String error = String.format("PlySearcher threw an exception: %s\n%s\n%s",
						e.getMessage(), this.rootPosition.unwindMoveStack(), buffer.toString());
				System.err.println(error);
				EubosEngineMain.logger.severe(error);
				System.exit(0);
			}
		}
	}
	
	private void logInfo(ProtocolInformationCommand command){
		StringBuilder sb = new StringBuilder();
		sb.append("info");

		if (command.getPvNumber() != null) {
			sb.append(String.format(" multipv %d", command.getPvNumber()));
		}
		if (command.getDepth() != null) {
			sb.append(String.format(" depth %d", command.getDepth()));

			if (command.getMaxDepth() != null) {
				sb.append(String.format(" seldepth %d", command.getMaxDepth()));
			}
		}
		if (command.getMate() != null) {
			sb.append(String.format(" score mate %s", command.getMate().toString()));
		} else if (command.getCentipawns() != null) {
			sb.append(String.format(" score cp %d", command.getCentipawns()));
		}
		if (command.getValue() != null) {
			switch (command.getValue()) {
			case EXACT:
				break;
			case ALPHA:
				sb.append(" upperbound");
				break;
			case BETA:
				sb.append(" lowerbound");
				break;
			default:
				assert false : command.getValue();
			}
		}
		if (command.getMoveList() != null) {
			sb.append(" pv");
			for (GenericMove move : command.getMoveList()) {
				sb.append(String.format(" %s", move.toString()));
			}
		}
		if (command.getCurrentMove() != null) {
			sb.append(String.format(" currmove " + command.getCurrentMove().toString()));
		}
		if (command.getCurrentMoveNumber() != null) {
			sb.append(String.format(" currmovenumber %d", command.getCurrentMoveNumber()));
		}
		if (command.getHash() != null) {
			sb.append(String.format(" hashfull %d", command.getHash()));
		}
		if (command.getNps() != null) {
			sb.append(String.format(" nps %d", command.getNps()));
		}
		if (command.getTime() != null) {
			sb.append(String.format(" time %d", command.getTime()));
		}
		if (command.getNodes() != null) {
			sb.append(String.format(" nodes %d", command.getNodes()));
		}
		logger.info(sb.toString());
	}
	
	private void convertToGenericAndSendBestMove(int nativeBestMove) {
		GenericMove bestMove = Move.toGenericMove(nativeBestMove);
		if (ENABLE_LOGGING) {
			logger.info(String.format("Best Move %s", bestMove));
		}
		ProtocolBestMoveCommand protocolBestMoveCommand = new ProtocolBestMoveCommand(bestMove, null);
		try {
			this.getProtocol().send(protocolBestMoveCommand);
		} catch (NoProtocolException e) {
			// In unit tests carry on without the protocol being connected
		}
	}
	
	private void updateReferenceScoreWhenMateFound(long tableRootTrans) {
		if (Score.isMate(Transposition.getScore(tableRootTrans))) {
			// It is likely we didn't send a uci info pv message, so we need to update the last score here
			ReferenceScore refScore = Colour.isWhite(rootPosition.getOnMove()) ? whiteRefScore : blackRefScore;
			refScore.updateLastScore(tableRootTrans);
		}
	}
	
	private void resetDrawCheckerIfBestMoveIsAPawnMoveOrCapture(int bestMove)
	{
		boolean bestMoveWasCaptureOrPawnMove = Move.isCapture(bestMove) || Move.isPawnMove(bestMove);
		int plyAfterMove = rootPosition.getPlyNumber();
		if (bestMoveWasCaptureOrPawnMove) {
			dc.reset(plyAfterMove);
			dc.setPositionReached(rootPosition.getHash(), plyAfterMove);
		}
		if (analysisMode) {
			dc.reset(plyAfterMove);
		}
	}
	
	private long compareTransWithSearchResult(SearchResult result, long trans) {
		int transBestMove = Transposition.getBestMove(trans);
		int transDepth = Transposition.getDepthSearchedInPly(trans);
		if (result.trusted && transDepth <= result.depth && !Move.areEqualForTrans(transBestMove, result.pv[0])) {
			if (ENABLE_LOGGING) {
				logger.warning(String.format("rootTrans %s inconsistent with search %s, updating hash",
						Transposition.report(trans, rootPosition.getTheBoard()),
						result.report(rootPosition.getTheBoard())));
			}
			if (ENABLE_OVERWRITE_TRANS_WITH_SEARCH) {
				trans = Transposition.setBestMove(trans, result.pv[0]);
				trans = Transposition.setDepthSearchedInPly(trans, (byte)result.depth);
				trans = Transposition.setType(trans, Score.typeUnknown); // We can't be sure which it was
			} else {
				trans = 0L;
			}
		}
		return trans;
	}
	
	private long repopulateRootTransFromCacheIfItWasOverwritten(SearchResult result) {
		long cachedRootTrans = result.rootTrans;
		long tableRootTrans = hashMap.getTransposition(rootPosition.getHash());
		long checkedTrans = 0L;
		// The transposition in the table could have been overwritten during the search;
		// If it has been removed we should rewrite it using the best we have, i.e. the cached version.
		if (tableRootTrans == 0L) {
			if (cachedRootTrans != 0L) {	
				if (ENABLE_LOGGING) {
					logger.info(String.format("rootTrans overwritten replacing with %s",
							Transposition.report(cachedRootTrans, rootPosition.getTheBoard())));
				}
				checkedTrans = compareTransWithSearchResult(result, cachedRootTrans);
			} else {
				logger.severe("repopulateRootTransFromCacheIfItWasOverwritten cache was null!");
			}
		} else {
			updateReferenceScoreWhenMateFound(tableRootTrans);
			checkedTrans = compareTransWithSearchResult(result, tableRootTrans);
		}
		if (checkedTrans != tableRootTrans && checkedTrans != 0L) {
			hashMap.putTransposition(rootPosition.getHash(), checkedTrans);
		}
		return checkedTrans;
	}
	
	public void sendBestMoveCommand(SearchResult result) {
		if (ENABLE_LOGGING) {
			logger.info(String.format("Search %s", result.report(rootPosition.getTheBoard())));
		}
		
		int trustedMove = Move.NULL_MOVE;
		long tableRootTrans = repopulateRootTransFromCacheIfItWasOverwritten(result);
		if (tableRootTrans == 0L) {
			trustedMove = result.pv[0];
		} else {
			trustedMove = Move.valueOfFromTransposition(tableRootTrans, rootPosition.getTheBoard());
		}

		rootPosition.performMove(trustedMove);
		resetDrawCheckerIfBestMoveIsAPawnMoveOrCapture(trustedMove);		
		convertToGenericAndSendBestMove(trustedMove);
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			// do a validation search to the same depth to check the PV move
			short expected_score = (short)-result.score;
			SearchDebugAgent sda = new SearchDebugAgent(rootPosition.getMoveNumber(), rootPosition.getOnMove() == Piece.Colour.white);
			PrincipalContinuation pc = new PrincipalContinuation(EubosEngineMain.SEARCH_DEPTH_IN_PLY, sda);
			PlySearcher ps = new PlySearcher(
					hashMap, 
					pc, 
					new SearchMetrics(rootPosition), 
					null, 
					(byte)(result.depth-1), 
					rootPosition,
					rootPosition,
					rootPosition.getPositionEvaluator(),
					new KillerList(),
					sda,
					new MoveList((PositionManager)rootPosition, 0),
					expected_score);
			int validation_score = ps.searchPly((short)-result.score);
			if (result.trusted) {
				// Does Killer ordering affect determinism of move selected?
				// Or is it to do with the moves that are not searched due to reductions?
				// Often seen when there was a beta cut???
				assert Math.abs(expected_score-validation_score) < 10 :
					String.format("DELTA=%d: Validation_score(%d) != result_score(%d), where PV moves are validation(%s), result(%s)",
							Math.abs(expected_score-validation_score),
							validation_score, result.score,
							pc.toStringAt(0), Move.toString(result.pv.length > 1 ? result.pv[1] : Move.NULL_MOVE));
				// Need to print the whole PV here
				// Need to handle the case where we got a beta cut for some reason and therefore have a single move in PV.
			}
		}
	}
	
	@Override
	protected void quit() {
		logger.info("Quitting Eubos");
		// Request an early terminate of the move searcher.
		if (ms != null)
			ms.halt();
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			
		}
		if (ms != null && ms.isAlive()) {
			logger.severe("failed to stop moveSearcher, killing VM.");
			System.exit(0);
		}
	}

	public static void main(String[] args) {
		if (ENABLE_LOGGING) {
			logStart();
		} else {
			logger.setLevel(Level.OFF);
			logger.setUseParentHandlers(false);
		}
		logger.fine(String.format("Starting Eubos\n %s\n %s\n Total moves %d bytes\n %s Total masks %d\n",
				Board.reportStaticDataSizes(), Piece.reportStaticDataSizes(), Piece.getStaticDataSize(),
				SquareAttackEvaluator.reportStaticDataSizes(), SquareAttackEvaluator.getStaticDataSize()));
		// start the Engine
		Thread EubosThread = new Thread( new EubosEngineMain() );
		EubosThread.setName("EubosMainThread");
		EubosThread.start();
	}
	
	private static void logStart() {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSSS").format(new java.util.Date());
		String computerName = System.getenv("EUBOS_HOST_NAME");
		String logFileName = timeStamp + ((computerName != null)?"_"+computerName:"") + "_eubos_uci_log.txt";
		try {
			fh = new FileHandler(logFileName);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.addHandler(fh);
		logger.setLevel(Level.INFO);
		logger.setUseParentHandlers(false);
	}
	
	public static void printStackTrace() {
		// Get all threads in Java.
		Set<Thread> threads = Thread.getAllStackTraces().keySet();
		Writer buffer = new StringWriter();
		PrintWriter pw = new PrintWriter(buffer);
		for (Thread thread : threads) {
			// Print the thread name and current state of thread.
			pw.append("Thread Name: ");
			pw.append(thread.getName());
			pw.append("\n");
			pw.append("Thread State: ");
			pw.append(thread.getState().toString());
			pw.append("\n");

			// Get the stack trace for the thread and print it.
			StackTraceElement[] stackTraceElements = thread.getStackTrace();
			for (StackTraceElement stackTraceElement : stackTraceElements) {
				pw.append("\t");
				pw.append(stackTraceElement.toString());
				pw.append("\n");
			}
			pw.append("\n");
		}
		String dump = String.format("Thread dump: %s", buffer.toString());
		logger.severe(dump);
	}
}
