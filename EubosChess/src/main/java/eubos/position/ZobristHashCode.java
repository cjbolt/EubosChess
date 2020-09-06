package eubos.position;

import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.Stack;

import com.fluxchess.jcpi.models.IntChessman;
import com.fluxchess.jcpi.models.IntFile;

import eubos.board.Piece;
import eubos.board.Piece.Colour;

public class ZobristHashCode {
	
	public long hashCode;
	
	private static final int NUM_COLOURS = 2;
	private static final int NUM_PIECES = 6;
	private static final int NUM_SQUARES = 64;
	// One entry for each piece at each square for each colour.
	private static final int INDEX_WHITE = 0;
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
	
	private static final int INDEX_PAWN = 0;
	private static final int INDEX_KNIGHT = 1;
	private static final int INDEX_BISHOP = 2;
	private static final int INDEX_ROOK = 3;
	private static final int INDEX_QUEEN = 4;
	private static final int INDEX_KING = 5;
	
	private IPositionAccessors pos;
	
	private Stack<Integer> prevEnPassantFile = null;
	private int prevCastlingMask = 0;
		
	static private final long prnLookupTable[] = new long[LENGTH_TABLE];
	static {
		// Set up the pseudo random number lookup table that shall be used
		Random randGen = new Random();
		for (int index = 0; index < prnLookupTable.length; index++) 
				// TODO: investigate using a better PRN generator here...
				prnLookupTable[index] = randGen.nextLong();
	};

	public ZobristHashCode(IPositionAccessors pm) {
		pos = pm;
		prevEnPassantFile = new Stack<Integer>();
		generate();
	}
	
