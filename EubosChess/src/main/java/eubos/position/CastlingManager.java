package eubos.position;

import eubos.board.BitBoard;
import eubos.board.Board;
import eubos.board.Piece;

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
	private static final int [] kscWhiteCheckSqs = {BitBoard.f1, BitBoard.g1};
	private static final int [] kscBlackCheckSqs = {BitBoard.f8, BitBoard.g8};
	private static final long [] kscWhiteEmptySqs = {1L << BitBoard.f1, 1L << BitBoard.g1};
	private static final long [] kscBlackEmptySqs = {1L << BitBoard.f8, 1L << BitBoard.g8};

	private static final int [] qscWhiteCheckSqs = {BitBoard.c1, BitBoard.d1};
	private static final int [] qscBlackCheckSqs = {BitBoard.c8, BitBoard.d8};
	private static final long [] qscWhiteEmptySqs = {1L << BitBoard.c1, 1L << BitBoard.d1, 1L << BitBoard.b1};
	private static final long [] qscBlackEmptySqs = {1L << BitBoard.c8, 1L << BitBoard.d8, 1L << BitBoard.b8};

	public static final int bksc = Move.valueOfCastlingBit(Move.TYPE_REGULAR_NONE, BitBoard.e8, (Piece.BLACK | Piece.KING), BitBoard.g8, Piece.NONE, Piece.NONE);
	public static final int wksc = Move.valueOfCastlingBit(Move.TYPE_REGULAR_NONE, BitBoard.e1, Piece.KING, BitBoard.g1, Piece.NONE, Piece.NONE);
	public static final int bqsc = Move.valueOfCastlingBit(Move.TYPE_REGULAR_NONE, BitBoard.e8, (Piece.BLACK | Piece.KING), BitBoard.c8, Piece.NONE, Piece.NONE);
	public static final int wqsc = Move.valueOfCastlingBit(Move.TYPE_REGULAR_NONE, BitBoard.e1, Piece.KING, BitBoard.c1, Piece.NONE, Piece.NONE);

	public static final int undo_bksc = Move.valueOfCastlingBit(Move.TYPE_REGULAR_NONE, BitBoard.g8, (Piece.BLACK | Piece.KING), BitBoard.e8, Piece.NONE, Piece.NONE);
	public static final int undo_wksc = Move.valueOfCastlingBit(Move.TYPE_REGULAR_NONE, BitBoard.g1, Piece.KING, BitBoard.e1, Piece.NONE, Piece.NONE);
	public static final int undo_bqsc = Move.valueOfCastlingBit(Move.TYPE_REGULAR_NONE, BitBoard.c8, (Piece.BLACK | Piece.KING), BitBoard.e8, Piece.NONE, Piece.NONE);
	public static final int undo_wqsc = Move.valueOfCastlingBit(Move.TYPE_REGULAR_NONE, BitBoard.c1, Piece.KING, BitBoard.e1, Piece.NONE, Piece.NONE);

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
	
	public void addCastlingMoves(boolean isWhiteOnMove, IAddMoves ml) {
		// The side on move should not have previously castled
		if (!castlingAvaillable(isWhiteOnMove))
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
		if (ksc != Move.NULL_MOVE)
			ml.addNormal(ksc);
		if (qsc != Move.NULL_MOVE)
			ml.addNormal(qsc);
	}

	private boolean castlingAvaillable(boolean whiteOnMove) {
		return whiteOnMove ? (flags & WHITE_CAN_CASTLE) != 0 : (flags & BLACK_CAN_CASTLE) != 0;
	}
	
	private boolean castleMoveLegal(int [] checkSqs,
			long [] emptySqs) {
		Board theBoard = pm.getTheBoard();
		// All the intervening squares between King and Rook should be empty
		for (long emptyOffset : emptySqs) {
			if ( !theBoard.squareIsEmpty(emptyOffset))
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
		return (castleMoveLegal(kscWhiteCheckSqs, kscWhiteEmptySqs)) ? wksc : 0;
	}

	private int getBlackKingsideCastleMove() {
		return (castleMoveLegal(kscBlackCheckSqs, kscBlackEmptySqs)) ? bksc : 0;
	}

	private int getWhiteQueensideCastleMove() {
		return (castleMoveLegal(qscWhiteCheckSqs, qscWhiteEmptySqs)) ? wqsc : 0;
	}

	private int getBlackQueensideCastleMove() {
		return (castleMoveLegal(qscBlackCheckSqs, qscBlackEmptySqs)) ? bqsc : 0;
	}

	public int updateFlags(int lastMove) {
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
					if (originBitOffset==BitBoard.a1) {
						flags &= ~WHITE_QUEENSIDE;
					} else if (originBitOffset==BitBoard.h1) {
						flags &= ~WHITE_KINGSIDE;
					}
				}
				break;
			case Piece.BLACK_ROOK:
				{
					// Rook moved
					int originBitOffset = Move.getOriginPosition(lastMove);
					if (originBitOffset==BitBoard.a8) {
						flags &= ~BLACK_QUEENSIDE;
					} else if (originBitOffset==BitBoard.h8) {
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
			if (targetPosition == BitBoard.a8) {
				flags &= ~BLACK_QUEENSIDE;
			} else if (targetPosition == BitBoard.h8) {
				flags &= ~BLACK_KINGSIDE;
			} else if (targetPosition == BitBoard.a1) {
				flags &= ~WHITE_QUEENSIDE;
			} else if (targetPosition == BitBoard.h1) {
				flags &= ~WHITE_KINGSIDE;
			}
		}
		return flags;
	}
}
