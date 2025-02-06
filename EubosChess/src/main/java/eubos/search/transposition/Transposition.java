package eubos.search.transposition;

import eubos.board.Board;
import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.search.Score;

public final class Transposition {
	
	private static final int BESTMOVE_BITS = 16;
	private static final long BESTMOVE_GUARD_MASK = (1L << BESTMOVE_BITS) - 1;
	private static final int BESTMOVE_SHIFT = 0;
	private static final long BESTMOVE_MASK = ~(BESTMOVE_GUARD_MASK << BESTMOVE_SHIFT);
	
	private static final int EVAL_BITS = 16;
	private static final long EVAL_GUARD_MASK = (1L << EVAL_BITS) - 1;
	private static final int EVAL_SHIFT = 16;
	private static final long EVAL_MASK = ~(EVAL_GUARD_MASK << EVAL_SHIFT);
	
	private static final int SCORE_BITS = 16;
	private static final long SCORE_GUARD_MASK = (1L << SCORE_BITS) - 1;
	private static final int SCORE_SHIFT = 32;
	private static final long SCORE_MASK = ~(SCORE_GUARD_MASK << SCORE_SHIFT);
	
	private static final int DEPTH_BITS = 8;
	private static final long DEPTH_GUARD_MASK = (1L << DEPTH_BITS) - 1;
	private static final int DEPTH_SHIFT = 48;
	private static final long DEPTH_MASK = ~(DEPTH_GUARD_MASK << DEPTH_SHIFT);
	
	private static final int TYPE_BITS = 2;
	private static final long TYPE_GUARD_MASK = (1L << TYPE_BITS) - 1;
	private static final int TYPE_SHIFT = 56;
	private static final long TYPE_MASK = ~(TYPE_GUARD_MASK << TYPE_SHIFT);
	
	private static final int AGE_BITS = 6;
	private static final long AGE_GUARD_MASK = (1L << AGE_BITS) - 1;
	private static final int AGE_SHIFT = 58;
	private static final long AGE_MASK = ~(AGE_GUARD_MASK << AGE_SHIFT);
	
//	public static long valueOf(byte depth, char score, byte bound, char bestMove, char age) {
//		long trans = 0L;
//		trans = setDepthSearchedInPly(trans, depth);
//		trans = setScore(trans, score);
//		trans = setType(trans, bound);
//		trans = setBestMove(trans, bestMove);
//		trans = setAge(trans, age);
//		trans = setStaticEval(trans, (char)Short.MAX_VALUE);
//		return trans;
//	}
	public static long valueOf(byte depth, char score, byte bound, char bestMove, char age) {
		return Transposition.valueOf(depth, (char) score, bound, (char) bestMove, (char) age, (char) Short.MAX_VALUE);
	}
	
	public static long valueOf(byte depth, short score, byte bound, short bestMove, int age)
	{
		//return Transposition.valueOf(depth, (char) score, bound, (char) bestMove, (char) age);
		return Transposition.valueOf(depth, (char) score, bound, (char) bestMove, (char) age, (char) Short.MAX_VALUE);
	}
	
	public static long valueOf(byte depth, char score, byte bound, char bestMove, char age, char static_eval) {
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert age <= Short.MAX_VALUE;
			assert bound == Score.lowerBound || bound == Score.upperBound;
		}
		long trans = bestMove + 
				(static_eval * (1L<<EVAL_SHIFT)) +                             
				(score * (1L<<SCORE_SHIFT)) +
				((depth & DEPTH_GUARD_MASK) * (1L<<DEPTH_SHIFT) ) +
				(bound * (1L<<TYPE_SHIFT)) +
				(age * (1L<<AGE_SHIFT));
		return trans;
	}
	
	public static byte getDepthSearchedInPly(long trans) {
		return (byte)(trans >>> DEPTH_SHIFT);
	}

//	public static long setDepthSearchedInPly(long trans, byte depthSearchedInPly) {
//		long limitedDepth = depthSearchedInPly & DEPTH_GUARD_MASK;
//		trans &= DEPTH_MASK;
//		trans |= limitedDepth << DEPTH_SHIFT;
//		return trans;
//	}
	
	public static byte getType(long trans) {
		return (byte)((trans >>> TYPE_SHIFT) & TYPE_GUARD_MASK);
	}

//	public static long setType(long trans, long type) {
//		if (EubosEngineMain.ENABLE_ASSERTS)
//			assert type == Score.lowerBound || type == Score.upperBound;
//		trans &= TYPE_MASK;
//		trans |= type << TYPE_SHIFT;
//		return trans;
//	}
	
	public static short getScore(long trans) {
		return (short) (trans >>> SCORE_SHIFT);
	}

//	protected static long setScore(long trans, long new_score) {
//		trans &= SCORE_MASK;
//		trans |= new_score << SCORE_SHIFT;
//		return trans;
//	}
	
	public static char getAge(long trans) {
		return (char) (trans >>> AGE_SHIFT);
	}

//	protected static long setAge(long trans, long new_age) {
//		if (EubosEngineMain.ENABLE_ASSERTS)
//			assert new_age <= Short.MAX_VALUE;
//		trans &= AGE_MASK;
//		trans |= new_age << AGE_SHIFT;
//		return trans;
//	}

	public static char getBestMove(long trans) {
		return (char)trans;
	}
	
//	public static long setBestMove(long trans, long bestMove) {
//		trans &= BESTMOVE_MASK;
//		trans |= bestMove;
//		return trans;
//	}
	
	public static short getStaticEval(long trans) {
		return (short) (trans >>> EVAL_SHIFT);
	}

//	public static long setStaticEval(long trans, long eval) {
//		trans &= EVAL_MASK;
//		trans |= eval << EVAL_SHIFT;
//		return trans;
//	}
	
	public static String report(long trans, Board theBoard) {
		String output = String.format("trans best=%s, dep=%d, sc=%s, type=%s age=%d static=%d", 
				Move.toString(Move.valueOfFromTransposition(trans, theBoard)),
				getDepthSearchedInPly(trans),
				Score.toString((short)(trans >>> 32)),
				getType(trans),
				(int)getAge(trans),
				getStaticEval(trans));
		return output;
	}
	
	public static String report(long trans) {
		String output = String.format("trans best=%s, dep=%d, sc=%s, type=%s age=%d static=%d",
				Move.toString(Move.valueOfFromTransposition(trans)),
				getDepthSearchedInPly(trans),
				Score.toString((short)(trans >>> 32)),
				getType(trans),
				(int)getAge(trans),
				getStaticEval(trans));
		return output;
	}
}
