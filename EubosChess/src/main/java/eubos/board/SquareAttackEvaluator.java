 package eubos.board;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.position.Position;

public class SquareAttackEvaluator {
	
	static final Direction [] allDirect = { Direction.left, Direction.up, Direction.right, Direction.down, Direction.downLeft, Direction.upLeft, Direction.upRight, Direction.downRight };
	
	private static final Map<Direction, Integer> directionIndex_Lut = new EnumMap<Direction, Integer>(Direction.class);
	static {
		// Indexes as specified by the order of the array SquareAttackEvaluator.allDirect
		directionIndex_Lut.put(Direction.left, 0);
		directionIndex_Lut.put(Direction.up, 1);
		directionIndex_Lut.put(Direction.right, 2);
		directionIndex_Lut.put(Direction.down, 3);
		directionIndex_Lut.put(Direction.downLeft, 4);
		directionIndex_Lut.put(Direction.upLeft, 5);
		directionIndex_Lut.put(Direction.upRight, 6);
		directionIndex_Lut.put(Direction.downRight, 7);
	}
	
	static private final int[][][] directPieceMove_Lut = new int[128][allDirect.length][];
	static {
		for (int square : Position.values) {
			directPieceMove_Lut[square] = createDiagonalForSq(square);
		}
	}
	static private int [][] createDiagonalForSq(int square) {
		int [][] ret = new int [allDirect.length][];
		int index = 0;
		for (Direction dir: allDirect) {
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
				currMask |= BitBoard.positionToMask_Lut[newSquare];
		}
		return currMask;
	}
	
	static final long[] KnightMove_Lut = new long[128];
	static {
		for (int square : Position.values) {
			KnightMove_Lut[square] = createKnightMovesAtSq(square);
		}
	}
	static long createKnightMovesAtSq(int atPos) {
		long mask = 0;
		for (Direction dir: Direction.values()) {
			int sq = Direction.getIndirectMoveSq(dir, atPos);
			if (sq != Position.NOPOSITION) {
				mask |= BitBoard.positionToMask_Lut[sq];
			}
		}
		return mask;
	}
	
	static final long[] allAttacksOnPosition_Lut = new long[128];
	static {
		for (int square : Position.values) {
			Long allAttacksMask = 0L;
			// Add direct attacks
			for (Direction dir: allDirect) {
				allAttacksMask = setAllInDirection(dir, square, allAttacksMask, 8);
			}
			// Add indirect attacks
			allAttacksMask |= KnightMove_Lut[square];
			allAttacksOnPosition_Lut[square] = allAttacksMask;
		}
	}
	
	static final long[] directAttacksOnPositionUp_Lut = new long[128];
	static {
		for (int square : Position.values) {
			long mask = 0L;
			Long finalMask = setAllInDirection(Direction.up, square, mask, 8);
			directAttacksOnPositionUp_Lut[square] = finalMask;
		}
	}
	
	static final long[] directAttacksOnPositionUpLeft_Lut = new long[128];
	static {
		for (int square : Position.values) {
			long mask = 0L;
			Long finalMask = setAllInDirection(Direction.upLeft, square, mask, 8);
			directAttacksOnPositionUpLeft_Lut[square] = finalMask;
		}
	}
	
	static final long[] directAttacksOnPositionLeft_Lut = new long[128];
	static {
		for (int square : Position.values) {
			long mask = 0L;
			Long finalMask = setAllInDirection(Direction.left, square, mask, 8);
			directAttacksOnPositionLeft_Lut[square] = finalMask;
		}
	}
	
	static final long[] directAttacksOnPositionDownLeft_Lut = new long[128];
	static {
		for (int square : Position.values) {
			long mask = 0L;
			Long finalMask = setAllInDirection(Direction.downLeft, square, mask, 8);
			directAttacksOnPositionDownLeft_Lut[square] = finalMask;
		}
	}
	
	static final long[] directAttacksOnPositionDown_Lut = new long[128];
	static {
		for (int square : Position.values) {
			long mask = 0L;
			Long finalMask = setAllInDirection(Direction.down, square, mask, 8);
			directAttacksOnPositionDown_Lut[square] = finalMask;
		}
	}
	
	static final long[] directAttacksOnPositionDownRight_Lut = new long[128];
	static {
		for (int square : Position.values) {
			long mask = 0L;
			Long finalMask = setAllInDirection(Direction.downRight, square, mask, 8);
			directAttacksOnPositionDownRight_Lut[square] = finalMask;
		}
	}
	
	static final long[] directAttacksOnPositionRight_Lut = new long[128];
	static {
		for (int square : Position.values) {
			long mask = 0L;
			Long finalMask = setAllInDirection(Direction.right, square, mask, 8);
			directAttacksOnPositionRight_Lut[square] = finalMask;
		}
	}
	
