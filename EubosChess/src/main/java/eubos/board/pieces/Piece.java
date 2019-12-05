package eubos.board.pieces;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.fluxchess.jcpi.models.GenericChessman;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;

import eubos.board.Board;
import eubos.board.Direction;

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
	};
	
	public static List<GenericMove> king_generateMoves(Board theBoard, GenericPosition atSquare, Piece.Colour ownSide) {
		List<GenericMove> moveList = new LinkedList<GenericMove>();
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

	private static void king_checkAddMove(Piece.Colour ownSide, GenericPosition atSquare, List<GenericMove> moveList, Board theBoard, GenericPosition targetSquare) {
		if ( targetSquare != null ) {
			PieceType targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if ( theBoard.squareIsEmpty(targetSquare) || 
					(targetPiece != PieceType.NONE && PieceType.isOppositeColour(ownSide, targetPiece))) {
				moveList.add( new GenericMove( atSquare, targetSquare ) );
			}
		}
	}	
	
	private static GenericPosition king_getOneSq( Direction dir, GenericPosition atSquare ) {
		return Direction.getDirectMoveSq(dir, atSquare);
	}
	
	private static void knight_checkAddMove(Piece.Colour ownSide, GenericPosition atSquare, List<GenericMove> moveList, Board theBoard, GenericPosition targetSquare) {
		if ( targetSquare != null ) {
			PieceType targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if (theBoard.squareIsEmpty(targetSquare)) {
				moveList.add( new GenericMove( atSquare, targetSquare ));
			}
			else if (targetPiece != PieceType.NONE && PieceType.isOppositeColour(ownSide, targetPiece)) {
				// Indicates a capture
				moveList.add( new GenericMove( atSquare, targetSquare ));
			}
			// Indicates blocked by own piece.
		}
	}
	
	public static List<GenericMove> knight_generateMoves(Board theBoard, GenericPosition atSquare, Piece.Colour ownSide) {
		List<GenericMove> moveList = new LinkedList<GenericMove>();
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
	
	
	public static List<GenericMove> rook_generateMoves(Board theBoard, GenericPosition atSquare, Piece.Colour ownSide) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.down, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.up, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.left, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.right, theBoard));
		return moveList;	
	}
	
	public static List<GenericMove> queen_generateMoves(Board theBoard, GenericPosition atSquare, Piece.Colour ownSide) {
		List<GenericMove> moveList = new LinkedList<GenericMove>();
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
	
	public static List<GenericMove> bishop_generateMoves(Board theBoard, GenericPosition atSquare, Piece.Colour ownSide) {
		List<GenericMove> moveList = new LinkedList<GenericMove>();
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.downLeft, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.upLeft, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.downRight, theBoard));
		multidirect_addMoves(atSquare, ownSide, moveList, theBoard, multidirect_getAllSqs(atSquare, Direction.upRight, theBoard));
		return moveList;	
	}
	
	
	private static List<GenericPosition> multidirect_getAllSqs(GenericPosition atSquare, Direction dir, Board theBoard) {
		ArrayList<GenericPosition> targetSquares = new ArrayList<GenericPosition>();
		GenericPosition currTargetSq = atSquare;
		while ((currTargetSq = Direction.getDirectMoveSq(dir, currTargetSq)) != null) {
			targetSquares.add(currTargetSq);
			if (multidirect_sqConstrainsAttack(theBoard, currTargetSq)) break;
		}
		return targetSquares;
	}
	
	private static boolean multidirect_checkAddMove(Piece.Colour ownSide, GenericPosition atSquare, List<GenericMove> moveList, Board theBoard, GenericPosition targetSquare) {
		boolean continueAddingMoves = false;
		if ( targetSquare != null ) {
			PieceType targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if (theBoard.squareIsEmpty(targetSquare)) {
				moveList.add( new GenericMove( atSquare, targetSquare ));
				continueAddingMoves = true;
			}
			else if (targetPiece != PieceType.NONE && PieceType.isOppositeColour(ownSide, targetPiece)) {
				// Indicates a capture
				moveList.add( new GenericMove( atSquare, targetSquare ));
			}
			// Indicates blocked by own piece.
		}
		return continueAddingMoves;
	}
		
	private static boolean multidirect_sqConstrainsAttack(Board theBoard, GenericPosition targetSquare) {
		boolean constrains = false;
		if ( targetSquare != null ) {
			PieceType targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if (targetPiece != PieceType.NONE) {
				constrains = true;
			}
		}
		return constrains;
	}	

	private static void multidirect_addMoves(GenericPosition atSquare, Piece.Colour ownSide, List<GenericMove> moveList, Board theBoard, List<GenericPosition> targetSqs) {
		boolean continueAddingMoves = true;
		Iterator<GenericPosition> it = targetSqs.iterator();
		while ( it.hasNext() && continueAddingMoves ) {
			continueAddingMoves = multidirect_checkAddMove(ownSide, atSquare, moveList, theBoard, it.next());
		}
	}
	
	
	public static boolean pawn_isAtInitialPosition(GenericPosition atSquare, Piece.Colour ownSide) {
		if (Colour.isBlack(ownSide)) {
			return (atSquare.rank.equals( GenericRank.R7 ));
		} else {
			return (atSquare.rank.equals( GenericRank.R2 ));
		}
	}

	private static GenericPosition pawn_genOneSqTarget(GenericPosition atSquare, Piece.Colour ownSide) {
		if (Colour.isBlack(ownSide)) {
			return king_getOneSq(Direction.down, atSquare);
		} else {
			return king_getOneSq(Direction.up, atSquare);
		}
	}	
	
	private static GenericPosition pawn_genTwoSqTarget(GenericPosition atSquare, Piece.Colour ownSide) {
		GenericPosition moveTo = null;
		if ( pawn_isAtInitialPosition(atSquare, ownSide) ) {
			if (Colour.isBlack(ownSide)) {
				moveTo = Direction.getDirectMoveSq(Direction.down, Direction.getDirectMoveSq(Direction.down, atSquare));
			} else {
				moveTo = Direction.getDirectMoveSq(Direction.up, Direction.getDirectMoveSq(Direction.up, atSquare));
			}
		}
		return moveTo;
	}
	
	private static GenericPosition pawn_genLeftCaptureTarget(GenericPosition atSquare, Piece.Colour ownSide) {
		if (Colour.isBlack(ownSide)) {
			return king_getOneSq(Direction.downRight, atSquare);
		} else {
			return king_getOneSq(Direction.upLeft, atSquare);
		}
	}
	
	private static GenericPosition pawn_genRightCaptureTarget(GenericPosition atSquare, Piece.Colour ownSide) {
		if (Colour.isBlack(ownSide)) {
			return king_getOneSq(Direction.downLeft, atSquare);
		} else {
			return king_getOneSq(Direction.upRight, atSquare);
		}		
	}
	
	private static boolean pawn_isCapturable(Piece.Colour ownSide, Board theBoard, GenericPosition captureAt ) {
		boolean isCapturable = false;
		PieceType queryPiece = theBoard.getPieceAtSquare( captureAt );
		if ( queryPiece != PieceType.NONE ) {
			isCapturable = PieceType.isOppositeColour( ownSide, queryPiece );
		} else if (captureAt == theBoard.getEnPassantTargetSq()) {
			isCapturable = true;
		}
		return isCapturable;
	}
	
	private static boolean pawn_checkPromotionPossible(Piece.Colour ownSide, GenericPosition targetSquare ) {
		return (( Colour.isBlack(ownSide) && targetSquare.rank == GenericRank.R1 ) || 
				( Colour.isWhite(ownSide) && targetSquare.rank == GenericRank.R8 ));
	}
	
	private static void pawn_checkPromotionAddMove(GenericPosition atSquare, Piece.Colour ownSide, List<GenericMove> moveList,
			GenericPosition targetSquare) {
		if ( pawn_checkPromotionPossible( ownSide, targetSquare )) {
			moveList.add( new GenericMove( atSquare, targetSquare, GenericChessman.KNIGHT ));
			moveList.add( new GenericMove( atSquare, targetSquare, GenericChessman.BISHOP ));
			moveList.add( new GenericMove( atSquare, targetSquare, GenericChessman.ROOK ));
			moveList.add( new GenericMove( atSquare, targetSquare, GenericChessman.QUEEN ));
		} else {
			moveList.add( new GenericMove( atSquare, targetSquare ) );
		}
	}	
	
	public static List<GenericMove> pawn_generateMoves(Board theBoard, GenericPosition atSquare, Piece.Colour ownSide) {
		List<GenericMove> moveList = new LinkedList<GenericMove>();
		// Check for standard one and two square moves
		GenericPosition moveTo = pawn_genOneSqTarget(atSquare, ownSide);
		if ( moveTo != null && theBoard.squareIsEmpty( moveTo )) {
			pawn_checkPromotionAddMove(atSquare, ownSide, moveList, moveTo);
			moveTo = pawn_genTwoSqTarget(atSquare, ownSide);
			if ( moveTo != null && theBoard.squareIsEmpty( moveTo )) {
				moveList.add( new GenericMove( atSquare, moveTo ) );
			}	
		}
		// Check for capture moves, includes en passant
		GenericPosition captureAt = pawn_genLeftCaptureTarget(atSquare, ownSide);
		if ( captureAt != null && pawn_isCapturable(ownSide, theBoard,captureAt)) {
			pawn_checkPromotionAddMove(atSquare, ownSide, moveList, captureAt);
		}
		captureAt = pawn_genRightCaptureTarget(atSquare, ownSide);
		if ( captureAt != null && pawn_isCapturable(ownSide, theBoard,captureAt)) {
			pawn_checkPromotionAddMove(atSquare, ownSide, moveList, captureAt);
		}
		return moveList;
	}		
}
