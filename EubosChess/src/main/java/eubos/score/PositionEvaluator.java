package eubos.score;

import java.util.Arrays;
import java.util.IntSummaryStatistics;

import eubos.board.BitBoard;
import eubos.board.Board;
import eubos.board.Piece;
import eubos.board.SquareAttackEvaluator;
import eubos.main.EubosEngineMain;
import eubos.position.IPositionAccessors;

public class PositionEvaluator implements IEvaluate {

	IPositionAccessors pm;
	
	public static final boolean ENABLE_KING_SAFETY_EVALUATION = true;
	public static final boolean ENABLE_TWEAKED_KING_FLIGHT_SQUARES = false;
	public static final boolean ENABLE_KING_TROPISM = true;
	
	public static final boolean ENABLE_DYNAMIC_POSITIONAL_EVALUATION = true;

	private static final int BISHOP_PAIR_BOOST = 25;
	
	boolean onMoveIsWhite;
	int midgameScore = 0;
	int endgameScore = 0;
	public boolean isDraw;
	public boolean passedPawnPresent;
	public short score;
	public Board bd;
	PawnEvaluator pawn_eval;
	
	private class LazyEvalStatistics {
		
		int MAX_DELTA = Piece.MATERIAL_VALUE_QUEEN - lazy_eval_threshold_cp; 
		long lazySavedCountAlpha;
		long lazySavedCountBeta;
		long nodeCount;
		int lazyThreshFailedCount[];
		int maxFailure;
		int maxFailureCount;
		String max_fen;
		int biggestError;
		int maxThreshUsed;
		
		public LazyEvalStatistics() {
			lazyThreshFailedCount = new int [MAX_DELTA];
			biggestError = 0;
		}
		
		public void report() {
			// We want to know the bin that corresponds to the max error, this is the average threshold exceeded
			int max_threshold = 0;
			int max_count = 0;
			boolean overflowed = false;
			if (maxFailureCount == 0) {
				for (int i=lazyThreshFailedCount.length-1; i >= 0; i--) {
					// We also want to know the last non-zero array element
					if (lazyThreshFailedCount[i] != 0) {
						max_threshold = i;
						max_count = lazyThreshFailedCount[i];
						break;
					}
				}
			} else {
				max_threshold = maxFailure;
				max_count = maxFailureCount;
				overflowed = true;
			}
			
			IntSummaryStatistics stats = Arrays.stream(lazyThreshFailedCount).summaryStatistics();
			EubosEngineMain.logger.info(String.format(
					"LazyStats A=%d B=%d maxLazyThresh=%d nodes=%d failSum=%d overflowed=%s maxOverLazy=%d maxOverLazyN=%d maxFen=%s",
					lazySavedCountAlpha, lazySavedCountBeta, maxThreshUsed, nodeCount, stats.getSum(), overflowed, max_threshold, max_count, max_fen));
		}
	}
	
	private void updateLazyStatistics(int crude, int lazyThresh) {
		int full = internalFullEval();
		int delta = full-crude;
		// We don't care if the score is better, only if it is worse
		if (delta < 0) {
			delta = Math.abs(delta);
			if (Math.abs(delta) > lazyThresh) {
				delta -= lazyThresh;
				//assert delta < 1500 : String.format("LazyFail delta=%d stack=%s", delta, pm.unwindMoveStack());
				if (delta < lazyStat.MAX_DELTA) {
					lazyStat.lazyThreshFailedCount[delta]++; /// can be double incremented on aspiration window failure?
					if (delta > lazyStat.biggestError) {
						lazyStat.max_fen = pm.getFen();
						lazyStat.biggestError = delta;
						lazyStat.maxThreshUsed = lazyThresh;
					}
				} else {
					lazyStat.maxFailureCount++;
					lazyStat.maxFailure = Math.max(delta, lazyStat.maxFailure);
					lazyStat.max_fen = pm.getFen();
				}
			}
		}
	}
	
	public void reportLazyStatistics() {
		if (TUNE_LAZY_EVAL) {
			lazyStat.report();
		}
	}
	
	LazyEvalStatistics lazyStat = null;
	
	/* The threshold for lazy evaluation was tuned by empirical evidence collected from
	running with the logging in TUNE_LAZY_EVAL for Eubos2.14 and post processing the logs.
	It will need to be re-tuned if the evaluation function is altered significantly. */
	public static int lazy_eval_threshold_cp = 275;
	private static final boolean TUNE_LAZY_EVAL = false;
	
