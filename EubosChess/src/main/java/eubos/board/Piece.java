package eubos.board;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.fluxchess.jcpi.models.IntChessman;
import com.fluxchess.jcpi.models.IntRank;

import eubos.position.Move;
import eubos.position.MoveList.MoveClassification;
import eubos.position.Position;

public abstract class Piece {
	public enum Colour { 
		white, black;
		
		public static Colour getOpposite( Colour arg ) { return (arg == white) ? black : white; }
		public static boolean isWhite( Colour arg ) { return arg == white; }
		public static boolean isBlack( Colour arg ) { return arg == black; }
	};
	
	public enum PieceType {
		WhiteKing,
		WhiteQueen,
		WhiteRook,
		WhiteBishop,
		WhiteKnight,
		WhitePawn,
		BlackKing,
		BlackQueen,
		BlackRook,
		BlackBishop,
		BlackKnight,
		BlackPawn,
		NONE;
		
		public static boolean isPawn(PieceType arg) { return arg==WhitePawn || arg==BlackPawn; }
		public static boolean isKing(PieceType arg) { return arg==WhiteKing || arg==BlackKing; }
		public static boolean isQueen(PieceType arg) { return arg==WhiteQueen || arg==BlackQueen; }
		public static boolean isRook(PieceType arg) { return arg==WhiteRook || arg==BlackRook; }
		public static boolean isBishop(PieceType arg) { return arg==WhiteBishop || arg==BlackBishop; }
		public static boolean isKnight(PieceType arg) { return arg==WhiteKnight || arg==BlackKnight; }
		
		public static boolean isOppositeColour(Colour ownColour, PieceType toCheck) {
			assert toCheck != PieceType.NONE;
			return Colour.isWhite(ownColour) ? isBlack(toCheck) : isWhite(toCheck);
		}
		public static boolean isWhite(PieceType arg) {
			return arg.ordinal() < PieceType.BlackKing.ordinal();
		}
		public static boolean isBlack(PieceType arg) {
			return arg.ordinal() >= PieceType.BlackKing.ordinal();
		}
		public static Colour getOpposite(PieceType arg) {
			return PieceType.isWhite(arg) ? Colour.black : Colour.white;
		} 
	};
	
