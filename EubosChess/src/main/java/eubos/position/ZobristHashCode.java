package eubos.position;

import java.util.Random;
import java.util.Stack;

import com.fluxchess.jcpi.models.IntFile;
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
	
	private Stack<Integer> prevEnPassantFile = null;
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
		prevEnPassantFile = new Stack<Integer>();
		generate();
	}
	
	@Override
	public void callback(int piece, int atPos) { 
		hashCode ^= getPrnForPiece(BitBoard.positionToBit_Lut[atPos], piece);
	}
	
	@Override
	public boolean condition_callback(int piece, int atPos) {
		return false;
	}
	
	// Generate a hash code for a position from scratch
	private long generate() {
		// add pieces
		hashCode = 0;
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
		if (enPassant!=Position.NOPOSITION) {
			int enPassantFile = Position.getFile(enPassant);
			prevEnPassantFile.push(enPassantFile);
			hashCode ^= prnLookupTable[(INDEX_ENP_A+enPassantFile)];
		}
		return hashCode;
	}
	
	static final int [] positionToZobristIndex_Lut = new int[64];
	static {
		for (int pos : Position.values) {
			int atFile = Position.getFile(pos);
			int atRank = Position.getRank(pos);
			int lookupIndex = atFile + atRank * 8;
			positionToZobristIndex_Lut[BitBoard.positionToBit_Lut[pos]] = lookupIndex;
		}
	}
			
	protected long getPrnForPiece(int bitOffset, int currPiece) {
		// compute prnLookup index to use, based on piece type, colour and square.
		int pieceType = (currPiece & Piece.PIECE_NO_COLOUR_MASK) - 1; // convert piece type to Zobrist index
		int lookupIndex = positionToZobristIndex_Lut[bitOffset] + pieceType * NUM_SQUARES;
		if (Piece.isBlack(currPiece)) {
			lookupIndex += INDEX_BLACK;
		}		
		return prnLookupTable[lookupIndex];
	}
	
	protected long getPrnForRook(int bitOffset, boolean isBlack) {
		int lookupIndex = positionToZobristIndex_Lut[bitOffset] + (Piece.ROOK - 1) * NUM_SQUARES;
		if (isBlack) {
			lookupIndex += INDEX_BLACK;
		}		
		return prnLookupTable[lookupIndex];
	}
	
	// Used to update the Zobrist hash code whenever a position changes due to a move being performed
	public void update(int move, int capturedPieceSquare, int enPassantFile) {
		// Unpack move
		piece = Move.getOriginPiece(move);
		originSquare = Move.getOriginPosition(move);
		targetSquare = Move.getTargetPosition(move);
		targetPiece = Move.getTargetPiece(move);
		promotedPiece = Move.getPromotion(move);
		// Update
		doBasicMove();
		doCapturedPiece(capturedPieceSquare);
		doEnPassant(enPassantFile);
     	doSecondaryMove(move);
		doCastlingFlags();
		doOnMove();
	}
	
	// Used to update the Zobrist hash code whenever a position changes due to a move being performed
	public void updateNullMove(int enPassantFile) {
		// Update
		doEnPassant(enPassantFile);
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

	private void setTargetFile(int enPasFile) {
		if (!prevEnPassantFile.isEmpty()) {
			clearTargetFile();
		}
		prevEnPassantFile.push(enPasFile);
		hashCode ^= prnLookupTable[INDEX_ENP_A+enPasFile];
	}
	
	private void clearTargetFile() {
		hashCode ^= prnLookupTable[INDEX_ENP_A+prevEnPassantFile.pop()];
	}
	
	protected void doEnPassant(int enPassantFile) {
		if (enPassantFile != IntFile.NOFILE) {
			setTargetFile(enPassantFile);
		} else if (!prevEnPassantFile.isEmpty()) {
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
			if (originSquare == BitBoard.positionToBit_Lut[Position.e1]) {
				if (Move.areEqual(move, CastlingManager.wksc)) {
					hashCode ^= getPrnForRook(BitBoard.positionToBit_Lut[Position.f1], false); // to
					hashCode ^= getPrnForRook(BitBoard.positionToBit_Lut[Position.h1], false); // from
				} else if (Move.areEqual(move, CastlingManager.wqsc)) {
					hashCode ^= getPrnForRook(BitBoard.positionToBit_Lut[Position.d1], false); // to
					hashCode ^= getPrnForRook(BitBoard.positionToBit_Lut[Position.a1], false); // from
				}
			} else if (originSquare == BitBoard.positionToBit_Lut[Position.g1]) {
				if (Move.areEqual(move, CastlingManager.undo_wksc)) {
					hashCode ^= getPrnForRook(BitBoard.positionToBit_Lut[Position.h1], false); // to
					hashCode ^= getPrnForRook(BitBoard.positionToBit_Lut[Position.f1], false); // from
				}
			} else if (originSquare == BitBoard.positionToBit_Lut[Position.c1]) {
				if (Move.areEqual(move, CastlingManager.undo_wqsc)) {
					hashCode ^= getPrnForRook(BitBoard.positionToBit_Lut[Position.a1], false); // to
					hashCode ^= getPrnForRook(BitBoard.positionToBit_Lut[Position.d1], false); // from
				}
			}
		} else if (piece == Piece.BLACK_KING) {
			if (originSquare == BitBoard.positionToBit_Lut[Position.e8]) {
				if (Move.areEqual(move, CastlingManager.bksc)) {
					hashCode ^= getPrnForRook(BitBoard.positionToBit_Lut[Position.f8], true); // to
					hashCode ^= getPrnForRook(BitBoard.positionToBit_Lut[Position.h8], true); // from
				} else if (Move.areEqual(move, CastlingManager.bqsc)) {
					hashCode ^= getPrnForRook(BitBoard.positionToBit_Lut[Position.d8], true); // to
					hashCode ^= getPrnForRook(BitBoard.positionToBit_Lut[Position.a8], true); // from
				} 
			} else if (originSquare == BitBoard.positionToBit_Lut[Position.g8]) {
				if (Move.areEqual(move, CastlingManager.undo_bksc)) {
					hashCode ^= getPrnForRook(BitBoard.positionToBit_Lut[Position.h8], true); // to
					hashCode ^= getPrnForRook(BitBoard.positionToBit_Lut[Position.f8], true); // from
				}
			} else if (originSquare == BitBoard.positionToBit_Lut[Position.c8]) {
				if (Move.areEqual(move,CastlingManager.undo_bqsc)) {
					hashCode ^= getPrnForRook(BitBoard.positionToBit_Lut[Position.a8], true); // to
					hashCode ^= getPrnForRook(BitBoard.positionToBit_Lut[Position.d8], true); // from
				}
			}
		} else {
			// cannot be a castling move
		}
	}
}