	public void reportPawnStatistics() {
		pawn_eval.reportPawnHashStatistics();
	}
	
	public boolean goForMate;
	public boolean goForMate() {
		return goForMate;
	}
	
	public PositionEvaluator(IPositionAccessors pm, PawnEvalHashTable pawnHash) {	
		this.pm = pm;
		bd = pm.getTheBoard();
		pawn_eval = new PawnEvaluator(pm, pawnHash);
		// If either side can't win (e.g. bare King) then do a mate search.
		goForMate = ((Long.bitCount(bd.getBlackPieces()) == 1) || 
				     (Long.bitCount(bd.getWhitePieces()) == 1));
		initialise();
		if (TUNE_LAZY_EVAL) {
			lazyStat = new LazyEvalStatistics();
		}
	}
	
	private void initialise() {
		onMoveIsWhite = pm.onMoveIsWhite();
		isDraw = pm.isThreefoldRepetitionPossible();
		if (!isDraw) {
			isDraw = bd.isInsufficientMaterial();
		}
		if (EubosEngineMain.ENABLE_COUNTED_PASSED_PAWN_MASKS) {
			if (!isDraw) {
				passedPawnPresent = bd.isPassedPawnPresent();
			}
		} else {
			passedPawnPresent = false;
		}
		score = 0;
		midgameScore = 0;
		endgameScore = 0;
	}
	
	private short taperEvaluation(int midgameScore, int endgameScore) {
		int phase = bd.me.getPhase();
		return (short)(((midgameScore * (4096 - phase)) + (endgameScore * phase)) / 4096);
	}
	
	public boolean isKingExposed() {
		int kingBitOffset = bd.pieceLists.getKingPos(onMoveIsWhite);
		// Only meant to cater for quite extreme situations
		long kingZone = SquareAttackEvaluator.KingZone_Lut[onMoveIsWhite ? 0 : 1][kingBitOffset];
		kingZone =  onMoveIsWhite ? kingZone >>> 8 : kingZone << 8;
		long defenders = onMoveIsWhite ? bd.getWhitePieces() : bd.getBlackPieces();
		int defenderCount = Long.bitCount(kingZone&defenders);

		int attackQueenOffset = bd.getQueenPosition(!onMoveIsWhite);
		if (attackQueenOffset != BitBoard.INVALID) {
			int attackingQueenDistance = BitBoard.ManhattanDistance[attackQueenOffset][kingBitOffset];
			return (defenderCount < 3 || attackingQueenDistance < 3);
		} else {
			return defenderCount < 3;
		}
	}
	
	public int lazyEvaluation(int alpha, int beta) {
		initialise();
		if (EubosEngineMain.ENABLE_LAZY_EVALUATION && bd.me.phase != 4096) {
			// Phase 1 - crude evaluation
			int crudeEval = internalCrudeEval();
			int lazyThresh = lazy_eval_threshold_cp;
			long pp = bd.getPassedPawns();
			if (pp != 0L) {
				// increase threshold as a function of the passed pawn imbalance
				int numWhitePassers = Long.bitCount(pp&bd.getWhitePieces());
				int numBlackPassers = Long.bitCount(pp&bd.getBlackPieces());
				int ppDelta = Math.abs(numBlackPassers-numWhitePassers);
				lazyThresh += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[ppDelta] + (ppDelta * 250 * bd.me.getPhase()) / 4096;
			}
			if (!bd.me.isEndgame() && isKingExposed()) {
				lazyThresh += 300;
			}
			if (TUNE_LAZY_EVAL) {
				lazyStat.nodeCount++;
			}
			if (crudeEval-lazyThresh >= beta) {
				// There is no move to put in the killer table when we stand Pat
				// According to lazy eval, we probably can't reach beta
				if (TUNE_LAZY_EVAL) {
					lazyStat.lazySavedCountBeta++;
					updateLazyStatistics(crudeEval, lazyThresh);
				}
				return beta;
			}
		}
		// Phase 2 full evaluation
		return internalFullEval();
	}
	
	public int getCrudeEvaluation() {
		isDraw = pm.isThreefoldRepetitionPossible();
		if (!isDraw) {
			isDraw = bd.isInsufficientMaterial();
		}
		onMoveIsWhite = pm.onMoveIsWhite();
		score = 0;
		midgameScore = 0;
		endgameScore = 0;

		return internalCrudeEval();
	}
	
