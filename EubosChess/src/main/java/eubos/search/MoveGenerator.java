package eubos.search;

import java.util.Iterator;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;
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

	protected void addCastlingMoves(LinkedList<GenericMove> ml) {
		// The side on move should not have previously castled
		if ( bm.hasCastled())
			return;
		// King should not have moved and be on its initial square
		King ownKing = bm.getKing(bm.getOnMove());
		if ( ownKing != null ) {
			if (ownKing.hasEverMoved() || !ownKing.isOnInitialSquare()) {
				return;
			}
		}
		// Check for castling king-side and queen side
		GenericMove ksc = bm.addKingSideCastle();
		if ( ksc != null )
			ml.add(ksc);
		GenericMove qsc = bm.addQueenSideCastle();
		if ( qsc != null )
			ml.add(qsc);
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