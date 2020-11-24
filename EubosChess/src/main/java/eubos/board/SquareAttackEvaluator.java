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
	
	static private final long[][][] directPieceMove_Lut = new long[128][allDirect.length][];
	static {
		for (int square : Position.values) {
			directPieceMove_Lut[square] = createDiagonalForSq(square);
		}
	}
	static private long [][] createDiagonalForSq(int square) {
		long [][] ret = new long [allDirect.length][];
		int index = 0;
		for (Direction dir: allDirect) {
			ret[index] = getSqsInDirection(dir, square);
			index++;
		}
		return ret;
	}
	static private long[] getSqsInDirection(Direction dir, int fromSq) {
		int newSquare = fromSq;
		long[] sqsInDirection = new long[8];
		int numSquares=0;
		while ((newSquare = Direction.getDirectMoveSq(dir, newSquare)) != Position.NOPOSITION) {
			sqsInDirection[numSquares++] = BitBoard.positionToMask_Lut[newSquare];
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
	
	public static boolean isAttacked( Board bd, int attackedSq, Piece.Colour ownColour ) {
		Colour attackingColour = Piece.Colour.getOpposite(ownColour);
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
				if (attackingPawnsMask != 0) {
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
			attacked = checkForDirectPieceAttacker(bd, attackingColour, attackedSq, isBlackAttacking);
			if (attacked) break;
		} while (false);
		return attacked;	
	}

	private static boolean checkForAttacksHelper(Board theBoard, int attackingPieceType, long attackingSquaresMask) {
		return (theBoard.getMaskForType(attackingPieceType) & attackingSquaresMask) != 0;
	}
	
	private static boolean attackedByPawn(Board theBoard, long attackingPawns, int attackerSq) {
		boolean attacked = false;
		if (attackerSq != Position.NOPOSITION) {
			long atPosMask = BitBoard.positionToMask_Lut[attackerSq];
			attacked = ((attackingPawns & atPosMask) != 0);
		}
		return attacked;
	}
	
	private static boolean checkForDirectPieceAttacker(Board bd, Colour attackingColour, int attackedSq, boolean isBlackAttacking) {
		boolean attacked = false;
		int occupiedSideToCheckFor = isBlackAttacking ? Board.OCCUPIED_BLACK : Board.OCCUPIED_WHITE;
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
					attacked = checkDirectionForDirectPieceAttacker(bd, occupiedSideToCheckFor, attackedSq, dir);
				break;
			case upLeft:
				if ((diagonalAttackersMask & directAttacksOnPositionUpLeft_Lut[attackedSq]) != 0)
					attacked = checkDirectionForDirectPieceAttacker(bd, occupiedSideToCheckFor, attackedSq, dir);
				break;
			case upRight:
				if ((diagonalAttackersMask & directAttacksOnPositionUpRight_Lut[attackedSq]) != 0)
					attacked = checkDirectionForDirectPieceAttacker(bd, occupiedSideToCheckFor, attackedSq, dir);
				break;
			case downRight:
				if ((diagonalAttackersMask & directAttacksOnPositionDownRight_Lut[attackedSq]) != 0)
					attacked = checkDirectionForDirectPieceAttacker(bd, occupiedSideToCheckFor, attackedSq, dir);
				break;
			case left:
				if ((rankFileAttackersMask & directAttacksOnPositionLeft_Lut[attackedSq]) != 0)
					attacked = checkDirectionForDirectPieceAttacker(bd, occupiedSideToCheckFor, attackedSq, dir);
				break;
			case up:
				if ((rankFileAttackersMask & directAttacksOnPositionUp_Lut[attackedSq]) != 0)
					attacked = checkDirectionForDirectPieceAttacker(bd, occupiedSideToCheckFor, attackedSq, dir);
				break;
			case right:
				if ((rankFileAttackersMask & directAttacksOnPositionRight_Lut[attackedSq]) != 0)
					attacked = checkDirectionForDirectPieceAttacker(bd, occupiedSideToCheckFor, attackedSq, dir);
				break;
			case down:
				if ((rankFileAttackersMask & directAttacksOnPositionDown_Lut[attackedSq]) != 0)
					attacked = checkDirectionForDirectPieceAttacker(bd, occupiedSideToCheckFor, attackedSq, dir);
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

	private static boolean checkDirectionForDirectPieceAttacker(Board theBoard, int occupiedSideToCheckFor, int targetSq, Direction dir) {
		boolean attacked = false;
		// one dimension for each direction, other dimension is array of squares in that direction
		long [][] array = SquareAttackEvaluator.directPieceMove_Lut[targetSq]; 
		switch(dir) {
		case downLeft:
		case upLeft:
		case upRight:
		case downRight:
			for (long attackerSq: array[directionIndex_Lut.get(dir)]) {
				int occupied = theBoard.isSquareOccupied(attackerSq);
				if (occupied == Board.OCCUPIED_NONE) {
					// empty square, continue
				} else if (occupied == occupiedSideToCheckFor) {
					// blocked by an enemy piece
					if (theBoard.isBishopOrQueen(attackerSq)) {
						return true;
					} else {
						// else blocked by non-attacking enemy
						return false;
					}
				} else {
					// else blocked by own piece
					return false;
				}
			}
			break;
		case left:
		case up:
		case right:
		case down:
			for (long attackerSq: array[directionIndex_Lut.get(dir)]) {
				int occupied = theBoard.isSquareOccupied(attackerSq);
				if (occupied == Board.OCCUPIED_NONE) {
					// empty square, continue
				} else if (occupied == occupiedSideToCheckFor) {
					// blocked by an enemy piece
					if (theBoard.isRookOrQueen(attackerSq)) {
						return true;
					} else {
						// else blocked by non-attacking enemy
						return false;
					}
				} else {
					// else blocked by own piece
					return false;
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
