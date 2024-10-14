package eubos.score;

import java.util.Arrays;

public class PawnEvalHashTable {
	
	private long [] data = null;
	
	private static final int PAWN_BB_SCALING = 8;
	private static final int SIZE_OF_PAWN_HASH = 65536*2;
	private static final int PAWN_HASH_MASK = (int)(Long.highestOneBit(SIZE_OF_PAWN_HASH)-1);
	
	public PawnEvalHashTable() {
		data = new long[SIZE_OF_PAWN_HASH];
		Arrays.fill(data, Short.MAX_VALUE << 48);
	}
	
	public synchronized short get(int hash, long pawns, boolean onMoveIsWhite) {
		int index = hash & PAWN_HASH_MASK;
		if ((data[index] & 0xFFFF_FFFF_FFFFL) == pawns >>> PAWN_BB_SCALING) {
			short score = (short) (data[index] >> 48);
			// Score saved in table is from white point of view
			if (!onMoveIsWhite) {
				score = (short)-score;
			}
			return score;
		}
		return Short.MAX_VALUE;
	}
	
	public synchronized void put(int hash, int score, long pawns, boolean onMoveIsWhite) {
		int index = hash & PAWN_HASH_MASK;
		// Score saved in table is from white point of view
		if (!onMoveIsWhite) {
			score = -score;
		}
		data[index] = (pawns >>> PAWN_BB_SCALING) | (((long)score) << 48);
	}
}
