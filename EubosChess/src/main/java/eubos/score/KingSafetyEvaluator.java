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
	public final int SQUARES_CONTROL_ROUND_KING_PENALTY = -150;
	public final int NO_FLIGHT_SQUARES_PENALTY = -50;
	
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
	int totalAttackingPieces;
	
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
	
	void testInitForSide(long[][][] attacks, boolean isWhite) {
		this.attacks = attacks;
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
		int evaluation = 0;
		initialiseForSide(isWhite);
		evaluation += EvaluateExposureOnOpenLines();
		if ( totalAttackingPieces != 0) {
			evaluation += EvaluateKingTropism();
			evaluation += EvaluatePawnShelterAndStorm(isWhite);
		}
		evaluation += EvaluateSquareControlRoundKing(isWhite, evaluation);
		
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
		totalAttackingPieces = numPotentialAttackers;
		
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
		totalAttackingPieces += numPotentialAttackers;
		totalAttackingPieces += attackingKnightCount;
		totalAttackingPieces -= attackingQueenCount; // Don't double count queens
		
		int forceOfAttackCoeff = evaluation * EXPOSURE_NUM_ATTACKERS_MODIFIER_LUT[totalAttackingPieces];
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
		long pawnShieldMask = isWhite ? surroundingSquares >>> 8 : surroundingSquares << 8;
		evaluation += PAWN_SHELTER_LUT[Long.bitCount(pawnShieldMask & blockers)];
		
		long attacking_pawns = bd.pieces[Piece.PAWN] & enemy;
		evaluation += PAWN_STORM_LUT[Long.bitCount(surroundingSquares & attacking_pawns)];
		
		return evaluation;
	}
	
	int flightCount(boolean isWhite) {
		long surroundingSquares = SquareAttackEvaluator.KingMove_Lut[kingBitOffset];
		// not attacked
		long flightMask = surroundingSquares & ~attacks[isWhite ? 1 : 0][3][0];
		// not blocked by own pieces
		flightMask &= ~own;
		int flightCount = Long.bitCount(flightMask);
		return flightCount;
	}
	
    static final int[] FLIGHT_COUNT_LUT;
    static {
        FLIGHT_COUNT_LUT = new int[64];
        for (int i=0; i < 64; i++) {
        	FLIGHT_COUNT_LUT[i] = Long.bitCount(SquareAttackEvaluator.KingMove_Lut[i]);
        }
    }
	
	int EvaluateSquareControlRoundKing(boolean isWhite, int evalSoFar) {
		int evaluation = 0;
		long kingZoneMask = SquareAttackEvaluator.KingMove_Lut[kingBitOffset];
		long aggregatedEnemyAttacksMask = attacks[isWhite ? 1 : 0][3][0];
		
		int attackedCount = Long.bitCount(kingZoneMask & aggregatedEnemyAttacksMask);
		if (attackedCount != 0) {
			// Flight squares are not attacked by enemy and not blocked by own pieces
			long flightMask = kingZoneMask & ~aggregatedEnemyAttacksMask;
			flightMask &= ~own;
			int flightCount = Long.bitCount(flightMask);
			
			int kingZoneCount = FLIGHT_COUNT_LUT[kingBitOffset];
			int fraction_attacked_q8 = (attackedCount * 256) / kingZoneCount;
			evaluation += ((SQUARES_CONTROL_ROUND_KING_PENALTY * fraction_attacked_q8) / 256);
			
			if (flightCount == 0) {
				// Deemed a high risk of mate, assuming is developed and greater than one attack nearby
				if (pm.getMoveNumber() > 10 && evalSoFar < -250) { 
					evaluation += NO_FLIGHT_SQUARES_PENALTY;
				}
			} else {
				int ratio_attacked_cf_flight_q8 = (attackedCount * 256) / flightCount;
				evaluation += ((NO_FLIGHT_SQUARES_PENALTY * ratio_attacked_cf_flight_q8) / 256);
			}
		}
		return evaluation;
	}
}
