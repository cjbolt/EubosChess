package eubos.board;

import java.util.LinkedList;
import java.util.List;

import com.fluxchess.jcpi.models.IntChessman;
import com.fluxchess.jcpi.models.IntRank;

import eubos.position.Move;
import eubos.position.Position;

public abstract class Piece {
	public enum Colour { 
		white, black;
		
		public static Colour getOpposite( Colour arg ) { return (arg == white) ? black : white; }
		public static boolean isWhite( Colour arg ) { return arg == white; }
		public static boolean isBlack( Colour arg ) { return arg == black; }
	};
	
	public static final int PIECE_NONE = 0x0;
	public static final int PIECE_KING = 0x1;
	public static final int PIECE_QUEEN = 0x2;
	public static final int PIECE_ROOK = 0x3;
	public static final int PIECE_BISHOP = 0x4;
	public static final int PIECE_KNIGHT = 0x5;
	public static final int PIECE_PAWN = 0x6;
	
	public static final int PIECE_BLACK = 0x8;
	
	public static final int PIECE_NO_COLOUR_MASK = 0x7;
	public static final int PIECE_WHOLE_MASK = 0xf;

	public static final int WHITE_QUEEN = PIECE_QUEEN;
	public static final int WHITE_BISHOP = PIECE_BISHOP;
	public static final int WHITE_KING = PIECE_KING;
	public static final int WHITE_KNIGHT = PIECE_KNIGHT;
	public static final int WHITE_ROOK = PIECE_ROOK;
	public static final int WHITE_PAWN = PIECE_PAWN;
	
	public static final int BLACK_QUEEN = (PIECE_BLACK|PIECE_QUEEN);
	public static final int BLACK_BISHOP = (PIECE_BLACK|PIECE_BISHOP);
	public static final int BLACK_KING = (PIECE_BLACK|PIECE_KING);
	public static final int BLACK_KNIGHT = (PIECE_BLACK|PIECE_KNIGHT);
	public static final int BLACK_ROOK = (PIECE_BLACK|PIECE_ROOK);
	public static final int BLACK_PAWN = (PIECE_BLACK|PIECE_PAWN);
	
	public static boolean isPawn(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == PIECE_PAWN; }
	public static boolean isKing(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == PIECE_KING; }
	public static boolean isQueen(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == PIECE_QUEEN; }
	public static boolean isRook(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == PIECE_ROOK; }
	public static boolean isBishop(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == PIECE_BISHOP; }
	public static boolean isKnight(int arg) { return (arg & PIECE_NO_COLOUR_MASK) == PIECE_KNIGHT; }
	
	public static boolean isOppositeColour(Colour ownColour, int toCheck) {
		assert (toCheck & PIECE_NO_COLOUR_MASK) != PIECE_NONE;
		return Colour.isWhite(ownColour) ? isBlack(toCheck) : isWhite(toCheck);
	}
	public static boolean isOppositeColourOrNone(Colour ownColour, int toCheck) {
		boolean retVal = true;
		if (toCheck != Piece.PIECE_NONE) {
			retVal = Colour.isWhite(ownColour) ? isBlack(toCheck) : isWhite(toCheck);
		}
		return retVal;
	}
	public static boolean isWhite(int arg) {
		return (arg&PIECE_BLACK) == 0;
	}
	public static boolean isBlack(int arg) {
		return (arg&PIECE_BLACK) == PIECE_BLACK;
	}
	public static Colour getOpposite(int arg) {
		return isWhite(arg) ? Colour.black : Colour.white;
	} 
	
	public static char toFenChar(int piece) {
		char chessman = 0;
		if (piece==Piece.WHITE_PAWN)
			chessman = 'P';
		else if (piece==Piece.WHITE_KNIGHT)
			chessman = 'N';
		else if (piece==Piece.WHITE_BISHOP)
			chessman = 'B';
		else if (piece==Piece.WHITE_ROOK)
			chessman = 'R';
		else if (piece==Piece.WHITE_QUEEN)
			chessman = 'Q';
		else if (piece==Piece.WHITE_KING)
			chessman = 'K';
		else if (piece==Piece.BLACK_PAWN)
			chessman = 'p';
		else if (piece==Piece.BLACK_KNIGHT)
			chessman = 'n';
		else if (piece==Piece.BLACK_BISHOP)
			chessman = 'b';
		else if (piece==Piece.BLACK_ROOK)
			chessman = 'r';
		else if (piece==Piece.BLACK_QUEEN)
			chessman = 'q';
		else if (piece==Piece.BLACK_KING)
			chessman = 'k';
		return chessman;
	}
		
