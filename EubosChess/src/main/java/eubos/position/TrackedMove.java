package eubos.position;

class TrackedMove {
	private int move = 0;
	private int capture = 0;
	private int enPassantTarget = Position.NOPOSITION;
	private int castlingFlags = 0;

	TrackedMove( int inMove ) { move = inMove; capture = 0; }
	TrackedMove( int inMove, int capture, int enP, int castling) {
		move = inMove; 
		this.capture = capture;
		enPassantTarget = enP;
		castlingFlags = castling;
	}

	int getMove() {
		return move;
	}
	void setMove(int move) {
		this.move = move;
	}
	int getCaptureData() {
		return capture;
	}
	void setCaptureData(int capturedPiece) {
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
	void setCastlingFlags(int flags) {
		this.castlingFlags = flags;
	}
}
