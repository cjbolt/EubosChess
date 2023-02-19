package eubos.position;

import java.util.Arrays;
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
	
	private IPositionAccessors pos;
	private CastlingManager castling;
	
	int index = 0;
	private byte[] prevEnPassantFile;
	private int prevCastlingMask = 0;
		
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

	public ZobristHashCode(IPositionAccessors pm, CastlingManager castling) {
		pos = pm;
		this.castling = castling;
		prevEnPassantFile = new byte[16]; // can't be more en passant moves than the number of pawns!
		Arrays.fill(prevEnPassantFile, (byte)8);
		generate();
	}
	
	@Override
	public void callback(int piece, int bitOffset) { 
		hashCode ^= getPrnForPiece(bitOffset, piece);
	}
	
	@Override
	public boolean condition_callback(int piece, int atPos) {
		return false;
	}
	
	// Generate a hash code for a position from scratch
	private long generate() {
		// add pieces
		hashCode = 0;
		index = 0;
		pos.getTheBoard().forEachPiece(this);
		// add castling
		prevCastlingMask = castling.getFlags();
		hashCode ^= prnLookupTable[INDEX_COMBINED_CASTLING+prevCastlingMask];
		// add on move
		if (!pos.onMoveIsWhite()) {
			doOnMove();
		}
		// add en passant
		int enPassant = pos.getTheBoard().getEnPassantTargetSq();
		if (enPassant != BitBoard.INVALID) {
			int enPassantFile = BitBoard.getFile(enPassant);
			prevEnPassantFile[++index] = (byte)enPassantFile;
			hashCode ^= prnLookupTable[(INDEX_ENP_A+enPassantFile)];
		}
		return hashCode;
	}
	
	static final int [] bitOffsetToZobristIndex_Lut = new int[64];
	static {
		for (int pos : Position.values) {
			int atFile = Position.getFile(pos);
			int atRank = Position.getRank(pos);
			int lookupIndex = atFile + atRank * 8;
			bitOffsetToZobristIndex_Lut[BitBoard.positionToBit_Lut[pos]] = lookupIndex;
		}
	}
			
	protected long getPrnForPiece(int bitOffset, int currPiece) {
		// compute prnLookup index to use, based on piece type, colour and square.
		int pieceType = (currPiece & Piece.PIECE_NO_COLOUR_MASK) - 1; // convert piece type to Zobrist index
		int lookupIndex = bitOffsetToZobristIndex_Lut[bitOffset] + pieceType * NUM_SQUARES;
		if (Piece.isBlack(currPiece)) {
			lookupIndex += INDEX_BLACK;
		}		
		return prnLookupTable[lookupIndex];
	}
	
	// Used to update the Zobrist hash code whenever a position changes due to a move being performed
	public void update(byte enPassantOffset) {
		// Update
		doEnPassant(enPassantOffset);
		doCastlingFlags();
		doOnMove();
	}
	
	// Used to update the Zobrist hash code whenever a position changes due to a move being performed
	public void updateNullMove(byte enPassantOffset) {
		// Update
		doEnPassant(enPassantOffset);
		doOnMove();
	}

	private void setTargetFile(byte enPasFile) {
		if (index > 0) {
			clearTargetFile();
		}
		prevEnPassantFile[++index] = enPasFile;
		hashCode ^= prnLookupTable[INDEX_ENP_A+enPasFile];
	}
	
	private void clearTargetFile() {
		int enPasFile = prevEnPassantFile[index--];
		hashCode ^= prnLookupTable[INDEX_ENP_A+enPasFile];
	}
	
	protected void doEnPassant(byte enPassantOffset) {
		if (enPassantOffset != BitBoard.INVALID) {
			setTargetFile(BitBoard.getFile(enPassantOffset));
		} else if (index > 0) {
			clearTargetFile();
		} else {
			// no action needed
		}
	}

	protected void doOnMove() {
	    hashCode ^= prnLookupTable[INDEX_SIDE_TO_MOVE];
	}

	protected void doCastlingFlags() {
		int currentCastlingFlags = castling.getFlags();
		int delta = currentCastlingFlags ^ this.prevCastlingMask;
		hashCode ^= prnLookupTable[INDEX_COMBINED_CASTLING+delta];
		this.prevCastlingMask = currentCastlingFlags;
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
