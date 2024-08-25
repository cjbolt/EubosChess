package eubos.score;

import java.util.Arrays;
import java.util.Random;

import eubos.board.IForEachPieceCallback;
import eubos.board.Piece;
import eubos.position.IPawnHash;
import eubos.position.IPositionAccessors;

public class PawnEvalHashTable implements IForEachPieceCallback, IPawnHash {
	
	private static final int NUM_COLOURS = 2;
	private static final int NUM_SQUARES = 64;
	// One entry pawn at each square for each colour.
	private static final int INDEX_BLACK = (NUM_SQUARES);
	private static final int LENGTH_TABLE = (NUM_COLOURS*NUM_SQUARES);

	static private final short prnLookupTable[] = new short[LENGTH_TABLE];
	static {
		// Set up the pseudo random number lookup table that shall be used
		Random randGen = new Random(0xDEAD);
		for (int index = 0; index < prnLookupTable.length; index++) {
			prnLookupTable[index] = (short)randGen.nextLong();
		}
	};
	
	public short hashCode = 0;
	public short getPawnHash() {
		return hashCode;
	}
	
	public short calculatePawnHash(IPositionAccessors pos) {
		hashCode = 0;
		pos.getTheBoard().forEachPawnOfSide(this, false);
		pos.getTheBoard().forEachPawnOfSide(this, true);
		return hashCode;
	}
	
	@Override
	public void callback(int piece, int bitOffset) { 
		hashCode ^= getPrnForPawn(bitOffset, piece);
	}
	
	@Override
	public boolean condition_callback(int piece, int atPos) {
		return false;
	}
	
	protected long getPrnForPawn(int bitOffset, int pawn) {
		int lookupIndex = bitOffset;
		if (Piece.isBlack(pawn)) {
			lookupIndex += INDEX_BLACK;
		}		
		return prnLookupTable[lookupIndex];
	}
	
	public void removePawn(int pawn, int at) {
		hashCode ^= getPrnForPawn(at, pawn);
	}
	
	public void addPawn(int pawn, int at) {
		hashCode ^= getPrnForPawn(at, pawn);
	}
	
	public void movePawn(int pawn, int from, int to) {
		hashCode ^= getPrnForPawn(from, pawn);
		hashCode ^= getPrnForPawn(to, pawn);
	}
	
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
