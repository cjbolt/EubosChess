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
import eubos.board.SquareAttackEvaluator;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.PositionManager;
import eubos.score.PawnEvalHashTable;
import eubos.score.PositionEvaluator;
import eubos.score.ReferenceScore;
import eubos.search.DrawChecker;
import eubos.search.Score;
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
	
	static final int EUBOS_MAJOR_VERSION = 3;
	static final int EUBOS_MINOR_VERSION = 8;
	
	public static final byte SEARCH_DEPTH_IN_PLY = Byte.MAX_VALUE;
	public static final int DEFAULT_NUM_SEARCH_THREADS = 1;
	
	public static final boolean ENABLE_UCI_INFO_SENDING = true;
	public static final boolean ENABLE_UCI_MOVE_NUMBER = true;
	
	public static final boolean ENABLE_LOGGING = false;
	public static final boolean ENABLE_ASSERTS = false;
	public static final boolean ENABLE_PERFT = false;
	public static final boolean ENABLE_TEST_SUITES = false;
	public static final boolean ENABLE_DEBUG_VALIDATION_SEARCH = false;
	public static final boolean ENABLE_DEBUG_VALIDATION_DRAWS = false;
	public static final boolean ENABLE_TT_DIAGNOSTIC_LOGGING = false;
	public static final boolean ENABLE_TT_DIMENSIONED_TO_POWER_OF_TWO = false;
	
	public static final boolean ENABLE_REPETITION_DETECTION = true;
	public static final boolean ENABLE_TRANSPOSITION_TABLE = true;
	public static final boolean ENABLE_QUIESCENCE_CHECK = true;
	public static final boolean ENABLE_ASPIRATION_WINDOWS = true;
	public static final boolean ENABLE_LAZY_EVALUATION = true;	
	public static final boolean ENABLE_LATE_MOVE_REDUCTION = true;
	public static final boolean ENABLE_NULL_MOVE_PRUNING = true;
	public static final boolean ENABLE_STAGED_MOVE_GENERATION = true;
	public static final boolean ENABLE_COUNTED_PASSED_PAWN_MASKS = true;
	public static final boolean ENABLE_FUTILITY_PRUNING = true;
	public static final boolean ENABLE_PINNED_TO_KING_CHECK_IN_ILLEGAL_DETECTION = true;
	public static final boolean ENABLE_PER_MOVE_FUTILITY_PRUNING = true;
	
	public static final boolean ENABLE_FUTILITY_PRUNING_OF_KILLER_MOVES = false;
	public static final boolean ENABLE_OVERWRITE_TRANS_WITH_SEARCH = false;
	public static final boolean ENABLE_TT_CUT_OFFS_IN_EXTENDED_SEARCH = true;
	
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
	private long lastOnMoveClock;
	
	// Multithreading configuration
	public static int numberOfWorkerThreads;
	public static int numCores;
	public static int defaultNumberOfWorkerThreads;
	static {
		numCores = Runtime.getRuntime().availableProcessors();
		defaultNumberOfWorkerThreads = 1;
		numberOfWorkerThreads = defaultNumberOfWorkerThreads;
	}
	
	int move_overhead = 10;
	
	// Hash configuration
	public static final int MIN_HASH_SIZE = 4;
	public static final int MAX_HASH_SIZE = 4*1000;
	public static final long DEFAULT_HASH_SIZE = FixedSizeTranspositionTable.MBYTES_DEFAULT_HASH_SIZE;

	public static long hashSize = DEFAULT_HASH_SIZE;

    public static Logger logger = Logger.getLogger("eubos.main");
    public static Logger error_logger = Logger.getLogger("eubos.error_logger");

	private static FileHandler fh;
	private static FileHandler efh;
	
    
	public EubosEngineMain() {
		// Attempt to use an auto-flushing output stream 
		super(new BufferedReader(new InputStreamReader(System.in)), new PrintStream(System.out, true));
	}
	
	public EubosEngineMain( PipedWriter out) throws IOException {
		super(new BufferedReader(new PipedReader(out)), new PrintStream(System.out, true));
		logger.setLevel(Level.INFO);
	}
	
	private void createEnginePermanentDataStructures() {
		try {
			hashMap = new FixedSizeTranspositionTable(hashSize, numberOfWorkerThreads);
			dc = new DrawChecker();
			pawnHash = new PawnEvalHashTable();
			whiteRefScore = new ReferenceScore(hashMap);
			blackRefScore = new ReferenceScore(hashMap);
		} catch (OutOfMemoryError oome) {
			long heapFreeSize = Runtime.getRuntime().freeMemory()/1_000_000L;
			logger.severe(String.format("Out of mem %s allocating hashMap=%d MB, trying %d free size",
					oome.getMessage(), hashSize, heapFreeSize));
			hashMap = new FixedSizeTranspositionTable(Math.max(heapFreeSize/2, 0), numberOfWorkerThreads);
        } catch (Exception e) {
        	logger.severe(String.format("Exception occurred allocating hashMap=%d MB: %s",
        			hashSize, e.getMessage()));
        } finally {
        	if (hashMap != null) {
        		createdHashTable = true;
        	}
        }
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
			createEnginePermanentDataStructures();
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
		createdHashTable = false;
		createEnginePermanentDataStructures();
	}

	public void receive(EngineAnalyzeCommand command) {
		if (ENABLE_LOGGING) {
			logger.fine(String.format("Analysing position: %s with moves %s",
					command.board.toString(), command.moves));
		}
		if (!createdHashTable) {
			createEnginePermanentDataStructures();
		}
		createPositionFromAnalyseCommand(command);
	}
	
	void createPositionFromAnalyseCommand(EngineAnalyzeCommand command) {
		lastFen = command.board.toString();
		rootPosition = new PositionManager(lastFen, dc, pawnHash);
		// Apply move history to ensure correct state in draw checker
		for (GenericMove nextMove : command.moves) {
			int eubosMove = Move.toMove(nextMove, rootPosition.getTheBoard());
			assert rootPosition.performMove(eubosMove) : 
				String.format("Illegal move in position command: %s %s %s", nextMove.toString(), lastFen, command.moves);
		}
		lastFen = rootPosition.getFen();
		sendInfoString(String.format("positionReceived %s", lastFen));
	}

	public void receive(EngineStartCalculatingCommand command) {
		if (ENABLE_TT_DIAGNOSTIC_LOGGING) {
			hashMap.resetDiagnostics();
		}
		int forcedMove = MoveList.getForcedMove(rootPosition);
		if (forcedMove != Move.NULL_MOVE) {
			sendInfoString(String.format("forced %s", Move.toString(forcedMove)));
			int [] pv = new int[] { forcedMove };
			SearchResult result = new SearchResult(pv, true, 0L, (byte) 1, true, 0);
			sendBestMoveCommand(result);
		} else {
			// The move searcher will report the best move found via a callback to this object, 
			// this will occur when the tree search is concluded and the thread completes execution.
			moveSearcherFactory(command);
			ms.start();
		}
	}
	
	private void moveSearcherFactory(EngineStartCalculatingCommand command) {
		// Update the Reference Score, used by the Search process, for the new root position
		ReferenceScore refScore = rootPosition.onMoveIsWhite() ? whiteRefScore : blackRefScore;
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
			lastOnMoveClock = clockTime;
			if (command.getDepth() != null) {
				byte searchDepth = (byte)((int)command.getDepth());
				logger.info(String.format("Search move, fixed depth %d", searchDepth));
				ms = new FixedDepthMoveSearcher(this, hashMap, lastFen, dc, searchDepth, refScore);
			} else {
				logger.info("Search move, clock time " + clockTime);
				ms = new MultithreadedIterativeMoveSearcher(this, hashMap, pawnHash, lastFen, dc, clockTime, clockInc,
						numberOfWorkerThreads, refScore, move_overhead);
			}
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
		if (ms != null) ms.halt();
	}

	public void receive(EnginePonderHitCommand command) {
	}
	
	public void sendInfoCommand(ProtocolInformationCommand infoCommand) {
		if (ENABLE_UCI_INFO_SENDING) {
			this.getProtocol().send(infoCommand);
			if (ENABLE_LOGGING) {
				if (infoCommand.getCurrentMoveNumber() == null) {
					logInfo(infoCommand);
					validatePv(infoCommand);
				}
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
		sendInfoString(rootPosition.getFen());
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
	
	private boolean transpositionIsValid(long trans) {
		return trans != 0L;
	}
	
	private long selectBestTranspositionData(long tableRoot, long cacheRoot) {
		long transToValidate = cacheRoot;
		if (transpositionIsValid(tableRoot)) {
			int tableDepth = Transposition.getDepthSearchedInPly(tableRoot);
			int cachedDepth = Transposition.getDepthSearchedInPly(cacheRoot);
			if (cachedDepth <= tableDepth) {
				transToValidate = tableRoot;
			} else {
				sendInfoString(String.format("selectBestTranspositionData using cache, table=%d cache=%d",
						                     tableDepth, cachedDepth));
			}
		} else {
			sendInfoString("selectBestTranspositionData using cache, ttable invalid");
		}
		return transToValidate;
	}
	
	private int getTrustedMove(SearchResult result) {
		int trustedMove = Move.NULL_MOVE;
		
		/* Table root trans has problems: the table hash move can be overwritten by lower depth moves. 
		   This can happen in deep searches if the root transposition is overwritten by aging
		   replacement scheme and then added again at a lower depth as it appears again as a
		   leaf in the search tree. */
		long tableRoot = hashMap.getTransposition(rootPosition.getHash());
		if (result != null) {
			//sendInfoString(String.format("getTrustedMove %s", result.report(rootPosition.getTheBoard())));

			long cacheRoot = result.rootTrans;
			long rootTrans = selectBestTranspositionData(tableRoot, cacheRoot);
			boolean trustedMoveWasFromTrans = false;
			
			if (result.pv != null && result.trusted) {
				trustedMove = result.pv[0];
			} else if (transpositionIsValid(rootTrans)) {
				trustedMove = Move.valueOfFromTransposition(rootTrans, rootPosition.getTheBoard());
				trustedMoveWasFromTrans = true;
			}
			if (EubosEngineMain.ENABLE_DEBUG_VALIDATION_SEARCH) {
				if (lastOnMoveClock > 30000) {
					trustedMove = new Validate(this).validate(trustedMoveWasFromTrans, rootTrans, result, trustedMove);
				}
			}
		} else if (transpositionIsValid(tableRoot)) {
			//sendInfoString("sendBestMoveCommand trans table");
			trustedMove = Move.valueOfFromTransposition(tableRoot, rootPosition.getTheBoard());
		}
		return trustedMove;
	}
	
	public void sendBestMoveCommand(SearchResult result) {
		int trustedMove = getTrustedMove(result);
		if (Move.areEqualForTrans(trustedMove, Move.NULL_MOVE)) {
			trustedMove = MoveList.getRandomMove(rootPosition);
		}
		assert !Move.areEqualForTrans(trustedMove, Move.NULL_MOVE);
		int moveNumber = rootPosition.getMoveNumber();
		
		if (ENABLE_DEBUG_VALIDATION_DRAWS) {
			String fen = rootPosition.getFen();
			rootPosition.performMove(trustedMove);
			// do a 1ply search and see if any moves allow a draw, if they do, throw exception, if we thought we were winning
			if (result != null && result.score > 0 && result.score < Score.PROVISIONAL_BETA-1) {
				new Validate(this).checkForDraws(dc, fen, trustedMove);
			}
		} else {
			rootPosition.performMove(trustedMove);
		}
		
		convertToGenericAndSendBestMove(trustedMove);
		
		if(!rootPosition.getTheBoard().me.isEndgame()) {
			hashMap.clearUp(this, moveNumber);
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
	
	public static void createErrorLog() {
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSSS").format(new java.util.Date());
		String computerName = System.getenv("EUBOS_HOST_NAME");
		String logFileName = timeStamp + ((computerName != null)?"_"+computerName:"") + "_search_validation_failure.txt";
		try {
			efh = new FileHandler(logFileName);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		error_logger.addHandler(efh);
		error_logger.setLevel(Level.SEVERE);
		error_logger.setUseParentHandlers(false);
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
	
	public static void handleFatalError(Throwable e, String err, IPositionAccessors pos) {
		Writer buffer = new StringWriter();
		PrintWriter pw = new PrintWriter(buffer);
		e.printStackTrace(pw);
		String errorFen = pos.getFen();
		String error = String.format("%s: %s\n%s\n%s\n%s",
				err, e.getMessage(), 
				errorFen, pos.unwindMoveStack(), buffer.toString());
		System.err.println(error);
		EubosEngineMain.logger.severe(error);
	}
	
	public void sendInfoString(String debug_string) {
		ProtocolInformationCommand info = new ProtocolInformationCommand();
		info.setString("Eubos "+debug_string);
		sendInfoCommand(info);
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
}
