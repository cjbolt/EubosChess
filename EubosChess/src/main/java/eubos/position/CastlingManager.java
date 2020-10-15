package eubos.position;

import java.util.List;

import com.fluxchess.jcpi.models.IntChessman;
import com.fluxchess.jcpi.models.IntFile;

import eubos.board.Board;
import eubos.board.Piece;
import eubos.board.Piece.Colour;

public class CastlingManager {
	private boolean whiteKsAvail = true;
	private boolean whiteQsAvail = true;
	private boolean blackKsAvail = true;
	private boolean blackQsAvail = true;
	private boolean whiteCastled = false;
	private boolean blackCastled = false;
	private PositionManager pm;

	private static final int [] kscWhiteCheckSqs = {Position.e1, Position.f1, Position.g1};
	private static final int [] kscBlackCheckSqs = {Position.e8, Position.f8, Position.g8};
	private static final int [] kscWhiteEmptySqs = {Position.f1, Position.g1};
	private static final int [] kscBlackEmptySqs = {Position.f8, Position.g8};

	private static final int [] qscWhiteCheckSqs = {Position.c1, Position.d1, Position.e1};
	private static final int [] qscBlackCheckSqs = {Position.c8, Position.d8, Position.e8};
	private static final int [] qscWhiteEmptySqs = {Position.c1, Position.d1, Position.b1};
	private static final int [] qscBlackEmptySqs = {Position.c8, Position.d8, Position.b8};

	public static final int bksc = Move.valueOf(Move.TYPE_CASTLE_MASK, Position.e8, (Piece.BLACK | Piece.KING), Position.g8, Piece.NONE, IntChessman.NOCHESSMAN);
	public static final int wksc = Move.valueOf(Move.TYPE_CASTLE_MASK, Position.e1, Piece.KING, Position.g1, Piece.NONE, IntChessman.NOCHESSMAN);
	public static final int bqsc = Move.valueOf(Move.TYPE_CASTLE_MASK, Position.e8, (Piece.BLACK | Piece.KING), Position.c8, Piece.NONE, IntChessman.NOCHESSMAN);
	public static final int wqsc = Move.valueOf(Move.TYPE_CASTLE_MASK, Position.e1, Piece.KING, Position.c1, Piece.NONE, IntChessman.NOCHESSMAN);

	public static final int undo_bksc = Move.valueOf(Move.TYPE_CASTLE_MASK, Position.g8, (Piece.BLACK | Piece.KING), Position.e8, Piece.NONE, IntChessman.NOCHESSMAN);
	public static final int undo_wksc = Move.valueOf(Move.TYPE_CASTLE_MASK, Position.g1, Piece.KING, Position.e1, Piece.NONE, IntChessman.NOCHESSMAN);
	public static final int undo_bqsc = Move.valueOf(Move.TYPE_CASTLE_MASK, Position.c8, (Piece.BLACK | Piece.KING), Position.e8, Piece.NONE, IntChessman.NOCHESSMAN);
	public static final int undo_wqsc = Move.valueOf(Move.TYPE_CASTLE_MASK, Position.c1, Piece.KING, Position.e1, Piece.NONE, IntChessman.NOCHESSMAN);

	CastlingManager(PositionManager Pm) { this( Pm, "-"); }

	CastlingManager(PositionManager Pm, String fenCastle) {
		pm = Pm;
		whiteKsAvail = false;
		whiteQsAvail = false;
		blackKsAvail = false;
		blackQsAvail = false;
		setFenFlags(fenCastle);
	}
	
	String getFenFlags() {
		StringBuilder fenCastle = new StringBuilder();
		if (whiteKsAvail && !whiteCastled)
			fenCastle.append("K");
		if (whiteQsAvail && !whiteCastled)
			fenCastle.append("Q");
		if (blackKsAvail && !blackCastled)
			fenCastle.append("k");
		if (blackQsAvail && !blackCastled)
			fenCastle.append("q");
		if (fenCastle.length() == 0)
			fenCastle.append("-");
		return fenCastle.toString();
	}
	
	void setFenFlags(String fenCastle) {
		if (fenCastle.contains("-")) {
			whiteKsAvail = false;
			whiteQsAvail = false;
			blackKsAvail = false;
			blackQsAvail = false;
		} else {
			if (fenCastle.contains("K")) {
				whiteKsAvail = true;
				whiteCastled = false;
			}
			if (fenCastle.contains("Q")) {
				whiteQsAvail = true;
				whiteCastled = false;
			}
			if (fenCastle.contains("k")) {
				blackKsAvail = true;
				blackCastled = false;
			}
			if (fenCastle.contains("q")) {
				blackQsAvail = true;
				blackCastled = false;
			}
		}
	}

	int getFlags() {
		int castleMask = 0;
		castleMask |= (whiteKsAvail ? PositionManager.WHITE_KINGSIDE : 0);
		castleMask |= (whiteQsAvail ? PositionManager.WHITE_QUEENSIDE : 0);
		castleMask |= (blackKsAvail ? PositionManager.BLACK_KINGSIDE : 0);
		castleMask |= (blackQsAvail ? PositionManager.BLACK_QUEENSIDE : 0);
		return castleMask;
	}
	
