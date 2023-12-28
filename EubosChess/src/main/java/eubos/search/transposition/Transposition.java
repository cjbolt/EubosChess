package eubos.search.transposition;

import eubos.board.Board;
import eubos.position.Move;
import eubos.search.Score;

public final class Transposition {
	
	private static final int BESTMOVE_BITS = 16;
	private static final long BESTMOVE_GUARD_MASK = (1L << BESTMOVE_BITS) - 1;
	private static final int BESTMOVE_SHIFT = 0;
	private static final long BESTMOVE_MASK = BESTMOVE_GUARD_MASK << BESTMOVE_SHIFT;
	
	private static final int EVAL_BITS = 16;
	private static final long EVAL_GUARD_MASK = (1L << EVAL_BITS) - 1;
	private static final int EVAL_SHIFT = 16;
	private static final long EVAL_MASK = EVAL_GUARD_MASK << EVAL_SHIFT;
	
	private static final int SCORE_BITS = 16;
	private static final long SCORE_GUARD_MASK = (1L << SCORE_BITS) - 1;
	private static final int SCORE_SHIFT = 32;
	
	private static final int DEPTH_BITS = 8;
	private static final long DEPTH_GUARD_MASK = (1L << DEPTH_BITS) - 1;
	private static final int DEPTH_SHIFT = 48;
	
	private static final int TYPE_BITS = 2;
	private static final long TYPE_GUARD_MASK = (1L << TYPE_BITS) - 1;
	private static final int TYPE_SHIFT = 56;
	
	private static final int AGE_BITS = 6;
	private static final long AGE_GUARD_MASK = (1L << AGE_BITS) - 1;
	private static final int AGE_SHIFT = 58;
	
	public static long valueOf(byte depth, short score, byte bound, int bestMove, int age) {
		long trans = 0L;
		trans = setDepthSearchedInPly(trans, depth);
		trans = setScore(trans, score);
		trans = setType(trans, bound);
		trans = setBestMove(trans, bestMove);
		trans = setAge(trans, age);
		trans = setStaticEval(trans, Short.MAX_VALUE);
		return trans;
	}
	
	public static byte getDepthSearchedInPly(long trans) {
		return (byte)((trans >>> DEPTH_SHIFT) & DEPTH_GUARD_MASK);
	}

	public static long setDepthSearchedInPly(long trans, byte depthSearchedInPly) {
		long limitedDepth = depthSearchedInPly & DEPTH_GUARD_MASK;
		trans |= limitedDepth << DEPTH_SHIFT;
		return trans;
	}
	
	public static byte getType(long trans) {
		return (byte)((trans >>> TYPE_SHIFT) & TYPE_GUARD_MASK);
	}

	public static long setType(long trans, byte type) {
		long temp = ((long)type) << TYPE_SHIFT;
		trans |= temp;
		return trans;
	}
	
	public static short getScore(long trans) {
		return (short) ((trans >>> SCORE_SHIFT) & SCORE_GUARD_MASK);
	}

	protected static long setScore(long trans, short new_score) {
		trans |= (new_score & SCORE_GUARD_MASK) << SCORE_SHIFT;
		return trans;
	}
	
	public static short getAge(long trans) {
		return (short) ((trans >>> AGE_SHIFT) & AGE_GUARD_MASK);
	}

	protected static long setAge(long trans, int new_age) {
		trans |= (new_age & AGE_GUARD_MASK) << AGE_SHIFT;
		return trans;
	}

	public static int getBestMove(long trans) {
		return (int)(trans & BESTMOVE_MASK);
	}
	
	public static long setBestMove(long trans, int bestMove) {
		bestMove &= BESTMOVE_MASK;
		trans &= ~BESTMOVE_MASK;
		trans |= bestMove;
		return trans;
	}
	
	public static short getStaticEval(long trans) {
		return (short) ((trans >>> EVAL_SHIFT) & EVAL_GUARD_MASK);
	}

	public static long setStaticEval(long trans, int eval) {
		trans &= ~EVAL_MASK;
		trans |= (eval & EVAL_GUARD_MASK) << EVAL_SHIFT;
		return trans;
	}
	
	public static String report(long trans, Board theBoard) {
		String output = String.format("trans best=%s, dep=%d, sc=%s, type=%s age=%d", 
				Move.toString(Move.valueOfFromTransposition(trans, theBoard)),
				getDepthSearchedInPly(trans),
				Score.toString((short)(trans >>> 32)),
				getType(trans),
				getAge(trans));
		return output;
	}
	
	public static String report(long trans) {
		String output = String.format("trans best=%s, dep=%d, sc=%s, type=%s age=%d",
				Move.toString(Move.valueOfFromTransposition(trans)),
				getDepthSearchedInPly(trans),
				Score.toString((short)(trans >>> 32)),
				getType(trans),
				getAge(trans));
		return output;
	}
}
