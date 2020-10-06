package eubos.position;

import eubos.board.Piece;

class TrackedMove {
	private int move = 0;
	private CaptureData capture = null;
	private int enPassantTarget = Position.NOPOSITION;
	private int castlingFlags = 0;

	TrackedMove( int inMove ) { move = inMove; capture = new CaptureData(); }
	TrackedMove( int inMove, CaptureData capture, int enP, int castling) {
		move = inMove; 
		this.capture = capture;
		enPassantTarget = enP;
		castlingFlags = castling;
	}
	boolean isCapture() { return (capture.target != Piece.NONE); }
	
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
	int getCastlingFlags() {
		return castlingFlags;
	}
	public boolean isEnPassantCapture() {
		return capture.enPassant;
	}
}
