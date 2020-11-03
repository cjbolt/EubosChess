package eubos.position;

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
		this.capture.square = capturedPiece.square;
		this.capture.target = capturedPiece.target;
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
