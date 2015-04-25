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
	
	private static final GenericPosition [] kscWhiteCheckSqs = {GenericPosition.e1, GenericPosition.f1, GenericPosition.g1};
	private static final GenericPosition [] kscBlackCheckSqs = {GenericPosition.e8, GenericPosition.f8, GenericPosition.g8};
	private static final GenericPosition [] kscWhiteEmptySqs = {GenericPosition.f1, GenericPosition.g1};
	private static final GenericPosition [] kscBlackEmptySqs = {GenericPosition.f8, GenericPosition.g8};
	
	private static final GenericPosition [] qscWhiteCheckSqs = {GenericPosition.c1, GenericPosition.d1, GenericPosition.e1};
	private static final GenericPosition [] qscBlackCheckSqs = {GenericPosition.c8, GenericPosition.d8, GenericPosition.e8};
	private static final GenericPosition [] qscWhiteEmptySqs = {GenericPosition.c1, GenericPosition.d1, GenericPosition.b1};
	private static final GenericPosition [] qscBlackEmptySqs = {GenericPosition.c8, GenericPosition.d8, GenericPosition.b8};
	
	private static final GenericMove bksc = new GenericMove( GenericPosition.e8, GenericPosition.g8);
	private static final GenericMove wksc = new GenericMove( GenericPosition.e1, GenericPosition.g1);
	private static final GenericMove bqsc = new GenericMove( GenericPosition.e8, GenericPosition.c8);
	private static final GenericMove wqsc = new GenericMove( GenericPosition.e1, GenericPosition.c1);
	
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
	
	public boolean castlingAvaillable(Colour colour) {
		boolean castlingAvaillable = false;
		if (colour == Colour.white && !whiteCastled) {
			if (whiteKsAvail || whiteQsAvail)
				castlingAvaillable = true;
		}
		if ((colour == Colour.black && !blackCastled)) {
			if (blackKsAvail || blackQsAvail)
				castlingAvaillable = true;
		}
		return castlingAvaillable;
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
	
	private boolean castleMoveLegal(GenericPosition rookSq,
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
	
	public boolean isCastlingMove(GenericMove move) {
		return (move.equals(bksc) || move.equals(wksc) || move.equals(bqsc) || move.equals(wqsc)) ? true : false;
	}
	
	private GenericMove getWhiteKingsideCastleMove() {
		return (castleMoveLegal(GenericPosition.h1, kscWhiteCheckSqs, kscWhiteEmptySqs)) ? wksc : null;
	}
	
	private GenericMove getBlackKingsideCastleMove() {
		return (castleMoveLegal(GenericPosition.h8, kscBlackCheckSqs, kscBlackEmptySqs)) ? bksc : null;
	}		
	
	private GenericMove getWhiteQueensideCastleMove() {
		return (castleMoveLegal(GenericPosition.a1, qscWhiteCheckSqs, qscWhiteEmptySqs)) ? wqsc : null;
	}
	
	private GenericMove getBlackQueensideCastleMove() {
		return (castleMoveLegal(GenericPosition.a8, qscBlackCheckSqs, qscBlackEmptySqs)) ? bqsc : null;
	}		
		
	public void addCastlingMoves(LinkedList<GenericMove> ml) {
		// The side on move should not have previously castled
		Colour onMove = bm.getOnMove();
		if ( !castlingAvaillable(onMove))
			return;
		// King should not have moved and be on its initial square
		King ownKing = bm.getKing(onMove);
		if ( ownKing != null ) {
			if (ownKing.hasEverMoved() || !ownKing.isOnInitialSquare()) {
				return;
			}
		}
		// Check for castling king-side
		GenericMove ksc = null;		
		if ( onMove == Colour.white ) {
			ksc = getWhiteKingsideCastleMove();
		} else {
			ksc = getBlackKingsideCastleMove();
		}
		if ( ksc != null )
			ml.add(ksc);
		// Check for castling queen side
		GenericMove qsc = null;
		if ( onMove == Colour.white ) {
			qsc = getWhiteQueensideCastleMove();
		} else {
			qsc = getBlackQueensideCastleMove();
		}
		if ( qsc != null )
			ml.add(qsc);
	}
}