	private int internalCrudeEval() {
		// Initialised in lazyEvaluation function
		if (!isDraw) {
			bd.me.dynamicPosition = 0;
			score += evaluateBishopPair();
			midgameScore = score + (onMoveIsWhite ? bd.me.getMiddleGameDelta() + bd.me.getPosition() : -(bd.me.getMiddleGameDelta() + bd.me.getPosition()));
			endgameScore = score + (onMoveIsWhite ? bd.me.getEndGameDelta() + bd.me.getEndgamePosition() : -(bd.me.getEndGameDelta() + bd.me.getEndgamePosition()));
			score = taperEvaluation(midgameScore, endgameScore);
		}
		return score;
	}
	
	public final int[] GO_FOR_MATE_KING_PROXIMITY_LUT = {0, 0, 40, 30, 20, 10, 0, -10, -20};
	private int internalFullEval() {
		// Initialised in lazyEvaluation function
		score = 0;
		midgameScore = 0;
		endgameScore = 0;
		if (!isDraw) {
			// Score factors common to each phase, material, pawn structure and piece mobility
			bd.me.dynamicPosition = 0;
			
			// Only generate full attack mask if passed pawn present and past opening stage
			long [][][] attacks;
			if (passedPawnPresent) {
				attacks = bd.mae.calculateCountedAttacksAndMobility(bd.me);
			} else {
				attacks = bd.mae.calculateBasicAttacksAndMobility(bd.me);
			}
			
			score += evaluateBishopPair();
			score += pawn_eval.evaluatePawnStructure(attacks);
			
			// Add phase specific static mobility (PSTs)
			midgameScore = score + (onMoveIsWhite ? bd.me.getMiddleGameDelta() + bd.me.getPosition() : -(bd.me.getMiddleGameDelta() + bd.me.getPosition()));
			endgameScore = score + (onMoveIsWhite ? bd.me.getEndGameDelta() + bd.me.getEndgamePosition() : -(bd.me.getEndGameDelta() + bd.me.getEndgamePosition()));
			// Add King Safety in middle game
			if (ENABLE_KING_SAFETY_EVALUATION && !bd.me.isEndgame()) {
				midgameScore += evaluateKingSafety(attacks);
			}
			if (!goForMate) {
				score = taperEvaluation(midgameScore, endgameScore);
			} else {
				score += onMoveIsWhite ? bd.me.getMiddleGameDelta() : -bd.me.getMiddleGameDelta();
				int ownKingPos = bd.getKingPosition(onMoveIsWhite);
				int enemyKingPos = bd.getKingPosition(!onMoveIsWhite);
				if (ownKingPos != BitBoard.INVALID && enemyKingPos != BitBoard.INVALID) {
					int distance = BitBoard.ManhattanDistance[enemyKingPos][ownKingPos];
					score += GO_FOR_MATE_KING_PROXIMITY_LUT[distance];
				}
			}
		}
		return score;
	}
	
	public int getFullEvaluation() {
		initialise();
		return internalFullEval();
	}
	
	int evaluateKingSafety(long[][][] attacks) {
		int kingSafetyScore = 0;
		kingSafetyScore = evaluateKingSafety(attacks, onMoveIsWhite);
		kingSafetyScore -= evaluateKingSafety(attacks, !onMoveIsWhite);
		return kingSafetyScore;
	}
	
	int evaluateBishopPair() {
		int score = 0;
		int onMoveBishopCount = onMoveIsWhite ? bd.me.numberOfPieces[Piece.WHITE_BISHOP] : bd.me.numberOfPieces[Piece.BLACK_BISHOP];
		if (onMoveBishopCount >= 2) {
			score += BISHOP_PAIR_BOOST;
		}
		int opponentBishopCount = onMoveIsWhite ? bd.me.numberOfPieces[Piece.BLACK_BISHOP] : bd.me.numberOfPieces[Piece.WHITE_BISHOP];
		if (opponentBishopCount >= 2) {
			score -= BISHOP_PAIR_BOOST;
		}
		return score;
	}
	
	// King Tropism by distance, in centipawns.
	public final int[] KT_QUEEN_DIST_LUT = {0, -100, -50, -12, -7, -5, 0, 0, 0};
	public final int[] KT_KNIGHT_DIST_LUT = {0, -25, -50, -25, -12, 0, 0, 0, 0};
	
