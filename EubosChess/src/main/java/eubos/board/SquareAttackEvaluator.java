 package eubos.board;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import eubos.position.Position;

public class SquareAttackEvaluator {
	
	static final Direction [] rankFile = { Direction.down, Direction.up, Direction.left, Direction.right };
	
	static final Direction [] diagonals = { Direction.downLeft, Direction.upLeft, Direction.downRight, Direction.upRight };
	
	static final Direction [] allDirect = { Direction.downLeft, Direction.upLeft, Direction.downRight, Direction.upRight, Direction.down, Direction.up, Direction.left, Direction.right };
	
	static final Map<Direction, Integer> directionIndex_Lut = new EnumMap<Direction, Integer>(Direction.class);
	static {
		// Indexes as specified by the order of the array SquareAttackEvaluator.allDirect
		directionIndex_Lut.put(Direction.downLeft, 0);
		directionIndex_Lut.put(Direction.upLeft, 1);
		directionIndex_Lut.put(Direction.downRight, 2);
		directionIndex_Lut.put(Direction.upRight, 3);
		directionIndex_Lut.put(Direction.down, 4);
		directionIndex_Lut.put(Direction.up, 5);
		directionIndex_Lut.put(Direction.left, 6);
		directionIndex_Lut.put(Direction.right, 7);
	}
	
	public static String reportStaticDataSizes() {
		StringBuilder s = new StringBuilder();
		s.append(String.format("DirectPieceMove_Lut_Size %d bytes\n", directPieceMove_Lut_Size*4));
		s.append(String.format("All In Direction Masks %d bytes\n", 8*128*8));
		s.append(String.format("Knight, King, Pawn W/B All + Direct masks %d bytes\n", 6*128*8));
		return s.toString();
	}
	
	public static int getStaticDataSize() {
		return (directPieceMove_Lut_Size * 4) + ((6+8)*128*8);
	}
	
