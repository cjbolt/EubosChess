package eubos.position;

class MoveTracker {
	
	private static final int CAPACITY = 400;
	private long[] passed_pawn_stack;
	private byte[] en_passant_square_stack;
	private byte[] castling_stack;
	private int[] move_stack;
	private int index = 0;
	
	MoveTracker() {
		passed_pawn_stack = new long[CAPACITY];
		en_passant_square_stack = new byte[CAPACITY];
		castling_stack = new byte[CAPACITY];
		move_stack = new int[CAPACITY];
		index = 0;
	}
	
	public void push(long pp, int move, int castling, int enPassant) {
		if (index < CAPACITY) {
			passed_pawn_stack[index] = pp;
			move_stack[index] = move;
			en_passant_square_stack[index] = (byte) enPassant;
			castling_stack[index] = (byte) castling;
			index += 1;
		}
	}
	
	public void pop() {
		if (!isEmpty()) {
			index -= 1;
		}
	}
	
	public long getPassedPawns() {
		return passed_pawn_stack[index];
	}
	
	public int getMove() {
		return move_stack[index];
	}
	
	public int getEnPassant() {
		return en_passant_square_stack[index];
	}
	
	public int getCastling() {
		return castling_stack[index];
	}
	
	public boolean isEmpty() {
		return index == 0;
	}
}