package eubos.score;

public class PawnEvalHashTable {	
	
	class PawnHashEntry {
		long black;
		long white;
		short phase;
		short score;
	};
	
	PawnHashEntry[] table;
	
	public static final int WHITE_SCALING_FOR_UPPER = (4*8)-8; // most significant 4 bytes of white pawns in upper
	public static final int WHITE_SCALING_FOR_LOWER = (6*8)-8; // least significant 2 bytes of white pawns in lower
	public static final int BLACK_SCALING_FOR_LOWER = 8;       // all 6 bytes of black pawns in lower (shift out 1st rank)
	
	public static final int SIZE_OF_PAWN_HASH = 65536;
	
	public static final int PAWN_HASH_MASK = (int)(Long.highestOneBit(SIZE_OF_PAWN_HASH)-1);
		
	public PawnEvalHashTable() {
		table = new PawnHashEntry[SIZE_OF_PAWN_HASH];
		for (int i=0; i<SIZE_OF_PAWN_HASH; i++) {
			table[i] = new PawnHashEntry();
		}
	}
	
	public synchronized short get(int hash, int phase_weighting, long white_pawns, long black_pawns, boolean onMoveIsWhite) {
		PawnHashEntry entry = table[hash & PAWN_HASH_MASK];
		if (entry.black == black_pawns && entry.white == white_pawns && entry.phase == phase_weighting) {
			short score = entry.score;
			if (!onMoveIsWhite) {
				// Score saved in table is from white point of view
				score = (short)-score;
			}
			return score;
		}
		return Short.MAX_VALUE;
	}
	
	public synchronized void put(int hash, int phase_weighting, int score, long white_pawns, long black_pawns, boolean onMoveIsWhite) {
		PawnHashEntry entry = table[hash & PAWN_HASH_MASK];
		if (!onMoveIsWhite) {
			// Score saved in table is from white point of view
			score = -score;
		}
		entry.black = black_pawns;
		entry.white = white_pawns;
		entry.phase = (short)phase_weighting;
		entry.score = (short)score;
	}
}
