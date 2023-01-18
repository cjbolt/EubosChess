package eubos.position;

import java.util.Arrays;
import java.util.Random;

import com.fluxchess.jcpi.models.IntRank;

import eubos.board.BitBoard;
import eubos.board.IForEachPieceCallback;
import eubos.board.Piece;

public class ZobristHashCode implements IForEachPieceCallback {
	
	public long hashCode;
	
	private static final int NUM_COLOURS = 2;
	private static final int NUM_PIECES = 6;
	private static final int NUM_SQUARES = 64;
	// One entry for each piece at each square for each colour.
	private static final int INDEX_BLACK = (NUM_PIECES*NUM_SQUARES);
	// One entry indicating that the side to move is black
	private static final int INDEX_SIDE_TO_MOVE = (NUM_COLOURS*NUM_PIECES*NUM_SQUARES);
	// Four entries for castling rights
	private static final int INDEX_WHITE_KSC = INDEX_SIDE_TO_MOVE+1;
	private static final int INDEX_WHITE_QSC = INDEX_WHITE_KSC+1;
	private static final int INDEX_BLACK_KSC = INDEX_WHITE_QSC+1;
	private static final int INDEX_BLACK_QSC = INDEX_BLACK_KSC+1;
    // Right entries for the en passant file, if en passant move is legal
	private static final int INDEX_ENP_A = INDEX_BLACK_QSC+1;
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
	
	private int piece;
	private int originSquare;
	private int targetSquare;
	private int targetPiece;
	private int promotedPiece;
		
	static private final long prnLookupTable[] = new long[LENGTH_TABLE];
	static {
		// Set up the pseudo random number lookup table that shall be used
		Random randGen = new Random();
		for (int index = 0; index < prnLookupTable.length; index++) {
			prnLookupTable[index] = randGen.nextLong();
		}
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
		updateCastling(prevCastlingMask);
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
	
	protected long getPrnForRook(int bitOffset, boolean isBlack) {
		int lookupIndex = bitOffsetToZobristIndex_Lut[bitOffset] + (Piece.ROOK - 1) * NUM_SQUARES;
		if (isBlack) {
			lookupIndex += INDEX_BLACK;
		}		
		return prnLookupTable[lookupIndex];
	}
	
	// Used to update the Zobrist hash code whenever a position changes due to a move being performed
	public void update(int move, int capturedPieceSquare, byte enPassantOffset) {
		// Unpack move
		piece = Move.getOriginPiece(move);
		originSquare = Move.getOriginPosition(move);
		targetSquare = Move.getTargetPosition(move);
		targetPiece = Move.getTargetPiece(move);
		promotedPiece = Move.getPromotion(move);
		// Update
		doBasicMove();
		doCapturedPiece(capturedPieceSquare);
		doEnPassant(enPassantOffset);
     	doSecondaryMove(move);
		doCastlingFlags();
		doOnMove();
	}
	
	// Used to update the Zobrist hash code whenever a position changes due to a move being performed
	public void updateNullMove(byte enPassantOffset) {
		// Update
		doEnPassant(enPassantOffset);
		doOnMove();
	}
	
	protected void doBasicMove() {
		if (promotedPiece == Piece.NONE) {
			// Basic move only
			hashCode ^= getPrnForPiece(targetSquare, piece);
			hashCode ^= getPrnForPiece(originSquare, piece);
		} else {
			// Promotion - first set the colour bit flag
			promotedPiece = Piece.isWhite(piece) ? promotedPiece : Piece.BLACK|promotedPiece;
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
	}

	protected void doCapturedPiece(int capturedPieceSquare) {
		if (targetPiece != Piece.NONE)
			hashCode ^= getPrnForPiece(capturedPieceSquare, targetPiece);
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
		if (delta != 0) {
			updateCastling(delta);
		}
		this.prevCastlingMask = currentCastlingFlags;
	}

	private void updateCastling(int delta) {
		if ((delta & CastlingManager.WHITE_KINGSIDE)==CastlingManager.WHITE_KINGSIDE) {
			hashCode ^= prnLookupTable[INDEX_WHITE_KSC];
		}
		if ((delta & CastlingManager.WHITE_QUEENSIDE)==CastlingManager.WHITE_QUEENSIDE) {
			hashCode ^= prnLookupTable[INDEX_WHITE_QSC];
		}
		if ((delta & CastlingManager.BLACK_KINGSIDE)==CastlingManager.BLACK_KINGSIDE) {
			hashCode ^= prnLookupTable[INDEX_BLACK_KSC];
		}
		if ((delta & CastlingManager.BLACK_QUEENSIDE)==CastlingManager.BLACK_QUEENSIDE) {
			hashCode ^= prnLookupTable[INDEX_BLACK_QSC];
		}
	}

	protected void doSecondaryMove(int move) {
		if (piece == Piece.WHITE_KING) {
			if (originSquare == BitBoard.e1) {
				if (targetSquare == BitBoard.g1) {
					hashCode ^= getPrnForRook(BitBoard.f1, false); // to
					hashCode ^= getPrnForRook(BitBoard.h1, false); // from
				} else if (targetSquare == BitBoard.c1) {
					hashCode ^= getPrnForRook(BitBoard.d1, false); // to
					hashCode ^= getPrnForRook(BitBoard.a1, false); // from
				}
			} else if (originSquare == BitBoard.g1) {
				if (targetSquare == BitBoard.e1) {
					hashCode ^= getPrnForRook(BitBoard.h1, false); // to
					hashCode ^= getPrnForRook(BitBoard.f1, false); // from
				}
			} else if (originSquare == BitBoard.c1) {
				if (targetSquare == BitBoard.e1) {
					hashCode ^= getPrnForRook(BitBoard.a1, false); // to
					hashCode ^= getPrnForRook(BitBoard.d1, false); // from
				}
			}
		} else if (piece == Piece.BLACK_KING) {
			if (originSquare == BitBoard.e8) {
				if (targetSquare == BitBoard.g8) {
					hashCode ^= getPrnForRook(BitBoard.f8, true); // to
					hashCode ^= getPrnForRook(BitBoard.h8, true); // from
				} else if (targetSquare == BitBoard.c8) {
					hashCode ^= getPrnForRook(BitBoard.d8, true); // to
					hashCode ^= getPrnForRook(BitBoard.a8, true); // from
				} 
			} else if (originSquare == BitBoard.g8) {
				if (targetSquare == BitBoard.e8) {
					hashCode ^= getPrnForRook(BitBoard.h8, true); // to
					hashCode ^= getPrnForRook(BitBoard.f8, true); // from
				}
			} else if (originSquare == BitBoard.c8) {
				if (targetSquare == BitBoard.e8) {
					hashCode ^= getPrnForRook(BitBoard.a8, true); // to
					hashCode ^= getPrnForRook(BitBoard.d8, true); // from
				}
			}
		} else {
			// cannot be a castling move
		}
	}
}
