package eubos.position;

import java.util.Random;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.IntRank;

import eubos.board.pieces.Bishop;
import eubos.board.pieces.King;
import eubos.board.pieces.Knight;
import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Queen;
import eubos.board.pieces.Rook;

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
	private static final int LENGTH_TABLE = INDEX_ENP_H;
	
	private static final int INDEX_PAWN = 0;
	private static final int INDEX_KNIGHT = 1;
	private static final int INDEX_BISHOP = 2;
	private static final int INDEX_ROOK = 3;
	private static final int INDEX_QUEEN = 4;
	private static final int INDEX_KING = 5;
	private static final int INDEX_PIECE_ERROR = -1;
	
	private IPositionAccessors pos;
	
	private GenericFile prevEnPassantFile = null;
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
		try {
			this.generate();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// Generate a hash code for a position from scratch
	public long generate() throws Exception {
		// add pieces
		hashCode = 0;
		for (Piece currPiece : pos.getTheBoard()) {
			hashCode ^= getPrnForPiece(currPiece.getSquare(), currPiece);
		}
		// add castling
		prevCastlingMask = pos.getCastlingAvaillability();	
		if ((prevCastlingMask & PositionManager.WHITE_KINGSIDE)==PositionManager.WHITE_KINGSIDE)
			hashCode ^= prnLookupTable[INDEX_WHITE_KSC];
		if ((prevCastlingMask & PositionManager.WHITE_QUEENSIDE)==PositionManager.WHITE_QUEENSIDE)
			hashCode ^= prnLookupTable[INDEX_WHITE_QSC];
		if ((prevCastlingMask & PositionManager.BLACK_KINGSIDE)==PositionManager.BLACK_KINGSIDE)
			hashCode ^= prnLookupTable[INDEX_BLACK_KSC];
		if ((prevCastlingMask & PositionManager.BLACK_QUEENSIDE)==PositionManager.BLACK_QUEENSIDE)
			hashCode ^= prnLookupTable[INDEX_BLACK_QSC];
		// add on move
		if (pos.getOnMove()==Piece.Colour.black) {
			hashCode ^= prnLookupTable[INDEX_SIDE_TO_MOVE];
		}
		// add en passant
		GenericPosition enPassant = pos.getTheBoard().getEnPassantTargetSq();
		if (enPassant!=null) {
			prevEnPassantFile = enPassant.file;
			int enPassantFile = IntFile.valueOf(enPassant.file);
			hashCode ^= prnLookupTable[(INDEX_ENP_A+enPassantFile)];
		}
		return hashCode;
	}

	protected long getPrnForPiece(GenericPosition pos, Piece currPiece) throws Exception {
		// compute prnLookup index to use, based on piece type, colour and square.
		int lookupIndex=INDEX_WHITE;
		int atFile = IntFile.valueOf(pos.file);
		int atRank = IntRank.valueOf(pos.rank);
		int pieceType = INDEX_PIECE_ERROR;
		if (currPiece instanceof Pawn) {
			pieceType = INDEX_PAWN;
		} else if (currPiece instanceof Knight) {
			pieceType = INDEX_KNIGHT;
		} else if (currPiece instanceof Bishop) {
			pieceType = INDEX_BISHOP;
		} else if (currPiece instanceof Rook) {
			pieceType = INDEX_ROOK;
		} else if (currPiece instanceof Queen) {
			pieceType = INDEX_QUEEN;
		} else if (currPiece instanceof King) {
			pieceType = INDEX_KING;
		}
		if (pieceType == INDEX_PIECE_ERROR) {
			throw new Exception();
		}
		lookupIndex = atFile + atRank * 8 + pieceType * NUM_SQUARES;
		if (currPiece.isBlack())
			lookupIndex += INDEX_BLACK;
		
		return prnLookupTable[lookupIndex];
	}
	
	private void setTargetFile(GenericFile enPasFile) {
		prevEnPassantFile = enPasFile;
		hashCode ^= prnLookupTable[(INDEX_ENP_A+IntFile.valueOf(enPasFile))];
	}
	
	private void clearTargetFile() {
		hashCode ^= prnLookupTable[(INDEX_ENP_A+IntFile.valueOf(prevEnPassantFile))];
		prevEnPassantFile = null;
	}
	
	// Used to update the Zobrist hash code for a position when that position changes due to a move
	public long update(GenericMove move, Piece captureTarget, Boolean setEnPassant) throws Exception {
		// deal with non-capture moves
		Piece piece = pos.getTheBoard().getPieceAtSquare(move.to);
		hashCode ^= getPrnForPiece(move.to, piece); // to
		hashCode ^= getPrnForPiece(move.from, piece); // from
		
		// Remove capture Target
		if (captureTarget != null)
			hashCode ^= getPrnForPiece(captureTarget.getSquare(), captureTarget);
		
		// deal with en Passant moves
		if (setEnPassant) {
			setTargetFile(move.from.file);
		} else if (prevEnPassantFile != null) {
			clearTargetFile();
		} else {
			// no action needed
		}
		
		// Deal with castling flags
		int currentCastlingFlags = pos.getCastlingAvaillability();
		int delta = currentCastlingFlags ^ this.prevCastlingMask;
		if (delta != 0)
		{
			if ((delta & PositionManager.WHITE_KINGSIDE)==PositionManager.WHITE_KINGSIDE)
				hashCode ^= prnLookupTable[INDEX_WHITE_KSC];
			if ((delta & PositionManager.WHITE_QUEENSIDE)==PositionManager.WHITE_QUEENSIDE)
				hashCode ^= prnLookupTable[INDEX_WHITE_QSC];
			if ((delta & PositionManager.BLACK_KINGSIDE)==PositionManager.BLACK_KINGSIDE)
				hashCode ^= prnLookupTable[INDEX_BLACK_KSC];
			if ((delta & PositionManager.BLACK_QUEENSIDE)==PositionManager.BLACK_QUEENSIDE)
				hashCode ^= prnLookupTable[INDEX_BLACK_QSC];
		}
		this.prevCastlingMask = currentCastlingFlags;
		
		// deal with side on move
	    hashCode ^= prnLookupTable[INDEX_SIDE_TO_MOVE];
		
		return hashCode;
	}
}
