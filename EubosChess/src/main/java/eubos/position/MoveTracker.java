package eubos.position;

import java.util.Stack;

class MoveTracker extends Stack<TrackedMove> {
	
	static final long serialVersionUID = 0x1L;

	MoveTracker() {}
	
	boolean lastMoveWasCapture() {
		boolean wasCapture = false;
		if ( !this.isEmpty()) {
			wasCapture = this.peek().isCapture();
		}
		return wasCapture;
	}
	
	public CaptureData getCapturedPiece() {
		CaptureData captured = null;
		if ( !this.isEmpty()) {
			captured = this.peek().getCaptureData();
		}
		return captured;
	}

	public boolean lastMoveWasPromotion() {
		boolean wasPromotion = false;
		if ( !this.isEmpty()) {
			wasPromotion = Move.isPromotion(this.peek().getMove());
		}
		return wasPromotion;
	}

	public boolean lastMoveWasCheck() {
		boolean wasCheck = false;
		if ( !this.isEmpty()) {
			wasCheck = Move.isCheck(this.peek().getMove());
		}
		return wasCheck;
	}
}