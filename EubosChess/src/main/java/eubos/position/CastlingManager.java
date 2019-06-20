package eubos.position;

import java.util.List;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.InvalidPieceException;
import eubos.board.pieces.King;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Rook;
import eubos.board.pieces.Piece.Colour;

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

	private static final GenericMove undo_bksc = new GenericMove( GenericPosition.g8, GenericPosition.e8);
	private static final GenericMove undo_wksc = new GenericMove( GenericPosition.g1, GenericPosition.e1);
	private static final GenericMove undo_bqsc = new GenericMove( GenericPosition.c8, GenericPosition.e8);
	private static final GenericMove undo_wqsc = new GenericMove( GenericPosition.c1, GenericPosition.e1);

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
		if (whiteKsAvail)
			fenCastle += "K";
		if (whiteQsAvail)
			fenCastle += "Q";
		if (blackKsAvail)
			fenCastle += "k";
		if (blackQsAvail)
			fenCastle += "q";
		return fenCastle;
	}
	
	void setFenFlags(String fenCastle) {
		if (fenCastle.matches("[KQkq-]+")) {
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

	void performSecondaryCastlingMove(GenericMove move) throws InvalidPieceException {
		if (move.equals(wksc)) {
			// Perform secondary white king side castle rook move
			Piece rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( GenericPosition.h1 );
			pm.updateSquarePieceOccupies( GenericPosition.f1, rookToCastle );
			whiteKsAvail = false;
			whiteCastled = true;
		} else if (move.equals(wqsc)) {
			// Perform secondary white queen side castle rook move
			Piece rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( GenericPosition.a1 );
			pm.updateSquarePieceOccupies( GenericPosition.d1, rookToCastle );
			whiteQsAvail = false;
			whiteCastled = true;
		} else if (move.equals(bksc)) {
			// Perform secondary black king side castle rook move
			Piece rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( GenericPosition.h8 );
			pm.updateSquarePieceOccupies( GenericPosition.f8, rookToCastle );
			blackKsAvail = false;
			blackCastled = true;
		} else if (move.equals(bqsc)) {
			// Perform secondary black queen side castle rook move
			Piece rookToCastle = pm.getTheBoard().pickUpPieceAtSquare( GenericPosition.a8 );
			pm.updateSquarePieceOccupies( GenericPosition.d8, rookToCastle );
			blackQsAvail = false;
			blackCastled = true;
		}
	}

	void unperformSecondaryCastlingMove(GenericMove move) throws InvalidPieceException {
		if (move.equals(undo_wksc)) {
			// Perform secondary king side castle rook move
			Rook rookToCastle = (Rook) pm.getTheBoard().pickUpPieceAtSquare( GenericPosition.f1 );
			pm.updateSquarePieceOccupies( GenericPosition.h1, rookToCastle );
			whiteKsAvail = true;
			whiteCastled = false;
		} else	if (move.equals(undo_wqsc)) {
			// Perform secondary queen side castle rook move
			Rook rookToCastle = (Rook) pm.getTheBoard().pickUpPieceAtSquare( GenericPosition.d1 );
			pm.updateSquarePieceOccupies( GenericPosition.a1, rookToCastle );
			whiteQsAvail = true;
			whiteCastled = false;
		} else if (move.equals(undo_bksc)) {
			// Perform secondary king side castle rook move
			Rook rookToCastle = (Rook) pm.getTheBoard().pickUpPieceAtSquare( GenericPosition.f8 );
			pm.updateSquarePieceOccupies( GenericPosition.h8, rookToCastle );
			blackKsAvail = true;
			blackCastled = false;
		} else if (move.equals(undo_bqsc)) {
			// Perform secondary queen side castle rook move
			Rook rookToCastle = (Rook) pm.getTheBoard().pickUpPieceAtSquare( GenericPosition.d8 );
			pm.updateSquarePieceOccupies( GenericPosition.a8, rookToCastle );
			blackQsAvail = true;
			blackCastled = false;
		}
	}

	void addCastlingMoves(List<GenericMove> ml) {
		// The side on move should not have previously castled
		Colour onMove = pm.getOnMove();
		if ( !castlingAvaillable(onMove))
			return;
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
	
	boolean everCastled(Colour colour){
		boolean everCastled = false;
		if (colour.equals(Colour.white)) {
			everCastled = whiteCastled;
		} else {
			everCastled = blackCastled;
		}
		return everCastled;
	}

	private boolean castleMoveLegal(GenericPosition rookSq,
			GenericPosition [] checkSqs,
			GenericPosition [] emptySqs) {
		Board theBoard = pm.getTheBoard();
		// All the intervening squares between King and Rook should be empty
		for ( GenericPosition emptySq : emptySqs ) {
			if ( !theBoard.squareIsEmpty(emptySq))
				return false;
		}
		// King cannot move through an attacked square
		for (GenericPosition sqToCheck: checkSqs) {
			if (pm.squareIsAttacked(sqToCheck, pm.getOnMove())) {
				return false;
			}
		}
		return true;
	}

	private GenericMove getWhiteKingsideCastleMove() {
		return (castleMoveLegal(GenericPosition.h1, kscWhiteCheckSqs, kscWhiteEmptySqs) && whiteKsAvail) ? wksc : null;
	}

	private GenericMove getBlackKingsideCastleMove() {
		return (castleMoveLegal(GenericPosition.h8, kscBlackCheckSqs, kscBlackEmptySqs) && whiteQsAvail) ? bksc : null;
	}

	private GenericMove getWhiteQueensideCastleMove() {
		return (castleMoveLegal(GenericPosition.a1, qscWhiteCheckSqs, qscWhiteEmptySqs) && blackKsAvail) ? wqsc : null;
	}

	private GenericMove getBlackQueensideCastleMove() {
		return (castleMoveLegal(GenericPosition.a8, qscBlackCheckSqs, qscBlackEmptySqs) && blackQsAvail) ? bqsc : null;
	}

	public void updateFlags(Piece movedPiece, GenericMove lastMove) {
		if (movedPiece instanceof King) {
			if (movedPiece.isWhite()) {
				whiteKsAvail = whiteQsAvail = false;
			} else {
				blackKsAvail = blackQsAvail = false;
			}
		}
		if 	(movedPiece instanceof Rook) {
			if (movedPiece.isWhite()) {
				if (lastMove.from.file.equals(GenericFile.Fa)) {
					whiteQsAvail = false;
				} else if (lastMove.from.file.equals(GenericFile.Fh)) {
					whiteKsAvail = false;
				}
			} else {
				if (lastMove.from.file.equals(GenericFile.Fa)) {
					blackQsAvail = false;
				} else if (lastMove.from.file.equals(GenericFile.Fh)) {
					blackKsAvail = false;
				}	
			}
		}
		
	}
}
