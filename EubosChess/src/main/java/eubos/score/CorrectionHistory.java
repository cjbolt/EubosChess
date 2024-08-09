package eubos.score;

import java.util.Arrays;

public class CorrectionHistory {	
	private short [] correction = null;
	
	public static final int SIZE_OF_CORR_HIST = 65536;
	
	public static final int CORR_HIST_MASK = (int)(Long.highestOneBit(SIZE_OF_CORR_HIST)-1);
		
	public CorrectionHistory() {
		correction = new short[SIZE_OF_CORR_HIST];
		Arrays.fill(correction, Short.MAX_VALUE);
	}
	
	public synchronized short get(int hash, boolean onMoveIsWhite) {
		int index = hash & CORR_HIST_MASK;
		short score = correction[index];
		if (score != Short.MAX_VALUE) {
			// Score saved in table is from white point of view
			if (!onMoveIsWhite) {
				score = (short)-score;
			}
		}
		return score;
	}
	
	public synchronized void put(int hash, short score, boolean onMoveIsWhite) {
		int index = hash & CORR_HIST_MASK;
		// Score saved in table is from white point of view
		if (!onMoveIsWhite) {
			score = (short)-score;
		}
		correction[index] = score;
	}
}
