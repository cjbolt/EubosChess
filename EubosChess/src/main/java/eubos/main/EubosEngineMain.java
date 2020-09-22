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
import com.fluxchess.jcpi.protocols.NoProtocolException;

import eubos.board.InvalidPieceException;
import eubos.board.Piece;
import eubos.position.Move;
import eubos.position.PositionManager;
import eubos.search.SearchDebugAgent;
import eubos.search.DrawChecker;
import eubos.search.searchers.AbstractMoveSearcher;
import eubos.search.searchers.FixedDepthMoveSearcher;
import eubos.search.searchers.FixedTimeMoveSearcher;
import eubos.search.searchers.IterativeMoveSearcher;
import eubos.search.transposition.FixedSizeTranspositionTable;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

public class EubosEngineMain extends AbstractEngine {
	
	private static final byte SEARCH_DEPTH_IN_PLY = 35;
	public static final boolean LOGGING_ENABLED = false;
	public static final boolean UCI_INFO_ENABLED = true;
	
	PositionManager pm;
	private AbstractMoveSearcher ms;
	private FixedSizeTranspositionTable hashMap = null;
	DrawChecker dc;
	
    public static Logger logger = Logger.getLogger("eubos.main");

	private static FileHandler fh; 
    Piece.Colour lastOnMove = null;
	
	public EubosEngineMain() { 
		super();
		hashMap = new FixedSizeTranspositionTable();
		dc = new DrawChecker();
	}
	public EubosEngineMain( PipedWriter out) throws IOException {
		super(new BufferedReader(new PipedReader(out)), System.out);
		hashMap = new FixedSizeTranspositionTable();
		dc = new DrawChecker();
		logger.setLevel(Level.INFO);
	}

	public void receive(EngineInitializeRequestCommand command) {
		logger.fine("Eubos Initialising");
		
		ProtocolInitializeAnswerCommand reply = new ProtocolInitializeAnswerCommand("Eubos 1.1.2","Chris Bolt");
		reply.addOption(Options.newHashOption((int)FixedSizeTranspositionTable.MBYTES_DEFAULT_HASH_SIZE, 32, 4*1000));
		this.getProtocol().send( reply );
		
		LocalDateTime dateTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH-mm-ss");
		SearchDebugAgent.setFileNameBaseString(dateTime.format(formatter));
		lastOnMove = null;
	}

	public void receive(EngineSetOptionCommand command) {
		logger.fine("SetOptionCommand is " +command.name);
		// If the GUI has configured the hash table size, reinitialise it at the correct size
		if (command.name.startsWith("Hash")) {
			hashMap = new FixedSizeTranspositionTable(Long.parseLong(command.value));
			logger.fine("MaxHashSizeInElements=" +hashMap.getHashMapMaxSize());
		}
	}

	public void receive(EngineDebugCommand command) {
	}

	public void receive(EngineReadyRequestCommand command) {
		this.getProtocol().send( new ProtocolReadyAnswerCommand("") );
	}

	public void receive(EngineNewGameCommand command) {
		logger.fine("New Game");
	}

	public void receive(EngineAnalyzeCommand command) {
		logAnalyse(command);
		createPositionFromAnalyseCommand(command);
	}
	
