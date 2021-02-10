package eubos.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;

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
import eubos.board.InvalidPieceException;
import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.board.SquareAttackEvaluator;
import eubos.position.Move;
import eubos.position.PositionManager;
import eubos.score.ReferenceScore;
import eubos.search.DrawChecker;
import eubos.search.searchers.AbstractMoveSearcher;
import eubos.search.searchers.FixedDepthMoveSearcher;
import eubos.search.searchers.FixedTimeMoveSearcher;
import eubos.search.searchers.MultithreadedIterativeMoveSearcher;
import eubos.search.transposition.FixedSizeTranspositionTable;

import java.text.SimpleDateFormat;
import java.util.logging.*;

public class EubosEngineMain extends AbstractEngine {
	
	public static final byte SEARCH_DEPTH_IN_PLY = 35;
	public static final int DEFAULT_NUM_SEARCH_THREADS = 2;
	
	public static final boolean LOGGING_ENABLED = true;
	public static final boolean UCI_INFO_ENABLED = true;
	public static final boolean ASSERTS_ENABLED = false;
	public static final boolean ENABLE_YIELD_IN_WORKER_THREADS = false;
	
	// Permanent data structures - static for the duration of a single game
	private FixedSizeTranspositionTable hashMap = null;
	DrawChecker dc;
	ReferenceScore whiteRefScore;
	ReferenceScore blackRefScore;
	
	// Temporary data structures - created and deleted at each analyse/find move instance
	private PositionManager rootPosition;
	private AbstractMoveSearcher ms;
	private Piece.Colour lastOnMove = null;
	String lastFen = null;
	private boolean createdHashTable = false;
	
	// Multithreading configuration
	public static int numberOfWorkerThreads;
	public static int numCores;
	public static int defaultNumberOfWorkerThreads;
	static {
		numCores = Runtime.getRuntime().availableProcessors();
		defaultNumberOfWorkerThreads = Math.max(numCores-2, 1);
		numberOfWorkerThreads = defaultNumberOfWorkerThreads;
	}
	
	// Hash configuration
	public static final int MIN_HASH_SIZE = 32;
	public static final int MAX_HASH_SIZE = 4*1000;
	public static final long DEFAULT_HASH_SIZE = FixedSizeTranspositionTable.MBYTES_DEFAULT_HASH_SIZE;
	public static long hashSize = DEFAULT_HASH_SIZE;

    public static Logger logger = Logger.getLogger("eubos.main");

	private static FileHandler fh; 
    
	public EubosEngineMain() { 
		super();
	}
	
	public EubosEngineMain( PipedWriter out) throws IOException {
		super(new BufferedReader(new PipedReader(out)), System.out);
		logger.setLevel(Level.INFO);
	}
	
	private void checkToCreateEnginePermanentDataStructures() {
		if (!createdHashTable) {
			hashMap = new FixedSizeTranspositionTable(hashSize, numberOfWorkerThreads);
			dc = new DrawChecker();
			whiteRefScore = new ReferenceScore(hashMap);
			blackRefScore = new ReferenceScore(hashMap);
			createdHashTable = true;
		}	
	}

	public void receive(EngineInitializeRequestCommand command) {
		logger.fine("Eubos Initialising");
		
		ProtocolInitializeAnswerCommand reply = new ProtocolInitializeAnswerCommand("Eubos 2.0","Chris Bolt");
		reply.addOption(Options.newHashOption((int)DEFAULT_HASH_SIZE, MIN_HASH_SIZE, MAX_HASH_SIZE));
		reply.addOption(new SpinnerOption("Threads", defaultNumberOfWorkerThreads, 1, numCores));
		logger.fine(String.format("Cores available=%d", numCores));
		this.getProtocol().send( reply );
		lastOnMove = null;
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
		String fen_to_use = getActualFenStringForPosition(command);
		rootPosition = new PositionManager(fen_to_use, dc, null);
		long hashCode = rootPosition.getHash();
		Piece.Colour nowOnMove = rootPosition.getOnMove();
		if (lastOnMove == null || (lastOnMove == nowOnMove && !fen_to_use.equals(lastFen))) {
			// Update the draw checker with the position following the opponents last move
			dc.incrementPositionReachedCount(hashCode);
		} else {
			/* Don't increment the position reached count, because it will have already been incremented 
			 * in the previous send move command (when Eubos is analysing both sides positions). */
			logger.fine("Not incrementing drawchecker reached count for initial position");
		}
		lastOnMove = nowOnMove;
		lastFen = fen_to_use;
		logger.info(String.format("positionReceived fen=%s hashCode=%d reachedCount=%d",
				fen_to_use, hashCode, dc.getPositionReachedCount(hashCode)));
	}

