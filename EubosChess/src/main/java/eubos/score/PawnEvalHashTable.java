package eubos.score;

import java.util.Arrays;

public class PawnEvalHashTable {	
	private long [] lower = null;
	private long [] upper = null;
	
	public static final int WHITE_SCALING_FOR_UPPER = (4*8)-8; // most significant 4 bytes of white pawns in upper
	public static final int WHITE_SCALING_FOR_LOWER = (6*8)-8; // least significant 2 bytes of white pawns in lower
	public static final int BLACK_SCALING_FOR_LOWER = 8;       // all 6 bytes of black pawns in lower (shift out 1st rank)
	
	public static final int SIZE_OF_PAWN_HASH = 65536;
	
	public static final int PAWN_HASH_MASK = (int)(Long.highestOneBit(SIZE_OF_PAWN_HASH)-1);
		
	public PawnEvalHashTable() {
		lower = new long[SIZE_OF_PAWN_HASH];
		upper = new long[SIZE_OF_PAWN_HASH];
		Arrays.fill(upper, Short.MAX_VALUE << 48);
	}
	
	public synchronized short get(int hash, int phase_weighting, long white_pawns, long black_pawns, boolean onMoveIsWhite) {
		int index = hash & PAWN_HASH_MASK;
		if (lower[index] == ((black_pawns >>> BLACK_SCALING_FOR_LOWER) | (white_pawns << WHITE_SCALING_FOR_LOWER))) {
			long composite = upper[index];
			if ((composite & 0xFFFF_FFFFL) == ((int)(white_pawns >>> WHITE_SCALING_FOR_UPPER))) {
				if (((composite >>> 32) & 0xFFL) == phase_weighting) {
					short score = (short) (composite >> 48);
					// Score saved in table is from white point of view
					if (!onMoveIsWhite) {
						score = (short)-score;
					}
					return score;
				}
			}
		}
		return Short.MAX_VALUE;
	}
	
	public synchronized void put(int hash, int phase_weighting, int score, long white_pawns, long black_pawns, boolean onMoveIsWhite) {
		int index = hash & PAWN_HASH_MASK;
		// Score saved in table is from white point of view
		if (!onMoveIsWhite) {
			score = -score;
		}
		lower[index] = (black_pawns >>> BLACK_SCALING_FOR_LOWER) | (white_pawns << WHITE_SCALING_FOR_LOWER);
		upper[index] = (white_pawns >>> WHITE_SCALING_FOR_UPPER) | (((long)phase_weighting) << 32) | (((long)score) << 48);
	}
}