	// Generate a hash code for a position from scratch
	private long generate() {
		// add pieces
		hashCode = 0;
		PrimitiveIterator.OfInt iter = pos.getTheBoard().iterator();
		while (iter.hasNext()) {
			int pieceSq = iter.nextInt();
			hashCode ^= getPrnForPiece(pieceSq, pos.getTheBoard().getPieceAtSquare(pieceSq));
		}
		// add castling
		prevCastlingMask = pos.getCastlingFlags();	
		if ((prevCastlingMask & PositionManager.WHITE_KINGSIDE)==PositionManager.WHITE_KINGSIDE)
			hashCode ^= prnLookupTable[INDEX_WHITE_KSC];
		if ((prevCastlingMask & PositionManager.WHITE_QUEENSIDE)==PositionManager.WHITE_QUEENSIDE)
			hashCode ^= prnLookupTable[INDEX_WHITE_QSC];
		if ((prevCastlingMask & PositionManager.BLACK_KINGSIDE)==PositionManager.BLACK_KINGSIDE)
			hashCode ^= prnLookupTable[INDEX_BLACK_KSC];
		if ((prevCastlingMask & PositionManager.BLACK_QUEENSIDE)==PositionManager.BLACK_QUEENSIDE)
			hashCode ^= prnLookupTable[INDEX_BLACK_QSC];
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

	protected long getPrnForPiece(int pos, int currPiece) {
		// compute prnLookup index to use, based on piece type, colour and square.
		int lookupIndex = INDEX_WHITE;
		int atFile = Position.getFile(pos);
		int atRank = Position.getRank(pos);
		int pieceType = INDEX_PAWN; // Default
		if (Piece.isKnight(currPiece)) {
			pieceType = INDEX_KNIGHT;
		} else if (Piece.isBishop(currPiece)) {
			pieceType = INDEX_BISHOP;
		} else if (Piece.isRook(currPiece)) {
			pieceType = INDEX_ROOK;
		} else if (Piece.isQueen(currPiece)) {
			pieceType = INDEX_QUEEN;
		} else if (Piece.isKing(currPiece)) {
			pieceType = INDEX_KING;
		}
		lookupIndex = atFile + atRank * 8 + pieceType * NUM_SQUARES;
		if (Piece.isBlack(currPiece))
			lookupIndex += INDEX_BLACK;
		
		return prnLookupTable[lookupIndex];
	}
	
	// Used to update the Zobrist hash code whenever a position changes due to a move being performed
	public void update(int move, CaptureData captureTarget, int enPassantFile) {
		int piece = Move.getOriginPiece(move);
		doBasicMove(move, piece);
		doCapturedPiece(captureTarget);
		doEnPassant(enPassantFile);
     	doSecondaryMove(move, piece);
		doCastlingFlags();
		doOnMove();
	}

	private int convertChessmanToPiece(int chessman) {
		int eubosPiece = Piece.NONE;
		if (chessman==IntChessman.KNIGHT)
			eubosPiece = (Colour.isBlack(pos.getOnMove())) ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT;
		else if (chessman==IntChessman.BISHOP)
			eubosPiece = (Colour.isBlack(pos.getOnMove())) ? Piece.WHITE_BISHOP : Piece.BLACK_BISHOP;
		else if (chessman==IntChessman.ROOK)
			eubosPiece = (Colour.isBlack(pos.getOnMove())) ? Piece.WHITE_ROOK : Piece.BLACK_ROOK;
		else if (chessman==IntChessman.QUEEN)
			eubosPiece = (Colour.isBlack(pos.getOnMove())) ? Piece.WHITE_QUEEN : Piece.BLACK_QUEEN;
		return eubosPiece;
	}
	
	protected int doBasicMove(int move, int piece) {
		int promotedChessman = Move.getPromotion(move);
		if (promotedChessman == IntChessman.NOCHESSMAN) {
			// Basic move only
			hashCode ^= getPrnForPiece(Move.getTargetPosition(move), piece);
			hashCode ^= getPrnForPiece(Move.getOriginPosition(move), piece);
		} else {
			// Promotion
			if (Piece.isPawn(piece)) {
				// is undoing promotion
				int promotedToPiece = convertChessmanToPiece(promotedChessman);
				hashCode ^= getPrnForPiece(Move.getTargetPosition(move), piece);
				hashCode ^= getPrnForPiece(Move.getOriginPosition(move), promotedToPiece);
				piece = promotedToPiece;
			} else if (Piece.isKnight(piece) ||
					   Piece.isBishop(piece) || 
					   Piece.isRook(piece) || 
					   Piece.isQueen(piece)) {
				// is doing a promotion
				int unpromotedPawn = Colour.isWhite(pos.getOnMove()) ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
				hashCode ^= getPrnForPiece(Move.getTargetPosition(move), piece);
				hashCode ^= getPrnForPiece(Move.getOriginPosition(move), unpromotedPawn);
			}
		}
		return piece;
	}

	protected void doCapturedPiece(CaptureData captureTarget) {
		if (captureTarget.target != Piece.NONE)
			hashCode ^= getPrnForPiece(captureTarget.square, captureTarget.target);
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
		int currentCastlingFlags = pos.getCastlingFlags();
		int delta = currentCastlingFlags ^ this.prevCastlingMask;
		if (delta != 0)
		{
			if ((delta & PositionManager.WHITE_KINGSIDE)==PositionManager.WHITE_KINGSIDE)
			{
				hashCode ^= prnLookupTable[INDEX_WHITE_KSC];
			}
			if ((delta & PositionManager.WHITE_QUEENSIDE)==PositionManager.WHITE_QUEENSIDE) {
				hashCode ^= prnLookupTable[INDEX_WHITE_QSC];
			}
			if ((delta & PositionManager.BLACK_KINGSIDE)==PositionManager.BLACK_KINGSIDE) {
				hashCode ^= prnLookupTable[INDEX_BLACK_KSC];
			}
			if ((delta & PositionManager.BLACK_QUEENSIDE)==PositionManager.BLACK_QUEENSIDE) {
				hashCode ^= prnLookupTable[INDEX_BLACK_QSC];
			}
		}
		this.prevCastlingMask = currentCastlingFlags;
	}

	protected void doSecondaryMove(int move, int piece) {
		if ( piece==Piece.WHITE_KING ) {
			if (Move.areEqual(move,CastlingManager.wksc)) {
				piece = pos.getTheBoard().getPieceAtSquare(Position.f1);
				hashCode ^= getPrnForPiece(Position.f1, piece); // to
				hashCode ^= getPrnForPiece(Position.h1, piece); // from
			}
			else if (Move.areEqual(move,CastlingManager.wqsc)) {
				piece = pos.getTheBoard().getPieceAtSquare(Position.d1);
				hashCode ^= getPrnForPiece(Position.d1, piece); // to
				hashCode ^= getPrnForPiece(Position.a1, piece); // from
			}
			else if (Move.areEqual(move,CastlingManager.undo_wksc))
			{
				piece = pos.getTheBoard().getPieceAtSquare(Position.h1);
				hashCode ^= getPrnForPiece(Position.h1, piece); // to
				hashCode ^= getPrnForPiece(Position.f1, piece); // from
			}
			else if (Move.areEqual(move,CastlingManager.undo_wqsc)) {
				piece = pos.getTheBoard().getPieceAtSquare(Position.a1);
				hashCode ^= getPrnForPiece(Position.a1, piece); // to
				hashCode ^= getPrnForPiece(Position.d1, piece); // from
			}
		} else if (piece==Piece.BLACK_KING) {
			if (Move.areEqual(move,CastlingManager.bksc)) {
				piece = pos.getTheBoard().getPieceAtSquare(Position.f8);
				hashCode ^= getPrnForPiece(Position.f8, piece); // to
				hashCode ^= getPrnForPiece(Position.h8, piece); // from
			}
			else if (Move.areEqual(move,CastlingManager.bqsc)) {
				piece = pos.getTheBoard().getPieceAtSquare(Position.d8);
				hashCode ^= getPrnForPiece(Position.d8, piece); // to
				hashCode ^= getPrnForPiece(Position.a8, piece); // from
			}
			else if (Move.areEqual(move,CastlingManager.undo_bksc)) {
				piece = pos.getTheBoard().getPieceAtSquare(Position.h8);
				hashCode ^= getPrnForPiece(Position.h8, piece); // to
				hashCode ^= getPrnForPiece(Position.f8, piece); // from
			}
			else if (Move.areEqual(move,CastlingManager.undo_bqsc)) {
				piece = pos.getTheBoard().getPieceAtSquare(Position.a8);
				hashCode ^= getPrnForPiece(Position.a8, piece); // to
				hashCode ^= getPrnForPiece(Position.d8, piece); // from
			}
		} else {
			// not actually a castle move
		}
	}
}