	static List<Integer> king_generateMoves(Board theBoard, int atSquare, Piece.Colour ownSide) {
		List<Integer> moveList = new LinkedList<Integer>();
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getDirectMoveSq(Direction.up, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getDirectMoveSq(Direction.upRight, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getDirectMoveSq(Direction.right, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getDirectMoveSq(Direction.downRight, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getDirectMoveSq(Direction.down, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getDirectMoveSq(Direction.downLeft, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getDirectMoveSq(Direction.left, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getDirectMoveSq(Direction.upLeft, atSquare));
		return moveList;
	}

	private static void king_checkAddMove(Piece.Colour ownSide, int atSquare, List<Integer> moveList, Board theBoard, int targetSquare) {
		if ( targetSquare != Position.NOPOSITION ) {
			int targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if (Piece.isOppositeColourOrNone(ownSide, targetPiece)) {
				moveList.add(Move.valueOf(Move.TYPE_NONE, atSquare, theBoard.getPieceAtSquare(atSquare), targetSquare, targetPiece, IntChessman.NOCHESSMAN));
			}
		}
	}
	
	private static void knight_checkAddMove(int ownPiece, Piece.Colour ownSide, int atSquare, List<Integer> moveList, Board theBoard, int targetSquare) {
		if ( targetSquare != Position.NOPOSITION ) {
			int targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if (Piece.isOppositeColourOrNone(ownSide, targetPiece)) {
				moveList.add( Move.valueOf(Move.TYPE_NONE, atSquare, ownPiece, targetSquare, targetPiece, IntChessman.NOCHESSMAN));
			} else {
				// Indicates blocked by own piece.
			}
		}
	}
	
	static List<Integer> knight_generateMoves(Board theBoard, int atSquare, Piece.Colour ownSide) {
		List<Integer> moveList = new LinkedList<Integer>();
		int ownPiece = Piece.PIECE_KNIGHT;
		if (Colour.isBlack(ownSide)) {
			ownPiece |= Piece.PIECE_BLACK;
		}
		knight_checkAddMove(ownPiece, ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.upRight, atSquare));
		knight_checkAddMove(ownPiece, ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.upLeft, atSquare));
		knight_checkAddMove(ownPiece, ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.rightUp, atSquare));
		knight_checkAddMove(ownPiece, ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.rightDown, atSquare));
		knight_checkAddMove(ownPiece, ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.downRight, atSquare));
		knight_checkAddMove(ownPiece, ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.downLeft, atSquare));
		knight_checkAddMove(ownPiece, ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.leftUp, atSquare));
		knight_checkAddMove(ownPiece, ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.leftDown, atSquare));
		return moveList;		
	}
	
	
	static List<Integer> rook_generateMoves(Board theBoard, int atSquare, Piece.Colour ownSide) {
		LinkedList<Integer> moveList = new LinkedList<Integer>();
		int ownPiece = Piece.PIECE_ROOK;
		if (Colour.isBlack(ownSide)) {
			ownPiece |= Piece.PIECE_BLACK;
		}
		multidirect_addMoves(ownPiece, atSquare, ownSide, moveList, theBoard, Direction.down);
		multidirect_addMoves(ownPiece, atSquare, ownSide, moveList, theBoard, Direction.up);
		multidirect_addMoves(ownPiece, atSquare, ownSide, moveList, theBoard, Direction.left);
		multidirect_addMoves(ownPiece, atSquare, ownSide, moveList, theBoard, Direction.right);
		return moveList;	
	}
	
	static List<Integer> queen_generateMoves(Board theBoard, int atSquare, Piece.Colour ownSide) {
		List<Integer> moveList = new LinkedList<Integer>();
		int ownPiece = Piece.PIECE_QUEEN;
		if (Colour.isBlack(ownSide)) {
			ownPiece |= Piece.PIECE_BLACK;
		}
		multidirect_addMoves(ownPiece, atSquare, ownSide, moveList, theBoard, Direction.downLeft);
		multidirect_addMoves(ownPiece, atSquare, ownSide, moveList, theBoard, Direction.upLeft);
		multidirect_addMoves(ownPiece, atSquare, ownSide, moveList, theBoard, Direction.downRight);
		multidirect_addMoves(ownPiece, atSquare, ownSide, moveList, theBoard, Direction.upRight);
		multidirect_addMoves(ownPiece, atSquare, ownSide, moveList, theBoard, Direction.down);
		multidirect_addMoves(ownPiece, atSquare, ownSide, moveList, theBoard, Direction.up);
		multidirect_addMoves(ownPiece, atSquare, ownSide, moveList, theBoard, Direction.left);
		multidirect_addMoves(ownPiece, atSquare, ownSide, moveList, theBoard, Direction.right);
		return moveList;	
	}
	
	static List<Integer> bishop_generateMoves(Board theBoard, int atSquare, Piece.Colour ownSide) {
		List<Integer> moveList = new LinkedList<Integer>();
		int ownPiece = Piece.PIECE_BISHOP;
		if (Colour.isBlack(ownSide)) {
			ownPiece |= Piece.PIECE_BLACK;
		}
		multidirect_addMoves(ownPiece, atSquare, ownSide, moveList, theBoard, Direction.downLeft);
		multidirect_addMoves(ownPiece, atSquare, ownSide, moveList, theBoard, Direction.upLeft);
		multidirect_addMoves(ownPiece, atSquare, ownSide, moveList, theBoard, Direction.downRight);
		multidirect_addMoves(ownPiece, atSquare, ownSide, moveList, theBoard, Direction.upRight);
		return moveList;	
	}

	private static void multidirect_addMoves(int ownPiece, int atSquare, Piece.Colour ownSide, List<Integer> moveList, Board theBoard, Direction dir) {
		boolean continueAddingMoves = true;
		int targetSquare = atSquare;
		while ( continueAddingMoves ) {
			targetSquare = Direction.getDirectMoveSq(dir, targetSquare);
			if ( targetSquare != Position.NOPOSITION ) {
				int targetPiece = theBoard.getPieceAtSquare(targetSquare);
				if (targetPiece == Piece.PIECE_NONE) {
					// Slider move
					moveList.add( Move.valueOf(Move.TYPE_NONE, atSquare, ownPiece, targetSquare, targetPiece, IntChessman.NOCHESSMAN));
					continueAddingMoves = true;
					continue;
				}
				else if (targetPiece != Piece.PIECE_NONE && Piece.isOppositeColour(ownSide, targetPiece)) {
					// Indicates a capture
					moveList.add( Move.valueOf(Move.TYPE_NONE, atSquare, ownPiece, targetSquare, targetPiece, IntChessman.NOCHESSMAN));
				} else {
					// Indicates blocked by own piece.
				}
			}
			continueAddingMoves = false;
		}
	}
	
	
	private static boolean pawn_isAtInitialPosition(int atSquare, Piece.Colour ownSide) {
		if (Colour.isBlack(ownSide)) {
			return (Position.getRank(atSquare) == IntRank.R7);
		} else {
			return (Position.getRank(atSquare) == IntRank.R2);
		}
	}

	private static int pawn_genOneSqTarget(int atSquare, Piece.Colour ownSide) {
		if (Colour.isBlack(ownSide)) {
			return Direction.getDirectMoveSq(Direction.down, atSquare);
		} else {
			return Direction.getDirectMoveSq(Direction.up, atSquare);
		}
	}	
	
	private static int pawn_genTwoSqTarget(int atSquare, Piece.Colour ownSide) {
		int moveTo = Position.NOPOSITION;
		if ( pawn_isAtInitialPosition(atSquare, ownSide) ) {
			if (Colour.isBlack(ownSide)) {
				moveTo = Direction.getDirectMoveSq(Direction.down, Direction.getDirectMoveSq(Direction.down, atSquare));
			} else {
				moveTo = Direction.getDirectMoveSq(Direction.up, Direction.getDirectMoveSq(Direction.up, atSquare));
			}
		}
		return moveTo;
	}
	
	private static int pawn_genLeftCaptureTarget(int atSquare, Piece.Colour ownSide) {
		if (Colour.isBlack(ownSide)) {
			return Direction.getDirectMoveSq(Direction.downRight, atSquare);
		} else {
			return Direction.getDirectMoveSq(Direction.upLeft, atSquare);
		}
	}
	
	private static int pawn_genRightCaptureTarget(int atSquare, Piece.Colour ownSide) {
		if (Colour.isBlack(ownSide)) {
			return Direction.getDirectMoveSq(Direction.downLeft, atSquare);
		} else {
			return Direction.getDirectMoveSq(Direction.upRight, atSquare);
		}		
	}
	
	private static int pawn_isCapturable(Piece.Colour ownSide, Board theBoard, int captureAt ) {
		int capturePiece = Piece.PIECE_NONE;
		int queryPiece = theBoard.getPieceAtSquare(captureAt);
		if ( queryPiece != Piece.PIECE_NONE ) {
			if (Piece.isOppositeColour( ownSide, queryPiece )) {
				capturePiece = queryPiece;
			}
		} else if (captureAt == theBoard.getEnPassantTargetSq()) {
			capturePiece = Colour.isBlack(ownSide) ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
		}
		return capturePiece;
	}
	
	private static boolean pawn_checkPromotionPossible(Piece.Colour ownSide, int targetSquare ) {
		return (( Colour.isBlack(ownSide) && Position.getRank(targetSquare) == IntRank.R1) || 
				( Colour.isWhite(ownSide) && Position.getRank(targetSquare) == IntRank.R8));
	}
	
	private static void pawn_checkPromotionAddMove(int ownPiece, Board theBoard, int atSquare, Piece.Colour ownSide, List<Integer> moveList,
			int targetSquare, int targetPiece) {
		if ( pawn_checkPromotionPossible( ownSide, targetSquare )) {
			moveList.add( Move.valueOf( Move.TYPE_KBR_PROMOTION, atSquare, ownPiece, targetSquare, targetPiece, IntChessman.KNIGHT ));
			moveList.add( Move.valueOf( Move.TYPE_KBR_PROMOTION, atSquare, ownPiece, targetSquare, targetPiece, IntChessman.BISHOP ));
			moveList.add( Move.valueOf( Move.TYPE_KBR_PROMOTION, atSquare, ownPiece, targetSquare, targetPiece, IntChessman.ROOK ));
			moveList.add( Move.valueOf( Move.TYPE_PROMOTION, atSquare, ownPiece, targetSquare, targetPiece, IntChessman.QUEEN ));
		} else {
			moveList.add( Move.valueOf( Move.TYPE_NONE, atSquare, ownPiece, targetSquare, targetPiece, IntChessman.NOCHESSMAN ) );
		}
	}	
	
	static List<Integer> pawn_generateMoves(Board theBoard, int atSquare, Piece.Colour ownSide) {
		List<Integer> moveList = new LinkedList<Integer>();
		int ownPiece = Piece.PIECE_PAWN;
		if (Colour.isBlack(ownSide)) {
			ownPiece |= Piece.PIECE_BLACK;
		}
		int capturePiece = Piece.PIECE_NONE;
		// Check for standard one and two square moves
		int moveTo = pawn_genOneSqTarget(atSquare, ownSide);
		if ( moveTo != Position.NOPOSITION && theBoard.squareIsEmpty( moveTo )) {
			pawn_checkPromotionAddMove(ownPiece, theBoard, atSquare, ownSide, moveList, moveTo, Piece.PIECE_NONE);
			moveTo = pawn_genTwoSqTarget(atSquare, ownSide);
			if ( moveTo != Position.NOPOSITION && theBoard.squareIsEmpty( moveTo )) {
				moveList.add( Move.valueOf( Move.TYPE_NONE, atSquare, ownPiece, moveTo , Piece.PIECE_NONE, IntChessman.NOCHESSMAN));
			}	
		}
		// Check for capture moves, includes en passant
		int captureAt = pawn_genLeftCaptureTarget(atSquare, ownSide);
		if ( captureAt != Position.NOPOSITION ) {
			capturePiece = pawn_isCapturable(ownSide, theBoard, captureAt);
			if (capturePiece != Piece.PIECE_NONE) {
				pawn_checkPromotionAddMove(ownPiece, theBoard, atSquare, ownSide, moveList, captureAt, capturePiece);
			}
		}
		captureAt = pawn_genRightCaptureTarget(atSquare, ownSide);
		if ( captureAt != Position.NOPOSITION ) {
			capturePiece = pawn_isCapturable(ownSide, theBoard, captureAt);
			if (capturePiece != Piece.PIECE_NONE) {
				pawn_checkPromotionAddMove(ownPiece, theBoard, atSquare, ownSide, moveList, captureAt, capturePiece);
			}
		}
		return moveList;
	}		
}