	static final long[] directAttacksOnPositionUpRight_Lut = new long[128];
	static {
		for (int square : Position.values) {
			long mask = 0L;
			Long finalMask = setAllInDirection(Direction.upRight, square, mask, 8);
			directAttacksOnPositionUpRight_Lut[square] = finalMask;
		}
	}
	
	static final long[] KingMove_Lut = new long[128];
	static {
		for (int square : Position.values) {
			KingMove_Lut[square] = createKingMovesAtSq(square);
		}
	}
	static long createKingMovesAtSq(int atPos) {
		long mask = 0;
		for (Direction dir: Direction.values()) {
			int sq = Direction.getDirectMoveSq(dir, atPos);
			if (sq != Position.NOPOSITION) {
				mask |= BitBoard.positionToMask_Lut[sq];
			}
		}
		return mask;
	}
	
	static final long[] BlackPawnAttacks_Lut = new long[128];
	static {
		for (int square : Position.values) {
			BlackPawnAttacks_Lut[square] = createBlackPawnMovesAtSq(square);
		}
	}
	static long createBlackPawnMovesAtSq(int atPos) {
		long mask = 0;
		if (Position.getRank(atPos) != 7) {
			int sq = Direction.getDirectMoveSq(Direction.upRight, atPos);
			if (sq != Position.NOPOSITION) {
				mask |= BitBoard.positionToMask_Lut[sq];
			}
			sq = Direction.getDirectMoveSq(Direction.upLeft, atPos);
			if (sq != Position.NOPOSITION) {
				mask |= BitBoard.positionToMask_Lut[sq];
			}
		}
		return mask;
	}
	
	static final long[] WhitePawnAttacks_Lut = new long[128];
	static {
		for (int square : Position.values) {
			WhitePawnAttacks_Lut[square] = createWhitePawnMovesAtSq(square);
		}
	}
	static long createWhitePawnMovesAtSq(int atPos) {
		long mask = 0;
		if (Position.getRank(atPos) != 0) {
			int sq = Direction.getDirectMoveSq(Direction.downRight, atPos);
			if (sq != Position.NOPOSITION) {
				mask |= BitBoard.positionToMask_Lut[sq];
			}
			sq = Direction.getDirectMoveSq(Direction.downLeft, atPos);
			if (sq != Position.NOPOSITION) {
				mask |= BitBoard.positionToMask_Lut[sq];
			}
		}
		return mask;
	}
	
	public static boolean isAttacked( Board bd, int attackedSq, Piece.Colour attackingColour ) {
		boolean isBlackAttacking = Colour.isBlack(attackingColour);
		
		// Early terminate, if no potential attackers
		long attackers = isBlackAttacking ? bd.getBlackPieces() : bd.getWhitePieces();
		if ((allAttacksOnPosition_Lut[attackedSq] & attackers) == 0)
			return false;
		
		long attackingPawnsMask = isBlackAttacking ? bd.getBlackPawns() : bd.getWhitePawns();
		boolean attacked = false;
		// do/while loop is to allow the function to return attacked=true at earliest possibility
		do {
			if (isBlackAttacking) {
				if (attackingPawnsMask != 0) {
					attacked = (attackingPawnsMask & BlackPawnAttacks_Lut[attackedSq]) != 0;
					if (attacked) break;
					attacked = (attackingPawnsMask & BlackPawnAttacks_Lut[attackedSq]) != 0;
					if (attacked) break;
				}
				attacked = (bd.getBlackKing() & KingMove_Lut[attackedSq]) != 0;;
				if (attacked) break;
				attacked = (bd.getBlackKnights() & KnightMove_Lut[attackedSq]) != 0;
				if (attacked) break;
			} else {
				if (attackingPawnsMask != 0) {
					attacked = (attackingPawnsMask & WhitePawnAttacks_Lut[attackedSq]) != 0;
					if (attacked) break;
					attacked = (attackingPawnsMask & WhitePawnAttacks_Lut[attackedSq]) != 0;
					if (attacked) break;
				}
				attacked = (bd.getWhiteKing() & KingMove_Lut[attackedSq]) != 0;
				if (attacked) break;
				attacked = (bd.getWhiteKnights() & KnightMove_Lut[attackedSq]) != 0;
				if (attacked) break;
			}
			attacked = checkForDirectPieceAttacker(bd, attackingColour, attackedSq, isBlackAttacking);
			if (attacked) break;
		} while (false);
		return attacked;	
	}

