package eubos.position;

import java.util.List;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

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

	private static final GenericPosition [] kscWhiteCheckSqs = {GenericPosition.e1, GenericPosition.f1, GenericPosition.g1};
	private static final GenericPosition [] kscBlackCheckSqs = {GenericPosition.e8, GenericPosition.f8, GenericPosition.g8};
	private static final GenericPosition [] kscWhiteEmptySqs = {GenericPosition.f1, GenericPosition.g1};
	private static final GenericPosition [] kscBlackEmptySqs = {GenericPosition.f8, GenericPosition.g8};

	private static final GenericPosition [] qscWhiteCheckSqs = {GenericPosition.c1, GenericPosition.d1, GenericPosition.e1};
	private static final GenericPosition [] qscBlackCheckSqs = {GenericPosition.c8, GenericPosition.d8, GenericPosition.e8};
	private static final GenericPosition [] qscWhiteEmptySqs = {GenericPosition.c1, GenericPosition.d1, GenericPosition.b1};
	private static final GenericPosition [] qscBlackEmptySqs = {GenericPosition.c8, GenericPosition.d8, GenericPosition.b8};

	static final int bksc = Move.toMove(new GenericMove( GenericPosition.e8, GenericPosition.g8), MoveClassification.CASTLE);
	static final int wksc = Move.toMove(new GenericMove( GenericPosition.e1, GenericPosition.g1), MoveClassification.CASTLE);
	static final int bqsc = Move.toMove(new GenericMove( GenericPosition.e8, GenericPosition.c8), MoveClassification.CASTLE);
	static final int wqsc = Move.toMove(new GenericMove( GenericPosition.e1, GenericPosition.c1), MoveClassification.CASTLE);

	static final int undo_bksc = Move.toMove(new GenericMove( GenericPosition.g8, GenericPosition.e8), MoveClassification.CASTLE);
	static final int undo_wksc = Move.toMove(new GenericMove( GenericPosition.g1, GenericPosition.e1), MoveClassification.CASTLE);
	static final int undo_bqsc = Move.toMove(new GenericMove( GenericPosition.c8, GenericPosition.e8), MoveClassification.CASTLE);
	static final int undo_wqsc = Move.toMove(new GenericMove( GenericPosition.c1, GenericPosition.e1), MoveClassification.CASTLE);

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
		if (move == wksc) {
			// Perform secondary white king side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( Position.h1 );
			pm.getTheBoard().setPieceAtSquare( Position.f1, rookToCastle );
		} else if (move == wqsc) {
			// Perform secondary white queen side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( Position.a1 );
			pm.getTheBoard().setPieceAtSquare( Position.d1, rookToCastle );
		} else if (move == bksc) {
			// Perform secondary black king side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( Position.h8 );
			pm.getTheBoard().setPieceAtSquare( Position.f8, rookToCastle );
		} else if (move == bqsc) {
			// Perform secondary black queen side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( Position.a8 );
			pm.getTheBoard().setPieceAtSquare( Position.d8, rookToCastle );
		}
	}

	void unperformSecondaryCastlingMove(int move) throws InvalidPieceException {
		PieceType rookToCastle = PieceType.NONE;
		if (move==undo_wksc) {
			// Perform secondary king side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( Position.f1 );
			pm.getTheBoard().setPieceAtSquare( Position.h1, rookToCastle );
		} else	if (move==undo_wqsc) {
			// Perform secondary queen side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( Position.d1 );
			pm.getTheBoard().setPieceAtSquare( Position.a1, rookToCastle );
		} else if (move==undo_bksc) {
			// Perform secondary king side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( Position.f8 );
			pm.getTheBoard().setPieceAtSquare( Position.h8, rookToCastle );
		} else if (move==undo_bqsc) {
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
			try {
				ksc = getWhiteKingsideCastleMove();
			} catch (Exception e) {
				whiteKsAvail = false;				
				e.printStackTrace();
			}
		} else if (Colour.isBlack(onMove) && blackKsAvail) {
			try {
				ksc = getBlackKingsideCastleMove();
			} catch (Exception e) {
				blackKsAvail = false;
				e.printStackTrace();
			}
		}
		if ( ksc != 0 )
			ml.add(ksc);
		// Check for castling queen side
		int qsc = 0;
		if (Colour.isWhite(onMove) && whiteQsAvail) {
			try {
				qsc = getWhiteQueensideCastleMove();
			} catch (Exception e) {
				whiteQsAvail = false;
				e.printStackTrace();
			}
		} else if (Colour.isBlack(onMove) && blackQsAvail) {
			try {
				qsc = getBlackQueensideCastleMove();
			} catch (Exception e) {
				blackQsAvail = false;
				e.printStackTrace();
			}
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

	private boolean castleMoveLegal(GenericPosition rookSq,
			GenericPosition [] checkSqs,
			GenericPosition [] emptySqs) throws Exception {
		Board theBoard = pm.getTheBoard();
		// Safeguard that the piece on the rook square is a rook, n.b. this shouldn't be needed
		PieceType theRook = theBoard.getPieceAtSquare(Position.valueOf(rookSq));
		if (theRook==PieceType.NONE)
		{
			throw new Exception("There was no piece on the castle rook square! This means castling flags are inconsistent with the position.");
		}
		if (!(PieceType.isRook(theRook)))
		{
			throw new Exception("The piece wasn't a Rook! This means castling flags are inconsistent with the position.");
		}
		// All the intervening squares between King and Rook should be empty
		for ( GenericPosition emptySq : emptySqs ) {
			if ( !theBoard.squareIsEmpty(Position.valueOf(emptySq)))
				return false;
		}
		// King cannot move through an attacked square
		for (GenericPosition sqToCheck: checkSqs) {
			if (theBoard.squareIsAttacked(Position.valueOf(sqToCheck), pm.getOnMove())) {
				return false;
			}
		}
		return true;
	}

	private int getWhiteKingsideCastleMove() throws Exception {
		return (castleMoveLegal(GenericPosition.h1, kscWhiteCheckSqs, kscWhiteEmptySqs)) ? wksc : 0;
	}

	private int getBlackKingsideCastleMove() throws Exception {
		return (castleMoveLegal(GenericPosition.h8, kscBlackCheckSqs, kscBlackEmptySqs)) ? bksc : 0;
	}

	private int getWhiteQueensideCastleMove() throws Exception {
		return (castleMoveLegal(GenericPosition.a1, qscWhiteCheckSqs, qscWhiteEmptySqs)) ? wqsc : 0;
	}

	private int getBlackQueensideCastleMove() throws Exception {
		return (castleMoveLegal(GenericPosition.a8, qscBlackCheckSqs, qscBlackEmptySqs)) ? bqsc : 0;
	}

	public void updateFlags(PieceType movedPiece, int lastMove) {
		// First handle castling moves
		if (PieceType.isKing(movedPiece)) {
			if (lastMove == wksc || lastMove == wqsc) {
				whiteKsAvail = whiteQsAvail = false;
				whiteCastled = true;
			} else if (lastMove==bksc || lastMove==bqsc) {
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
			if (Move.toGenericMove(lastMove).from.file.equals(GenericFile.Fa)) {
				whiteQsAvail = false;
			} 
			if (Move.toGenericMove(lastMove).from.file.equals(GenericFile.Fh)) {
				whiteKsAvail = false;
			}
		} else if (movedPiece.equals(PieceType.BlackRook)) {
			if (Move.toGenericMove(lastMove).from.file.equals(GenericFile.Fa)) {
				blackQsAvail = false;
			} else if (Move.toGenericMove(lastMove).from.file.equals(GenericFile.Fh)) {
				blackKsAvail = false;
			}	
		}
	}
}
