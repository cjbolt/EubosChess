package eubos.search;

import java.util.Iterator;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.BoardManager;
import eubos.pieces.King;
import eubos.pieces.Piece;

public class MoveGenerator {

	protected BoardManager bm;

	public MoveGenerator( BoardManager bm ) {
		super();
		this.bm = bm;
	}

	protected boolean inCheck(King ownKing) {
		// For each opposite colour piece, see if it currently attacks the king.
		// N.b. the onMove colour has been advanced when the move was performed!
		boolean inCheck = false;
		if ( ownKing != null ) {
			Iterator<Piece> iterPotentialAttackers = bm.getTheBoard().iterateColour(Piece.Colour.getOpposite(ownKing.getColour()));
			while (iterPotentialAttackers.hasNext()) {
				Piece currPiece = iterPotentialAttackers.next();
				GenericPosition [] pos = { ownKing.getSquare() };
				if (currPiece.attacks( bm, pos )) {
					inCheck = true;
					break;
				}
			}
		}
		return inCheck;
	}

}