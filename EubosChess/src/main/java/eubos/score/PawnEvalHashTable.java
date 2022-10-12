package eubos.score;

import java.util.Arrays;

public class PawnEvalHashTable {	
	private short [] eval = null;
	private long [] black = null;
	private int [] white = null;
	private byte [] weight = null;
		
	public PawnEvalHashTable() {
		eval = new short[65536];
		black = new long[65536];
		white = new int[65536];
		weight = new byte[65536];
		Arrays.fill(eval, Short.MAX_VALUE);
	}
	
	public synchronized short get(int hash, int phase_weighting, long white_pawns, long black_pawns, boolean onMoveIsWhite) {
		int index = hash & 0xFFFF;
		if (weight[index] == phase_weighting) {
			if (white[index] == ((int)(white_pawns >>> ((4*8)-8)))) { 
				if (black[index] == ((black_pawns >>> 8) | (white_pawns << ((6*8)-8)))) {
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
		int index = hash & 0xFFFF;
		// Score saved in table is from white point of view
		if (!onMoveIsWhite) {
			score = -score;
		}
		black[index] = (black_pawns >>> 8) | (white_pawns << ((6*8)-8));
		white[index] = (int)(white_pawns >>> ((4*8)-8));
		weight[index] = (byte)phase_weighting;
		eval[index] = (short)score;
	}
}
