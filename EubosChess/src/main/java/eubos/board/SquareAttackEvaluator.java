 package eubos.board;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.pieces.Piece;
import eubos.board.pieces.Piece.PieceType;
import eubos.board.pieces.Piece.Colour;

public class SquareAttackEvaluator {
	
	static private final Map<GenericPosition, GenericPosition[][]> directPieceMove_Lut = new EnumMap<GenericPosition, GenericPosition[][]>(GenericPosition.class);
	static {
		for (GenericPosition square : GenericPosition.values()) {
			directPieceMove_Lut.put(square, createDiagonalForSq(square));
		}
	}
	static private GenericPosition [][] createDiagonalForSq(GenericPosition square) {
		ArrayList<GenericPosition> squaresInDirection = new ArrayList<GenericPosition>();
		GenericPosition [][] ret = new GenericPosition [Direction.values().length][];
		int index = 0;
		for (Direction dir: Direction.values()) {
			squaresInDirection.addAll(getSqsInDirection(dir, square));
			ret[index] = squaresInDirection.toArray(new GenericPosition [0]);
			squaresInDirection.clear();
			index++;
		}
		return ret;
	}
	static private List<GenericPosition> getSqsInDirection(Direction dir, GenericPosition fromSq) {
		GenericPosition newSquare = fromSq;
		ArrayList<GenericPosition> sqsInDirection = new ArrayList<GenericPosition>();
		while ((newSquare = Direction.getDirectMoveSq(dir, newSquare)) != null) {
			sqsInDirection.add(newSquare);
		}
		return sqsInDirection;
	}
	
	static private final Map<GenericPosition, BitBoard> KnightMove_Lut = new EnumMap<GenericPosition, BitBoard>(GenericPosition.class);
	static {
		for (GenericPosition square : GenericPosition.values()) {
			KnightMove_Lut.put(square, createKnightMovesAtSq(square));
		}
	}
	static BitBoard createKnightMovesAtSq(GenericPosition atPos) {
		long mask = 0;
		for (Direction dir: Direction.values()) {
			GenericPosition sq = Direction.getIndirectMoveSq(dir, atPos);
			if (sq != null) {
				mask |= 1L << BitBoard.positionToBit_Lut.get(sq);
			}
		}
		return new BitBoard(mask);
	}
	
	static private final Map<GenericPosition, BitBoard> KingMove_Lut = new EnumMap<GenericPosition, BitBoard>(GenericPosition.class);
	static {
		for (GenericPosition square : GenericPosition.values()) {
			KingMove_Lut.put(square, createKingMovesAtSq(square));
		}
	}
	static BitBoard createKingMovesAtSq(GenericPosition atPos) {
		long mask = 0;
		for (Direction dir: Direction.values()) {
			GenericPosition sq = Direction.getDirectMoveSq(dir, atPos);
			if (sq != null) {
				mask |= 1L << BitBoard.positionToBit_Lut.get(sq);
			}
		}
		return new BitBoard(mask);
	}
	
	public static boolean isAttacked( Board bd, GenericPosition attackedSq, Piece.Colour ownColour ) {
		Colour attackingColour = Piece.Colour.getOpposite(ownColour);
		BitBoard attackingPawnsMask = Colour.isBlack(attackingColour) ? bd.getBlackPawns() : bd.getWhitePawns();
		boolean attacked = false;
		// do/while loop is to allow the function to return attacked=true at earliest possibility
		do {
			if (Colour.isBlack(attackingColour)) {
				if (attackingPawnsMask.isNonZero()) {
					// TODO could have a mask LUT of pawnAttacker masks for each position and colour
					attacked = attackedByPawn(bd, attackingPawnsMask, Direction.getDirectMoveSq(Direction.upRight,attackedSq));
					if (attacked) break;
					attacked = attackedByPawn(bd, attackingPawnsMask, Direction.getDirectMoveSq(Direction.upLeft,attackedSq));
					if (attacked) break;
				}
				attacked = checkForAttacksHelper(PieceType.BlackKing, KingMove_Lut, bd, attackedSq);
				if (attacked) break;
				attacked = checkForAttacksHelper(PieceType.BlackKnight, KnightMove_Lut, bd, attackedSq);
				if (attacked) break;
			} else {
				if (attackingPawnsMask.isNonZero()) {
					attacked = attackedByPawn(bd, attackingPawnsMask, Direction.getDirectMoveSq(Direction.downRight,attackedSq));
					if (attacked) break;
					attacked = attackedByPawn(bd, attackingPawnsMask, Direction.getDirectMoveSq(Direction.downLeft,attackedSq));
					if (attacked) break;
				}
				attacked = checkForAttacksHelper(PieceType.WhiteKing, KingMove_Lut, bd, attackedSq);
				if (attacked) break;
				attacked = checkForAttacksHelper(PieceType.WhiteKnight, KnightMove_Lut, bd, attackedSq);
				if (attacked) break;
			}
			attacked = checkForDirectPieceAttacker(bd, attackingColour, attackedSq);
			if (attacked) break;
		} while (false);
		return attacked;	
	}

	private static boolean checkForAttacksHelper(PieceType AttackerToCheckFor, Map<GenericPosition, BitBoard> map, Board theBoard, GenericPosition attackedSq) {
		boolean attacked = false;
		BitBoard attackersToCheckForMask = theBoard.getMaskForType(AttackerToCheckFor);
		BitBoard attackedBySqs = map.get(attackedSq);
		attacked = attackersToCheckForMask.and(attackedBySqs).isNonZero();
		return attacked;
	}	

	private static boolean checkForDirectPieceAttacker(Board theBoard, Colour attackingColour, GenericPosition targetSq) {
		boolean attacked = false;
		GenericPosition [][] array = SquareAttackEvaluator.directPieceMove_Lut.get(targetSq);
		int index = 0;
		for (Direction dir: Direction.values()) { 
			for (GenericPosition attackerSq: array[index]) {
				PieceType currPiece = theBoard.getPieceAtSquare(attackerSq);
				if (currPiece != PieceType.NONE ) {
					if (dir == Direction.downLeft || dir == Direction.upLeft || dir == Direction.upRight || dir == Direction.downRight) {
						if (Colour.isWhite(attackingColour)) {
							if (currPiece == PieceType.WhiteQueen || currPiece == PieceType.WhiteBishop) {
								attacked = true;
							}
						} else {
							if (currPiece == PieceType.BlackQueen || currPiece == PieceType.BlackBishop) {
								attacked = true;
							}
						} // else blocked by own piece or non-attacking enemy
						break;
					} else if (dir == Direction.left || dir == Direction.up || dir == Direction.right || dir == Direction.down) {
						if (Colour.isWhite(attackingColour)) {
							if (currPiece == PieceType.WhiteQueen || currPiece == PieceType.WhiteRook) {
								attacked = true;
							}
						} else {
							if (currPiece == PieceType.BlackQueen || currPiece == PieceType.BlackRook) {
								attacked = true;
							}
						} // else blocked by own piece or non-attacking enemy
						break;
					}
				}
			}
			if (attacked) break;
			index++;
		}
		return attacked;
	}

	private static boolean attackedByPawn(Board theBoard, BitBoard attackingPawns, GenericPosition attackerSq) {
		boolean attacked = false;
		if (attackerSq != null) {
			BitBoard atPosMask = BitBoard.positionToMask_Lut.get(attackerSq);
			attacked = attackingPawns.and(atPosMask).isNonZero();
		}
		return attacked;
	}
}
