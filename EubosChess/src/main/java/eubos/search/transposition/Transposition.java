package eubos.search.transposition;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.search.Score;

public final class Transposition {
	private static final int DEPTH_BITS = 8;
	private static final long DEPTH_GUARD_MASK = (1L << DEPTH_BITS) - 1;
	private static final int DEPTH_SHIFT = 48;
	private static final long DEPTH_CLEAR_MASK = 0xFFL << DEPTH_SHIFT;
	
	private static final int TYPE_BITS = 2;
	private static final long TYPE_GUARD_MASK = (1L << TYPE_BITS) - 1;
	private static final int TYPE_SHIFT = 56;
	private static final long TYPE_MASK = TYPE_GUARD_MASK << TYPE_SHIFT;
	
	private static final int SCORE_BITS = 16;
	private static final long SCORE_GUARD_MASK = (1L << SCORE_BITS) - 1;
	private static final int SCORE_SHIFT = 32;
	private static final long SCORE_MASK = SCORE_GUARD_MASK << SCORE_SHIFT;
	
	private static final int BESTMOVE_BITS = 32;
	private static final long BESTMOVE_GUARD_MASK = (1L << BESTMOVE_BITS) - 1;
	private static final int BESTMOVE_SHIFT = 0;
	private static final long BESTMOVE_MASK = BESTMOVE_GUARD_MASK << BESTMOVE_SHIFT;
	
	public static long valueOf(byte depth, short score, byte bound, GenericMove bestMove) {
		// Only used by unit tests, when we don't care about value
		return valueOf(depth, score, bound, Move.toMove(bestMove, null, Move.TYPE_REGULAR_NONE));
	}
	
	public static long valueOf(byte depth, short score, byte bound, int bestMove) {
		long trans = 0L;
		trans = setDepthSearchedInPly(trans, depth);
		trans = setScore(trans, score);
		trans = setType(trans, bound);
		trans = setBestMove(trans, bestMove);
		return trans;
	}
	
	public static byte getDepthSearchedInPly(long trans) {
		return (byte)((trans >> DEPTH_SHIFT) & DEPTH_GUARD_MASK);
	}

	protected static long setDepthSearchedInPly(long trans, byte depthSearchedInPly) {
		long limitedDepth = depthSearchedInPly & DEPTH_GUARD_MASK;
		trans &= ~DEPTH_CLEAR_MASK;
		trans |= limitedDepth << DEPTH_SHIFT;
		return trans;
	}
	
	public static byte getType(long trans) {
		return (byte)((trans >>> TYPE_SHIFT) & TYPE_GUARD_MASK);
	}

	protected static long setType(long trans, byte type) {
		long temp = ((long)type) << TYPE_SHIFT;
		trans &= ~TYPE_MASK;
		trans |= temp;
		return trans;
	}
	
	public static short getScore(long trans) {
		return (short) ((trans >>> SCORE_SHIFT) & 0xFFFFL);
	}

	protected static long setScore(long trans, short new_score) {
		trans &= ~SCORE_MASK;
		trans |= (new_score & SCORE_GUARD_MASK) << SCORE_SHIFT;
		return trans;
	}

	public static int getBestMove(long trans) {
		int trans_move = (int) (trans & BESTMOVE_MASK);
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert trans_move != Move.NULL_MOVE : "Tranposition move was null.";
		}
		return trans_move;
	}
	
	protected static long setBestMove(long trans, int bestMove) {
		// Is always best move, but killer flag could be different
		bestMove = Move.setBest(bestMove);
		trans &= ~BESTMOVE_MASK;
		trans |= bestMove;
		return trans;
	}
	
	public static String report(long trans) {
		String output = String.format("trans best=%s, dep=%d, sc=%s, type=%s", 
				Move.toGenericMove(Transposition.getBestMove(trans)),
				getDepthSearchedInPly(trans),
				Score.toString((short)(trans >>> 32)),
				getType(trans));
		return output;
	}
	
	public static synchronized long checkUpdate(long trans,
			byte new_Depth, 
			short new_score,
			byte new_bound,
			int new_bestMove) {	
		boolean updateTransposition = false;
		if (getDepthSearchedInPly(trans) < new_Depth) {
			updateTransposition = true;	
		} else if (getDepthSearchedInPly(trans) == new_Depth) {
			if (getType(trans) != Score.exact) {
				updateTransposition = true;
			} else {
				// don't update, worse bound score than we currently have
			}
		} else {
			// don't update, depth is less than what we have
		}
		if (updateTransposition) {
			trans = setDepthSearchedInPly(trans, new_Depth);
			trans = setScore(trans, new_score);
			trans = setType(trans, new_bound);
			trans = setBestMove(trans, new_bestMove);
		}
		return trans;
	}
}
