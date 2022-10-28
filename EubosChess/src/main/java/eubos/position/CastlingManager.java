package eubos.position;

import com.fluxchess.jcpi.models.IntFile;

import eubos.board.BitBoard;
import eubos.board.Board;
import eubos.board.Piece;
import eubos.main.EubosEngineMain;

public class CastlingManager {
	
	public static final int WHITE_KINGSIDE = 1<<0;
	public static final int WHITE_QUEENSIDE = 1<<1;
	public static final int BLACK_KINGSIDE = 1<<2;
	public static final int BLACK_QUEENSIDE = 1<<3;
	public static final int WHITE_CAN_CASTLE = (WHITE_KINGSIDE | WHITE_QUEENSIDE);
	public static final int BLACK_CAN_CASTLE = (BLACK_KINGSIDE | BLACK_QUEENSIDE);
	private int flags = 0;
	
	private PositionManager pm;

	// Optimisation - King cannot be in check when we call castling manager, so we don't check King square.
	private static final int [] kscWhiteCheckSqs = {BitBoard.positionToBit_Lut[Position.f1], BitBoard.positionToBit_Lut[Position.g1]};
	private static final int [] kscBlackCheckSqs = {BitBoard.positionToBit_Lut[Position.f8], BitBoard.positionToBit_Lut[Position.g8]};
	private static final int [] kscWhiteEmptySqs = {Position.f1, Position.g1};
	private static final int [] kscBlackEmptySqs = {Position.f8, Position.g8};

	private static final int [] qscWhiteCheckSqs = {BitBoard.positionToBit_Lut[Position.c1], BitBoard.positionToBit_Lut[Position.d1]};
	private static final int [] qscBlackCheckSqs = {BitBoard.positionToBit_Lut[Position.c8], BitBoard.positionToBit_Lut[Position.d8]};
	private static final int [] qscWhiteEmptySqs = {Position.c1, Position.d1, Position.b1};
	private static final int [] qscBlackEmptySqs = {Position.c8, Position.d8, Position.b8};

	public static final int bksc = Move.valueOf(Move.TYPE_REGULAR_NONE, Position.e8, (Piece.BLACK | Piece.KING), Position.g8, Piece.NONE, Piece.NONE);
	public static final int wksc = Move.valueOf(Move.TYPE_REGULAR_NONE, Position.e1, Piece.KING, Position.g1, Piece.NONE, Piece.NONE);
	public static final int bqsc = Move.valueOf(Move.TYPE_REGULAR_NONE, Position.e8, (Piece.BLACK | Piece.KING), Position.c8, Piece.NONE, Piece.NONE);
	public static final int wqsc = Move.valueOf(Move.TYPE_REGULAR_NONE, Position.e1, Piece.KING, Position.c1, Piece.NONE, Piece.NONE);

	public static final int undo_bksc = Move.valueOf(Move.TYPE_REGULAR_NONE, Position.g8, (Piece.BLACK | Piece.KING), Position.e8, Piece.NONE, Piece.NONE);
	public static final int undo_wksc = Move.valueOf(Move.TYPE_REGULAR_NONE, Position.g1, Piece.KING, Position.e1, Piece.NONE, Piece.NONE);
	public static final int undo_bqsc = Move.valueOf(Move.TYPE_REGULAR_NONE, Position.c8, (Piece.BLACK | Piece.KING), Position.e8, Piece.NONE, Piece.NONE);
	public static final int undo_wqsc = Move.valueOf(Move.TYPE_REGULAR_NONE, Position.c1, Piece.KING, Position.e1, Piece.NONE, Piece.NONE);

	CastlingManager(PositionManager Pm) { this( Pm, "-"); }

	CastlingManager(PositionManager Pm, String fenCastle) {
		pm = Pm;
		flags = 0;
		setFenFlags(fenCastle);
	}
	
	String getFenFlags() {
		StringBuilder fenCastle = new StringBuilder();
		if (((flags & WHITE_KINGSIDE) != 0))
			fenCastle.append("K");
		if (((flags & WHITE_QUEENSIDE) != 0))
			fenCastle.append("Q");
		if (((flags & BLACK_KINGSIDE) != 0))
			fenCastle.append("k");
		if (((flags & BLACK_QUEENSIDE) != 0))
			fenCastle.append("q");
		if (fenCastle.length() == 0)
			fenCastle.append("-");
		return fenCastle.toString();
	}
	
	void setFenFlags(String fenCastle) {
		flags = 0;
		if (fenCastle.contains("K")) {
			flags |= WHITE_KINGSIDE;
		}
		if (fenCastle.contains("Q")) {
			flags |= WHITE_QUEENSIDE;
		}
		if (fenCastle.contains("k")) {
			flags |= BLACK_KINGSIDE;
		}
		if (fenCastle.contains("q")) {
			flags |= BLACK_QUEENSIDE;
		}
	}

	int getFlags() {
		return flags;
	}
	
	void setFlags(int flags) {
		this.flags = flags;
	}
	
