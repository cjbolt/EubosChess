package eubos.position;

import java.util.List;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.InvalidPieceException;
import eubos.board.pieces.Piece.Colour;
import eubos.board.pieces.Piece.PieceType;

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

	static final GenericMove bksc = new GenericMove( GenericPosition.e8, GenericPosition.g8);
	static final GenericMove wksc = new GenericMove( GenericPosition.e1, GenericPosition.g1);
	static final GenericMove bqsc = new GenericMove( GenericPosition.e8, GenericPosition.c8);
	static final GenericMove wqsc = new GenericMove( GenericPosition.e1, GenericPosition.c1);

	static final GenericMove undo_bksc = new GenericMove( GenericPosition.g8, GenericPosition.e8);
	static final GenericMove undo_wksc = new GenericMove( GenericPosition.g1, GenericPosition.e1);
	static final GenericMove undo_bqsc = new GenericMove( GenericPosition.c8, GenericPosition.e8);
	static final GenericMove undo_wqsc = new GenericMove( GenericPosition.c1, GenericPosition.e1);

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

	void performSecondaryCastlingMove(GenericMove move) throws InvalidPieceException {
		PieceType rookToCastle = PieceType.NONE;
		if (move.equals(wksc)) {
			// Perform secondary white king side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( GenericPosition.h1 );
			pm.getTheBoard().setPieceAtSquare( GenericPosition.f1, rookToCastle );
		} else if (move.equals(wqsc)) {
			// Perform secondary white queen side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( GenericPosition.a1 );
			pm.getTheBoard().setPieceAtSquare( GenericPosition.d1, rookToCastle );
		} else if (move.equals(bksc)) {
			// Perform secondary black king side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( GenericPosition.h8 );
			pm.getTheBoard().setPieceAtSquare( GenericPosition.f8, rookToCastle );
		} else if (move.equals(bqsc)) {
			// Perform secondary black queen side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( GenericPosition.a8 );
			pm.getTheBoard().setPieceAtSquare( GenericPosition.d8, rookToCastle );
		}
	}

	void unperformSecondaryCastlingMove(GenericMove move) throws InvalidPieceException {
		PieceType rookToCastle = PieceType.NONE;
		if (move.equals(undo_wksc)) {
			// Perform secondary king side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( GenericPosition.f1 );
			pm.getTheBoard().setPieceAtSquare( GenericPosition.h1, rookToCastle );
		} else	if (move.equals(undo_wqsc)) {
			// Perform secondary queen side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( GenericPosition.d1 );
			pm.getTheBoard().setPieceAtSquare( GenericPosition.a1, rookToCastle );
		} else if (move.equals(undo_bksc)) {
			// Perform secondary king side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( GenericPosition.f8 );
			pm.getTheBoard().setPieceAtSquare( GenericPosition.h8, rookToCastle );
		} else if (move.equals(undo_bqsc)) {
			// Perform secondary queen side castle rook move
			rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( GenericPosition.d8 );
			pm.getTheBoard().setPieceAtSquare( GenericPosition.a8, rookToCastle );
		}
	}

	void addCastlingMoves(List<GenericMove> ml) {
		// The side on move should not have previously castled
		Colour onMove = pm.getOnMove();
		if ( !castlingAvaillable(onMove))
			return;
		// Check for castling king-side
		GenericMove ksc = null;		
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
		if ( ksc != null )
			ml.add(ksc);
		// Check for castling queen side
		GenericMove qsc = null;
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
		if ( qsc != null )
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
		PieceType theRook = theBoard.getPieceAtSquare(rookSq);
		if (theRook==PieceType.NONE)
		{
			throw new Exception("There was no piece on the castle rook square! This means castling flags are inconsistent with the position.");
		}
		if (!(theRook==PieceType.WhiteRook || theRook==PieceType.BlackRook))
		{
			throw new Exception("The piece wasn't a Rook! This means castling flags are inconsistent with the position.");
		}
		// All the intervening squares between King and Rook should be empty
		for ( GenericPosition emptySq : emptySqs ) {
			if ( !theBoard.squareIsEmpty(emptySq))
				return false;
		}
		// King cannot move through an attacked square
		for (GenericPosition sqToCheck: checkSqs) {
			if (theBoard.squareIsAttacked(sqToCheck, pm.getOnMove())) {
				return false;
			}
		}
		return true;
	}

	private GenericMove getWhiteKingsideCastleMove() throws Exception {
		return (castleMoveLegal(GenericPosition.h1, kscWhiteCheckSqs, kscWhiteEmptySqs)) ? wksc : null;
	}

	private GenericMove getBlackKingsideCastleMove() throws Exception {
		return (castleMoveLegal(GenericPosition.h8, kscBlackCheckSqs, kscBlackEmptySqs)) ? bksc : null;
	}

	private GenericMove getWhiteQueensideCastleMove() throws Exception {
		return (castleMoveLegal(GenericPosition.a1, qscWhiteCheckSqs, qscWhiteEmptySqs)) ? wqsc : null;
	}

	private GenericMove getBlackQueensideCastleMove() throws Exception {
		return (castleMoveLegal(GenericPosition.a8, qscBlackCheckSqs, qscBlackEmptySqs)) ? bqsc : null;
	}

	public void updateFlags(PieceType movedPiece, GenericMove lastMove) {
		// First handle castling moves
		if (movedPiece.equals(PieceType.WhiteKing) || movedPiece.equals(PieceType.BlackKing)) {
			if (lastMove.equals(wksc) || lastMove.equals(wqsc)) {
				whiteKsAvail = whiteQsAvail = false;
				whiteCastled = true;
			} else if (lastMove.equals(bksc) || lastMove.equals(bqsc)) {
				blackKsAvail = blackQsAvail = false;
				blackCastled = true;
			} 
		}
		// After this, the move wasn't castling, but may have caused castling to be no longer possible
		// A rook got captured
		if (blackQsAvail && lastMove.to.equals(GenericPosition.a8)) {
			blackQsAvail = false;
		} else if (blackKsAvail && lastMove.to.equals(GenericPosition.h8)) {
			blackKsAvail = false;
		} else if (whiteQsAvail && lastMove.to.equals(GenericPosition.a1)) {
			whiteQsAvail = false;
		} else if (whiteKsAvail && lastMove.to.equals(GenericPosition.h1)) {
			whiteKsAvail = false;
		}
		// King moved
		if (movedPiece.equals(PieceType.WhiteKing)) {
			whiteKsAvail = whiteQsAvail = false;
		} else if (movedPiece.equals(PieceType.BlackKing)) {
			blackKsAvail = blackQsAvail = false;
		// Rook moved	
		} else if (movedPiece.equals(PieceType.WhiteRook)) { 
			if (lastMove.from.file.equals(GenericFile.Fa)) {
				whiteQsAvail = false;
			} 
			if (lastMove.from.file.equals(GenericFile.Fh)) {
				whiteKsAvail = false;
			}
		} else if (movedPiece.equals(PieceType.BlackRook)) {
			if (lastMove.from.file.equals(GenericFile.Fa)) {
				blackQsAvail = false;
			} else if (lastMove.from.file.equals(GenericFile.Fh)) {
				blackKsAvail = false;
			}	
		}
	}
}