	private String getActualFenStringForPosition(EngineAnalyzeCommand command) {
		String uci_fen_string = command.board.toString();
		String fen_to_use = null;
		boolean lastMoveWasCaptureOrPawnMove = false;
		if (!command.moves.isEmpty()) {
			// This temporary pm is to ensure that the correct position is used to initialise the search 
			// context of the position evaluator, required when we get a position and move list to apply to it.
			rootPosition = new PositionManager(uci_fen_string, dc, null);
			try {
				for (GenericMove nextMove : command.moves) {
					int move = Move.toMove(nextMove, rootPosition.getTheBoard());
					rootPosition.performMove(move, false); // don't update draw checker or hash
					lastMoveWasCaptureOrPawnMove = Move.isCapture(move) || Move.isPawnMove(move);
				}
			} catch(InvalidPieceException e ) {
				System.err.println(String.format(
				    "Serious error: Eubos can't find a piece on the board whilst applying previous moves, at %s", e.getAtPosition()));
			}
			fen_to_use = rootPosition.getFen();

			if (lastMoveWasCaptureOrPawnMove) {
				// Pawn moves and captures are irreversible, so if needed, clear the draw checker
				dc.reset();
			}
		} else {
			fen_to_use = uci_fen_string;
		}
		return fen_to_use;
	}

	public void receive(EngineStartCalculatingCommand command) {
		// The move searcher will report the best move found via a callback to this object, 
		// this will occur when the tree search is concluded and the thread completes execution.
		moveSearcherFactory(command);
		ms.start();
	}
	
	private void moveSearcherFactory(EngineStartCalculatingCommand command) {
		// Update the Reference Score, used by the Search process, for the new root position
		ReferenceScore refScore = Colour.isWhite(rootPosition.getOnMove()) ? whiteRefScore : blackRefScore;
		// Parse clock time data
		boolean clockTimeValid = true;
		long clockTime = 0;
		long clockInc = 0;
		try {
			GenericColor side = rootPosition.onMoveIsWhite() ? GenericColor.WHITE : GenericColor.BLACK;
			clockTime = command.getClock(side);
			clockInc = command.getClockIncrement(side);
		} catch (NullPointerException e) {
			clockTimeValid = false;
		}
		// Create Move Searcher
		if (clockTimeValid) {
			logger.info("Search move, clock time " + clockTime);
			ms = new MultithreadedIterativeMoveSearcher(this, hashMap, lastFen, dc, clockTime, clockInc, numberOfWorkerThreads, refScore);
		}
		else if (command.getMoveTime() != null) {
			logger.info("Search move, fixed time " + command.getMoveTime());
			ms = new FixedTimeMoveSearcher(this, hashMap, lastFen, dc, command.getMoveTime());
		} else {
			// Analyse mode
			byte searchDepth = 0;
			if (command.getInfinite()) {
				// We shall use a timed search which, for all intents and purposes, is infinite.
			} else if (command.getDepth() != null) {
				searchDepth = (byte)((int)command.getDepth());
			}	
			if (searchDepth != 0) {
				logger.info(String.format("Search move, fixed depth %d", searchDepth));
				ms = new FixedDepthMoveSearcher(this, hashMap, lastFen, dc, searchDepth);
			} else {
				logger.info(String.format("Search move, infinite search, threads %d", numberOfWorkerThreads));
				ms = new MultithreadedIterativeMoveSearcher(this, hashMap, lastFen, dc, Long.MAX_VALUE, clockInc, numberOfWorkerThreads, refScore);
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
		if (UCI_INFO_ENABLED) {
			this.getProtocol().send(infoCommand);
			logInfo(infoCommand);
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
		logger.fine(uciInfo);
	}
	
	public void sendBestMoveCommand(ProtocolBestMoveCommand protocolBestMoveCommand) {
		try {
			this.getProtocol().send(protocolBestMoveCommand);
		} catch (NoProtocolException e) {
			// In unit tests carry on without the protocol being connected
		}
		if (protocolBestMoveCommand.bestMove != null) {
			try {
				int bestMove = Move.toMove(protocolBestMoveCommand.bestMove, rootPosition.getTheBoard(), Move.TYPE_REGULAR_NONE);
				// Apply the best move to update the DrawChecker state
				rootPosition.performMove(bestMove);
				boolean bestMoveWasCaptureOrPawnMove = Move.isCapture(bestMove) || Move.isPawnMove(bestMove);
				if (bestMoveWasCaptureOrPawnMove) {
					dc.reset();
					dc.incrementPositionReachedCount(rootPosition.getHash());
				}
				logger.info(String.format("BestMove=%s hashCode=%d positionReachedCount=%d",
						protocolBestMoveCommand.bestMove, rootPosition.getHash(), dc.getPositionReachedCount(rootPosition.getHash())));
			} catch (InvalidPieceException e) {
				logger.severe("Error in sendBestMoveCommand!");
			}
		} else {
			logger.severe("Best move is null!");
		}
	}
	
	@Override
	protected void quit() {
	}

	public static void main(String[] args) {
		if (LOGGING_ENABLED) {
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
		logger.setLevel(Level.ALL);
		logger.setUseParentHandlers(false);
	}
}
