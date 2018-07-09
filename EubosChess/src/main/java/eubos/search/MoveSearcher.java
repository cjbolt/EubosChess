package eubos.search;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.models.GenericMove;

import eubos.main.EubosEngineMain;
import eubos.position.InvalidPieceException;
import eubos.position.PositionManager;

public class MoveSearcher extends Thread {
	
	private EubosEngineMain eubosEngine;
	private PositionManager pm;
	private MiniMaxMoveGenerator mg;
	
	public MoveSearcher( EubosEngineMain eubos, PositionManager inputPm, int searchDepth ) {
		eubosEngine = eubos;
		pm = inputPm;
		mg = new MiniMaxMoveGenerator( eubosEngine, pm, searchDepth );
	}
	
	public void halt() {
		mg.terminateFindMove();
	}
	
	public void run() {
		try {
			GenericMove selectedMove = mg.findMove();
			pm.performMove(selectedMove);
			eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( selectedMove, null ));
		} catch( NoLegalMoveException e ) {
			System.out.println( "Eubos has run out of legal moves for side " + pm.getOnMove().toString() );
		} catch(InvalidPieceException e ) {
			System.out.println( 
					"Serious error: Eubos can't find a piece on the board whilst searching findMove(), at "
							+ e.getAtPosition().toString() );
		}
	}
}
