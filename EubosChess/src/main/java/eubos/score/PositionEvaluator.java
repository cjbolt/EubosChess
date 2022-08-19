package eubos.score;

import java.util.Arrays;
import java.util.IntSummaryStatistics;

import com.fluxchess.jcpi.models.IntFile;

import eubos.board.BitBoard;
import eubos.board.Board;
import eubos.board.CountedBitBoard;
import eubos.board.IForEachPieceCallback;
import eubos.board.Piece;
import eubos.board.SquareAttackEvaluator;
import eubos.main.EubosEngineMain;
import eubos.position.IPositionAccessors;
import eubos.position.Position;

public class PositionEvaluator implements IEvaluate {

	IPositionAccessors pm;
	
	public static final int DOUBLED_PAWN_HANDICAP = 12;
	public static final int ISOLATED_PAWN_HANDICAP = 33;
	public static final int BACKWARD_PAWN_HANDICAP = 12;
	public static final int NO_PAWNS_HANDICAP = 50;
	
	public static final int PASSED_PAWN_BOOST = 15;
	public static final int ROOK_FILE_PASSED_PAWN_BOOST = 10;
	public static final int CANDIDATE_PAWN = 8;
	public static final int ROOK_FILE_CANDIDATE_PAWN = 5;
	public static final int SAFE_MOBILE_PASSED_PAWN = 10;
	public static final int MOBILE_PASSED_PAWN = 5;
	public static final int CONNECTED_PASSED_PAWN_BOOST = 75;
	public static final int HEAVY_PIECE_BEHIND_PASSED_PAWN = 20;
	
	public static final boolean ENABLE_PAWN_EVALUATION = true;
	public static final boolean ENABLE_KPK_EVALUATION = true;
	public static final boolean ENABLE_CANDIDATE_PP_EVALUATION = true;
	public static final boolean ENABLE_PP_IMBALANCE_EVALUATION = false;
	
	public static final boolean ENABLE_KING_SAFETY_EVALUATION = true;
	public static final boolean ENABLE_TWEAKED_KING_FLIGHT_SQUARES = false;
	
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
	
	/* The threshold for lazy evaluation was tuned by empirical evidence collected from
	running with the logging in TUNE_LAZY_EVAL for Eubos2.13 and post processing the logs.
	It will need to be re-tuned if the evaluation function is altered significantly. */
	public static int lazy_eval_threshold_cp = 450;
	private static final boolean TUNE_LAZY_EVAL = false;
	
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
		
		public LazyEvalStatistics() {
			lazyThreshFailedCount = new int [MAX_DELTA];
			biggestError = 0;
		}
		
