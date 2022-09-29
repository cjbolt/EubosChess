package eubos.score;

import java.util.Arrays;

import eubos.main.EubosEngineMain;

public class PawnEvalHashTable {	
	private short [] eval = null;
	private long [] black = null;
	private long [] white = null;
	
	static final int RANGE_TO_SEARCH = 20;
	
	public PawnEvalHashTable() {
		eval = new short[65536];
		black = new long[65536];
		white = new long[65536];
		Arrays.fill(eval, Short.MAX_VALUE);
	}
	
	public synchronized short get(int hash, long white_pawns, long black_pawns, boolean onMoveIsWhite) {
		int index = hash & 0xFFFF;
		for (int i=index; (i < index+RANGE_TO_SEARCH) && (i < 0xFFFF); i++) {
			if (black[i] == black_pawns && white[i] == white_pawns) {
				int score = eval[i];
				// Score saved in table is from white point of view
				if (!onMoveIsWhite) {
					score = -score;
				}
				return (short)score;
			}
		}
		return Short.MAX_VALUE;
	}
	
	public synchronized void put(int hash, int score, long white_pawns, long black_pawns, boolean onMoveIsWhite) {
		int index = hash & 0xFFFF;
		// Score saved in table is from white point of view
		if (!onMoveIsWhite) {
			score = -score;
		}
		for (int i=index; (i < index+RANGE_TO_SEARCH) && (i < 0xFFFF); i++) {
			// If exact hash match, overwrite entry in table
			if (black[i] == black_pawns && white[i] == white_pawns) {
				eval[i] = (short)score;
				return;
			}
			// Try to find a free slot near the hash index
			else if (eval[i] == Short.MAX_VALUE) {
				// Store
				black[i] = black_pawns;
				white[i] = white_pawns;
				eval[i] = (short)score;
				return;
			}
		}
		black[index] = black_pawns;
		white[index] = white_pawns;
		eval[index] = (short)score;
	}
}
