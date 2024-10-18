package eubos.position;

import java.util.Random;

import eubos.board.IForEachPieceCallback;
import eubos.board.Piece;

public class PawnHashCode implements IForEachPieceCallback, IPawnHash {
	
	private static final int NUM_COLOURS = 2;
	private static final int NUM_SQUARES = 64;
	// One entry pawn at each square for each colour.
	private static final int INDEX_BLACK = (NUM_SQUARES);
	private static final int LENGTH_TABLE = (NUM_COLOURS*NUM_SQUARES);

	static private final int prnLookupTable[] = new int[LENGTH_TABLE];
	static {
		// Set up the pseudo random number lookup table that shall be used
		Random randGen = new Random(0xDEAD);
		for (int index = 0; index < prnLookupTable.length; index++) {
			prnLookupTable[index] = randGen.nextInt();
		}
	};
	
	public int hashCode = 0;
	public int getPawnHash() {
		return hashCode;
	}
	
	public int calculatePawnHash(IPositionAccessors pos) {
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
}
