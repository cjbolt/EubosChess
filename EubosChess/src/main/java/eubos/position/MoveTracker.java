package eubos.position;

import eubos.board.BitBoard;
import eubos.main.EubosEngineMain;

class MoveTracker {
	class MoveStack {
		long passed_pawn;
		long hash;
		byte en_passant_square;
		byte castling;
		int move;
		int draw_check_ply;
		
		MoveStack() {
			passed_pawn = 0L;
			hash = 0L;
			en_passant_square = BitBoard.INVALID;
			castling = 0;
			move = Move.NULL_MOVE;
			draw_check_ply = 0;
		}
	}
	private static final int CAPACITY = 400;
	private MoveStack[] stack;
	private int index = 0;
	
	MoveTracker() {
		stack = new MoveStack[CAPACITY];
		for (int i=0; i < stack.length; i++) {
			stack[i] = new MoveStack();
		}
		index = 0;
	}
	
	public void push(long pp, int move, int castling, int enPassant, long hash, int dc_index) {
		stack[index].passed_pawn = pp;
		stack[index].move = move;
		stack[index].en_passant_square = (byte) enPassant;
		stack[index].castling = (byte) castling;
		stack[index].hash = hash;
		stack[index].draw_check_ply = dc_index;
		index += 1;
	}
	
	public MoveStack pop() {
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert index > 0 : String.format("%s %s %s", Move.toString(stack[0].move), Move.toString(stack[1].move), Move.toString(stack[2].move));
		}
		index -= 1;
		return stack[index];
	}
	
	public long getPassedPawns() {
		return stack[index].passed_pawn;
	}
	
	public long getHash() {
		return stack[index].hash;
	}
	
	public int getMove() {
		return stack[index].move;
	}
	
	public int getEnPassant() {
		return stack[index].en_passant_square;
	}
	
	public int getCastling() {
		return stack[index].castling;
	}
	
	public int getDrawCheckPly() {
		return stack[index].draw_check_ply;
	}
	
	public boolean isEmpty() {
		return index == 0;
	}
}