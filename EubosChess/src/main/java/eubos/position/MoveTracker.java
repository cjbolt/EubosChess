package eubos.position;

class MoveTracker {
	
	private static final int CAPACITY = 400;
	private long[] stack;
	private int index = 0;
	
	MoveTracker() {
		stack = new long[CAPACITY];
		index = 0;
	}
	
	public void push(long tm) {
		if (index < CAPACITY) {
			stack[index] = tm;
			index += 1;
		}
	}
	
	public long pop() {
		long tm = TrackedMove.NULL_TRACKED_MOVE;
		if (!isEmpty()) {
			index -= 1;
			tm = stack[index];
		}
		return tm;
	}
	
	public int getCapturedPieceSquare() {
		int captured = 0;
		if ( !isEmpty()) {
			captured = TrackedMove.getCapturedPieceSquare(stack[index-1]);
		}
		return captured;
	}

	public boolean lastMoveWasCheck() {
		boolean wasCheck = false;
		if ( !isEmpty()) {
			wasCheck = Move.isCheck(TrackedMove.getMove(stack[index-1]));
		}
		return wasCheck;
	}
	
	public boolean isEmpty() {
		return index == 0;
	}
}