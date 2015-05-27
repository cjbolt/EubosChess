package eubos.board;

import java.util.Stack;

public class MoveTracker extends Stack<TrackedMove> {
	
	static final long serialVersionUID = 0x1L;

	public MoveTracker() {
		//previousMoves = new Stack<TrackedMove>();
	}
	
	public boolean lastMoveWasCapture() {
		boolean wasCapture = false;
		if ( !this.isEmpty()) {
			wasCapture = this.peek().isCapture();
		}
		return wasCapture;
	}
}