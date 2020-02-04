package eubos.position;

import eubos.board.Piece;
import eubos.position.CaptureData;

class TrackedMove {
	private int move = 0;
	private CaptureData capture = null;
	private int enPassantTarget = Position.NOPOSITION;
	private String castleFenFlags = null;

	TrackedMove( int inMove ) { move = inMove; capture = new CaptureData(); }
	TrackedMove( int inMove, CaptureData capture, int enP, String castleFen) {
		move = inMove; 
		this.capture = capture;
		enPassantTarget = enP;
		castleFenFlags = castleFen;
	}
	boolean isCapture() { return (capture.target != Piece.PIECE_NONE); }
	
	boolean isCastle() { 
		if (Move.areEqual(move, CastlingManager.bksc) || 
			Move.areEqual(move, CastlingManager.bqsc) ||
			Move.areEqual(move, CastlingManager.wksc) ||
			Move.areEqual(move, CastlingManager.wqsc)) {
			return true;
		}
		return false;
	}

	int getMove() {
		return move;
	}
	void setMove(int move) {
		this.move = move;
	}
	CaptureData getCaptureData() {
		return capture;
	}
	void setCaptureData(CaptureData capturedPiece) {
		this.capture = capturedPiece;
	}
	int getEnPassantTarget() {
		return enPassantTarget;
	}
	void setEnPassantTarget(int enPassantTarget) {
		this.enPassantTarget = enPassantTarget;
	}
	String getFenFlags() {
		return castleFenFlags;
	}
}
