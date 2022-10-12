package eubos.score;

import java.util.Arrays;

public class PawnEvalHashTable {	
	private short [] eval = null;
	private long [] lower = null;
	private int [] upper = null;
	private byte [] weight = null;
	
	public static final int WHITE_SCALING_FOR_UPPER = (4*8)-8; // most significant 4 bytes of white pawns in upper
	public static final int WHITE_SCALING_FOR_LOWER = (6*8)-8; // least significant 2 bytes of white pawns in lower
	public static final int BLACK_SCALING_FOR_LOWER = 8;       // all 6 bytes of black pawns in lower (shift out 1st rank)
	
	public static final int SIZE_OF_PAWN_HASH = 65536;
	public static final int PAWN_HASH_MASK = SIZE_OF_PAWN_HASH-1;
		
	public PawnEvalHashTable() {
		eval = new short[SIZE_OF_PAWN_HASH];
		lower = new long[SIZE_OF_PAWN_HASH];
		upper = new int[SIZE_OF_PAWN_HASH];
		weight = new byte[SIZE_OF_PAWN_HASH];
		Arrays.fill(eval, Short.MAX_VALUE);
	}
	
	public synchronized short get(int hash, int phase_weighting, long white_pawns, long black_pawns, boolean onMoveIsWhite) {
		int index = hash & PAWN_HASH_MASK;
		if (weight[index] == phase_weighting) {
			if (upper[index] == ((int)(white_pawns >>> WHITE_SCALING_FOR_UPPER))) { 
				if (lower[index] == ((black_pawns >>> BLACK_SCALING_FOR_LOWER) | (white_pawns << WHITE_SCALING_FOR_LOWER))) {
					int score = eval[index];
					// Score saved in table is from white point of view
					if (!onMoveIsWhite) {
						score = -score;
					}
					return (short)score;
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
		upper[index] = (int)(white_pawns >>> WHITE_SCALING_FOR_UPPER);
		weight[index] = (byte)phase_weighting;
		eval[index] = (short)score;
	}
}
