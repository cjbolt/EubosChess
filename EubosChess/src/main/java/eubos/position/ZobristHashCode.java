package eubos.position;

import java.util.Random;

import com.fluxchess.jcpi.models.IntRank;

import eubos.board.BitBoard;
import eubos.board.IForEachPieceCallback;
import eubos.board.Piece;

public class ZobristHashCode implements IForEachPieceCallback, IZobristUpdate {
	
	public long hashCode;
	
	private static final int NUM_COLOURS = 2;
	private static final int NUM_PIECES = 6;
	private static final int NUM_SQUARES = 64;
	// One entry for each piece at each square for each colour.
	private static final int INDEX_BLACK = (NUM_PIECES*NUM_SQUARES);
	// One entry indicating that the side to move is black
	private static final int INDEX_SIDE_TO_MOVE = (NUM_COLOURS*NUM_PIECES*NUM_SQUARES);
	// 16 entries for castling rights
	private static final int INDEX_COMBINED_CASTLING = INDEX_SIDE_TO_MOVE+1;
    // Right entries for the en passant file, if en passant move is legal
	private static final int INDEX_ENP_A = INDEX_COMBINED_CASTLING+16;
	private static final int INDEX_ENP_B = INDEX_ENP_A+1;
	private static final int INDEX_ENP_C = INDEX_ENP_B+1;
	private static final int INDEX_ENP_D = INDEX_ENP_C+1;
	private static final int INDEX_ENP_E = INDEX_ENP_D+1;
	private static final int INDEX_ENP_F = INDEX_ENP_E+1;
	private static final int INDEX_ENP_G = INDEX_ENP_F+1;
	private static final int INDEX_ENP_H = INDEX_ENP_G+1;
	private static final int LENGTH_TABLE = INDEX_ENP_H+1;
	
	static private final long prnLookupTable[] = new long[LENGTH_TABLE];
	static {
		// Set up the pseudo random number lookup table that shall be used
		Random randGen = new Random(0xDEAD);
		for (int index = 0; index < prnLookupTable.length; index++) {
			prnLookupTable[index] = randGen.nextLong();
		}
		// Note: If there is no delta in the castling flags, then an xor with zero will result in no change
		// to the hash code
		prnLookupTable[INDEX_COMBINED_CASTLING] = 0;
	};

	public ZobristHashCode(IPositionAccessors pos, CastlingManager castling) {
		// Add Pieces
		hashCode = 0;
		pos.getTheBoard().forEachPiece(this);
		// Add Castling
		hashCode ^= prnLookupTable[INDEX_COMBINED_CASTLING+castling.getFlags()];
		// add on move
		if (!pos.onMoveIsWhite()) {
			doOnMove();
		}
		// Add En passant
		int enPassant = pos.getTheBoard().getEnPassantTargetSq();
		if (enPassant != BitBoard.INVALID) {
			hashCode ^= prnLookupTable[INDEX_ENP_A+BitBoard.getFile(enPassant)];
		}
	}
	
	@Override
	public void callback(int piece, int bitOffset) { 
		hashCode ^= getPrnForPiece(bitOffset, piece);
	}
	
	@Override
	public boolean condition_callback(int piece, int atPos) {
		return false;
	}
	
	protected long getPrnForPiece(int bitOffset, int currPiece) {
		/* Compute prnLookup index to use, based on piece type, colour and square.
		 * Note: convert piece type to Zobrist index, which is 0 to 5. The max is not 
		 * theoretically required, but I suspect hash collisions can cause very 
		 * infrequent crashes in this code otherwise. */
		int pieceType = Math.max(0, (currPiece & Piece.PIECE_NO_COLOUR_MASK) - 1);
		int lookupIndex = pieceType * NUM_SQUARES + bitOffset;
		if (Piece.isBlack(currPiece)) {
			lookupIndex += INDEX_BLACK;
		}		
		return prnLookupTable[lookupIndex];
	}

	public void doEnPassant(int oldEnPassantOffset, int newEnPassantOffset) {
		if (oldEnPassantOffset != BitBoard.INVALID) {
			hashCode ^= prnLookupTable[INDEX_ENP_A+BitBoard.getFile(oldEnPassantOffset)];
		}
		if (newEnPassantOffset != BitBoard.INVALID) {
			hashCode ^= prnLookupTable[INDEX_ENP_A+BitBoard.getFile(newEnPassantOffset)];
		}
	}

	public void doOnMove() {
	    hashCode ^= prnLookupTable[INDEX_SIDE_TO_MOVE];
	}

	public void doCastlingFlags(int oldFlags, int newFlags) {
		int delta = oldFlags ^ newFlags;
		hashCode ^= prnLookupTable[INDEX_COMBINED_CASTLING+delta];
	}

	@Override
	public void doBasicMove(int targetSquare, int originSquare, int piece) {
		hashCode ^= getPrnForPiece(targetSquare, piece);
		hashCode ^= getPrnForPiece(originSquare, piece);
	}

	@Override
	public void doPromotionMove(int targetSquare, int originSquare, int piece, int promotedPiece) {
		if ((BitBoard.getRank(targetSquare) == IntRank.R1) ||
			(BitBoard.getRank(targetSquare) == IntRank.R8)) {
			// is doing a promotion
			hashCode ^= getPrnForPiece(targetSquare, promotedPiece);
			hashCode ^= getPrnForPiece(originSquare, piece);
		} else {
			// is undoing promotion
			hashCode ^= getPrnForPiece(targetSquare, piece);
			hashCode ^= getPrnForPiece(originSquare, promotedPiece);
		}
	}
	
	@Override
	public void doCapturedPiece(int capturedPieceSquare, int targetPiece) {
		hashCode ^= getPrnForPiece(capturedPieceSquare, targetPiece);
	}
}