	public boolean isCastlingMove(int move) {
		if (Move.areEqualForBestKiller(move, wksc) ||
			Move.areEqualForBestKiller(move, bksc) ||
			Move.areEqualForBestKiller(move, wqsc) ||
			Move.areEqualForBestKiller(move, bqsc)) {
			return true;
		}
		return false;
	}

	public void addCastlingMoves(boolean isWhiteOnMove, IAddMoves ml) {
		// The side on move should not have previously castled
		if ( !castlingAvaillable(isWhiteOnMove))
			return;
		
		int ksc = Move.NULL_MOVE;
		int qsc = Move.NULL_MOVE;
		if (isWhiteOnMove) {
			if ((flags & WHITE_KINGSIDE) != 0) {
				ksc = getWhiteKingsideCastleMove();
			}
			if ((flags & WHITE_QUEENSIDE) != 0) {
				qsc = getWhiteQueensideCastleMove();
			}
		} else {
			// Black
			if ((flags & BLACK_KINGSIDE) != 0) {
				ksc = getBlackKingsideCastleMove();
			}
			if ((flags & BLACK_QUEENSIDE) != 0) {
				qsc = getBlackQueensideCastleMove();
			}
		}
		if ( ksc != Move.NULL_MOVE )
			ml.addNormal(ksc);
		if ( qsc != Move.NULL_MOVE )
			ml.addNormal(qsc);
	}

	private boolean castlingAvaillable(boolean whiteOnMove) {
		return whiteOnMove ? (flags & WHITE_CAN_CASTLE) != 0 : (flags & BLACK_CAN_CASTLE) != 0;
	}
	
	private boolean castleMoveLegal(int rookSq,
			int [] checkSqs,
			int [] emptySqs) {
		Board theBoard = pm.getTheBoard();
		if (EubosEngineMain.ENABLE_ASSERTS)
			assert Piece.isRook(theBoard.getPieceAtSquare(rookSq));
		
		// All the intervening squares between King and Rook should be empty
		for ( int emptySq : emptySqs ) {
			if ( !theBoard.squareIsEmpty(emptySq))
				return false;
		}
		// King cannot move through an attacked square
		for (int sqToCheck: checkSqs) {
			if (theBoard.squareIsAttacked(sqToCheck, pm.onMoveIsWhite())) {
				return false;
			}
		}
		return true;
	}

	private int getWhiteKingsideCastleMove() {
		return (castleMoveLegal(Position.h1, kscWhiteCheckSqs, kscWhiteEmptySqs)) ? wksc : 0;
	}

	private int getBlackKingsideCastleMove() {
		return (castleMoveLegal(Position.h8, kscBlackCheckSqs, kscBlackEmptySqs)) ? bksc : 0;
	}

	private int getWhiteQueensideCastleMove() {
		return (castleMoveLegal(Position.a1, qscWhiteCheckSqs, qscWhiteEmptySqs)) ? wqsc : 0;
	}

	private int getBlackQueensideCastleMove() {
		return (castleMoveLegal(Position.a8, qscBlackCheckSqs, qscBlackEmptySqs)) ? bqsc : 0;
	}

	public void updateFlags(int lastMove) {
		if (flags !=0) {
			// This code can only clear flags, so don't execute if they are already cleared	
			int movedPiece = Move.getOriginPiece(lastMove);
			switch(movedPiece) {
			case Piece.WHITE_KING:
				// King moved
				flags &= ~WHITE_CAN_CASTLE;
				break;
			case Piece.BLACK_KING:
				// King moved
				flags &= ~BLACK_CAN_CASTLE;
				break;
			case Piece.WHITE_ROOK:
				{
					// Rook moved
					int originBitOffset = Move.getOriginPosition(lastMove);
					if (BitBoard.getFile(originBitOffset)==IntFile.Fa) {
						flags &= ~WHITE_QUEENSIDE;
					} 
					if (BitBoard.getFile(originBitOffset)==IntFile.Fh) {
						flags &= ~WHITE_KINGSIDE;
					}
				}
				break;
			case Piece.BLACK_ROOK:
				{
					// Rook moved
					int originBitOffset = Move.getOriginPosition(lastMove);
					if (BitBoard.getFile(originBitOffset)==IntFile.Fa) {
						flags &= ~BLACK_QUEENSIDE;
					} else if (BitBoard.getFile(originBitOffset)==IntFile.Fh) {
						flags &= ~BLACK_KINGSIDE;
					}
				}
				break;
			default:
				break;
			}
			// After this, the move wasn't castling, but may have caused castling to be no longer possible
			// If a rook got captured
			int targetPosition = Move.getTargetPosition(lastMove);
			if (targetPosition == BitBoard.positionToBit_Lut[Position.a8]) {
				flags &= ~BLACK_QUEENSIDE;
			} else if (targetPosition == BitBoard.positionToBit_Lut[Position.h8]) {
				flags &= ~BLACK_KINGSIDE;
			} else if (targetPosition == BitBoard.positionToBit_Lut[Position.a1]) {
				flags &= ~WHITE_QUEENSIDE;
			} else if (targetPosition == BitBoard.positionToBit_Lut[Position.h1]) {
				flags &= ~WHITE_KINGSIDE;
			}
		}
	}
}
