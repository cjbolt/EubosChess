package eubos.board;

import java.util.Iterator;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.pieces.King;
import eubos.pieces.Piece;

public class MoveGenerator {

	protected BoardManager bm;
	protected Piece.Colour onMove;

	public MoveGenerator( BoardManager bm, Piece.Colour sideToMove ) {
		super();
		this.bm = bm;
		this.onMove = sideToMove;
	}

	protected void addCastlingMoves(LinkedList<GenericMove> ml) {
		// The side on move should not have previously castled
		if ( bm.hasCastled(onMove))
			return;
		// King should not have moved and be on its initial square
		King ownKing = bm.getKing(onMove);
		if ( ownKing != null ) {
			if (ownKing.hasEverMoved() || !ownKing.isOnInitialSquare()) {
				return;
			}
		}
		// Check for castling king-side and queen side
		GenericMove ksc = bm.addKingSideCastle(onMove);
		if ( ksc != null )
			ml.add(ksc);
		GenericMove qsc = bm.addQueenSideCastle(onMove);
		if ( qsc != null )
			ml.add(qsc);
	}

	protected boolean inCheck() {
		// For each opposite colour piece, see if it currently attacks the king.
		boolean inCheck = false;
		King ownKing = bm.getKing(onMove);
		if ( ownKing != null ) {
			Iterator<Piece> iterPotentialAttackers = bm.getTheBoard().iterateColour(Piece.Colour.getOpposite(onMove));
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