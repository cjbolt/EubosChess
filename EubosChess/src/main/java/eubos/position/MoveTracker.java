package eubos.position;

class MoveTracker {
	
	private static final int CAPACITY = 400;
	private long[] stack;
	private int index = 0;
	
	MoveTracker() {
		stack = new long[CAPACITY];
		for (int i = 0; i < CAPACITY; i++) {
			stack[i] = Move.NULL_MOVE;
		}
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
	
	public int lastMoveTargetSquare() {
		int targetSq = Position.NOPOSITION;
		if (!isEmpty()) {
			targetSq = Move.getTargetPosition(TrackedMove.getMove(stack[index-1]));
		}
		return targetSq;
	}
	
	public boolean isEmpty() {
		return index == 0;
	}
}