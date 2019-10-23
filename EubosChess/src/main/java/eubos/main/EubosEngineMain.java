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

import eubos.board.InvalidPieceException;
import eubos.position.PositionManager;
import eubos.search.FixedSizeTranspositionTable;
import eubos.search.FixedTimeMoveSearcher;
import eubos.search.IterativeMoveSearcher;
import eubos.search.SearchDebugAgent;
import eubos.search.FixedDepthMoveSearcher;
import eubos.search.AbstractMoveSearcher;

import java.text.SimpleDateFormat;
import java.util.logging.*;

public class EubosEngineMain extends AbstractEngine {
	
	private static final byte SEARCH_DEPTH_IN_PLY = 25;
	
	private PositionManager pm;
	private AbstractMoveSearcher ms;
	private OpeningBook open = new OpeningBook();
	private GenericMove nextBookMove = null;
	private FixedSizeTranspositionTable hashMap = null;
	
    private static Logger logger = Logger.getLogger("eubos.main");
    private static FileHandler fh; 
	
	public EubosEngineMain() { 
		super();
		hashMap = new FixedSizeTranspositionTable();
		}
	public EubosEngineMain( PipedWriter out) throws IOException {
		super(new BufferedReader(new PipedReader(out)), System.out);
		hashMap = new FixedSizeTranspositionTable();
	}

	public void receive(EngineInitializeRequestCommand command) {
		logger.fine("Eubos Initialising");
		this.getProtocol().send( new ProtocolInitializeAnswerCommand("Eubos","Chris Bolt") );
		SearchDebugAgent.open();
	}

	public void receive(EngineSetOptionCommand command) {
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
		// Import position received from GUI and apply any instructed moves.
		logAnalyse(command);
		pm = new PositionManager(command.board.toString());
		try {
			for (GenericMove nextMove : command.moves) {
				pm.performMove(nextMove);
			}
		} catch(InvalidPieceException e ) {
			System.out.println( 
					"Serious error: Eubos can't find a piece on the board whilst applying previous moves, at "
							+e.getAtPosition().toString());
		}
		// Check Opening Book
		if (command.moves != null && !command.moves.isEmpty()) {
			nextBookMove = open.getMove(command.moves);
		} else {
			nextBookMove = null;
		}
	}
	
	private void logAnalyse(EngineAnalyzeCommand command) {
		logger.fine("Analysing position: " + command.board.toString() +
				    " with moves " + command.moves);
	}

	public void receive(EngineStartCalculatingCommand command) {
		if (nextBookMove == null) {
			// The move searcher will report the best move found via a callback to this object, 
			// this will occur when the tree search is concluded and the thread completes execution.
			moveSearcherFactory(command);
			ms.start();
		} else {
			sendBestMoveCommand(new ProtocolBestMoveCommand(nextBookMove, null));
		}
	}
	
	private void moveSearcherFactory(EngineStartCalculatingCommand command) {
		long clockTime = extractClockTime(command);
		if (clockTime != 0) {
			logger.info("Search move, clock time " + clockTime);
			ms = new IterativeMoveSearcher(this, hashMap, pm, pm, clockTime, pm.getPositionEvaluator());
		}
		else if (command.getMoveTime() != null) {
			logger.info("Search move, fixed time " + command.getMoveTime());
			ms = new FixedTimeMoveSearcher(this, hashMap, pm, pm, command.getMoveTime(), pm.getPositionEvaluator());
		} else {
			byte searchDepth = SEARCH_DEPTH_IN_PLY;
			if (command.getInfinite()) {

			} else if (command.getDepth() != null) {
				searchDepth = (byte)((int)command.getDepth());
			}
			logger.info("Search move, fixed depth " + searchDepth);
			ms = new FixedDepthMoveSearcher(this, hashMap, pm, pm, searchDepth, pm.getPositionEvaluator());
		}
	}
	
	private long extractClockTime(EngineStartCalculatingCommand command) {
		long clockTime = 0;
		try {
			clockTime = command.getClock((pm.onMoveIsWhite()) ? GenericColor.WHITE : GenericColor.BLACK);
		} catch (NullPointerException e) {
			clockTime = 0;
		}
		return clockTime;
	}

	public void receive(EngineStopCalculatingCommand command) {
		// Request an early terminate of the move searcher.
		ms.halt();
	}

	public void receive(EnginePonderHitCommand command) {
	}
	
	public void sendInfoCommand(ProtocolInformationCommand infoCommand) {
		this.getProtocol().send(infoCommand);
		logInfo(infoCommand);
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
	
	public void sendBestMoveCommand(ProtocolBestMoveCommand protocolBestMoveCommand) {
		this.getProtocol().send(protocolBestMoveCommand);
		logger.info("Best move " + protocolBestMoveCommand.bestMove);
		logger.info("Transposition Table Size " + hashMap.getHashMapSize());
	}
	
	@Override
	protected void quit() {
		SearchDebugAgent.close();
	}

	public static void main(String[] args) {
		logStart();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.addHandler(fh);
		logger.setLevel(Level.ALL);
		logger.setUseParentHandlers(false);
	}
}
