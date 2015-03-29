package eubos.board;

import java.util.Iterator;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.pieces.King;
import eubos.pieces.Piece;

public class MoveGenerator {

	protected BoardManager bm;

	public MoveGenerator( BoardManager bm ) {
		super();
		this.bm = bm;
	}

	protected void addCastlingMoves(LinkedList<GenericMove> ml, Piece.Colour colourOnMove) {
		// The side on move should not have previously castled
		if ( bm.hasCastled(colourOnMove))
			return;
		// King should not have moved and be on its initial square
		King ownKing = bm.getKing(colourOnMove);
		if ( ownKing != null ) {
			if (ownKing.hasEverMoved() || !ownKing.isOnInitialSquare()) {
				return;
			}
		}
		// Check for castling king-side and queen side
		GenericMove ksc = bm.addKingSideCastle(colourOnMove);
		if ( ksc != null )
			ml.add(ksc);
		GenericMove qsc = bm.addQueenSideCastle(colourOnMove);
		if ( qsc != null )
			ml.add(qsc);
	}

	protected boolean inCheck( Piece.Colour colourOnMove ) {
		// For each opposite colour piece, see if it currently attacks the king.
		boolean inCheck = false;
		King ownKing = bm.getKing(colourOnMove);
		if ( ownKing != null ) {
			Iterator<Piece> iterPotentialAttackers = bm.getTheBoard().iterateColour(Piece.Colour.getOpposite(colourOnMove));
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