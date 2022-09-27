package eubos.score;

import java.util.Arrays;

import eubos.main.EubosEngineMain;

public class PawnEvalHashTable {	
	private short [] eval = null;
	private long [] black = null;
	private long [] white = null;
	
	public PawnEvalHashTable() {
		eval = new short[65536];
		black = new long[65536];
		white = new long[65536];
		Arrays.fill(eval, Short.MAX_VALUE);
	}
	
	public synchronized short get(int hash, long white_pawns, long black_pawns, boolean onMoveIsWhite) {
		if (EubosEngineMain.ENABLE_ASSERTS) assert ((hash >>> 16) == 0);
		if (black[hash] == black_pawns && white[hash] == white_pawns) {
			int score = eval[hash];
			// Score saved in table is from white point of view
			if (!onMoveIsWhite) {
				score = -score;
			}
			return (short)score;
		}
		return Short.MAX_VALUE;
	}
	
	public synchronized void put(int hash, int score, long white_pawns, long black_pawns, boolean onMoveIsWhite) {
		if (EubosEngineMain.ENABLE_ASSERTS) assert ((hash >>> 16) == 0);
		if (EubosEngineMain.ENABLE_ASSERTS) assert (score <= Short.MAX_VALUE && score >= Short.MIN_VALUE);
		// Score saved in table is from white point of view
		if (!onMoveIsWhite) {
			score = -score;
		}
		// Store
		black[hash] = black_pawns;
		white[hash] = white_pawns;
		eval[hash] = (short)score;
	}
}
