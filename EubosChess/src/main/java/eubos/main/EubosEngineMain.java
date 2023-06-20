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
	
	static final int EUBOS_MAJOR_VERSION = 2;
	static final int EUBOS_MINOR_VERSION = 22;
	
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
	public static final boolean ENABLE_LAZY_EVALUATION = true;	
	public static final boolean ENABLE_LATE_MOVE_REDUCTION = true;
	public static final boolean ENABLE_NULL_MOVE_PRUNING = true;
	public static final boolean ENABLE_STAGED_MOVE_GENERATION = true;
	public static final boolean ENABLE_COUNTED_PASSED_PAWN_MASKS = true;
	public static final boolean ENABLE_STORE_PV_IN_TRANS_TABLE = true;
	public static final boolean ENABLE_ITERATIVE_DEEPENING = true;
	public static final boolean ENABLE_FUTILITY_PRUNING = true;
	public static final boolean ENABLE_RAZORING_ON_QUIESCENCE = false;
	
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
		logger.fine(String.format("Analysing position: %s with moves %s",
				command.board.toString(), command.moves));
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
		logger.info(String.format("positionReceived fen=%s hashCode=%d",
				lastFen, hashCode));
	}

	public void receive(EngineStartCalculatingCommand command) {
		// The move searcher will report the best move found via a callback to this object, 
		// this will occur when the tree search is concluded and the thread completes execution.
		long rootHash = rootPosition.getHash();
		long rootTrans = hashMap.getTransposition(rootHash);
		if (Score.isMate(Transposition.getScore(rootTrans))) {
			int [] pv = new int[] { Transposition.getBestMove(rootTrans) };
			ReferenceScore refScore = Colour.isWhite(rootPosition.getOnMove()) ? whiteRefScore : blackRefScore;
			refScore.updateReference(rootPosition);
			logger.info(String.format("EngineStartCalculatingCommand - Mate in transposition %s", Transposition.report(rootTrans)));
			SearchResult result = new SearchResult(pv, true, rootTrans, Transposition.getDepthSearchedInPly(rootTrans));
			sendBestMoveCommand(result);
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
		String uciInfo = "info";

		if (command.getPvNumber() != null) {
			uciInfo += " multipv " + command.getPvNumber().toString();
		}
		if (command.getDepth() != null) {
			uciInfo += " depth " + command.getDepth().toString();

			if (command.getMaxDepth() != null) {
				uciInfo += " seldepth " + command.getMaxDepth().toString();
			}
		}
		if (command.getMate() != null) {
			uciInfo += " score mate " + command.getMate().toString();
		} else if (command.getCentipawns() != null) {
			uciInfo += " score cp " + command.getCentipawns().toString();
		}
		if (command.getValue() != null) {
			switch (command.getValue()) {
			case EXACT:
				break;
			case ALPHA:
				uciInfo += " upperbound";
				break;
			case BETA:
				uciInfo += " lowerbound";
				break;
			default:
				assert false : command.getValue();
			}
		}
		if (command.getMoveList() != null) {
			uciInfo += " pv";
			for (GenericMove move : command.getMoveList()) {
				uciInfo += " ";
				uciInfo += move.toString();
			}
		}
		if (command.getRefutationList() != null) {
			uciInfo += " refutation";
			for (GenericMove move : command.getRefutationList()) {
				uciInfo += " ";
				uciInfo += move.toString();
			}
		}
		if (command.getCurrentMove() != null) {
			uciInfo += " currmove " + command.getCurrentMove().toString();
		}
		if (command.getCurrentMoveNumber() != null) {
			uciInfo += " currmovenumber " + command.getCurrentMoveNumber().toString();
		}
		if (command.getHash() != null) {
			uciInfo += " hashfull " + command.getHash().toString();
		}
		if (command.getNps() != null) {
			uciInfo += " nps " + command.getNps().toString();
		}
		if (command.getTime() != null) {
			uciInfo += " time " + command.getTime().toString();
		}
		if (command.getNodes() != null) {
			uciInfo += " nodes " + command.getNodes().toString();
		}
		if (command.getString() != null) {
			uciInfo += " string " + command.getString();
		}
		logger.info(uciInfo);
	}
	
	private void convertToGenericAndSendBestMove(int nativeBestMove) {
		GenericMove bestMove = Move.toGenericMove(nativeBestMove);
		logger.info(String.format("Best Move %s", bestMove));
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
	
	private long repopulateRootTransFromCacheIfItWasOverwritten(SearchResult result) {
		long cachedRootTrans = result.rootTrans;
		long tableRootTrans = hashMap.getTransposition(rootPosition.getHash());
		// The transposition in the table could have been overwritten during the search;
		// If it has been removed we should rewrite it using the best we have, i.e. the cached version.
		if (tableRootTrans == 0L) {
			if (cachedRootTrans != 0) {
				logger.info(String.format("rootTrans overwritten replacing with %s", Transposition.report(cachedRootTrans)));
				hashMap.putTransposition(rootPosition.getHash(), cachedRootTrans);
				tableRootTrans = cachedRootTrans;
			} else {
				logger.severe("repopulateRootTransFromCacheIfItWasOverwritten cache was null!");
			}
		}
		return tableRootTrans;
	}
	
	public void sendBestMoveCommand(SearchResult result) {
		if (ENABLE_LOGGING) {
			logger.info(String.format("Search %s", result.report()));
		}
		int pcBestMove = result.pv[0];
		long tableRootTrans = repopulateRootTransFromCacheIfItWasOverwritten(result);
		if (tableRootTrans == 0L) {
			// In extremely rare cases, if the Zobrist matches 0L, we can't use the Transposition,
			// in that case, return the PV move from the search result.
			convertToGenericAndSendBestMove(pcBestMove);
			return;
		}
		int transDepth = Transposition.getDepthSearchedInPly(tableRootTrans);
		if ((result.depth-1) > transDepth) {
			// If the Transposition doesn't tally with the search depth reported, suspect unspecified 
			// defect occurred, we should use the PV move.
			logger.info(String.format("rootTrans %s inconsistent with search depth %d, ignoring hash",
					Transposition.report(tableRootTrans), result.depth));
			convertToGenericAndSendBestMove(pcBestMove);
			return;
		}
		
		updateReferenceScoreWhenMateFound(tableRootTrans);
		
		// The search depth and the score to put in any created transpositions are derived from the root transposition
		logger.info(String.format("tableRootTrans %s", Transposition.report(tableRootTrans)));
		int transBestMove = Transposition.getBestMove(tableRootTrans);
		
		// Always check to reset the drawchecker
		rootPosition.performMove(transBestMove);
		int movesApplied = 1;
		resetDrawCheckerIfBestMoveIsAPawnMoveOrCapture(transBestMove);
		
		if (ENABLE_STORE_PV_IN_TRANS_TABLE) {
			boolean preservePvInHashTable = true;
			if (!Move.areEqualForBestKiller(pcBestMove, transBestMove)) {
				logger.warning(String.format("At root, pc best=%s != trans best=%s, will not preserve PV in hash...", 
						Move.toString(pcBestMove), Move.toString(transBestMove)));
				preservePvInHashTable = false;
			}
			if (preservePvInHashTable) {
				short theScore = Transposition.getScore(tableRootTrans);
				// Apply all the moves in the pv and check the resulting position is in the hash table
				byte i=0;
				// The new hash code is *following* the best move at the ply having been applied
				long new_hash = rootPosition.getHash();
				long trans = hashMap.getTransposition(new_hash);
				
				// Check we still have transposition entries for the PV move positions. If they are not present create them.
				// If they are different, leave it be (the Transposition could be based on a deeper search) and abort the checking.
				for (i=1; i < Math.min(result.pv.length, result.depth-1); i++) {
					int currMove = result.pv[i];
					if (currMove == Move.NULL_MOVE) break;
					if (trans != 0L && !Move.areEqualForBestKiller(currMove, Transposition.getBestMove(trans))) break;
					
					if (ENABLE_ASSERTS) {
						assert rootPosition.getTheBoard().isPlayableMove(currMove, rootPosition.isKingInCheck(), rootPosition.getCastling()):
							String.format("%s not playable after %s fen=%s", Move.toString(currMove), rootPosition.unwindMoveStack(), rootPosition.getFen());
					}
					if (trans == 0L) {
						byte depth = (byte)(transDepth-i);
						trans = hashMap.setTransposition(new_hash, trans, depth, theScore, Score.typeUnknown, currMove, rootPosition.getMoveNumber());
						if (ENABLE_LOGGING) {
							logger.info(String.format("At ply %d, hash table entry lost, regenerating with bestMove from pc=%s",
									i, Move.toString(currMove), Transposition.report(trans)));
						}
					}
					
					rootPosition.performMove(currMove);
					movesApplied += 1;
					new_hash = rootPosition.getHash();
					trans = hashMap.getTransposition(new_hash);
				}
			}
			while (movesApplied > 0) {
				rootPosition.unperformMove();
				movesApplied--;
			}
		}
		convertToGenericAndSendBestMove(transBestMove);
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
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new java.util.Date());
		try {
			fh = new FileHandler(timeStamp+"_eubos_uci_log.txt");
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
		logger.info(dump);
	}
}