	void createPositionFromAnalyseCommand(EngineAnalyzeCommand command) {
		// This temporary pm is to ensure that the correct position is used to initialise the search 
		// context in the position evaluator, required when we get a position and move list to apply.
		String uci_fen_string = command.board.toString();
		String fen_to_use = null;
		boolean lastMoveWasCaptureOrPawnMove = false;
		if (!command.moves.isEmpty()) {
			PositionManager temp_pm = new PositionManager(uci_fen_string, dc);
			try {
				for (GenericMove nextMove : command.moves) {
					int move = Move.toMove(nextMove, temp_pm.getTheBoard());
					temp_pm.performMove(move);
					lastMoveWasCaptureOrPawnMove = temp_pm.getCapturedPiece().getPiece() != Piece.NONE || Piece.isPawn(Move.getOriginPiece(move));
				}
			} catch(InvalidPieceException e ) {
				System.out.println( 
						"Serious error: Eubos can't find a piece on the board whilst applying previous moves, at "
								+e.getAtPosition().toString());
			}
			// Assign the actual pm fen to use
			fen_to_use = temp_pm.getFen();
			// unwind the moves made to get the fen so that the draw checker position count is correct; we don't double count
			for (int i=0; i<command.moves.size(); i++) {
				try {
					temp_pm.unperformMove();
				} catch (InvalidPieceException e) {
					e.printStackTrace();
				}
			}
		} else {
			fen_to_use = uci_fen_string;
		}
		pm = new PositionManager(fen_to_use, dc);
		if (lastMoveWasCaptureOrPawnMove) {
			// Pawn moves and captures are irreversible, clear the draw checker
			dc.reset();
		}
		long hashCode = pm.getHash();
		Piece.Colour nowOnMove = pm.getOnMove();
		if (lastOnMove == null || lastOnMove == nowOnMove) {
			// Update the draw checker with the position following the opponents last move
			dc.incrementPositionReachedCount(hashCode);
		} else {
			/* Don't increment the position reached count, because it will have already been incremented 
			 * in the previous send move command (when Eubos is analysing both sides positions). */
			logger.fine("Not incrementing drawchecker reached count for initial position");
		}
		lastOnMove = nowOnMove;
		logger.info(String.format("positionReceived fen=%s hashCode=%d reachedCount=%d",
				fen_to_use, hashCode, dc.getPositionReachedCount(hashCode)));
	}
	
	private void logAnalyse(EngineAnalyzeCommand command) {
		logger.fine("Analysing position: " + command.board.toString() +
				    " with moves " + command.moves);
	}

	public void receive(EngineStartCalculatingCommand command) {
		// The move searcher will report the best move found via a callback to this object, 
		// this will occur when the tree search is concluded and the thread completes execution.
		moveSearcherFactory(command);
		ms.start();
	}
	
	private void moveSearcherFactory(EngineStartCalculatingCommand command) {
		boolean clockTimeValid = true;
		long clockTime = 0;
		long clockInc = 0;
		try {
			GenericColor side = pm.onMoveIsWhite() ? GenericColor.WHITE : GenericColor.BLACK;
			clockTime = command.getClock(side);
			clockInc = command.getClockIncrement(side);
		} catch (NullPointerException e) {
			clockTimeValid = false;
		}
		if (clockTimeValid) {
			logger.info("Search move, clock time " + clockTime);
			ms = new IterativeMoveSearcher(this, hashMap, pm, pm, clockTime, clockInc);
		}
		else if (command.getMoveTime() != null) {
			logger.info("Search move, fixed time " + command.getMoveTime());
			ms = new FixedTimeMoveSearcher(this, hashMap, pm, pm, command.getMoveTime());
		} else {
			byte searchDepth = SEARCH_DEPTH_IN_PLY;
			if (command.getInfinite()) {

			} else if (command.getDepth() != null) {
				searchDepth = (byte)((int)command.getDepth());
			}
			logger.info("Search move, fixed depth " + searchDepth);
			ms = new FixedDepthMoveSearcher(this, hashMap, pm, pm, searchDepth);
		}
	}

	public void receive(EngineStopCalculatingCommand command) {
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
				// Apply the best move to update the DrawChecker state
				pm.performMove(Move.toMove(protocolBestMoveCommand.bestMove, pm.getTheBoard(), Move.TYPE_NONE));
				logger.info(String.format("BestMove=%s hashCode=%d positionReachedCount=%d",
						protocolBestMoveCommand.bestMove, pm.getHash(), dc.getPositionReachedCount(pm.getHash())));
			} catch (InvalidPieceException e) {
				logger.severe("Error in sendBestMoveCommand!");
			}
		} else {
			logger.severe("Best move is null!");
		}
	}
	
	@Override
	protected void quit() {
		SearchDebugAgent.close();
	}

	public static void main(String[] args) {
		if (LOGGING_ENABLED) {
			logStart();
		} else {
			logger.setLevel(Level.OFF);
			logger.setUseParentHandlers(false);
		}
		logger.fine("Starting Eubos");
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
