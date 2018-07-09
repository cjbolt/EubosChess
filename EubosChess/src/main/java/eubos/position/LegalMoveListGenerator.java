package eubos.position;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.pieces.Piece;
import eubos.board.pieces.Piece.Colour;

class LegalMoveListGenerator {
	
	private PositionManager pm;
	
	LegalMoveListGenerator( PositionManager pm ) {
		this.pm = pm;
	}
	
	List<GenericMove> createMoveList() throws InvalidPieceException {
		List<GenericMove> entireMoveList = new ArrayList<GenericMove>();
		Colour onMove = pm.getOnMove();
		// For each piece of the "on Move" colour, add it's legal moves to the entire move list
		Iterator<Piece> iter_p = pm.getTheBoard().iterateColour(pm.getOnMove());
		while ( iter_p.hasNext() ) {
			Piece currPiece = iter_p.next();
			entireMoveList.addAll( currPiece.generateMoves( pm.getTheBoard() ));
		}
		pm.castling.addCastlingMoves(entireMoveList);
		List<GenericMove> newMoveList = new ArrayList<GenericMove>();
		Iterator<GenericMove> iter_ml = entireMoveList.iterator();
		while ( iter_ml.hasNext() ) {
			GenericMove currMove = iter_ml.next();
			pm.performMove( currMove );
			// Scratch any moves resulting in the king being in check
			if (pm.isKingInCheck(onMove))
				iter_ml.remove();
			// Groom the movelist so that the moves expected to be best are searched first.
			// This is to get max benefit form alpha beta algorithm
			else if (pm.lastMoveWasCaptureOrCastle()) {
				newMoveList.add(0, currMove);
			} else {
				newMoveList.add(currMove);
			}
			pm.unperformMove();
		}
		entireMoveList = newMoveList;
		return entireMoveList;
	}
}
