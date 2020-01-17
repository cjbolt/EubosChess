package eubos.position;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IntChessman;
import com.fluxchess.jcpi.models.IntPiece;

import eubos.board.Piece.PieceType;
import eubos.position.MoveList.MoveClassification;

/**
 * This class represents a move as a int value. The fields are represented by
 * the following bits.
 * <p/>
 *  0 -  2: the type (required)
 *  3 -  9: the origin position (required)
 * 10 - 16: the target position (required)
 * 17 - 21: the origin piece (required)
 * 22 - 26: the target piece (optional)
 * 27 - 29: the promotion chessman (optional)
 */
public final class Move {
	
	private static final int TYPE_SHIFT = 0;
	private static final int TYPE_MASK = /*Type.MASK*/0xF << TYPE_SHIFT;
	private static final int ORIGINPOSITION_SHIFT = 4;
	private static final int ORIGINPOSITION_MASK = Position.MASK << ORIGINPOSITION_SHIFT;
	private static final int TARGETPOSITION_SHIFT = 11;
	private static final int TARGETPOSITION_MASK = Position.MASK << TARGETPOSITION_SHIFT;
	private static final int ORIGINPIECE_SHIFT = 18;
	private static final int ORIGINPIECE_MASK = IntPiece.MASK << ORIGINPIECE_SHIFT;
	private static final int TARGETPIECE_SHIFT = 23;
	private static final int TARGETPIECE_MASK = IntPiece.MASK << TARGETPIECE_SHIFT;
	private static final int PROMOTION_SHIFT = 28;
	private static final int PROMOTION_MASK = IntChessman.MASK << PROMOTION_SHIFT;

	private Move() {
	}

	public static int valueOf(int type, int originPosition, int targetPosition, int originPiece, int targetPiece, int promotion) {
		int move = 0;

		// Encode move classification
		assert     type == MoveClassification.PROMOTION_AND_CAPTURE_WITH_CHECK.ordinal()	
				|| type == MoveClassification.PROMOTION_AND_CAPTURE.ordinal()
				|| type == MoveClassification.PROMOTION.ordinal()
				|| type == MoveClassification.OTHER_PROMOTION.ordinal()
				|| type == MoveClassification.CAPTURE_WITH_CHECK.ordinal()
				|| type == MoveClassification.CAPTURE.ordinal()	
				|| type == MoveClassification.CASTLE.ordinal()
				|| type == MoveClassification.CHECK.ordinal()	
				|| type == MoveClassification.REGULAR.ordinal();
		move |= type << TYPE_SHIFT;

		// Encode origin position
		assert (originPosition & 0x88) == 0;
		move |= originPosition << ORIGINPOSITION_SHIFT;

		// Encode target position
		assert (targetPosition & 0x88) == 0;
		move |= targetPosition << TARGETPOSITION_SHIFT;

		// Encode origin piece
		assert originPiece == PieceType.WhiteKing.ordinal()
				|| originPiece == PieceType.WhiteQueen.ordinal()
				|| originPiece == PieceType.WhiteRook.ordinal()
				|| originPiece == PieceType.WhiteBishop.ordinal()
				|| originPiece == PieceType.WhiteKnight.ordinal()
				|| originPiece == PieceType.WhitePawn.ordinal()
				|| originPiece == PieceType.BlackKing.ordinal()
				|| originPiece == PieceType.BlackQueen.ordinal()
				|| originPiece == PieceType.BlackRook.ordinal()
				|| originPiece == PieceType.BlackBishop.ordinal()
				|| originPiece == PieceType.BlackKnight.ordinal()
				|| originPiece == PieceType.BlackPawn.ordinal();
		move |= originPiece << ORIGINPIECE_SHIFT;

		// Encode target piece
		//assert IntPiece.isValid(targetPiece) || targetPiece == IntPiece.NOPIECE;
		//move |= targetPiece << TARGETPIECE_SHIFT;

		// Encode promotion
		assert (IntChessman.isValid(promotion) && IntChessman.isValidPromotion(promotion))
		|| promotion == IntChessman.NOCHESSMAN;
		move |= promotion << PROMOTION_SHIFT;

		return move;
	}
	
	public static int toMove(GenericMove move, MoveClassification type) {
		int targetPosition = Position.valueOf(move.to);
		int originPosition = Position.valueOf(move.from);
		int promotion = (move.promotion != null) ? IntChessman.valueOf(move.promotion) : IntChessman.NOCHESSMAN;
		return Move.valueOf(type.ordinal(), originPosition, targetPosition, 0, 0, promotion);
	}

	public static GenericMove toGenericMove(int move) {
		if (move == 0)
			return null;
		
		int type = getType(move);
		int originPosition = getOriginPosition(move);
		int targetPosition = getTargetPosition(move);

		if (type > MoveClassification.OTHER_PROMOTION.ordinal()) {
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

	public static int getType(int move) {
		int type = (move & TYPE_MASK) >>> TYPE_SHIFT;
		/*assert type == Type.CASTLING;*/

		return type;
	}

	public static int getOriginPosition(int move) {
		int originPosition = (move & ORIGINPOSITION_MASK) >>> ORIGINPOSITION_SHIFT;
		assert (originPosition & 0x88) == 0;

		return originPosition;
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

	public static int setTargetPositionAndPiece(int move, int targetPosition, int targetPiece) {
		// Zero out target position and target piece
		move &= ~TARGETPOSITION_MASK;
		move &= ~TARGETPIECE_MASK;

		// Encode target position
		assert (targetPosition & 0x88) == 0;
		move |= targetPosition << TARGETPOSITION_SHIFT;

		// Encode target piece
		assert IntPiece.isValid(targetPiece) || targetPiece == IntPiece.NOPIECE;
		move |= targetPiece << TARGETPIECE_SHIFT;

		return move;
	}

	public static int getOriginPiece(int move) {
		int originPiece = (move & ORIGINPIECE_MASK) >>> ORIGINPIECE_SHIFT;
		assert IntPiece.isValid(originPiece);

		return originPiece;
	}

	public static int getTargetPiece(int move) {
		int targetPiece = (move & TARGETPIECE_MASK) >>> TARGETPIECE_SHIFT;
		assert IntPiece.isValid(targetPiece) || targetPiece == IntPiece.NOPIECE;

		return targetPiece;
	}

	public static int getPromotion(int move) {
		int promotion = (move & PROMOTION_MASK) >>> PROMOTION_SHIFT;
		assert (IntChessman.isValid(promotion) && IntChessman.isValidPromotion(promotion))
		|| promotion == IntChessman.NOCHESSMAN;

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

	public static String toString(int move) {
		String string = "";
		if (move != 0) {
			string += toGenericMove(move).toString();

			if (getType(move) <= MoveClassification.PROMOTION.ordinal()) {
				string += ":";
				string += IntChessman.toGenericChessman(getPromotion(move));
			}
		}
		return string;
	}

}
