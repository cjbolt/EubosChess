package eubos.score;

import eubos.board.BitBoard;
import eubos.board.Board;
import eubos.board.Piece;
import eubos.board.SquareAttackEvaluator;
import eubos.position.IPositionAccessors;

public class KingSafetyEvaluator {
	
	public static final boolean ENABLE_KING_SAFETY_EVALUATION = true;
	public static final boolean ENABLE_TWEAKED_KING_FLIGHT_SQUARES = false;
	public static final boolean ENABLE_KING_TROPISM = true;
	public static final boolean ENABLE_KNIGHT_CHECK_THREAT = true;
	
	IPositionAccessors pm;
	public Board bd;
	
	// King Tropism by distance, in centipawns.
	public final int[] KT_QUEEN_DIST_LUT = {0, -100, -50, -12, -7, -5, 0, 0, 0};
	public final int[] KT_KNIGHT_DIST_LUT = {0, -25, -50, -25, -12, 0, 0, 0, 0};
	
	// Make function of game phase?
	public final int[] PAWN_SHELTER_LUT = {-100, -50, -15, 2, 4, 4, 0, 0, 0};
	public final int[] PAWN_STORM_LUT = {0, -12, -30, -75, -150, -250, 0, 0, 0};
	
	public final int[] EXPOSURE_NUM_ATTACKERS_MODIFIER_LUT = {0, 2, 2, 3, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
	
	public final int EXPOSURE_MAX_PENALTY = -300;
	public final int SQUARES_CONTROL_ROUND_KING_PENALTY = -200;
	public final int NO_FLIGHT_SQUARES_PENALTY = -200;
	public final int KNIGHT_CHECK_THREAT_PENALTY = -25; 
	
	long own, enemy;
	long kingMask, blockers;
	int kingBitOffset;	
	long[][][] attacks;
	
	long black;
	long white;
	
	int attackingQueenCount;
	int attackingBishopCount;
	int attackingRookCount;
	int attackingKnightCount;
	
	public KingSafetyEvaluator(IPositionAccessors pm) {
		this.pm = pm;
		bd = pm.getTheBoard();
	}
	
	public int evaluateKingSafety(long[][][] attacks, boolean onMoveIsWhite) {
		int kingSafetyScore = 0;
		this.attacks = attacks;
		white = bd.getWhitePieces();
		black = bd.getBlackPieces();
		if (ENABLE_KING_SAFETY_EVALUATION && !bd.me.isEndgame()) {
			kingSafetyScore = evaluateKingSafetyForSide(attacks, onMoveIsWhite);
			kingSafetyScore -= evaluateKingSafetyForSide(attacks, !onMoveIsWhite);
		}
		return kingSafetyScore;
	}
	
	void testInitForSide(boolean isWhite) {
		white = bd.getWhitePieces();
		black = bd.getBlackPieces();
		initialiseForSide(isWhite);
	}
	
	void initialiseForSide(boolean isWhite) {
		if (isWhite) {
			own = white;
			enemy = black;
			attackingQueenCount = bd.me.numberOfPieces[Piece.BLACK_QUEEN];
			attackingBishopCount = bd.me.numberOfPieces[Piece.BLACK_BISHOP];
			attackingRookCount = bd.me.numberOfPieces[Piece.BLACK_ROOK];
			attackingKnightCount = bd.me.numberOfPieces[Piece.BLACK_KNIGHT];
		} else {
			enemy = white;
			own = black;
			attackingQueenCount = bd.me.numberOfPieces[Piece.WHITE_QUEEN];
			attackingBishopCount = bd.me.numberOfPieces[Piece.WHITE_BISHOP];
			attackingRookCount = bd.me.numberOfPieces[Piece.WHITE_ROOK];
			attackingKnightCount = bd.me.numberOfPieces[Piece.WHITE_KNIGHT];
		}
		
		// King
		kingMask = bd.pieces[Piece.KING] & own;
		kingBitOffset = BitBoard.convertToBitOffset(kingMask);
		blockers = bd.pieces[Piece.PAWN] & own;
	}
	
	int evaluateKingSafetyForSide(long[][][] attacks, boolean isWhite) {
		/* Score is for the King being attacked, so isWhite means black is attacking. We 
		 * therefore generate a negative value in this function, representing a penalty to
		 *  the side's score. */
		int evaluation = 0;
		initialiseForSide(isWhite);

		evaluation += EvaluateExposureOnOpenLines();
		evaluation += EvaluateKingTropism();
		evaluation += EvaluatePawnShelterAndStorm(isWhite);
		evaluation += EvaluateSquareControlRoundKing(isWhite);
		if (ENABLE_KNIGHT_CHECK_THREAT) {
			evaluation += EvaluateKnightCheckThreat(isWhite);
		}
		
		return evaluation;
	}
	
	int EvaluateExposureOnOpenLines() {
		int evaluation = 0;
		
		// First score according to King exposure on open diagonals
		int numPotentialAttackers = attackingQueenCount + attackingBishopCount;
		long mobility_mask = 0x0;
		if (numPotentialAttackers > 0) {
			long defendingBishopsMask = bd.pieces[Piece.BISHOP] & own;
			// only own side pawns should block an attack ray, not any piece, so don't use empty mask as propagator
			long inDirection = BitBoard.downLeftOccludedEmpty(kingMask, ~blockers);
			mobility_mask |= ((inDirection & defendingBishopsMask) == 0) ? inDirection : 0;
			inDirection = BitBoard.upLeftOccludedEmpty(kingMask, ~blockers);
			mobility_mask |= ((inDirection & defendingBishopsMask) == 0) ? inDirection : 0;
			inDirection = BitBoard.upRightOccludedEmpty(kingMask, ~blockers);
			mobility_mask |= ((inDirection & defendingBishopsMask) == 0) ? inDirection : 0;
			inDirection = BitBoard.downRightOccludedEmpty(kingMask, ~blockers);
			mobility_mask |= ((inDirection & defendingBishopsMask) == 0) ? inDirection : 0;
			evaluation = Long.bitCount(mobility_mask ^ kingMask) * -numPotentialAttackers;
		}
		int totalAttackers = numPotentialAttackers;
		
		// Then score according to King exposure on open rank/files
		numPotentialAttackers = attackingQueenCount + attackingRookCount;
		if (numPotentialAttackers > 0) {
			mobility_mask = 0x0;
			long defendingRooksMask = bd.pieces[Piece.ROOK] & own;
			long inDirection = BitBoard.downOccludedEmpty(kingMask, ~blockers);
			mobility_mask |= ((inDirection & defendingRooksMask) == 0) ? inDirection : 0;
			inDirection = BitBoard.upOccludedEmpty(kingMask, ~blockers);
			mobility_mask |= ((inDirection & defendingRooksMask) == 0) ? inDirection : 0;
			inDirection = BitBoard.rightOccludedEmpty(kingMask, ~blockers);
			mobility_mask |= ((inDirection & defendingRooksMask) == 0) ? inDirection : 0;
			inDirection = BitBoard.leftOccludedEmpty(kingMask, ~blockers);
			mobility_mask |= ((inDirection & defendingRooksMask) == 0) ? inDirection : 0;
			evaluation += Long.bitCount(mobility_mask ^ kingMask) * -numPotentialAttackers;
		}
		totalAttackers += numPotentialAttackers;
		totalAttackers += attackingKnightCount;
		totalAttackers -= attackingQueenCount; // Don't double count queens
		
		int forceOfAttackCoeff = evaluation * EXPOSURE_NUM_ATTACKERS_MODIFIER_LUT[totalAttackers];
		return Math.max(EXPOSURE_MAX_PENALTY, forceOfAttackCoeff / 2);
	}
	
	int EvaluateKingTropism() {
		int evaluation = 0;
		
		// Then, do king tropism for proximity
		if (ENABLE_KING_TROPISM) {
			int kt_score = 0;
			long scratchBitBoard = bd.pieces[Piece.KNIGHT] & enemy;
			while (scratchBitBoard != 0x0L) {
				int bit_offset = BitBoard.convertToBitOffset(scratchBitBoard);
				int distance = BitBoard.ManhattanDistance[bit_offset][kingBitOffset];
				kt_score += KT_KNIGHT_DIST_LUT[distance];
				scratchBitBoard ^= (1L << bit_offset);
			}
			scratchBitBoard = bd.pieces[Piece.QUEEN] & enemy;;
			while (scratchBitBoard != 0x0L) {
				int bit_offset = BitBoard.convertToBitOffset(scratchBitBoard);
				int distance = BitBoard.ManhattanDistance[bit_offset][kingBitOffset];
				kt_score += KT_QUEEN_DIST_LUT[distance];
				scratchBitBoard ^= (1L << bit_offset);
			}
			evaluation += kt_score;
		}
		
		return evaluation;
	}
	
	int EvaluatePawnShelterAndStorm(boolean isWhite) {
		int evaluation = 0;
		// Hit with a penalty if few defending pawns in the king zone and/or pawn storm
		long surroundingSquares = SquareAttackEvaluator.KingZone_Lut[isWhite ? 0 : 1][kingBitOffset];		
		long pawnShieldMask =  isWhite ? surroundingSquares >>> 8 : surroundingSquares << 8;
		evaluation += PAWN_SHELTER_LUT[Long.bitCount(pawnShieldMask & blockers)];
		
		long attacking_pawns = bd.pieces[Piece.PAWN] & enemy;
		evaluation += PAWN_STORM_LUT[Long.bitCount(surroundingSquares & attacking_pawns)];
		
		return evaluation;
	}
	
	int EvaluateSquareControlRoundKing(boolean isWhite) {
		int evaluation = 0;
		// Then account for attacks on the squares around the king
		long surroundingSquares = SquareAttackEvaluator.KingMove_Lut[kingBitOffset];
		int attackedCount = Long.bitCount(surroundingSquares & attacks[isWhite ? 1 : 0][3][0]);
		int flightCount = Long.bitCount(surroundingSquares);
		int fraction_attacked_q8 = (attackedCount * 256) / flightCount;
		evaluation += ((SQUARES_CONTROL_ROUND_KING_PENALTY * fraction_attacked_q8) / 256);
		if (attackedCount == flightCount) {
			// there are no flight squares, high risk of mate
			evaluation += NO_FLIGHT_SQUARES_PENALTY;
		}
		return evaluation;
	}
	
	int EvaluateKnightCheckThreat(boolean isWhite) {
		return bd.potentialKnightCheck(isWhite) ? KNIGHT_CHECK_THREAT_PENALTY : 0;
	}
}
