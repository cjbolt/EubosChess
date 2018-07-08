package eubos.board;

import java.util.Stack;

class MoveTracker extends Stack<TrackedMove> {
	
	static final long serialVersionUID = 0x1L;

	public MoveTracker() {}
	
	public boolean lastMoveWasCapture() {
		boolean wasCapture = false;
		if ( !this.isEmpty()) {
			wasCapture = this.peek().isCapture();
		}
		return wasCapture;
	}
	
	public boolean lastMoveWasCastle() {
		boolean wasCastle = false;
		if ( !this.isEmpty()) {
			wasCastle = this.peek().isCastle();
		}
		return wasCastle;
	}
}