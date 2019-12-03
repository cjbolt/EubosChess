package eubos.position;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.pieces.Piece.PieceType;
import eubos.position.CaptureData;

class TrackedMove {
	private GenericMove move = null;
	private CaptureData capture = null;
	private GenericPosition enPassantTarget = null;
	private String castleFenFlags = null;

	TrackedMove( GenericMove inMove ) { move = inMove; capture = new CaptureData(); }
	TrackedMove( GenericMove inMove, CaptureData capture, GenericPosition enP, String castleFen) {
		move = inMove; 
		this.capture = capture;
		enPassantTarget = enP;
		castleFenFlags = castleFen;
	}
	boolean isCapture() { return (capture.target != PieceType.NONE); }
	
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
	CaptureData getCaptureData() {
		return capture;
	}
	void setCaptureData(CaptureData capturedPiece) {
		this.capture = capturedPiece;
	}
	GenericPosition getEnPassantTarget() {
		return enPassantTarget;
	}
	void setEnPassantTarget(GenericPosition enPassantTarget) {
		this.enPassantTarget = enPassantTarget;
	}
	String getFenFlags() {
		return castleFenFlags;
	}
}