	private static boolean checkForDirectPieceAttacker(Board bd, Colour attackingColour, int attackedSq, boolean isBlackAttacking) {
		boolean attacked = false;
		// direct piece check is computationally heavy, so just do what is necessary
		long attackingQueensMask = isBlackAttacking ? bd.getBlackQueens() : bd.getWhiteQueens();
		long attackingRooksMask = isBlackAttacking ? bd.getBlackRooks() : bd.getWhiteRooks();
		long attackingBishopsMask = isBlackAttacking ? bd.getBlackBishops() : bd.getWhiteBishops();
		// create masks of attackers
		long diagonalAttackersMask = attackingQueensMask | attackingBishopsMask;
		long rankFileAttackersMask = attackingQueensMask | attackingRooksMask;	
		for (Direction dir: allDirect) { 
			switch(dir) {
			case downLeft:
				if ((diagonalAttackersMask & directAttacksOnPositionDownLeft_Lut[attackedSq]) != 0)
					attacked = checkDirectionForDirectPieceAttacker(bd, isBlackAttacking, attackedSq, dir);
				break;
			case upLeft:
				if ((diagonalAttackersMask & directAttacksOnPositionUpLeft_Lut[attackedSq]) != 0)
					attacked = checkDirectionForDirectPieceAttacker(bd, isBlackAttacking, attackedSq, dir);
				break;
			case upRight:
				if ((diagonalAttackersMask & directAttacksOnPositionUpRight_Lut[attackedSq]) != 0)
					attacked = checkDirectionForDirectPieceAttacker(bd, isBlackAttacking, attackedSq, dir);
				break;
			case downRight:
				if ((diagonalAttackersMask & directAttacksOnPositionDownRight_Lut[attackedSq]) != 0)
					attacked = checkDirectionForDirectPieceAttacker(bd, isBlackAttacking, attackedSq, dir);
				break;
			case left:
				if ((rankFileAttackersMask & directAttacksOnPositionLeft_Lut[attackedSq]) != 0)
					attacked = checkDirectionForDirectPieceAttacker(bd, isBlackAttacking, attackedSq, dir);
				break;
			case up:
				if ((rankFileAttackersMask & directAttacksOnPositionUp_Lut[attackedSq]) != 0)
					attacked = checkDirectionForDirectPieceAttacker(bd, isBlackAttacking, attackedSq, dir);
				break;
			case right:
				if ((rankFileAttackersMask & directAttacksOnPositionRight_Lut[attackedSq]) != 0)
					attacked = checkDirectionForDirectPieceAttacker(bd, isBlackAttacking, attackedSq, dir);
				break;
			case down:
				if ((rankFileAttackersMask & directAttacksOnPositionDown_Lut[attackedSq]) != 0)
					attacked = checkDirectionForDirectPieceAttacker(bd, isBlackAttacking, attackedSq, dir);
				break;
			default:
				if (EubosEngineMain.ASSERTS_ENABLED)
					assert false; // should not receive indirect moves here!
				break;
			}
			if (attacked) break;
		}
		return attacked;
	}

	private static boolean checkDirectionForDirectPieceAttacker(Board theBoard, boolean attackerIsBlack, int targetSq, Direction dir) {
		boolean attacked = false;
		// one dimension for each direction, other dimension is array of squares in that direction
		int [][] array = SquareAttackEvaluator.directPieceMove_Lut[targetSq]; 
		switch(dir) {
		case downLeft:
		case upLeft:
		case upRight:
		case downRight:
			for (int attackerSq: array[directionIndex_Lut.get(dir)]) {
				int currPiece = theBoard.getPieceAtSquare(attackerSq);
				if (currPiece != Piece.NONE ) {
					if (attackerIsBlack) {
						if (currPiece == Piece.BLACK_QUEEN || currPiece == Piece.BLACK_BISHOP) {
							attacked = true;
						}
					} else {
						if (currPiece == Piece.WHITE_QUEEN || currPiece == Piece.WHITE_BISHOP) {
							attacked = true;
						}
					} // else blocked by own piece or non-attacking enemy
					break;
				}
			}
			break;
		case left:
		case up:
		case right:
		case down:
			for (int attackerSq: array[directionIndex_Lut.get(dir)]) {
				int currPiece = theBoard.getPieceAtSquare(attackerSq);
				if (currPiece != Piece.NONE ) {
					if (attackerIsBlack) {
						if (currPiece == Piece.BLACK_QUEEN || currPiece == Piece.BLACK_ROOK) {
							attacked = true;
						}
					} else {
						if (currPiece == Piece.WHITE_QUEEN || currPiece == Piece.WHITE_ROOK) {
							attacked = true;
						}
					} // else blocked by own piece or non-attacking enemy
					break;
				} 
			}
			break;
		default:
			if (EubosEngineMain.ASSERTS_ENABLED)
				assert false; // should not receive indirect moves here!
			break;
		}
		return attacked;
	}
}