	/* 3-dimensional array:
	 * 1st is a position integer, this is the origin square
	 * 2nd is a direction from the origin square (diagonal/rank+file) i.e all direct attack directions from origin square
	 * 3rd is a position integer, representing all the squares on the board in that direction */
	static int directPieceMove_Lut_Size = 0;
	static final int[][][] directPieceMove_Lut = new int[128][allDirect.length][];
	static {
		for (int square : Position.values) {
			directPieceMove_Lut[square] = createLinesFromSq(square);
		}
	}
	static private int [][] createLinesFromSq(int square) {
		int [][] ret = new int [allDirect.length][];
		int index = 0;
		for (Direction dir: allDirect) {
			ret[index] = getSqsInDirection(dir, square);
			directPieceMove_Lut_Size += ret[index].length;
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
	
	/* 1-dimensional array:
	 * 1st index is a position integer, this is the target square
	 * indexes a bit mask of the squares that attack the target square by a Knight (indirect) move */
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
	
	/* 1-dimensional array:
	 * 1st index is a position integer, this is the target square
	 * indexes a bit mask of all the squares on the board that can attack the target square by either a direct or indirect move */
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
	
	/* The following 1-dimensional arrays provide bit masks of all the squares that can directly attack the target square:
	 * 1st index is a position integer, this is the target square */	
	static final long[] directAttacksOnPosition_Lut = new long[128];
	static {
		for (int square : Position.values) {
			Long allAttacksMask = 0L;
			for (Direction dir: allDirect) {
				allAttacksMask = setAllInDirection(dir, square, allAttacksMask, 8);
			}
			directAttacksOnPosition_Lut[square] = allAttacksMask;
		}
	}
	
	/* The following 1-dimensional arrays provide bit masks of all the squares that can directly attack the target square:
	 * 1st index is a position integer, this is the target square */	
	static final long[] directRankFileAttacksOnPosition_Lut = new long[128];
	static {
		for (int square : Position.values) {
			Long allAttacksMask = 0L;
			for (Direction dir: rankFile) {
				allAttacksMask = setAllInDirection(dir, square, allAttacksMask, 8);
			}
			directRankFileAttacksOnPosition_Lut[square] = allAttacksMask;
		}
	}
	
	/* The following 1-dimensional arrays provide bit masks of all the squares that can directly attack the target square:
	 * 1st index is a position integer, this is the target square */	
	static final long[] directDiagonalAttacksOnPosition_Lut = new long[128];
	static {
		for (int square : Position.values) {
			Long allAttacksMask = 0L;
			for (Direction dir: diagonals) {
				allAttacksMask = setAllInDirection(dir, square, allAttacksMask, 8);
			}
			directDiagonalAttacksOnPosition_Lut[square] = allAttacksMask;
		}
	}
	
	/* The following 1-dimensional arrays provide bit masks of all the squares in a direction that can attack the target square:
	 * 1st index is a position integer, this is the target square */
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
	
	static final long[][] directAttacksOnPositionAll_Lut = new long[allDirect.length][];
	static {
		directAttacksOnPositionAll_Lut[directionIndex_Lut.get(Direction.up)] = directAttacksOnPositionUp_Lut;
		directAttacksOnPositionAll_Lut[directionIndex_Lut.get(Direction.upRight)] = directAttacksOnPositionUpRight_Lut;
		directAttacksOnPositionAll_Lut[directionIndex_Lut.get(Direction.right)] = directAttacksOnPositionRight_Lut;
		directAttacksOnPositionAll_Lut[directionIndex_Lut.get(Direction.downRight)] = directAttacksOnPositionDownRight_Lut;
		directAttacksOnPositionAll_Lut[directionIndex_Lut.get(Direction.down)] = directAttacksOnPositionDown_Lut;
		directAttacksOnPositionAll_Lut[directionIndex_Lut.get(Direction.downLeft)] = directAttacksOnPositionDownLeft_Lut;
		directAttacksOnPositionAll_Lut[directionIndex_Lut.get(Direction.left)] = directAttacksOnPositionLeft_Lut;
		directAttacksOnPositionAll_Lut[directionIndex_Lut.get(Direction.upLeft)] = directAttacksOnPositionUpLeft_Lut;
	}
	
	/* 1-dimensional array:
	 * 1st index is a position integer, this is the origin square
	 * indexes a bit mask of the squares that the origin square can attack by a King move */
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
	
	/* 1-dimensional array:
	 * 1st index is a position integer, this is the origin square
	 * indexes a bit mask of the squares that the origin square can attack by a Black Pawn capture */
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
	
	/* 1-dimensional array:
	 * 1st index is a position integer, this is the origin square
	 * indexes a bit mask of the squares that the origin square can attack by a White Pawn capture */
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
	
	public static boolean isAttacked( Board bd, int attackedSq, boolean isBlackAttacking) {
		boolean attacked = false;
		
		// Knights
		long attackingKnightsMask = isBlackAttacking ? bd.getBlackKnights() : bd.getWhiteKnights();
		attacked = (attackingKnightsMask & KnightMove_Lut[attackedSq]) != 0;
		if (attacked) return true;
		
		// Pawns
		long attackingPawnsMask = isBlackAttacking ? bd.getBlackPawns() : bd.getWhitePawns();
		attacked = (attackingPawnsMask & (isBlackAttacking ? BlackPawnAttacks_Lut[attackedSq] : WhitePawnAttacks_Lut[attackedSq])) != 0;
		if (attacked) return true;
		
		// Kings
		long attackingKingMask = isBlackAttacking ? bd.getBlackKing() : bd.getWhiteKing();
		attacked = (attackingKingMask & KingMove_Lut[attackedSq]) != 0;
		if (attacked) return true;
		
		// Sliders
		long diagonalAttackersMask = isBlackAttacking ? bd.getBlackDiagonal() : bd.getWhiteDiagonal();
		long rankFileAttackersMask = isBlackAttacking ? bd.getBlackRankFile() : bd.getWhiteRankFile();
		
		long empty = bd.getEmpty();
		long target = BitBoard.positionToMask_Lut[attackedSq];
		long attackMask = 0L;
		
		if ((directDiagonalAttacksOnPosition_Lut[attackedSq] & diagonalAttackersMask) != 0) {
			if ((directAttacksOnPositionUpRight_Lut[attackedSq] & diagonalAttackersMask) != 0) {
				attackMask = BitBoard.downLeftAttacks(diagonalAttackersMask, empty);
				if ((attackMask & target) != 0) return true;
			}
			if ((directAttacksOnPositionUpLeft_Lut[attackedSq] & diagonalAttackersMask) != 0) {
				attackMask = BitBoard.downRightAttacks(diagonalAttackersMask, empty);
				if ((attackMask & target) != 0) return true;
			}
			if ((directAttacksOnPositionDownLeft_Lut[attackedSq] & diagonalAttackersMask) != 0) {
				attackMask = BitBoard.upRightAttacks(diagonalAttackersMask, empty);
				if ((attackMask & target) != 0) return true;
			}
			if ((directAttacksOnPositionDownRight_Lut[attackedSq] & diagonalAttackersMask) != 0) {
				attackMask = BitBoard.upLeftAttacks(diagonalAttackersMask, empty);
				if ((attackMask & target) != 0) return true;
			}
		}
		if ((directRankFileAttacksOnPosition_Lut[attackedSq] & rankFileAttackersMask) != 0) {
			if ((directAttacksOnPositionUp_Lut[attackedSq] & rankFileAttackersMask) != 0) {
				attackMask = BitBoard.downAttacks(rankFileAttackersMask, empty);
				if ((attackMask & target) != 0) return true;
			}
			if ((directAttacksOnPositionLeft_Lut[attackedSq] & rankFileAttackersMask) != 0) {
				attackMask = BitBoard.rightAttacks(rankFileAttackersMask, empty);
				if ((attackMask & target) != 0) return true;
			}
			if ((directAttacksOnPositionDown_Lut[attackedSq] & rankFileAttackersMask) != 0) {
				attackMask = BitBoard.upAttacks(rankFileAttackersMask, empty);
				if ((attackMask & target) != 0) return true;
			}
			if ((directAttacksOnPositionRight_Lut[attackedSq] & rankFileAttackersMask) != 0) {
				attackMask = BitBoard.leftAttacks(rankFileAttackersMask, empty);
				if ((attackMask & target) != 0) return true;
			}
		}
		
		return attacked;
	}
	
	/* 1-dimensional array:
	 * 1st index is a position integer, this is the origin square
	 * indexes a bit mask of the squares that the origin square can attack by a Black Pawn capture */
	static final long[] BlackPawnAttacksFromPosition_Lut = new long[128];
	static {
		for (int square : Position.values) {
			BlackPawnAttacksFromPosition_Lut[square] = createBlackPawnMovesFromSq(square);
		}
	}
	static long createBlackPawnMovesFromSq(int atPos) {
		long mask = 0;
		if (Position.getRank(atPos) != 7) {
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
	
	/* 1-dimensional array:
	 * 1st index is a position integer, this is the origin square
	 * indexes a bit mask of the squares that the origin square can attack by a White Pawn capture */
	static final long[] WhitePawnAttacksFromPosition_Lut = new long[128];
	static {
		for (int square : Position.values) {
			WhitePawnAttacksFromPosition_Lut[square] = createWhitePawnMovesFromSq(square);
		}
	}
	static long createWhitePawnMovesFromSq(int atPos) {
		long mask = 0;
		if (Position.getRank(atPos) != 0) {
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
}
