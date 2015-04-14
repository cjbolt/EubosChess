package eubos.search;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.BoardManager;
import eubos.board.InvalidPieceException;
import eubos.main.EubosEngineMain;

public class MoveSearcher extends Thread {
	
	private static final int SEARCH_DEPTH_IN_PLY = 6;
	private EubosEngineMain eubosEngine;
	private BoardManager bm;
	private MiniMaxMoveGenerator mg;
	
	public MoveSearcher( EubosEngineMain eubos, BoardManager inputBm ) {
		eubosEngine = eubos;
		bm = inputBm;
		mg = new MiniMaxMoveGenerator( eubosEngine, bm, SEARCH_DEPTH_IN_PLY );
	}
	
	public void halt() {
		mg.terminateFindMove();
	}
	
	public void run() {
		try {
			GenericMove selectedMove = mg.findMove();
			bm.performMove(selectedMove);
			eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( selectedMove, null ));
		} catch( NoLegalMoveException e ) {
			System.out.println( "Eubos has run out of legal moves for side " + bm.getOnMove().toString() );
		} catch(InvalidPieceException e ) {
			System.out.println( 
					"Serious error: Eubos can't find a piece on the board whilst searching findMove(), at "
							+ e.getAtPosition().toString() );
		}
	}
}
