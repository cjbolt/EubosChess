 package eubos.board;

import java.util.Arrays;

import eubos.board.Piece.Colour;
import eubos.position.Position;

public class SquareAttackEvaluator {
	static private final int[][][] directPieceMove_Lut = new int[256][8][];
	static {
		for (int square : Position.values) {
			directPieceMove_Lut[square] = createDiagonalForSq(square);
		}
	}
	static private int [][] createDiagonalForSq(int square) {
		int [][] ret = new int [Direction.values().length][];
		int index = 0;
		for (Direction dir: Direction.values()) {
			ret[index] = getSqsInDirection(dir, square);
			index++;
		}
		return ret;
	}
	static private int[] getSqsInDirection(Direction dir, int fromSq) {
		int newSquare = fromSq;
		int[] sqsInDirection = new int[8];
		int numSquares=0;
		while ((newSquare = Direction.getDirectMoveSq(dir, newSquare)) != Position.NOPOSITION) {
			sqsInDirection[numSquares++] = newSquare;
		}
		return Arrays.copyOf(sqsInDirection, numSquares);
	}
	
	static private Long setAllInDirection(Direction dir, int fromSq, Long currMask, int index) {
		int newSquare = fromSq;
		for (int i=0; i < index; i++) {
			if (newSquare != Position.NOPOSITION)
				newSquare = Direction.getDirectMoveSq(dir, newSquare);
			if (newSquare != Position.NOPOSITION)
				currMask |= BitBoard.positionToMask_Lut[newSquare].getValue();
		}
		return currMask;
	}
	
	static final BitBoard[] KnightMove_Lut = new BitBoard[256];
	static {
		for (int square : Position.values) {
			KnightMove_Lut[square] = createKnightMovesAtSq(square);
		}
	}
	static BitBoard createKnightMovesAtSq(int atPos) {
		long mask = 0;
		for (Direction dir: Direction.values()) {
			int sq = Direction.getIndirectMoveSq(dir, atPos);
			if (sq != Position.NOPOSITION) {
				mask |= 1L << BitBoard.positionToBit_Lut[sq];
			}
		}
		return new BitBoard(mask);
	}
	static final BitBoard[] allAttacksOnPosition_Lut = new BitBoard[256];
	static {
		Direction [] allDirect = { Direction.left, Direction.up, Direction.right, Direction.down, Direction.downLeft, Direction.upLeft, Direction.upRight, Direction.downRight };
		for (int square : Position.values) {
			Long allAttacksMask = 0L;
			// Add direct attacks
			for (Direction dir: allDirect) {
				allAttacksMask = setAllInDirection(dir, square, allAttacksMask, 8);
			}
			// Add indirect attacks
			allAttacksMask |= KnightMove_Lut[square].getValue();
			allAttacksOnPosition_Lut[square] =  new BitBoard(allAttacksMask);
		}
	}
	
	static final BitBoard[] KingMove_Lut = new BitBoard[256];
	static {
		for (int square : Position.values) {
			KingMove_Lut[square] = createKingMovesAtSq(square);
		}
	}
	static BitBoard createKingMovesAtSq(int atPos) {
		long mask = 0;
		for (Direction dir: Direction.values()) {
			int sq = Direction.getDirectMoveSq(dir, atPos);
			if (sq != Position.NOPOSITION) {
				mask |= 1L << BitBoard.positionToBit_Lut[sq];
			}
		}
		return new BitBoard(mask);
	}
	
