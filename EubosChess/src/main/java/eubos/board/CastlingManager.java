package eubos.board;

import java.util.Iterator;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.pieces.King;
import eubos.pieces.Piece;
import eubos.pieces.Rook;
import eubos.pieces.Piece.Colour;

public 	class CastlingManager {
	private boolean whiteKsAvail = true;
	private boolean whiteQsAvail = true;
	private boolean blackKsAvail = true;
	private boolean blackQsAvail = true;
	private boolean whiteCastled = false;
	private boolean blackCastled = false;
	private BoardManager bm;
	
	private final GenericPosition [] kscWhiteCheckSqs = {GenericPosition.e1, GenericPosition.f1, GenericPosition.g1};
	private final GenericPosition [] kscBlackCheckSqs = {GenericPosition.e8, GenericPosition.f8, GenericPosition.g8};
	private final GenericPosition [] kscWhiteEmptySqs = {GenericPosition.f1, GenericPosition.g1};
	private final GenericPosition [] kscBlackEmptySqs = {GenericPosition.f8, GenericPosition.g8};
	
	private final GenericPosition [] qscWhiteCheckSqs = {GenericPosition.c1, GenericPosition.d1, GenericPosition.e1};
	private final GenericPosition [] qscBlackCheckSqs = {GenericPosition.c8, GenericPosition.d8, GenericPosition.e8};
	private final GenericPosition [] qscWhiteEmptySqs = {GenericPosition.c1, GenericPosition.d1, GenericPosition.b1};
	private final GenericPosition [] qscBlackEmptySqs = {GenericPosition.c8, GenericPosition.d8, GenericPosition.b8};
	
	public CastlingManager(BoardManager Bm) { bm = Bm; }
	
	public CastlingManager(BoardManager Bm, String fenCastle) {
		bm = Bm;
		whiteKsAvail = false;
		whiteQsAvail = false;
		blackKsAvail = false;
		blackQsAvail = false;
		if (fenCastle.matches("[KQkq-]")) {
			if (fenCastle.contains("K"))
				whiteKsAvail = true;
			if (fenCastle.contains("Q"))
				whiteQsAvail = true;
			if (fenCastle.contains("k"))
				blackKsAvail = true;
			if (fenCastle.contains("q"))
				blackQsAvail = true;				
		}
	}		
	
	public boolean canCastle(Colour colour) {
		if (colour == Colour.white && !whiteCastled) {
			return (whiteKsAvail || whiteQsAvail) ? true:false;
		}
		if ((colour == Colour.black && !blackCastled)) {
			return (blackKsAvail || blackQsAvail) ? true:false;
		}
		return false;
	}

	public boolean isKingsideAvail(Colour colour) {
		if (colour == Colour.white) {
			return whiteKsAvail;
		} else {
			return blackKsAvail;
		}
	}

	public void setKingsideAvail(Colour colour, boolean kingsideAvail) {
		if (colour == Colour.white) {
			whiteKsAvail = kingsideAvail;
		} else {
			blackKsAvail = kingsideAvail;
		}
	}

	public boolean isQueensideAvail(Colour colour) {
		if (colour == Colour.white) {
			return whiteQsAvail;
		} else {
			return blackQsAvail;
		}
	}

	public void setQueensideAvail(Colour colour, boolean queensideAvail) {
		if (colour == Colour.white) {
			whiteKsAvail = queensideAvail;
		} else {
			blackKsAvail = queensideAvail;
		}
	}
	
	public boolean canCastle(GenericPosition rookSq,
			GenericPosition [] checkSqs,
			GenericPosition [] emptySqs) {
		Board theBoard = bm.getTheBoard();
		// Target rook should not have moved and be on it initial square
		Piece rook = theBoard.getPieceAtSquare( rookSq );
		if ( !(rook instanceof Rook) || rook.hasEverMoved())
			return false;
		// All the intervening squares between King and Rook should be empty
		for ( GenericPosition emptySq : emptySqs ) {
			if ( !theBoard.squareIsEmpty(emptySq))
				return false;
		}
		Iterator<Piece> iterPotentialAttackers = theBoard.iterateColour(Piece.Colour.getOpposite(bm.getOnMove()));
		while (iterPotentialAttackers.hasNext()) {
			// None of the intervening squares between King and Rook should be attacked
			// the king cannot be in check at the start or end of the move
			Piece currPiece = iterPotentialAttackers.next();
			if (currPiece.attacks( bm, checkSqs)) {
				return false;
			}
		}
		return true;
	}
	
	private GenericMove getWhiteKingsideCastleMove() {
		if ( canCastle(GenericPosition.h1, kscWhiteCheckSqs, kscWhiteEmptySqs)) {
			return new GenericMove(GenericPosition.e1,GenericPosition.g1);
		} else return null;
	}
	
	private GenericMove getBlackKingsideCastleMove() {
		if ( canCastle(GenericPosition.h8, kscBlackCheckSqs, kscBlackEmptySqs)) {
			return new GenericMove(GenericPosition.e8,GenericPosition.g8);
		} else return null;
	}		
	
	public GenericMove getKingsideCastleMove() {
		if ( bm.getOnMove() == Colour.white ) {
			return getWhiteKingsideCastleMove();
		} else {
			return getBlackKingsideCastleMove();
		}
	}
	
	private GenericMove getWhiteQueensideCastleMove() {
		if ( canCastle(GenericPosition.a1, qscWhiteCheckSqs, qscWhiteEmptySqs)) {
			return new GenericMove(GenericPosition.e1,GenericPosition.c1);
		} else return null;
	}
	
	private GenericMove getBlackQueensideCastleMove() {
		if ( canCastle(GenericPosition.a8, qscBlackCheckSqs, qscBlackEmptySqs)) {
			return new GenericMove(GenericPosition.e8,GenericPosition.c8);
		} else return null;
	}		
	
	public GenericMove getQueensideCastleMove() {
		if ( bm.getOnMove() == Colour.white ) {
			return getWhiteQueensideCastleMove();
		} else {
			return getBlackQueensideCastleMove();
		}
	}
	
	public void addCastlingMoves(LinkedList<GenericMove> ml) {
		// The side on move should not have previously castled
		Colour onMove = bm.getOnMove();
		if ( !canCastle(onMove))
			return;
		// King should not have moved and be on its initial square
		King ownKing = bm.getKing(onMove);
		if ( ownKing != null ) {
			if (ownKing.hasEverMoved() || !ownKing.isOnInitialSquare()) {
				return;
			}
		}
		// Check for castling king-side and queen side
		GenericMove ksc = getKingsideCastleMove();
		if ( ksc != null )
			ml.add(ksc);
		GenericMove qsc = getQueensideCastleMove();
		if ( qsc != null )
			ml.add(qsc);
	}
}
