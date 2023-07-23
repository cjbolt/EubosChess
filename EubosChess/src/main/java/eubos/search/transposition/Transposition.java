package eubos.search.transposition;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.Board;
import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.position.CastlingManager;
import eubos.position.Move;
import eubos.search.Score;

public final class Transposition {
	
	private static final int BESTMOVE_BITS = 16;
	private static final long BESTMOVE_GUARD_MASK = (1L << BESTMOVE_BITS) - 1;
	private static final int BESTMOVE_SHIFT = 0;
	private static final long BESTMOVE_MASK = BESTMOVE_GUARD_MASK << BESTMOVE_SHIFT;
	
	private static final int ORIGIN_BITS = 6;
	private static final long ORIGIN_GUARD_MASK = (1L << ORIGIN_BITS) - 1;
	private static final int ORIGIN_SHIFT = 6;
	private static final long ORIGIN_MASK = ORIGIN_GUARD_MASK << ORIGIN_SHIFT;
	
	private static final int TARGET_BITS = 6;
	private static final long TARGET_GUARD_MASK = (1L << TARGET_BITS) - 1;
	private static final int TARGET_SHIFT = 0;
	private static final long TARGET_MASK = TARGET_GUARD_MASK << TARGET_SHIFT;
	
	private static final int PROMOTION_BITS = 3;
	private static final long PROMOTION_GUARD_MASK = (1L << PROMOTION_BITS) - 1;
	private static final int PROMOTION_SHIFT = 12;
	private static final long PROMOTION_MASK = PROMOTION_GUARD_MASK << PROMOTION_SHIFT;
	
	private static final int EN_PASSANT_BITS = 1;
	private static final long EN_PASSANT_GUARD_MASK = (1L << EN_PASSANT_BITS) - 1;
	private static final int EN_PASSANT_SHIFT = 15;
	private static final long EN_PASSANT_MASK = EN_PASSANT_GUARD_MASK << EN_PASSANT_SHIFT;
	
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
	
	public static long valueOf(byte depth, short score, byte bound, GenericMove bestMove) {
		// Only used by unit tests, when we don't care about value
		return valueOf(depth, score, bound, Move.toMove(bestMove, null, Move.TYPE_REGULAR_NONE), 0);
	}
	
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

	protected static long setDepthSearchedInPly(long trans, byte depthSearchedInPly) {
		long limitedDepth = depthSearchedInPly & DEPTH_GUARD_MASK;
		trans |= limitedDepth << DEPTH_SHIFT;
		return trans;
	}
	
	public static byte getType(long trans) {
		return (byte)((trans >>> TYPE_SHIFT) & TYPE_GUARD_MASK);
	}

	protected static long setType(long trans, byte type) {
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

	public static int getBestMove(long trans, Board theBoard) {
		int trans_move = 0;
		int orig = (int)((trans >>> ORIGIN_SHIFT) & ORIGIN_GUARD_MASK);
		int target = (int)((trans >>> TARGET_SHIFT) & TARGET_GUARD_MASK);
		int promo = (int)((trans >>> PROMOTION_SHIFT) & PROMOTION_GUARD_MASK);
		boolean is_en_passant_capture = ((trans & EN_PASSANT_MASK) == EN_PASSANT_MASK);

		int originPiece = theBoard.getPieceAtSquare(1L << orig);
		int targetPiece = is_en_passant_capture ? (Piece.isWhite(originPiece)?Piece.BLACK_PAWN:Piece.WHITE_PAWN): theBoard.getPieceAtSquare(1L << target);
		//trans_move = Move.valueOfBit(promo != Piece.NONE ? Move.TYPE_PROMOTION_MASK: Move.TYPE_REGULAR_NONE, orig, originPiece, target, targetPiece, promo);
		trans_move = Move.valueOfBitFromTransposition(
				(int) (trans & BESTMOVE_MASK),
				promo != Piece.NONE ? Move.TYPE_PROMOTION_MASK: Move.TYPE_REGULAR_NONE,
			    originPiece, 
			    targetPiece);
		if (is_en_passant_capture) {
			trans_move |= Move.MISC_EN_PASSANT_CAPTURE_MASK;
		} else if (Piece.isKing(originPiece) && targetPiece == Piece.NONE) {
			if (Move.areEqualForBestKiller(CastlingManager.bksc, trans_move) ||
				Move.areEqualForBestKiller(CastlingManager.wksc, trans_move) ||
				Move.areEqualForBestKiller(CastlingManager.bqsc, trans_move) ||
				Move.areEqualForBestKiller(CastlingManager.wqsc, trans_move)) {
				trans_move |= Move.MISC_CASTLING_MASK;
			}
		}
		if (EubosEngineMain.ENABLE_ASSERTS) {
			assert trans_move != Move.NULL_MOVE : "Tranposition move was null.";
		}
		
		return Move.setBest(trans_move);
	}
	
	protected static long setBestMove(long trans, int bestMove) {
		// Extract just the parts of move we need to store in the table
		trans &= ~BESTMOVE_MASK;
//		int orig = Move.getOriginPosition(bestMove);
//		trans |= orig << ORIGIN_SHIFT;
//		int target = Move.getTargetPosition(bestMove);
//		trans |= target << TARGET_SHIFT;
//		int promo = Move.getPromotion(bestMove);
//		trans |= promo << PROMOTION_SHIFT;
//		if (Move.isEnPassantCapture(bestMove)) {
//			trans |= EN_PASSANT_MASK;
//		}
		bestMove &= BESTMOVE_MASK;
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
				Move.toGenericMove(Transposition.getBestMove(trans, theBoard)),
				getDepthSearchedInPly(trans),
				Score.toString((short)(trans >>> 32)),
				getType(trans),
				getAge(trans));
		return output;
	}
	
	public static String report(long trans) {
		String output = String.format("trans dep=%d, sc=%s, type=%s age=%d", 
				getDepthSearchedInPly(trans),
				Score.toString((short)(trans >>> 32)),
				getType(trans),
				getAge(trans));
		return output;
	}
}