	public static boolean isAttacked( Board bd, int attackedSq, Piece.Colour ownColour ) {
		Colour attackingColour = Piece.Colour.getOpposite(ownColour);
		boolean isBlackAttacking = Colour.isBlack(attackingColour);
		
		// Early terminate, if no potential attackers
		BitBoard attackers = isBlackAttacking ? bd.getBlackPieces() : bd.getWhitePieces();
		if (allAttacksOnPosition_Lut[attackedSq].and(attackers).isZero())
			return false;
		
		BitBoard attackingPawnsMask = isBlackAttacking ? bd.getBlackPawns() : bd.getWhitePawns();
		// direct piece check is computationally heavy, so just do what is necessary
		boolean doDiagonalCheck = false;
		boolean doRankFileCheck = false;
		boolean attackingQueenPresent = isBlackAttacking ? bd.getBlackQueens().isNonZero() : bd.getWhiteQueens().isNonZero();
		if (attackingQueenPresent) {
			doDiagonalCheck = true;
			doRankFileCheck = true;
		} else {
			boolean attackingRookPresent = isBlackAttacking ? bd.getBlackRooks().isNonZero() : bd.getWhiteRooks().isNonZero();
			if (attackingRookPresent) {
				doRankFileCheck = true;
			}
			boolean attackingBishopPresent = isBlackAttacking ? bd.getBlackBishops().isNonZero() : bd.getWhiteBishops().isNonZero();
			if (attackingBishopPresent) {
				doDiagonalCheck = true;
			}
		}
		boolean attacked = false;
		// do/while loop is to allow the function to return attacked=true at earliest possibility
		do {
			if (isBlackAttacking) {
				if (attackingPawnsMask.isNonZero()) {
					// TODO could have a mask LUT of pawnAttacker masks for each position and colour
					attacked = attackedByPawn(bd, attackingPawnsMask, Direction.getDirectMoveSq(Direction.upRight,attackedSq));
					if (attacked) break;
					attacked = attackedByPawn(bd, attackingPawnsMask, Direction.getDirectMoveSq(Direction.upLeft,attackedSq));
					if (attacked) break;
				}
				attacked = checkForAttacksHelper(bd, Piece.BLACK_KING, KingMove_Lut[attackedSq]);
				if (attacked) break;
				attacked = checkForAttacksHelper(bd, Piece.BLACK_KNIGHT, KnightMove_Lut[attackedSq]);
				if (attacked) break;
			} else {
				if (attackingPawnsMask.isNonZero()) {
					attacked = attackedByPawn(bd, attackingPawnsMask, Direction.getDirectMoveSq(Direction.downRight,attackedSq));
					if (attacked) break;
					attacked = attackedByPawn(bd, attackingPawnsMask, Direction.getDirectMoveSq(Direction.downLeft,attackedSq));
					if (attacked) break;
				}
				attacked = checkForAttacksHelper(bd, Piece.WHITE_KING, KingMove_Lut[attackedSq]);
				if (attacked) break;
				attacked = checkForAttacksHelper(bd, Piece.WHITE_KNIGHT, KnightMove_Lut[attackedSq]);
				if (attacked) break;
			}
			attacked = checkForDirectPieceAttacker(bd, attackingColour, attackedSq, doDiagonalCheck, doRankFileCheck);
			if (attacked) break;
		} while (false);
		return attacked;	
	}

	private static boolean checkForAttacksHelper(Board theBoard, int attackingPieceType, BitBoard attackingSquaresMask) {
		return theBoard.getMaskForType(attackingPieceType).and(attackingSquaresMask).isNonZero();
	}
	
	private static boolean attackedByPawn(Board theBoard, BitBoard attackingPawns, int attackerSq) {
		boolean attacked = false;
		if (attackerSq != Position.NOPOSITION) {
			BitBoard atPosMask = BitBoard.positionToMask_Lut[attackerSq];
			attacked = attackingPawns.and(atPosMask).isNonZero();
		}
		return attacked;
	}

	private static boolean checkForDirectPieceAttacker(Board theBoard, Colour attackingColour,
			int targetSq, boolean doDiagonalCheck, boolean doRankFileCheck) {
		boolean attacked = false;
		// one dimension for each direction, other dimension is array of squares in that direction
		int [][] array = SquareAttackEvaluator.directPieceMove_Lut[targetSq];
		int index = 0;
		for (Direction dir: Direction.values()) { 
			switch(dir) {
			case downLeft:
			case upLeft:
			case upRight:
			case downRight:
				if (doDiagonalCheck) {
					for (int attackerSq: array[index]) {
						int currPiece = theBoard.getPieceAtSquare(attackerSq);
						if (currPiece != Piece.NONE ) {
							if (Colour.isWhite(attackingColour)) {
								if (currPiece == Piece.WHITE_QUEEN || currPiece == Piece.WHITE_BISHOP) {
									attacked = true;
								}
							} else {
								if (currPiece == Piece.BLACK_QUEEN || currPiece == Piece.BLACK_BISHOP) {
									attacked = true;
								}
							} // else blocked by own piece or non-attacking enemy
							break; // break out of this direction search, i.e. get next direction
						}
					}
				}
				break;
			case left:
			case up:
			case right:
			case down:
				if (doRankFileCheck) {
					for (int attackerSq: array[index]) {
						int currPiece = theBoard.getPieceAtSquare(attackerSq);
						if (currPiece != Piece.NONE ) {
							if (Colour.isWhite(attackingColour)) {
								if (currPiece == Piece.WHITE_QUEEN || currPiece == Piece.WHITE_ROOK) {
									attacked = true;
								}
							} else {
								if (currPiece == Piece.BLACK_QUEEN || currPiece == Piece.BLACK_ROOK) {
									attacked = true;
								}
							} // else blocked by own piece or non-attacking enemy
							break; // break out of this direction search, i.e. get next direction
						} 
					}
				}
				break;
			default:
				break; // indirect move?
			}
			if (attacked) break;
			index++;
		}
		return attacked;
	}
}