	void setFlags(int flags) {
		if (flags == 0) {
			whiteKsAvail = false;
			whiteQsAvail = false;
			blackKsAvail = false;
			blackQsAvail = false;
		} else {
			if ((flags & PositionManager.WHITE_KINGSIDE)==PositionManager.WHITE_KINGSIDE) {
				whiteKsAvail = true;
				whiteCastled = false;
			}
			if ((flags & PositionManager.WHITE_QUEENSIDE)==PositionManager.WHITE_QUEENSIDE) {
				whiteQsAvail = true;
				whiteCastled = false;
			}
			if ((flags & PositionManager.BLACK_KINGSIDE)==PositionManager.BLACK_KINGSIDE) {
				blackKsAvail = true;
				blackCastled = false;
			}
			if ((flags & PositionManager.BLACK_QUEENSIDE)==PositionManager.BLACK_QUEENSIDE) {
				blackQsAvail = true;
				blackCastled = false;
			}
		}
	}

	void addCastlingMoves(List<Integer> ml) {
		// The side on move should not have previously castled
		Colour onMove = pm.getOnMove();
		if ( !castlingAvaillable(onMove))
			return;
		// Check for castling king-side
		int ksc = 0;		
		if (Colour.isWhite(onMove) && whiteKsAvail) {
			ksc = getWhiteKingsideCastleMove();
		} else if (Colour.isBlack(onMove) && blackKsAvail) {
			ksc = getBlackKingsideCastleMove();
		}
		if ( ksc != 0 )
			ml.add(ksc);
		// Check for castling queen side
		int qsc = 0;
		if (Colour.isWhite(onMove) && whiteQsAvail) {
			qsc = getWhiteQueensideCastleMove();
		} else if (Colour.isBlack(onMove) && blackQsAvail) {
			qsc = getBlackQueensideCastleMove();
		}
		if ( qsc != 0 )
			ml.add(qsc);
	}

	private boolean castlingAvaillable(Colour colour) {
		boolean castlingAvaillable = false;
		if (Colour.isWhite(colour) && (whiteKsAvail || whiteQsAvail)) {
			castlingAvaillable = true;
		} else if (Colour.isBlack(colour) && (blackKsAvail || blackQsAvail)) {
			castlingAvaillable = true;
		}
		return castlingAvaillable;
	}
	
	boolean everCastled(Colour colour){
		return Colour.isWhite(colour) ? whiteCastled : blackCastled;
	}

	private boolean castleMoveLegal(int rookSq,
			int [] checkSqs,
			int [] emptySqs) {
		Board theBoard = pm.getTheBoard();
		assert Piece.isRook(theBoard.getPieceAtSquare(rookSq));
		
		// All the intervening squares between King and Rook should be empty
		for ( int emptySq : emptySqs ) {
			if ( !theBoard.squareIsEmpty(emptySq))
				return false;
		}
		// King cannot move through an attacked square
		for (int sqToCheck: checkSqs) {
			if (theBoard.squareIsAttacked(sqToCheck, pm.getOnMove())) {
				return false;
			}
		}
		return true;
	}

	private int getWhiteKingsideCastleMove() {
		return (castleMoveLegal(Position.h1, kscWhiteCheckSqs, kscWhiteEmptySqs)) ? wksc : 0;
	}

	private int getBlackKingsideCastleMove() {
		return (castleMoveLegal(Position.h8, kscBlackCheckSqs, kscBlackEmptySqs)) ? bksc : 0;
	}

	private int getWhiteQueensideCastleMove() {
		return (castleMoveLegal(Position.a1, qscWhiteCheckSqs, qscWhiteEmptySqs)) ? wqsc : 0;
	}

	private int getBlackQueensideCastleMove() {
		return (castleMoveLegal(Position.a8, qscBlackCheckSqs, qscBlackEmptySqs)) ? bqsc : 0;
	}

	public void updateFlags(int lastMove) {
		int movedPiece = Move.getOriginPiece(lastMove);
		int targetPosition = Move.getTargetPosition(lastMove);
		int originPosition = Move.getOriginPosition(lastMove);
		// First handle castling moves
		if (Piece.isKing(movedPiece)) {
			if (Move.areEqual(lastMove,wksc) || Move.areEqual(lastMove,wqsc)) {
				whiteKsAvail = whiteQsAvail = false;
				whiteCastled = true;
			} else if (Move.areEqual(lastMove,bksc) || Move.areEqual(lastMove,bqsc)) {
				blackKsAvail = blackQsAvail = false;
				blackCastled = true;
			} 
		}
		// After this, the move wasn't castling, but may have caused castling to be no longer possible
		// A rook got captured
		if (blackQsAvail && (targetPosition == Position.a8)) {
			blackQsAvail = false;
		} else if (blackKsAvail && (targetPosition == Position.h8)) {
			blackKsAvail = false;
		} else if (whiteQsAvail && (targetPosition == Position.a1)) {
			whiteQsAvail = false;
		} else if (whiteKsAvail && (targetPosition == Position.h1)) {
			whiteKsAvail = false;
		}
		// King moved
		if (movedPiece == Piece.WHITE_KING) {
			whiteKsAvail = whiteQsAvail = false;
		} else if (movedPiece == Piece.BLACK_KING) {
			blackKsAvail = blackQsAvail = false;
		// Rook moved	
		} else if (movedPiece == Piece.WHITE_ROOK) { 
			if (Position.getFile(originPosition)==IntFile.Fa) {
				whiteQsAvail = false;
			} 
			if (Position.getFile(originPosition)==IntFile.Fh) {
				whiteKsAvail = false;
			}
		} else if (movedPiece == Piece.BLACK_ROOK) {
			if (Position.getFile(originPosition)==IntFile.Fa) {
				blackQsAvail = false;
			} else if (Position.getFile(originPosition)==IntFile.Fh) {
				blackKsAvail = false;
			}	
		}
	}
}
