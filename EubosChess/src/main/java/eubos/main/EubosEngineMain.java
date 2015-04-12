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
import com.fluxchess.jcpi.commands.ProtocolInformationCommand;
import com.fluxchess.jcpi.commands.ProtocolInitializeAnswerCommand;
import com.fluxchess.jcpi.commands.ProtocolReadyAnswerCommand;
import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.models.*;

import eubos.board.*;

public class EubosEngineMain extends AbstractEngine {
	
	private static final int SEARCH_DEPTH_IN_PLY = 6;
	
	private BoardManager bm;

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
		bm = new BoardManager();
	}

	public void receive(EngineAnalyzeCommand command) {
		// Note: command contains the move list and can be interrogated to set up the engine.
		bm = new BoardManager();
		try {
			for ( GenericMove nextMove : command.moves ) {
				bm.performMove( nextMove );
			}
		} catch(InvalidPieceException e ) {
			System.out.println( "Serious error: Eubos can't find a piece on the board whilst applying previous moves, at " + e.getAtPosition().toString() );
		} 
	}

	public void receive(EngineStartCalculatingCommand command) {
		try {
			MiniMaxMoveGenerator mg = new MiniMaxMoveGenerator( this, bm, SEARCH_DEPTH_IN_PLY );
			GenericMove selectedMove = mg.findMove();
			bm.performMove(selectedMove);
			this.getProtocol().send( new ProtocolBestMoveCommand( selectedMove, null ));
		} catch( NoLegalMoveException e ) {
			System.out.println( "Eubos has run out of legal moves for side " + bm.getOnMove().toString() );
		} catch(InvalidPieceException e ) {
			System.out.println( "Serious error: Eubos can't find a piece on the board whilst searching findMove(), at " + e.getAtPosition().toString() );
		}
	}

	public void receive(EngineStopCalculatingCommand command) {
	}

	public void receive(EnginePonderHitCommand command) {
	}
	
	public void dispatchInfoMessage(ProtocolInformationCommand command) {
		this.getProtocol().send(command);
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
