package eubos.position;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IntChessman;

import eubos.board.Board;
import eubos.board.Piece;

/**
 * This class represents a move as a int value. The fields are represented by
 * the following bits.
 * <p/>
 *  0 -  3: the type (required)
 *  4 - 10: the origin position (required)
 * 11 - 17: the target position (required)
 * 18 - 20: the promotion chessman (optional)
 */
public final class Move {
	
	public static final int TYPE_PROMOTION_AND_CAPTURE_WITH_CHECK = 0;
	public static final int TYPE_PROMOTION_AND_CAPTURE = 1;
	public static final int TYPE_PROMOTION = 2;
	public static final int TYPE_KBR_PROMOTION = 3;
	public static final int TYPE_CAPTURE_WITH_CHECK = 4;
	public static final int TYPE_CAPTURE_QUEEN = 5;
	public static final int TYPE_CAPTURE_ROOK = 6;
	public static final int TYPE_CAPTURE_PIECE = 7;
	public static final int TYPE_CAPTURE_PAWN = 8;
	public static final int TYPE_CASTLE = 9;
	public static final int TYPE_CHECK = 10;
	public static final int TYPE_REGULAR = 11;
	public static final int TYPE_NONE = 12;
	
	private static final int TYPE_SHIFT = 0;
	private static final int TYPE_MASK = 0xF << TYPE_SHIFT;
	
	private static final int ORIGINPOSITION_SHIFT = 4;
	private static final int ORIGINPOSITION_MASK = Position.MASK << ORIGINPOSITION_SHIFT;
	private static final int TARGETPOSITION_SHIFT = 11;
	private static final int TARGETPOSITION_MASK = Position.MASK << TARGETPOSITION_SHIFT;
	private static final int PROMOTION_SHIFT = 18;
	private static final int PROMOTION_MASK = IntChessman.MASK << PROMOTION_SHIFT;
	private static final int ORIGIN_PIECE_SHIFT = 21;
	private static final int ORIGIN_PIECE_MASK = Piece.PIECE_WHOLE_MASK << ORIGIN_PIECE_SHIFT;
	private static final int TARGET_PIECE_SHIFT = 25;
	private static final int TARGET_PIECE_MASK = Piece.PIECE_WHOLE_MASK << TARGET_PIECE_SHIFT;
	
	private Move() {
	}
	
	public static int valueOf(int originPosition, int originPiece, int targetPosition, int targetPiece) {
		return Move.valueOf(Move.TYPE_NONE, originPosition, originPiece, targetPosition, targetPiece, IntChessman.NOCHESSMAN);
	}

	public static int valueOf(int type, int originPosition, int originPiece, int targetPosition, int targetPiece, int promotion) {
		int move = 0;

		// Encode move classification
		assert (type >= Move.TYPE_PROMOTION_AND_CAPTURE_WITH_CHECK || type <= Move.TYPE_NONE);
		move |= type << TYPE_SHIFT;

		// Encode origin position
		assert (originPosition & 0x88) == 0;
		move |= originPosition << ORIGINPOSITION_SHIFT;
		
		// Encode Origin Piece
		assert (originPiece & ~Piece.PIECE_WHOLE_MASK) == 0;
		move |= originPiece << ORIGIN_PIECE_SHIFT;

		// Encode target position
		assert (targetPosition & 0x88) == 0;
		move |= targetPosition << TARGETPOSITION_SHIFT;

		// Encode Target Piece
		assert (targetPiece & ~Piece.PIECE_WHOLE_MASK) == 0;
		move |= targetPiece << TARGET_PIECE_SHIFT;
		
		// Encode promotion
		assert (IntChessman.isValid(promotion) && IntChessman.isValidPromotion(promotion))
		|| promotion == IntChessman.NOCHESSMAN;
		move |= promotion << PROMOTION_SHIFT;

		return move;
	}
	
	public static int toMove(GenericMove move, Board theBoard, int type) {
		int intMove = 0;
		int targetPosition = Position.valueOf(move.to);
		int originPosition = Position.valueOf(move.from);
		int promotion = IntChessman.NOCHESSMAN;
		int originPiece = Piece.PIECE_NONE;
		int targetPiece = Piece.PIECE_NONE;
		if (theBoard != null) {
			originPiece = theBoard.getPieceAtSquare(originPosition);
			targetPiece = theBoard.getPieceAtSquare(targetPosition);
		}
		if (move.promotion != null) {
			promotion = IntChessman.valueOf(move.promotion);
			intMove = Move.valueOf(Move.TYPE_KBR_PROMOTION, originPosition, originPiece, targetPosition, targetPiece, promotion);
		} else {
			intMove = Move.valueOf(type, originPosition, originPiece, targetPosition, targetPiece, promotion);
		}
		return intMove;
	}
	
	public static int toMove(GenericMove move, Board theBoard) {
		return Move.toMove(move, theBoard, Move.TYPE_NONE);
	}
	
	public static int toMove(GenericMove move) {
		return Move.toMove(move, null, Move.TYPE_NONE);
	}