	// Make function of game phase?
	public final int[] PAWN_SHELTER_LUT = {-100, -50, -15, 2, 4, 4, 0, 0, 0};
	public final int[] PAWN_STORM_LUT = {0, -12, -30, -75, -150, -250, 0, 0, 0};
	
	public int evaluateKingSafety(long[][][] attacks, boolean isWhite) {
		int evaluation = 0;

		// King
		long kingMask = isWhite ? bd.getWhiteKing() : bd.getBlackKing();
		int kingBitOffset = bd.getKingPosition(isWhite);
		long blockers = isWhite ? bd.getWhitePawns() : bd.getBlackPawns();
		
		// Attackers
		long attackingQueensMask = isWhite ? bd.getBlackQueens() : bd.getWhiteQueens();
		long attackingRooksMask = isWhite ? bd.getBlackRooks() : bd.getWhiteRooks();
		long attackingBishopsMask = isWhite ? bd.getBlackBishops() : bd.getWhiteBishops();

		// create masks of attackers
		long pertinentBishopMask = attackingBishopsMask;//& ((isKingOnDarkSq) ? DARK_SQUARES_MASK : LIGHT_SQUARES_MASK);
		long diagonalAttackersMask = attackingQueensMask | pertinentBishopMask;
		long rankFileAttackersMask = attackingQueensMask | attackingRooksMask;
		
		// First score according to King exposure on open diagonals
		int numPotentialAttackers = Long.bitCount(diagonalAttackersMask);
		long mobility_mask = 0x0;
		if (numPotentialAttackers > 0) {
			long defendingBishopsMask = isWhite ? bd.getWhiteBishops() : bd.getBlackBishops();
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
		
		// Then score according to King exposure on open rank/files
		numPotentialAttackers = Long.bitCount(rankFileAttackersMask);
		if (numPotentialAttackers > 0) {
			mobility_mask = 0x0;
			long defendingRooksMask = isWhite ? bd.getWhiteRooks() : bd.getBlackRooks();
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
		
		// Then, do king tropism for proximity
		if (ENABLE_KING_TROPISM) {
			int kt_score = 0;
			long scratchBitBoard = isWhite ? bd.getBlackKnights() : bd.getWhiteKnights();
			while (scratchBitBoard != 0x0L) {
				int bit_offset = BitBoard.convertToBitOffset(scratchBitBoard);
				int distance = BitBoard.ManhattanDistance[bit_offset][kingBitOffset];
				kt_score += KT_KNIGHT_DIST_LUT[distance];
				scratchBitBoard ^= (1L << bit_offset);
			}
			scratchBitBoard = isWhite ? bd.getBlackQueens() : bd.getWhiteQueens();
			while (scratchBitBoard != 0x0L) {
				int bit_offset = BitBoard.convertToBitOffset(scratchBitBoard);
				int distance = BitBoard.ManhattanDistance[bit_offset][kingBitOffset];
				kt_score += KT_QUEEN_DIST_LUT[distance];
				scratchBitBoard ^= (1L << bit_offset);
			}
			evaluation += kt_score;
		}
		
		// Hit with a penalty if few defending pawns in the king zone and/or pawn storm
		long surroundingSquares = SquareAttackEvaluator.KingZone_Lut[isWhite ? 0 : 1][kingBitOffset];		
		long pawnShieldMask =  isWhite ? surroundingSquares >>> 8 : surroundingSquares << 8;
		evaluation += PAWN_SHELTER_LUT[Long.bitCount(pawnShieldMask & blockers)];
		
		long attacking_pawns = isWhite ? bd.getBlackPawns() : bd.getWhitePawns();
		evaluation += PAWN_STORM_LUT[Long.bitCount(surroundingSquares & attacking_pawns)];
		
		// Then account for attacks on the squares around the king
		surroundingSquares = SquareAttackEvaluator.KingMove_Lut[kingBitOffset];
		int attackedCount = Long.bitCount(surroundingSquares & attacks[isWhite ? 1 : 0][3][0]);
		int flightCount = Long.bitCount(surroundingSquares);
		int fraction_attacked_q8 = (attackedCount * 256) / flightCount;
		evaluation += ((-150 * fraction_attacked_q8) / 256);
		if (attackedCount == flightCount) {
			// there are no flight squares, high risk of mate
			evaluation += -100;
		}
		
		return evaluation;
	}
}
