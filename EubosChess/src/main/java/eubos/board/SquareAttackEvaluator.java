 package eubos.board;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.position.Position;

public class SquareAttackEvaluator {
	
	static final Direction [] rankFile = { Direction.down, Direction.up, Direction.left, Direction.right };
	
	static final Map<Direction, Integer> rankFileDirectionIndex_Lut = new EnumMap<Direction, Integer>(Direction.class);
	static {
		// Indexes as specified by the order of the array SquareAttackEvaluator.rankFile
		rankFileDirectionIndex_Lut.put(Direction.down, 0);
		rankFileDirectionIndex_Lut.put(Direction.up, 1);
		rankFileDirectionIndex_Lut.put(Direction.left, 2);
		rankFileDirectionIndex_Lut.put(Direction.right, 3);
	}
	
	static final Direction [] diagonals = { Direction.downLeft, Direction.upLeft, Direction.downRight, Direction.upRight };
	
	static final Map<Direction, Integer> diagonalsDirectionIndex_Lut = new EnumMap<Direction, Integer>(Direction.class);
	static {
		// Indexes as specified by the order of the array SquareAttackEvaluator.diagonals
		diagonalsDirectionIndex_Lut.put(Direction.downLeft, 0);
		diagonalsDirectionIndex_Lut.put(Direction.upLeft, 1);
		diagonalsDirectionIndex_Lut.put(Direction.downRight, 2);
		diagonalsDirectionIndex_Lut.put(Direction.upRight, 3);
	}
	
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
			attacked = checkForDirectPieceAttacker(bd, attackedSq, isBlackAttacking);
			if (attacked) break;
		} while (false);
		return attacked;	
	}

	private static boolean checkForDirectPieceAttacker(Board bd, int attackedSq, boolean isBlackAttacking) {
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
				int currPiece = theBoard.getPieceAtSquareOptimise(attackerSq, attackerIsBlack);
				switch(currPiece) {
				case Piece.NONE:
					continue;
				case Piece.BLACK_BISHOP:
				case Piece.BLACK_QUEEN:
					attacked = attackerIsBlack;
					break;
				case Piece.WHITE_BISHOP:
				case Piece.WHITE_QUEEN:
					attacked = !attackerIsBlack;
					break;
				default:
					break;
				}
				break; 
			}
			break;
		case left:
		case up:
		case right:
		case down:
			for (int attackerSq: array[directionIndex_Lut.get(dir)]) {
				int currPiece = theBoard.getPieceAtSquareOptimise(attackerSq, attackerIsBlack);
				switch(currPiece) {
				case Piece.NONE:
					continue;
				case Piece.BLACK_ROOK:
				case Piece.BLACK_QUEEN:
					attacked = attackerIsBlack;
					break;
				case Piece.WHITE_ROOK:
				case Piece.WHITE_QUEEN:
					attacked = !attackerIsBlack;
					break;
				default:
					break;
				}
				break; 
			}
			break;
		default:
			if (EubosEngineMain.ASSERTS_ENABLED)
				assert false; // should not receive indirect moves here!
			break;
		}
		return attacked;
	}
	
	public static boolean moveCouldLeadToDiscoveredCheck(Integer move, int kingPosition) {
		int atSquare = Move.getOriginPosition(move);
		// Establish if the initial square is on a multiple square slider mask from the king position
		long square = BitBoard.positionToMask_Lut[atSquare];
		long attackingSquares = directAttacksOnPosition_Lut[kingPosition];
		return ((square & attackingSquares) != 0);
	}
	
	public static Direction findDirectionToTarget(int atSquare, int targetSq, Direction[] directionsToConsider) {
		long targetMask = BitBoard.positionToMask_Lut[targetSq];
		Direction attackDir = null;
		for (Direction direction : directionsToConsider) {
			long directionMask = directAttacksOnPositionAll_Lut[directionIndex_Lut.get(direction)][atSquare];
			if ((targetMask & directionMask) != 0) {
				attackDir = direction;
				break;
			}
		}
		return attackDir;
	}
}