	public static GenericMove toGenericMove(int move) {
		if (move == 0)
			return null;
		
		int type = getType(move);
		int originPosition = getOriginPosition(move);
		int targetPosition = getTargetPosition(move);

		if (type > Move.TYPE_KBR_PROMOTION) {
			return new GenericMove(
					Position.toGenericPosition(originPosition),
					Position.toGenericPosition(targetPosition));
		} else {
			return new GenericMove(
					Position.toGenericPosition(originPosition),
					Position.toGenericPosition(targetPosition),
					IntChessman.toGenericChessman(getPromotion(move)));
		}
	}
	
	public static boolean areEqual(int move1, int move2) {
		boolean areEqual = false;
		if (Move.getOriginPosition(move1)==Move.getOriginPosition(move2) &&
			Move.getTargetPosition(move1)==Move.getTargetPosition(move2) &&
			Move.getPromotion(move1)==Move.getPromotion(move2)) {
			areEqual = true;
		}
		return areEqual;
	}

	public static int getType(int move) {
		int type = (move & TYPE_MASK) >>> TYPE_SHIFT;

		assert (type >= Move.TYPE_PROMOTION_AND_CAPTURE_WITH_CHECK || type <= Move.TYPE_NONE);

		return type;
	}

	public static int getOriginPosition(int move) {
		int originPosition = (move & ORIGINPOSITION_MASK) >>> ORIGINPOSITION_SHIFT;
		assert (originPosition & 0x88) == 0;

		return originPosition;
	}
	
	private static int setOriginPosition(int move, int originPosition) {
		// Zero out origin position
		move &= ~ORIGINPOSITION_MASK;

		// Encode origin position
		assert (originPosition & 0x88) == 0;
		move |= originPosition << ORIGINPOSITION_SHIFT;

		return move;
	}

	public static int getTargetPosition(int move) {
		int targetPosition = (move & TARGETPOSITION_MASK) >>> TARGETPOSITION_SHIFT;
		assert (targetPosition & 0x88) == 0;

		return targetPosition;
	}

	public static int setTargetPosition(int move, int targetPosition) {
		// Zero out target position
		move &= ~TARGETPOSITION_MASK;

		// Encode target position
		assert (targetPosition & 0x88) == 0;
		move |= targetPosition << TARGETPOSITION_SHIFT;

		return move;
	}

	public static int getPromotion(int move) {
		int promotion = (move & PROMOTION_MASK) >>> PROMOTION_SHIFT;
		if (move != 0) {
			assert (IntChessman.isValid(promotion) && IntChessman.isValidPromotion(promotion))
			|| promotion == IntChessman.NOCHESSMAN;
		}
		return promotion;
	}

	public static int setPromotion(int move, int promotion) {
		// Zero out promotion chessman
		move &= ~PROMOTION_MASK;

		// Encode promotion
		assert (IntChessman.isValid(promotion) && IntChessman.isValidPromotion(promotion))
		|| promotion == IntChessman.NOCHESSMAN;
		move |= promotion << PROMOTION_SHIFT;

		return move;
	}
	
	public static int getOriginPiece(int move) {
		int piece = (move & ORIGIN_PIECE_MASK) >>> ORIGIN_PIECE_SHIFT;
		//assert (piece & Piece.PIECE_NO_COLOUR_MASK) != Piece.PIECE_NONE;
		
		return piece;
	}

	public static int setOriginPiece(int move, int piece) {
		//assert (piece & Piece.PIECE_NO_COLOUR_MASK) != Piece.PIECE_NONE;
		
		move &= ~ORIGIN_PIECE_MASK;
		move |= piece << ORIGIN_PIECE_SHIFT;
		return move;
	}
	
	public static int getTargetPiece(int move) {
		int piece = (move & TARGET_PIECE_MASK) >>> TARGET_PIECE_SHIFT;
		//assert (piece & Piece.PIECE_NO_COLOUR_MASK) != Piece.PIECE_NONE;
		
		return piece;
	}

	public static int setTargetPiece(int move, int piece) {
		//assert (piece & Piece.PIECE_NO_COLOUR_MASK) != Piece.PIECE_NONE;
		
		move &= ~TARGET_PIECE_MASK;
		move |= piece << TARGET_PIECE_SHIFT;
		return move;
	}
	
	public static String toString(int move) {
		String string = "";
		if (move != 0) {
			string += toGenericMove(move).toString();

			if (getType(move) <= Move.TYPE_KBR_PROMOTION) {
				string += ":";
				string += IntChessman.toGenericChessman(getPromotion(move));
			}
		}
		return string;
	}

	public static int setType(int move, int type) {
		// Zero out type
		move &= ~TYPE_MASK;
		
		assert (type >= Move.TYPE_PROMOTION_AND_CAPTURE_WITH_CHECK || type <= Move.TYPE_NONE);
		
		return move |= type << TYPE_SHIFT;
	}
	
	public static int reverse(int move) {
		int reversedMove = move;
		reversedMove = Move.setTargetPosition(reversedMove, Move.getOriginPosition(move));
		reversedMove = Move.setOriginPosition(reversedMove, Move.getTargetPosition(move));
		return reversedMove;
	}

}
