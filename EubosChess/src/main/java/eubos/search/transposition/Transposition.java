package eubos.search.transposition;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.search.Score;

public final class Transposition {

	public static long valueOf(byte depth, short score, byte bound, GenericMove bestMove) {
		// Only used by tests
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
		return (byte)((trans >> 48) & 0xFF);
	}

	protected static long setDepthSearchedInPly(long trans, byte depthSearchedInPly) {
		short limitedDepth = (short)Math.min(0xFF, depthSearchedInPly);
		trans &= ~(0xFFL << 48);
		trans |= ((limitedDepth & 0xFFL) << 48);
		return trans;
	}
	
	public static byte getType(long trans) {
		return (byte)((trans >>> 56) & 0x3);
	}

	protected static long setType(long trans, byte type) {
		long temp = ((long)type) << 56;
		trans &= ~(0x3L << 56);
		trans |= temp;
		return trans;
	}
	
	public static short getScore(long trans) {
		return (short) ((trans >>> 32) & 0xFFFFL);
	}

	protected static long setScore(long trans, short new_score) {
		long temp = ((long)new_score) << 32;
		temp &= 0xFFFF00000000L;
		trans &= ~(0xFFFFL << 32);
		trans |= temp;
		return trans;
	}

	public static int getBestMove(long trans) {
		int trans_move = (int) (trans & 0xFFFFFFFFL);
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert trans_move != Move.NULL_MOVE : "Tranposition move was null.";
		}
		return trans_move;
	}
	
	protected static long setBestMove(long trans, int bestMove) {
		// Is always best move, but killer flag could be different
		bestMove = Move.setBest(bestMove);
		trans &= ~(0xFFFFFFFFL << 0);
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
