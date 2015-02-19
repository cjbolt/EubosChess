package eubos.main;

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

import com.fluxchess.jcpi.commands.ProtocolInitializeAnswerCommand;
import com.fluxchess.jcpi.commands.ProtocolReadyAnswerCommand;
import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.models.*;

import eubos.board.*;

public class EubosEngineMain extends AbstractEngine {
	
	private Board theChessBoard;

	public void receive(EngineInitializeRequestCommand command) {
		this.getProtocol().send( new ProtocolInitializeAnswerCommand("Eubos","Chris Bolt") );
	}

	public void receive(EngineSetOptionCommand command) {
		//System.out.println("receive(EngineSetOptionCommand): Eubos Chess Engine.");
	}

	public void receive(EngineDebugCommand command) {
		//System.out.println("receive(EngineDebugCommand): Eubos Chess Engine.");
	}

	public void receive(EngineReadyRequestCommand command) {
		this.getProtocol().send( new ProtocolReadyAnswerCommand("") );
	}

	public void receive(EngineNewGameCommand command) {
		//System.out.println("receive(EngineNewGameCommand): Eubos Chess Engine.");
		theChessBoard = new Board();
	}

	public void receive(EngineAnalyzeCommand command) {
		// Note: command contains the move list and can be interrogated to set up the engine.
	}

	public void receive(EngineStartCalculatingCommand command) {
		try {
			GenericMove selectedMove = theChessBoard.findBestMove();
			theChessBoard.update(selectedMove);
			this.getProtocol().send( new ProtocolBestMoveCommand( selectedMove, null ));
		} catch( IllegalNotationException e ) {
			System.out.println( "whoops:" + e.toString() );
		}
	}

	public void receive(EngineStopCalculatingCommand command) {
	}

	public void receive(EnginePonderHitCommand command) {
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
