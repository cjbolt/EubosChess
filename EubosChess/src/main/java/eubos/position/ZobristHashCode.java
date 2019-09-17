package eubos.position;

import java.util.Random;
import java.util.Stack;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericChessman;
import com.fluxchess.jcpi.models.IllegalNotationException;
import com.fluxchess.jcpi.models.IntFile;
import com.fluxchess.jcpi.models.IntRank;

import eubos.board.pieces.Bishop;
import eubos.board.pieces.King;
import eubos.board.pieces.Knight;
import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Queen;
import eubos.board.pieces.Rook;
import eubos.board.pieces.Piece.Colour;

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
	private static final int INDEX_PIECE_ERROR = -1;
	
	private IPositionAccessors pos;
	
	private Stack<GenericFile> prevEnPassantFile = null;
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
		prevEnPassantFile = new Stack<GenericFile>();
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
			doOnMove();
		}
		// add en passant
		GenericPosition enPassant = pos.getTheBoard().getEnPassantTargetSq();
		if (enPassant!=null) {
			prevEnPassantFile.push(enPassant.file);
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
	
	// Used to update the Zobrist hash code whenever a position changes due to a move being performed
	public void update(GenericMove move, Piece captureTarget, GenericFile enPassantFile) throws Exception {
		Piece piece = doBasicMove(move);
		
		doCapturedPiece(captureTarget);
		
		doEnPassant(move, enPassantFile);
		
		doSecondaryMove(move, piece);
		
		doCastlingFlags();
		
		doOnMove();
	}

	private Piece convertChessmanToPiece(GenericChessman chessman, GenericMove move) {
		Piece eubosPiece = null;
		if (chessman.equals(GenericChessman.KNIGHT))
			eubosPiece = new eubos.board.pieces.Knight(Colour.getOpposite(pos.getOnMove()), move.to);
		else if (chessman.equals(GenericChessman.BISHOP))
			eubosPiece = new eubos.board.pieces.Bishop(Colour.getOpposite(pos.getOnMove()), move.to);
		else if (chessman.equals(GenericChessman.ROOK))
			eubosPiece = new eubos.board.pieces.Rook(Colour.getOpposite(pos.getOnMove()), move.to);
		else if (chessman.equals(GenericChessman.QUEEN))
			eubosPiece = new eubos.board.pieces.Queen(Colour.getOpposite(pos.getOnMove()), move.to);
		return eubosPiece;
	}
	
	protected Piece doBasicMove(GenericMove move) throws Exception {
		Piece piece = pos.getTheBoard().getPieceAtSquare(move.to);
		GenericChessman promotedChessman = move.promotion;
		if (promotedChessman == null) {
			// Basic move only
			hashCode ^= getPrnForPiece(move.to, piece);
			hashCode ^= getPrnForPiece(move.from, piece);
		} else {
			// Promotion
			if (piece instanceof Pawn) {
				// is undoing promotion
				Piece promotedToPiece = convertChessmanToPiece(promotedChessman, move);
				hashCode ^= getPrnForPiece(move.to, piece);
				hashCode ^= getPrnForPiece(move.from, promotedToPiece);
				piece = promotedToPiece;
			} else if (piece instanceof Knight || 
					piece instanceof Bishop || 
					piece instanceof Rook|| 
					piece instanceof Queen) {
				// is a promotion
				Piece unpromotedPawn = new eubos.board.pieces.Pawn(pos.getOnMove(), move.from);
				hashCode ^= getPrnForPiece(move.to, piece);
				hashCode ^= getPrnForPiece(move.from, unpromotedPawn);
			}
		}
		return piece;
	}

	protected void doCapturedPiece(Piece captureTarget) throws Exception {
		if (captureTarget != null)
			hashCode ^= getPrnForPiece(captureTarget.getSquare(), captureTarget);
	}

	private void setTargetFile(GenericFile enPasFile) {
		if (!prevEnPassantFile.isEmpty()) {
			clearTargetFile();
		}
		prevEnPassantFile.push(enPasFile);
		hashCode ^= prnLookupTable[(INDEX_ENP_A+IntFile.valueOf(enPasFile))];
	}
	
	private void clearTargetFile() {
		hashCode ^= prnLookupTable[(INDEX_ENP_A+IntFile.valueOf(prevEnPassantFile.pop()))];
	}
	
	protected void doEnPassant(GenericMove move, GenericFile enPassantFile) {
		if (enPassantFile != null) {
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
		int currentCastlingFlags = pos.getCastlingAvaillability();
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

	protected void doSecondaryMove(GenericMove move, Piece piece)
			throws IllegalNotationException, Exception {
		if (piece instanceof King) {
			if (move.equals(CastlingManager.wksc)) {
				piece = pos.getTheBoard().getPieceAtSquare(GenericPosition.f1);
				hashCode ^= getPrnForPiece(GenericPosition.f1, piece); // to
				hashCode ^= getPrnForPiece(GenericPosition.h1, piece); // from
			}
			else if (move.equals(CastlingManager.wqsc)) {
				piece = pos.getTheBoard().getPieceAtSquare(GenericPosition.d1);
				hashCode ^= getPrnForPiece(GenericPosition.d1, piece); // to
				hashCode ^= getPrnForPiece(GenericPosition.a1, piece); // from
			}
			else if (move.equals(CastlingManager.bksc)) {
				piece = pos.getTheBoard().getPieceAtSquare(GenericPosition.f8);
				hashCode ^= getPrnForPiece(GenericPosition.f8, piece); // to
				hashCode ^= getPrnForPiece(GenericPosition.h8, piece); // from
			}
			else if (move.equals(CastlingManager.bqsc)) {
				piece = pos.getTheBoard().getPieceAtSquare(GenericPosition.d8);
				hashCode ^= getPrnForPiece(GenericPosition.d8, piece); // to
				hashCode ^= getPrnForPiece(GenericPosition.a8, piece); // from
			}
			else if (move.equals(CastlingManager.undo_wksc))
			{
				piece = pos.getTheBoard().getPieceAtSquare(GenericPosition.h1);
				hashCode ^= getPrnForPiece(GenericPosition.h1, piece); // to
				hashCode ^= getPrnForPiece(GenericPosition.f1, piece); // from
			}
			else if (move.equals(CastlingManager.undo_wqsc)) {
				piece = pos.getTheBoard().getPieceAtSquare(GenericPosition.a1);
				hashCode ^= getPrnForPiece(GenericPosition.a1, piece); // to
				hashCode ^= getPrnForPiece(GenericPosition.d1, piece); // from
			}
			else if (move.equals(CastlingManager.undo_bksc)) {
				piece = pos.getTheBoard().getPieceAtSquare(GenericPosition.h8);
				hashCode ^= getPrnForPiece(GenericPosition.h8, piece); // to
				hashCode ^= getPrnForPiece(GenericPosition.f8, piece); // from
			}
			else if (move.equals(CastlingManager.undo_bqsc)) {
				piece = pos.getTheBoard().getPieceAtSquare(GenericPosition.a8);
				hashCode ^= getPrnForPiece(GenericPosition.a8, piece); // to
				hashCode ^= getPrnForPiece(GenericPosition.d8, piece); // from
			}
			else {
				// not castle move
			}
		}
	}
}
