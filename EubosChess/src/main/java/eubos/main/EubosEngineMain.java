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
import eubos.board.pieces.Piece.Colour;
import eubos.position.PositionManager;
import eubos.search.FixedTimeMoveSearcher;
import eubos.search.IterativeMoveSearcher;
import eubos.search.FixedDepthMoveSearcher;
import eubos.search.AbstractMoveSearcher;
//import eubos.search.SearchDebugAgent;

public class EubosEngineMain extends AbstractEngine {
	
	private static final int SEARCH_DEPTH_IN_PLY = 12;
	
	private PositionManager pm;
	private AbstractMoveSearcher ms;
	private OpeningBook open = new OpeningBook();
	private GenericMove nextBookMove = null;
	
	public EubosEngineMain() { super(); }
	public EubosEngineMain( PipedWriter out) throws IOException {
		super(new BufferedReader(new PipedReader(out)), System.out);
	}

	public void receive(EngineInitializeRequestCommand command) {
		this.getProtocol().send( new ProtocolInitializeAnswerCommand("Eubos","Chris Bolt") );
		//SearchDebugAgent.open();
	}

	public void receive(EngineSetOptionCommand command) {
	}

	public void receive(EngineDebugCommand command) {
	}

	public void receive(EngineReadyRequestCommand command) {
		this.getProtocol().send( new ProtocolReadyAnswerCommand("") );
	}

	public void receive(EngineNewGameCommand command) {
	}

	public void receive(EngineAnalyzeCommand command) {
		// Import position received from GUI and apply any instructed moves.
		pm = new PositionManager(command.board.toString());
		try {
			for (GenericMove nextMove : command.moves) {
				pm.performMove( nextMove );
			}
		} catch(InvalidPieceException e ) {
			System.out.println( 
					"Serious error: Eubos can't find a piece on the board whilst applying previous moves, at "
							+e.getAtPosition().toString() );
		}
		// Check Opening Book
		if (command.moves != null && !command.moves.isEmpty()) {
			nextBookMove = open.getMove(command.moves);
		} else {
			nextBookMove = null;
		}
	}

	public void receive(EngineStartCalculatingCommand command) {
		if (nextBookMove == null) {
			// The move searcher will report the best move found via a callback to this object, 
			// this will occur when the tree search is concluded and the thread completes execution.
			long clockTime = 0;
			try {
				clockTime = command.getClock((pm.getOnMove() == Colour.white) ? GenericColor.WHITE : GenericColor.BLACK);
			} catch (NullPointerException e) {
				clockTime = 0;
			}
			if (clockTime != 0) {
				ms = new IterativeMoveSearcher(this, pm, pm, pm, clockTime);
			}
			else if (command.getMoveTime() != null) {
				ms = new FixedTimeMoveSearcher(this, pm, pm, pm, command.getMoveTime());
			} else {
				int searchDepth = SEARCH_DEPTH_IN_PLY;
				if (command.getInfinite()) {
	
				} else if (command.getDepth() != null) {
					searchDepth = command.getDepth();
				}
				ms = new FixedDepthMoveSearcher(this, pm, pm, pm, searchDepth);
			}
			ms.start();
		} else {
			sendBestMoveCommand(new ProtocolBestMoveCommand(nextBookMove, null));
		}
	}

	public void receive(EngineStopCalculatingCommand command) {
		// Request an early terminate of the move searcher.
		ms.halt();
	}

	public void receive(EnginePonderHitCommand command) {
	}
	
	public void sendInfoCommand(ProtocolInformationCommand infoCommand) {
		this.getProtocol().send(infoCommand);
	}
	
	public void sendBestMoveCommand(ProtocolBestMoveCommand protocolBestMoveCommand) {
		this.getProtocol().send(protocolBestMoveCommand);
	}
	
	@Override
	protected void quit() {
		//SearchDebugAgent.close();
	}

	public static void main(String[] args) {
		// start the Engine
		Thread EubosThread = new Thread( new EubosEngineMain() );
		EubosThread.start();
	}
}