	static List<Integer> king_generateMoves(Board theBoard, int atSquare, Piece.Colour ownSide) {
		List<Integer> moveList = new LinkedList<Integer>();
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, king_getOneSq(Direction.up, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, king_getOneSq(Direction.upRight, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, king_getOneSq(Direction.right, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, king_getOneSq(Direction.downRight, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, king_getOneSq(Direction.down, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, king_getOneSq(Direction.downLeft, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, king_getOneSq(Direction.left, atSquare));
		king_checkAddMove(ownSide, atSquare, moveList, theBoard, king_getOneSq(Direction.upLeft, atSquare));
		return moveList;
	}

	private static void king_checkAddMove(Piece.Colour ownSide, int atSquare, List<Integer> moveList, Board theBoard, int targetSquare) {
		if ( targetSquare != Position.NOPOSITION ) {
			PieceType targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if ( theBoard.squareIsEmpty(targetSquare) || 
					(targetPiece != PieceType.NONE && PieceType.isOppositeColour(ownSide, targetPiece))) {
				moveList.add(Move.valueOf(atSquare, targetSquare));
			}
		}
	}	
	
	private static int king_getOneSq( Direction dir, int atSquare ) {
		return Direction.getDirectMoveSq(dir, atSquare);
	}
	
	private static void knight_checkAddMove(Piece.Colour ownSide, int atSquare, List<Integer> moveList, Board theBoard, int targetSquare) {
		if ( targetSquare != Position.NOPOSITION ) {
			PieceType targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if (theBoard.squareIsEmpty(targetSquare)) {
				moveList.add( Move.valueOf(atSquare, targetSquare));
			}
			else if (targetPiece != PieceType.NONE && PieceType.isOppositeColour(ownSide, targetPiece)) {
				// Indicates a capture
				moveList.add( Move.valueOf(atSquare, targetSquare));
			}
			// Indicates blocked by own piece.
		}
	}
	
	static List<Integer> knight_generateMoves(Board theBoard, int atSquare, Piece.Colour ownSide) {
		List<Integer> moveList = new LinkedList<Integer>();
		knight_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.upRight, atSquare));
		knight_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.upLeft, atSquare));
		knight_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.rightUp, atSquare));
		knight_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.rightDown, atSquare));
		knight_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.downRight, atSquare));
		knight_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.downLeft, atSquare));
		knight_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.leftUp, atSquare));
		knight_checkAddMove(ownSide, atSquare, moveList, theBoard, Direction.getIndirectMoveSq(Direction.leftDown, atSquare));
		return moveList;		
	}
	
	
	static List<Integer> rook_generateMoves(Board theBoard, int atSquare, Piece.Colour ownSide) {
		LinkedList<Integer> moveList = new LinkedList<Integer>();
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.down, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.up, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.left, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.right, theBoard));
		return moveList;	
	}
	
	static List<Integer> queen_generateMoves(Board theBoard, int atSquare, Piece.Colour ownSide) {
		List<Integer> moveList = new LinkedList<Integer>();
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.downLeft, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.upLeft, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.downRight, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.upRight, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.down, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.up, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.left, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.right, theBoard));
		return moveList;	
	}
	
	static List<Integer> bishop_generateMoves(Board theBoard, int atSquare, Piece.Colour ownSide) {
		List<Integer> moveList = new LinkedList<Integer>();
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.downLeft, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.upLeft, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.downRight, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.upRight, theBoard));
		return moveList;	
	}
	
	
	private static List<Integer> multidirect_getAllSqs(int atSquare, Direction dir, Board theBoard) {
		ArrayList<Integer> targetSquares = new ArrayList<Integer>();
		int currTargetSq = atSquare;
		while ((currTargetSq = Direction.getDirectMoveSq(dir, currTargetSq)) != Position.NOPOSITION) {
			targetSquares.add(currTargetSq);
			if (multidirect_sqConstrainsAttack(theBoard, currTargetSq)) break;
		}
		return targetSquares;
	}
	
	private static boolean multidirect_checkAddMove(Piece.Colour ownSide, int atSquare, List<Integer> moveList, Board theBoard, int targetSquare) {
		boolean continueAddingMoves = false;
		if ( targetSquare != Position.NOPOSITION ) {
			PieceType targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if (theBoard.squareIsEmpty(targetSquare)) {
				moveList.add( Move.valueOf(atSquare, targetSquare));
				continueAddingMoves = true;
			}
			else if (targetPiece != PieceType.NONE && PieceType.isOppositeColour(ownSide, targetPiece)) {
				// Indicates a capture
				moveList.add( Move.valueOf(atSquare, targetSquare));
			}
			// Indicates blocked by own piece.
		}
		return continueAddingMoves;
	}
		
	private static boolean multidirect_sqConstrainsAttack(Board theBoard, int targetSquare) {
		boolean constrains = false;
		if ( targetSquare != Position.NOPOSITION ) {
			PieceType targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if (targetPiece != PieceType.NONE) {
				constrains = true;
			}
		}
		return constrains;
	}	

	private static void multidirect_addMoves(int atSquare, Piece.Colour ownSide, List<Integer> moveList, Board theBoard, List<Integer> targetSqs) {
		boolean continueAddingMoves = true;
		Iterator<Integer> it = targetSqs.iterator();
		while ( it.hasNext() && continueAddingMoves ) {
			continueAddingMoves = multidirect_checkAddMove(ownSide, atSquare, moveList, theBoard, it.next());
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
			return king_getOneSq(Direction.down, atSquare);
		} else {
			return king_getOneSq(Direction.up, atSquare);
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
			return king_getOneSq(Direction.downRight, atSquare);
		} else {
			return king_getOneSq(Direction.upLeft, atSquare);
		}
	}
	
	private static int pawn_genRightCaptureTarget(int atSquare, Piece.Colour ownSide) {
		if (Colour.isBlack(ownSide)) {
			return king_getOneSq(Direction.downLeft, atSquare);
		} else {
			return king_getOneSq(Direction.upRight, atSquare);
		}		
	}
	
	private static boolean pawn_isCapturable(Piece.Colour ownSide, Board theBoard, int captureAt ) {
		boolean isCapturable = false;
		PieceType queryPiece = theBoard.getPieceAtSquare( captureAt );
		if ( queryPiece != PieceType.NONE ) {
			isCapturable = PieceType.isOppositeColour( ownSide, queryPiece );
		} else if (captureAt == theBoard.getEnPassantTargetSq()) {
			isCapturable = true;
		}
		return isCapturable;
	}
	
	private static boolean pawn_checkPromotionPossible(Piece.Colour ownSide, int targetSquare ) {
		return (( Colour.isBlack(ownSide) && Position.getRank(targetSquare) == IntRank.R1) || 
				( Colour.isWhite(ownSide) && Position.getRank(targetSquare) == IntRank.R8));
	}
	
	private static void pawn_checkPromotionAddMove(int atSquare, Piece.Colour ownSide, List<Integer> moveList,
			int targetSquare) {
		if ( pawn_checkPromotionPossible( ownSide, targetSquare )) {
			moveList.add( Move.valueOf( MoveClassification.OTHER_PROMOTION.ordinal(), atSquare, targetSquare, IntChessman.KNIGHT ));
			moveList.add( Move.valueOf( MoveClassification.OTHER_PROMOTION.ordinal(), atSquare, targetSquare, IntChessman.BISHOP ));
			moveList.add( Move.valueOf( MoveClassification.OTHER_PROMOTION.ordinal(), atSquare, targetSquare, IntChessman.ROOK ));
			moveList.add( Move.valueOf( MoveClassification.PROMOTION.ordinal(), atSquare, targetSquare, IntChessman.QUEEN ));
		} else {
			moveList.add( Move.valueOf( atSquare, targetSquare ) );
		}
	}	
	
	static List<Integer> pawn_generateMoves(Board theBoard, int atSquare, Piece.Colour ownSide) {
		List<Integer> moveList = new LinkedList<Integer>();
		// Check for standard one and two square moves
		int moveTo = pawn_genOneSqTarget(atSquare, ownSide);
		if ( moveTo != Position.NOPOSITION && theBoard.squareIsEmpty( moveTo )) {
			pawn_checkPromotionAddMove(atSquare, ownSide, moveList, moveTo);
			moveTo = pawn_genTwoSqTarget(atSquare, ownSide);
			if ( moveTo != Position.NOPOSITION && theBoard.squareIsEmpty( moveTo )) {
				moveList.add( Move.valueOf( atSquare, moveTo ) );
			}	
		}
		// Check for capture moves, includes en passant
		int captureAt = pawn_genLeftCaptureTarget(atSquare, ownSide);
		if ( captureAt != Position.NOPOSITION && pawn_isCapturable(ownSide, theBoard,captureAt)) {
			pawn_checkPromotionAddMove(atSquare, ownSide, moveList, captureAt);
		}
		captureAt = pawn_genRightCaptureTarget(atSquare, ownSide);
		if ( captureAt != Position.NOPOSITION && pawn_isCapturable(ownSide, theBoard,captureAt)) {
			pawn_checkPromotionAddMove(atSquare, ownSide, moveList, captureAt);
		}
		return moveList;
	}		
}
