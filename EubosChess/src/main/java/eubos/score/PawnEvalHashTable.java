package eubos.score;

import java.util.Arrays;

public class PawnEvalHashTable {	
	private long [] lower = null;
	private long [] upper = null;
	
	private static final int WHITE_SCALING_FOR_UPPER = (4*8)-8; // most significant 4 bytes of white pawns in upper
	private static final int WHITE_SCALING_FOR_LOWER = (6*8)-8; // least significant 2 bytes of white pawns in lower
	private static final int BLACK_SCALING_FOR_LOWER = 8;       // all 6 bytes of black pawns in lower (shift out 1st rank)
	
	private static final int SIZE_OF_PAWN_HASH = 65536;
	
	private static final int PAWN_HASH_MASK = (int)(Long.highestOneBit(SIZE_OF_PAWN_HASH)-1);
		
	public PawnEvalHashTable() {
		lower = new long[SIZE_OF_PAWN_HASH];
		upper = new long[SIZE_OF_PAWN_HASH];
		Arrays.fill(upper, Short.MAX_VALUE << 48);
	}
	
	private long createLower(long white_pawns, long black_pawns) {
		return (black_pawns >>> BLACK_SCALING_FOR_LOWER) | (white_pawns << WHITE_SCALING_FOR_LOWER);
	}
	
	public synchronized short get(int hash, long white_pawns, long black_pawns, boolean onMoveIsWhite) {
		int index = hash & PAWN_HASH_MASK;
		if (lower[index] == createLower(white_pawns, black_pawns)) {
			long composite = upper[index];
			if ((composite & 0xFFFF_FFFFL) == ((int)(white_pawns >>> WHITE_SCALING_FOR_UPPER))) {
				short score = (short) (composite >> 48);
				// Score saved in table is from white point of view
				if (!onMoveIsWhite) {
					score = (short)-score;
				}
				return score;
			}
		}
		return Short.MAX_VALUE;
	}
	
	public synchronized void put(int hash, int score, long white_pawns, long black_pawns, boolean onMoveIsWhite) {
		int index = hash & PAWN_HASH_MASK;
		// Score saved in table is from white point of view
		if (!onMoveIsWhite) {
			score = -score;
		}
		lower[index] = createLower(white_pawns, black_pawns);
		upper[index] = (white_pawns >>> WHITE_SCALING_FOR_UPPER) | (((long)score) << 48);
	}
}
