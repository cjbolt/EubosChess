package eubos.board;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.pieces.Piece;
import eubos.pieces.Piece.Colour;

public class LegalMoveListGenerator {
	
	private BoardManager bm;
	
	public LegalMoveListGenerator( BoardManager bm ) {
		this.bm = bm;
	}
	
	public List<GenericMove> createMoveList() throws InvalidPieceException {
		List<GenericMove> entireMoveList = new ArrayList<GenericMove>();
		Colour onMove = bm.getOnMove();
		// For each piece of the "on Move" colour, add it's legal moves to the entire move list
		Iterator<Piece> iter_p = bm.getTheBoard().iterateColour(bm.getOnMove());
		while ( iter_p.hasNext() ) {
			Piece currPiece = iter_p.next();
			entireMoveList.addAll( currPiece.generateMoves( bm ));
		}
		bm.addCastlingMoves(entireMoveList);
		List<GenericMove> newMoveList = new ArrayList<GenericMove>();
		Iterator<GenericMove> iter_ml = entireMoveList.iterator();
		while ( iter_ml.hasNext() ) {
			GenericMove currMove = iter_ml.next();
			bm.performMove( currMove );
			// Scratch any moves resulting in the king being in check
			if (bm.isKingInCheck(onMove))
				iter_ml.remove();
			// Groom the movelist so that the moves expected to be best are searched first.
			// This is to get max benefit form alpha beta algorithm
			else if (bm.lastMoveWasCapture()) {
				newMoveList.add(0, currMove);
			} else {
				newMoveList.add(currMove);
			}
			bm.unperformMove();
		}
		entireMoveList = newMoveList;
		return entireMoveList;
	}
}