		public void report() {
			// We want to know the bin that corresponds to the max error, this is the average threshold exceeded
			// We also want to know the last non-zero array element
			int max_threshold = 0;
			int max_count = 0;
			if (maxFailureCount == 0) {
				for (int i=lazyThreshFailedCount.length-1; i >= 0; i--) {
					if (lazyThreshFailedCount[i] != 0) {
						max_threshold = i;
						max_count = lazyThreshFailedCount[i];
						break;
					}
				}
			} else {
				max_threshold = maxFailure;
				max_count = maxFailureCount;
			}
			
			IntSummaryStatistics stats = Arrays.stream(lazyThreshFailedCount).summaryStatistics();
			EubosEngineMain.logger.info(String.format(
					"LazyStats A=%d B=%d nodes=%d failSum=%d exceededCount=%d maxExceeded=%d maxFen=%s",
					lazySavedCountAlpha, lazySavedCountBeta, nodeCount, stats.getSum(), max_count, max_threshold, max_fen));
		}
	}
	
	private void updateLazyStatistics(int plyScore) {
		int delta = Math.abs(plyScore-internalFullEval());
		if (delta > lazy_eval_threshold_cp) {
			delta -= lazy_eval_threshold_cp;
			assert delta < 1500 : String.format("LazyFail delta=%d stack=%s", delta, pm.unwindMoveStack());
			if (delta < lazyStat.MAX_DELTA) {
				lazyStat.lazyThreshFailedCount[delta]++;
				if (delta > lazyStat.biggestError) {
					lazyStat.max_fen = pm.getFen();
					lazyStat.biggestError = delta;
				}
			} else {
				lazyStat.maxFailureCount++;
				lazyStat.maxFailure = Math.max(delta, lazyStat.maxFailure);
			}
		}
	}
	
	public void reportLazyStatistics() {
		if (TUNE_LAZY_EVAL) {
			lazyStat.report();
		}
	}
	
	LazyEvalStatistics lazyStat = null;
	
	public boolean goForMate;
	public boolean goForMate() {
		return goForMate;
	}
	
	public PositionEvaluator(IPositionAccessors pm) {	
		this.pm = pm;
		bd = pm.getTheBoard();
		ktc = new KingTropismChecker();
		pawn_eval = new PawnEvaluator();
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
				// check for king danger or passed pawn
//				int kingPos = bd.pieceLists.getKingPos(onMoveIsWhite);
//				long enemy_pieces = onMoveIsWhite ? bd.getBlackPieces(): bd.getWhitePieces();
//				long surroundingSquares = SquareAttackEvaluator.KingZone_Lut[onMoveIsWhite ? 0 : 1][kingPos];
//				boolean enemyNearKing = ((enemy_pieces & surroundingSquares) != 0L) && !bd.me.isEndgame();
				passedPawnPresent = /*enemyNearKing ||*/ bd.isPassedPawnPresent(pawn_eval);
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
	
	public int lazyEvaluation(int alpha, int beta) {
		initialise();
		if (EubosEngineMain.ENABLE_LAZY_EVALUATION) {
			// Phase 1 - crude evaluation
			int crudeEval = internalCrudeEval();
			int lazyThresh = bd.me.isEndgame() ? 750 : passedPawnPresent ? 450 : 275;
			if (TUNE_LAZY_EVAL) {
				lazyStat.nodeCount++;
			}
			if (crudeEval-lazyThresh >= beta) {
				// There is no move to put in the killer table when we stand Pat
				// According to lazy eval, we probably can't reach beta
				if (TUNE_LAZY_EVAL) {
					lazyStat.lazySavedCountBeta++;
					updateLazyStatistics(crudeEval);
				}
				return beta;
			}
//			/* Note call to quiescence check is last as it could be very computationally heavy! */
//			if (crudeEval+lazyThresh <= alpha && pm.isQuiescent()) {
//				// According to lazy eval, we probably can't increase alpha
//				if (TUNE_LAZY_EVAL) {
//					lazyStat.lazySavedCountAlpha++;
//					updateLazyStatistics(crudeEval);
//				}
//				return Short.MIN_VALUE;
//			}
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
		
		if (!isDraw) {
			bd.me.dynamicPosition = 0;
			score += evaluateBishopPair();
			midgameScore = score + (onMoveIsWhite ? bd.me.getMiddleGameDelta() + bd.me.getPosition() : -(bd.me.getMiddleGameDelta() + bd.me.getPosition()));
			endgameScore = score + (onMoveIsWhite ? bd.me.getEndGameDelta() + bd.me.getEndgamePosition() : -(bd.me.getEndGameDelta() + bd.me.getEndgamePosition()));
			score = taperEvaluation(midgameScore, endgameScore);
		}
		return score;
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
			
			if (ENABLE_PAWN_EVALUATION) {
				score += pawn_eval.evaluatePawnStructure(attacks);
			}
			// Add phase specific static mobility (PSTs)
			midgameScore = score + (onMoveIsWhite ? bd.me.getMiddleGameDelta() + bd.me.getPosition() : -(bd.me.getMiddleGameDelta() + bd.me.getPosition()));
			endgameScore = score + (onMoveIsWhite ? bd.me.getEndGameDelta() + bd.me.getEndgamePosition() : -(bd.me.getEndGameDelta() + bd.me.getEndgamePosition()));
			// Add King Safety in middle game
			if (ENABLE_KING_SAFETY_EVALUATION && !goForMate) {
				midgameScore += evaluateKingSafety(attacks);
			}
			if (!goForMate) {
				score = taperEvaluation(midgameScore, endgameScore);
			} else {
				score += onMoveIsWhite ? bd.me.getMiddleGameDelta() : -bd.me.getMiddleGameDelta();
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
	
	public class PawnEvaluator implements IForEachPieceCallback{
		
		public int piecewisePawnScoreAccumulator = 0;
		public long[][][] attacks;
		protected int queeningDistance;
		protected int weighting;
		protected boolean pawnIsBlack;
		protected int[] ppCount = {0,0};
		
		public final int[] ppImbalanceTable = {0, 15, 200, 400, 700, 900, 900, 900, 900};
		
		protected int getScaleFactorForGamePhase() {
			return 1 + ((bd.me.phase+640) / 4096) + ((bd.me.phase+320) / 4096);
		}
		
		protected void setQueeningDistance(int atPos, boolean pawnIsWhite) {
			pawnIsBlack = !pawnIsWhite;
			int rank = Position.getRank(atPos);
			if (pawnIsBlack) {
				queeningDistance = rank;
				weighting = 7-queeningDistance;
			} else {
				queeningDistance = 7-rank;
				weighting = rank;
			}
		}
		
		protected void evaluateKpkEndgame(int atPos, boolean isOwnPawn, long[][] ownAttacks) {
			// Special case, it is a KPK endgame
			int file = Position.getFile(atPos);
			int queeningSquare = pawnIsBlack ? Position.valueOf(file, 0) : Position.valueOf(file, 7);
			int oppoKingPos = bd.getKingPosition(pawnIsBlack);
			int oppoDistance = Position.distance(queeningSquare, oppoKingPos);
			if (!isOwnPawn) {
				// if king is on move, assume it can get towards the square of the pawn
				oppoDistance -= 1;
			}
			if (oppoDistance > queeningDistance) {
				// can't be caught by opposite king, as outside square of pawn
				piecewisePawnScoreAccumulator += 700;
			} else {
				if (bd.isFrontspanControlledInKpk(atPos, !pawnIsBlack, ownAttacks[3])) {
					// Rationale is whole frontspan can be blocked off from opposite King by own King
					piecewisePawnScoreAccumulator += 700;
				} else {
					// increase score also if we think the pawn can be defended by own king
					int ownKingPos = bd.getKingPosition(!pawnIsBlack);
					int ownDistance = Position.distance(queeningSquare, ownKingPos);
					if (ownDistance-1 <= oppoDistance) {
						piecewisePawnScoreAccumulator += 300;
					}
				}
			}
		}
		
		protected void evaluatePassedPawn(int atPos, boolean pawnIsWhite, long[][] own_attacks, long [][] enemy_attacks) {
			weighting *= getScaleFactorForGamePhase();
			int value = (Position.getFile(atPos) == IntFile.Fa || Position.getFile(atPos) == IntFile.Fh) ?
					ROOK_FILE_PASSED_PAWN_BOOST : PASSED_PAWN_BOOST;
			
			int score = 0;
			if (!bd.isPawnBlockaded(atPos, pawnIsWhite)) {
				int heavySupportIndication = bd.checkForHeavyPieceBehindPassedPawn(atPos, pawnIsWhite);
				if (heavySupportIndication > 0) {
					score += HEAVY_PIECE_BEHIND_PASSED_PAWN;
				} else if (heavySupportIndication < 0) {
					score -= HEAVY_PIECE_BEHIND_PASSED_PAWN;
				} else {
					// neither attacked or defended along the rear span
				}
				if (bd.isPawnFrontspanSafe(atPos, pawnIsWhite, own_attacks[3], enemy_attacks[3], heavySupportIndication > 0)) {
					value += SAFE_MOBILE_PASSED_PAWN;
				} else if (bd.canPawnAdvance(atPos, pawnIsWhite, own_attacks[3], enemy_attacks[3])) {
					value += MOBILE_PASSED_PAWN;
				}
			}
			score += weighting*value;
			piecewisePawnScoreAccumulator += score;
		}
		
		@SuppressWarnings("unused")
		@Override
		public void callback(int piece, int atPos) {
			boolean pawnIsWhite = Piece.isWhite(piece);
			long[][] enemy_attacks = attacks[pawnIsWhite ? 1:0];
			long[][] own_attacks = attacks[pawnIsWhite ? 0:1];
			
			if (bd.isPassedPawn(atPos, pawnIsWhite)) {
				boolean isOwnPawn = (onMoveIsWhite && pawnIsWhite) || (!onMoveIsWhite && !pawnIsWhite);
				ppCount[isOwnPawn ? 0:1] += 1;
				setQueeningDistance(atPos, pawnIsWhite);
				if (ENABLE_KPK_EVALUATION && bd.me.phase == 4096) {
					evaluateKpkEndgame(atPos, isOwnPawn, own_attacks);
				} else {
					evaluatePassedPawn(atPos, pawnIsWhite, own_attacks, enemy_attacks);
				}
			} else if (ENABLE_CANDIDATE_PP_EVALUATION) {
				// TODO make it resolve the number of attacks...
				if (bd.isCandidatePassedPawn(atPos, pawnIsWhite, own_attacks[0], enemy_attacks[0])) {
					setQueeningDistance(atPos, pawnIsWhite);
					weighting *= getScaleFactorForGamePhase();
					if (Position.getFile(atPos) == IntFile.Fa || Position.getFile(atPos) == IntFile.Fh) {
						piecewisePawnScoreAccumulator += weighting*ROOK_FILE_CANDIDATE_PAWN;
					} else {
						piecewisePawnScoreAccumulator += weighting*CANDIDATE_PAWN;
					}
				}
			}
			if (bd.isIsolatedPawn(atPos, pawnIsWhite)) {
				piecewisePawnScoreAccumulator -= ISOLATED_PAWN_HANDICAP;
			} else if (bd.isBackwardsPawn(atPos, pawnIsWhite)) {
				piecewisePawnScoreAccumulator -= BACKWARD_PAWN_HANDICAP;
			}
		}
		
		@Override
		public boolean condition_callback(int piece, int atPos) {
			return bd.isPassedPawn(atPos, Piece.isWhite(piece));
		}
		
		public int getDoubledPawnsHandicap(long pawns) {
			return -bd.countDoubledPawns(pawns)*DOUBLED_PAWN_HANDICAP;
		}
		
		void initialise(long[][][] attacks) {
			ppCount[0] = ppCount[1] = 0;
			pawn_eval.attacks = attacks;
		}
		
		@SuppressWarnings("unused")
		int evaluatePawnStructure(long[][][] attacks) {
			initialise(attacks);
			int pawnEvaluationScore = 0;
			long ownPawns = onMoveIsWhite ? bd.getWhitePawns() : bd.getBlackPawns();
			long enemyPawns = onMoveIsWhite ? bd.getBlackPawns() : bd.getWhitePawns();
			if (ownPawns != 0x0) {
				piecewisePawnScoreAccumulator = 0;
				int pawnHandicap = getDoubledPawnsHandicap(ownPawns);
				bd.forEachPawnOfSide(this, !onMoveIsWhite);
				pawnEvaluationScore = pawnHandicap + piecewisePawnScoreAccumulator;
			} else {
				pawnEvaluationScore -= NO_PAWNS_HANDICAP;
			}
			if (enemyPawns != 0x0) {
				piecewisePawnScoreAccumulator = 0;
				int pawnHandicap = getDoubledPawnsHandicap(enemyPawns);
				bd.forEachPawnOfSide(this, onMoveIsWhite);
				pawnEvaluationScore -= (pawnHandicap + piecewisePawnScoreAccumulator);
			} else {
				pawnEvaluationScore += NO_PAWNS_HANDICAP;
			}
			// Add a modification according to the imbalance of passed pawns in the position
			if (ENABLE_PP_IMBALANCE_EVALUATION && bd.me.phase > 2048) {
				int lookupIndex = ppCount[0] - ppCount[1];
				int ppImbalanceFactor = ppImbalanceTable[Math.abs(lookupIndex)];
				if (lookupIndex < 0) {
					// If negative, on move has fewer passed pawns, so subtract from score
					ppImbalanceFactor = -ppImbalanceFactor;
				}
				pawnEvaluationScore += ppImbalanceFactor;
			}
			return pawnEvaluationScore;
		}
	}
	
	public class KingTropismChecker implements IForEachPieceCallback {
		
		// by distance, in centipawns.
		public final int[] QUEEN_DIST_LUT = {0, -100, -100, -25, -12, -5, 0, 0, 0};
		public final int[] KNIGHT_DIST_LUT = {0, -25, -50, -25, -12, 0, 0, 0, 0};
		public final int[] BISHOP_DIST_LUT = {0, -20, -15, -12, -8, -4, -2, -1, 0};
		public final int[] ROOK_DIST_LUT = {0, -50, -30, -20, -10, -8, -4, -2, 0};
		public final int[] PAWN_DIST_LUT = {0, -20, -10, -3, 0, 0, 0, 0, 0};
		
		int score = 0;
		int kingSquare = Position.NOPOSITION;
		
		public void callback(int piece, int position) {
			int distance = Position.distance(position, kingSquare);
			piece &= ~Piece.BLACK;
			switch(piece) {
			case Piece.QUEEN:
				score += QUEEN_DIST_LUT[distance];
				break;
			case Piece.KNIGHT:
				score += KNIGHT_DIST_LUT[distance];
				break;
			case Piece.BISHOP:
				score += BISHOP_DIST_LUT[distance];
				break;
			case Piece.ROOK:
				score += ROOK_DIST_LUT[distance];
				break;
			default:
				break;
			}
		}
		
		@Override
		public boolean condition_callback(int piece, int atPos) {
			return false;
		}
		
		public int getScore(int kingPos, int [] attackers) {
			score = 0;
			kingSquare = kingPos;
			bd.pieceLists.forEachPieceOfTypeDoCallback(this, attackers);
			return score;
		}
	}
	
	KingTropismChecker ktc;
	
	public int evaluateKingSafety(long[][][] attacks, boolean isWhite) {
		int evaluation = 0;

		// King
		long kingMask = isWhite ? bd.getWhiteKing() : bd.getBlackKing();
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
		int kingPos = bd.pieceLists.getKingPos(isWhite);
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
		
		// Then, do king tropism for proximity, including pawn storm
		final int[] BLACK_ATTACKERS = {Piece.BLACK_QUEEN, Piece.BLACK_KNIGHT, Piece.BLACK_PAWN};
		final int[] WHITE_ATTACKERS = {Piece.WHITE_QUEEN, Piece.WHITE_KNIGHT, Piece.WHITE_PAWN};
		evaluation += ktc.getScore(kingPos, isWhite ? BLACK_ATTACKERS : WHITE_ATTACKERS);
		
		// Hit with a penalty if few defending pawns in the king zone
		long surroundingSquares = SquareAttackEvaluator.KingZone_Lut[isWhite ? 0 : 1][kingPos];		
		long pawnShieldMask =  isWhite ? surroundingSquares >>> 8 : surroundingSquares << 8;
		evaluation += PAWN_SHELTER_LUT[Long.bitCount(pawnShieldMask & blockers)];
		
		// Then account for attacks on the squares around the king
		surroundingSquares = SquareAttackEvaluator.KingMove_Lut[kingPos];
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
	
	public int evaluateKingSafetyV2(long[][][] attacks, boolean isWhite) {
		int evaluation = 0;
		int kingPos = bd.pieceLists.getKingPos(isWhite);
		
		// Then account for attacks on the squares around the king
		long [] our_attacks = attacks[isWhite ? 0 : 1][3];
		long [] enemy_attacks = attacks[isWhite ? 1 : 0][3];
		long surroundingSquares = SquareAttackEvaluator.KingZone_Lut[isWhite ? 0 : 1][kingPos];
		 
		int num_squares_controlled_by_enemy = passedPawnPresent ? 
				CountedBitBoard.evaluate(our_attacks, enemy_attacks, surroundingSquares) :
				Long.bitCount((our_attacks[0]^enemy_attacks[0]) & surroundingSquares);
		evaluation -= ENEMY_SQUARE_CONTROL_LUT[num_squares_controlled_by_enemy];
		
		// Then evaluate the check mate threat
		if (num_squares_controlled_by_enemy >= 2) {
			surroundingSquares = SquareAttackEvaluator.KingMove_Lut[kingPos];
			int attackedCount = Long.bitCount(surroundingSquares & attacks[isWhite ? 1 : 0][3][0]);
			int flightCount = Long.bitCount(surroundingSquares & bd.getEmpty());
			if (flightCount-attackedCount <= 1) {
				// There are no flight squares, high risk of mate
				int fraction_squares_controlled_by_enemy_q8 = (256 * attackedCount) / Math.max(flightCount, 1);
				evaluation += ((-250 * fraction_squares_controlled_by_enemy_q8) / 256); 
			}
		}
		
		// Hit with a penalty if few defending pawns in the king zone
		long blockers = isWhite ? bd.getWhitePawns() : bd.getBlackPawns();
		long pawnShieldMask =  isWhite ? surroundingSquares >>> 8 : surroundingSquares << 8;
		evaluation += PAWN_SHELTER_LUT[Long.bitCount(pawnShieldMask & blockers)];
		
		return evaluation;
	}
	
	// Make function of game phase?
	public final int[] PAWN_SHELTER_LUT = {-100, -50, -15, 2, 4, 4, 0, 0, 0, 0};
	
	public final int[] ENEMY_SQUARE_CONTROL_LUT = {
			0, 5, 10, 30, 
			75, 150, 240, 350, 
			400, 450, 500, 550,
			600, 600, 600, 600};
	
	private int getQ8SquareControlRoundKing(long[] own_attacks, long[] enemy_attacks, long squares) {
		int enemy_control_count = 0;
		int total_squares = 0;
		while (squares != 0L) {
			// Get square to analyse
			long square = Long.lowestOneBit(squares);
			if (!CountedBitBoard.weControlContestedSquares(own_attacks, enemy_attacks, square)) {
				enemy_control_count++;
			}
			// unset LSB
			squares ^= square;
			total_squares++;
		}
		int fraction_squares_controlled_by_enemy_q8 = (256 * enemy_control_count) / total_squares;
		return fraction_squares_controlled_by_enemy_q8;
	}
	
	public int evaluateSquareControlRoundKing(long[] own_attacks, long[] enemy_attacks, long squares) {
		int fraction_squares_controlled_by_enemy_q8 = getQ8SquareControlRoundKing(own_attacks, enemy_attacks, squares);
		return ((-150 * fraction_squares_controlled_by_enemy_q8) / 256);
	}
}
