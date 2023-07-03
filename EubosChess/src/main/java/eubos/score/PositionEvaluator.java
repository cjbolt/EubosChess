package eubos.score;

import java.util.Arrays;
import java.util.IntSummaryStatistics;

import eubos.board.BitBoard;
import eubos.board.Board;
import eubos.board.Piece;
import eubos.board.SquareAttackEvaluator;
import eubos.main.EubosEngineMain;
import eubos.position.IPositionAccessors;
import eubos.position.Move;

public class PositionEvaluator implements IEvaluate {

	IPositionAccessors pm;
	
	public static final boolean ENABLE_DYNAMIC_POSITIONAL_EVALUATION = true;
	public static final boolean ENABLE_THREAT_EVALUATION = true;

	private static final int BISHOP_PAIR_BOOST = 25;
	
	boolean onMoveIsWhite;
	int midgameScore = 0;
	int endgameScore = 0;
	public boolean isDraw;
	public boolean passedPawnPresent;
	public short score;
	public Board bd;
	PawnEvaluator pawn_eval;
	KingSafetyEvaluator ks_eval;
	
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
		ks_eval = new KingSafetyEvaluator(pm);
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
			isDraw = pm.isInsufficientMaterial();
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
			isDraw = pm.isInsufficientMaterial();
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
			midgameScore += ks_eval.evaluateKingSafety(attacks, onMoveIsWhite);
			midgameScore += evaluateThreats(attacks, onMoveIsWhite);
			
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
	
	public int getFullEvalNotCheckingForDraws() {
		onMoveIsWhite = pm.onMoveIsWhite();
		isDraw = false;
		passedPawnPresent = bd.isPassedPawnPresent();
		score = 0;
		midgameScore = 0;
		endgameScore = 0;
		return internalFullEval();
	}
	
	public int evaluateThreatsForSide(long[][][] attacks, boolean onMoveIsWhite) {
		int threatScore = 0;
		long own = onMoveIsWhite ? bd.getWhitePieces() : bd.getBlackPieces();
		own &= ~(onMoveIsWhite ? bd.getWhiteKing() : bd.getBlackKing()); // Don't include King in this evaluation
		// if a piece is attacked and not defended, add a penalty
		long enemyAttacks = attacks[onMoveIsWhite ? 1 : 0][3][0];
		long ownAttacks = attacks[onMoveIsWhite ? 0 : 1][3][0];
		long attackedOnlyByEnemy = enemyAttacks & ~ownAttacks;
		long scratchBitBoard = own & attackedOnlyByEnemy;
		int bit_offset;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {		
			// Add a penalty based on the value of the piece not defended, 10% of piece value
			threatScore -= Math.abs((Piece.PIECE_TO_MATERIAL_LUT[0][bd.getPieceAtSquare(scratchBitBoard)] / 10));
			scratchBitBoard ^= (1L << bit_offset);
		}
		return threatScore;
	}
	
	public int evaluateThreats(long[][][] attacks, boolean onMoveIsWhite) {
		int threatScore = 0;
		if (ENABLE_THREAT_EVALUATION) {
			threatScore = evaluateThreatsForSide(attacks, onMoveIsWhite);
			threatScore -= evaluateThreatsForSide(attacks, !onMoveIsWhite);
		}
		return threatScore;
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
	
	public static final int FUTILITY_MARGIN = 50;
	
	int futilityC(int move) {
		int futility = FUTILITY_MARGIN;
		int originPiece = Move.getOriginPiece(move);
		int originNoColour = originPiece & Piece.PIECE_NO_COLOUR_MASK;
		
		if (originNoColour == Piece.PAWN) {
			int pawnIsAt = Move.getOriginPosition(move);
			long pawnMask = 1L << pawnIsAt;
			long pp = pm.getTheBoard().getPassedPawns();
			if ((pp & pawnMask) != 0L) {
				/* If the moving pawn is already passed, inflate futility. */
				futility += 50;
			} else {
			int pawnWillBeAt = Move.getOriginPosition(move);
				if (bd.isPassedPawn(pawnWillBeAt, pawnMask)) {
					/* If the moving pawn is becoming passed, inflate futility. */
					futility += 75;
				}
			}
			
		} else if (!bd.me.isEndgame() && (originNoColour == Piece.KNIGHT || originNoColour == Piece.QUEEN)) {
			/* Theory; move could likely effect King tropism score. */
			boolean ownSideIsWhite = Piece.isWhite(originPiece);
			int enemyKingBit = pm.getTheBoard().getKingPosition(!ownSideIsWhite);
			int distBefore = BitBoard.ManhattanDistance[Move.getOriginPosition(move)][enemyKingBit];
			int distAfter = BitBoard.ManhattanDistance[Move.getTargetPosition(move)][enemyKingBit];
			if (distBefore > 3 && distAfter < 3) {
				futility += 50;
			}
		}
		return futility;
	}
	
	public int estimateMovePositionalContribution(int move) {
		return 0;
	}
	
	public int getStaticEvaluation() {
		int evaluation = 0;
		if (pm.getTheBoard().getPassedPawns() != 0L || isKingExposed()) {
			evaluation = getFullEvalNotCheckingForDraws(); 
		} else {
			evaluation = getCrudeEvaluation();
		}
		return evaluation;
	}
}
