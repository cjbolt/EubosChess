package eubos.position;

import eubos.main.EubosEngineMain;

class MoveTracker {
	
	private static final int CAPACITY = 400;
	private long[] passed_pawn_stack;
	private long[] hash_stack;
	private byte[] en_passant_square_stack;
	private byte[] castling_stack;
	private int[] move_stack;
	private int index = 0;
	
	MoveTracker() {
		passed_pawn_stack = new long[CAPACITY];
		en_passant_square_stack = new byte[CAPACITY];
		castling_stack = new byte[CAPACITY];
		move_stack = new int[CAPACITY];
		hash_stack = new long[CAPACITY];
		index = 0;
	}
	
	public void push(long pp, int move, int castling, int enPassant, long hash) {
		passed_pawn_stack[index] = pp;
		move_stack[index] = move;
		en_passant_square_stack[index] = (byte) enPassant;
		castling_stack[index] = (byte) castling;
		hash_stack[index] = hash;
		index += 1;
	}
	
	public void pop() {
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert index > 0 : String.format("%s %s %s", Move.toString(move_stack[0]), Move.toString(move_stack[1]), Move.toString(move_stack[2]));
		}
		index -= 1;
	}
	
	public long getPassedPawns() {
		return passed_pawn_stack[index];
	}
	
	public long getHash() {
		return hash_stack[index];
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