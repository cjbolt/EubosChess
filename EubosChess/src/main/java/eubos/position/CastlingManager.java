package eubos.position;

import java.util.List;

import com.fluxchess.jcpi.models.IntChessman;
import com.fluxchess.jcpi.models.IntFile;

import eubos.board.Board;
import eubos.board.InvalidPieceException;
import eubos.board.Piece.Colour;
import eubos.board.Piece.PieceType;
import eubos.position.MoveList.MoveClassification;

class CastlingManager {
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

	static final int bksc = Move.valueOf(MoveClassification.CASTLE.ordinal(), Position.e8, Position.g8, IntChessman.NOCHESSMAN);
	static final int wksc = Move.valueOf(MoveClassification.CASTLE.ordinal(), Position.e1, Position.g1, IntChessman.NOCHESSMAN);
	static final int bqsc = Move.valueOf(MoveClassification.CASTLE.ordinal(), Position.e8, Position.c8, IntChessman.NOCHESSMAN);
	static final int wqsc = Move.valueOf(MoveClassification.CASTLE.ordinal(), Position.e1, Position.c1, IntChessman.NOCHESSMAN);

	static final int undo_bksc = Move.valueOf(MoveClassification.CASTLE.ordinal(), Position.g8, Position.e8, IntChessman.NOCHESSMAN);
	static final int undo_wksc = Move.valueOf(MoveClassification.CASTLE.ordinal(), Position.g1, Position.e1, IntChessman.NOCHESSMAN);
	static final int undo_bqsc = Move.valueOf(MoveClassification.CASTLE.ordinal(), Position.c8, Position.e8, IntChessman.NOCHESSMAN);
	static final int undo_wqsc = Move.valueOf(MoveClassification.CASTLE.ordinal(), Position.c1, Position.e1, IntChessman.NOCHESSMAN);

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
		String fenCastle = "";
		if (whiteKsAvail && !whiteCastled)
			fenCastle += "K";
		if (whiteQsAvail && !whiteCastled)
			fenCastle += "Q";
		if (blackKsAvail && !blackCastled)
			fenCastle += "k";
		if (blackQsAvail && !blackCastled)
			fenCastle += "q";
		if (fenCastle.isEmpty())
			fenCastle = "-";
		return fenCastle;
	}
	
	void setFenFlags(String fenCastle) {
		if (fenCastle.matches("[KQkq-]+")) {
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
			if (fenCastle.contains("-")) {
				whiteKsAvail = false;
				whiteQsAvail = false;
				blackKsAvail = false;
				blackQsAvail = false;
			}
		}
	}

	void performSecondaryCastlingMove(int move) throws InvalidPieceException {
		PieceType rookToCastle = PieceType.NONE;
		if (Move.areEqual(move, wksc)) {
			// Perform secondary white king side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( Position.h1 );
			pm.getTheBoard().setPieceAtSquare( Position.f1, rookToCastle );
		} else if (Move.areEqual(move, wqsc)) {
			// Perform secondary white queen side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( Position.a1 );
			pm.getTheBoard().setPieceAtSquare( Position.d1, rookToCastle );
		} else if (Move.areEqual(move, bksc)) {
			// Perform secondary black king side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( Position.h8 );
			pm.getTheBoard().setPieceAtSquare( Position.f8, rookToCastle );
		} else if (Move.areEqual(move, bqsc)) {
			// Perform secondary black queen side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( Position.a8 );
			pm.getTheBoard().setPieceAtSquare( Position.d8, rookToCastle );
		}
	}

	void unperformSecondaryCastlingMove(int move) throws InvalidPieceException {
		PieceType rookToCastle = PieceType.NONE;
		if (Move.areEqual(move, undo_wksc)) {
			// Perform secondary king side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( Position.f1 );
			pm.getTheBoard().setPieceAtSquare( Position.h1, rookToCastle );
		} else	if (Move.areEqual(move, undo_wqsc)) {
			// Perform secondary queen side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( Position.d1 );
			pm.getTheBoard().setPieceAtSquare( Position.a1, rookToCastle );
		} else if (Move.areEqual(move, undo_bksc)) {
			// Perform secondary king side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( Position.f8 );
			pm.getTheBoard().setPieceAtSquare( Position.h8, rookToCastle );
		} else if (Move.areEqual(move, undo_bqsc)) {
			// Perform secondary queen side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( Position.d8 );
			pm.getTheBoard().setPieceAtSquare( Position.a8, rookToCastle );
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
	
	boolean isWhiteKsAvail() {
		return whiteKsAvail;
	}

	boolean isWhiteQsAvail() {
		return whiteQsAvail;
	}

	boolean isBlackKsAvail() {
		return blackKsAvail;
	}

	boolean isBlackQsAvail() {
		return blackQsAvail;
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
		assert PieceType.isRook(theBoard.getPieceAtSquare(rookSq));
		
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

	public void updateFlags(PieceType movedPiece, int lastMove) {
		// First handle castling moves
		if (PieceType.isKing(movedPiece)) {
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
		if (blackQsAvail && (Move.getTargetPosition(lastMove) == Position.a8)) {
			blackQsAvail = false;
		} else if (blackKsAvail && (Move.getTargetPosition(lastMove) == Position.h8)) {
			blackKsAvail = false;
		} else if (whiteQsAvail && (Move.getTargetPosition(lastMove) == Position.a1)) {
			whiteQsAvail = false;
		} else if (whiteKsAvail && (Move.getTargetPosition(lastMove) == Position.h1)) {
			whiteKsAvail = false;
		}
		// King moved
		if (movedPiece.equals(PieceType.WhiteKing)) {
			whiteKsAvail = whiteQsAvail = false;
		} else if (movedPiece.equals(PieceType.BlackKing)) {
			blackKsAvail = blackQsAvail = false;
		// Rook moved	
		} else if (movedPiece.equals(PieceType.WhiteRook)) { 
			if (Position.getFile(Move.getOriginPosition(lastMove))==IntFile.Fa) {
				whiteQsAvail = false;
			} 
			if (Position.getFile(Move.getOriginPosition(lastMove))==IntFile.Fh) {
				whiteKsAvail = false;
			}
		} else if (movedPiece.equals(PieceType.BlackRook)) {
			if (Position.getFile(Move.getOriginPosition(lastMove))==IntFile.Fa) {
				blackQsAvail = false;
			} else if (Position.getFile(Move.getOriginPosition(lastMove))==IntFile.Fh) {
				blackKsAvail = false;
			}	
		}
	}
}
