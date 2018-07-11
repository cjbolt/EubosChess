package eubos.position;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.pieces.Piece;

class TrackedMove {
	private GenericMove move = null;
	private Piece capturedPiece = null;
	private GenericPosition enPassantTarget = null; 

	TrackedMove( GenericMove inMove ) { move = inMove; }
	TrackedMove( GenericMove inMove, Piece capture, GenericPosition enP ) {
		move = inMove; 
		capturedPiece = capture;
		enPassantTarget = enP;
	}
	boolean isCapture() { return ((capturedPiece != null) ? true : false); }
	
	boolean isCastle() { 
		if (move.equals(CastlingManager.bksc) || 
			move.equals(CastlingManager.bqsc) ||
			move.equals(CastlingManager.wksc) ||
			move.equals(CastlingManager.wqsc)) {
			return true;
		}
		return false;
	}

	GenericMove getMove() {
		return move;
	}
	void setMove(GenericMove move) {
		this.move = move;
	}
	Piece getCapturedPiece() {
		return capturedPiece;
	}
	void setCapturedPiece(Piece capturedPiece) {
		this.capturedPiece = capturedPiece;
	}
	GenericPosition getEnPassantTarget() {
		return enPassantTarget;
	}
	void setEnPassantTarget(GenericPosition enPassantTarget) {
		this.enPassantTarget = enPassantTarget;
	}
}
