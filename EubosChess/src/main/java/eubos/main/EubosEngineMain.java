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

import eubos.board.BoardManager;
import eubos.board.InvalidPieceException;
import eubos.search.MoveSearcher;

public class EubosEngineMain extends AbstractEngine {
	
	private BoardManager bm;
	private MoveSearcher ms;
	
	public EubosEngineMain() { super(); }
	public EubosEngineMain( PipedWriter out) throws IOException {
		super(new BufferedReader(new PipedReader(out)), System.out);
	}

	public void receive(EngineInitializeRequestCommand command) {
		this.getProtocol().send( new ProtocolInitializeAnswerCommand("Eubos","Chris Bolt") );
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
		bm = new BoardManager(command.board.toString());
		try {
			for (GenericMove nextMove : command.moves) {
				bm.performMove( nextMove );
			}
		} catch(InvalidPieceException e ) {
			System.out.println( 
					"Serious error: Eubos can't find a piece on the board whilst applying previous moves, at "
							+e.getAtPosition().toString() );
		} 
	}

	public void receive(EngineStartCalculatingCommand command) {
		// The move searcher will report the best move found via a callback to this object, 
		// this will occur when the tree search is concluded and the thread completes execution.
		ms = new MoveSearcher(this, bm);
		ms.start();
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
	}

	public static void main(String[] args) {
		// start the Engine
		Thread EubosThread = new Thread( new EubosEngineMain() );
		EubosThread.start();
	}
}
