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
				     (Long.bitCount(bd.getWhitePieces()) == 1)) ||
				Math.abs(bd.me.getMiddleGameDelta()) > 4000;
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
	
	boolean isKingExposed() {
		int kingBitOffset = bd.getKingPosition(onMoveIsWhite);
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
		if (EubosEngineMain.ENABLE_LAZY_EVALUATION) {
			if (bd.me.phase != 4096) {
				// Phase 1 - crude evaluation
				int crudeEval = internalCrudeEval();
				int lazyThresh = lazy_eval_threshold_cp;
				long pp = bd.getPassedPawns();
				if (pp != 0L) {
					// increase threshold as a function of the passed pawn imbalance
					int numWhitePassers = Long.bitCount(pp&bd.getWhitePieces());
					int numBlackPassers = Long.bitCount(pp&bd.getBlackPieces());
					int ppDelta = Math.abs(numBlackPassers-numWhitePassers);
					lazyThresh += ppDelta * 250 * bd.me.getPhase() / 4096;
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
	
	private void doMaterialAndPst() {
		if (onMoveIsWhite) {
			midgameScore = bd.me.getMiddleGameDelta() + bd.me.getPosition();
			endgameScore = bd.me.getEndGameDelta() + bd.me.getEndgamePosition();
		} else {
			midgameScore = -(bd.me.getMiddleGameDelta() + bd.me.getPosition());
			endgameScore = -(bd.me.getEndGameDelta() + bd.me.getEndgamePosition());
		}
	}
	
	private void doMateKingProximity() {
		int ownKingPos = bd.getKingPosition(onMoveIsWhite);
		int enemyKingPos = bd.getKingPosition(!onMoveIsWhite);
		if (ownKingPos != BitBoard.INVALID && enemyKingPos != BitBoard.INVALID) {
			int distance = BitBoard.ManhattanDistance[enemyKingPos][ownKingPos];
			score += GO_FOR_MATE_KING_PROXIMITY_LUT[distance];
		}
	}
	
	private int internalCrudeEval() {
		// Initialised in lazyEvaluation function
		if (!isDraw) {
			bd.me.dynamicPosition = 0;
			// Add phase specific static mobility (PSTs)
			doMaterialAndPst();
			score = taperEvaluation(midgameScore, endgameScore);
			score += evaluateBishopPair();
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
			if (goForMate) {
				score += onMoveIsWhite ? bd.me.getMiddleGameDelta() : -bd.me.getMiddleGameDelta();
				doMateKingProximity();
			} else {
				// Only generate full attack mask if passed pawn present
				bd.me.dynamicPosition = 0;
				long [][][] attacks;
				if (passedPawnPresent) {
					attacks = bd.mae.calculateCountedAttacksAndMobility(bd.me);
				} else {
					attacks = bd.mae.calculateBasicAttacksAndMobility(bd.me);
				}
				
				if (bd.isLikelyDrawnEndgame(onMoveIsWhite, attacks)) {
					return score;
				}
				
				// Add phase specific static mobility (PSTs)
				doMaterialAndPst();

				// Evaluate king safety, en prise threats in middle game
				midgameScore += ks_eval.evaluateKingSafety(attacks, onMoveIsWhite);
				midgameScore += evaluateThreats(attacks, onMoveIsWhite);
				
				score = taperEvaluation(midgameScore, endgameScore);
				score += evaluateBishopPair();
				score += pawn_eval.evaluatePawnStructure(attacks);
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
	
	public int getCrudeEvalNotCheckingForDraws() {
		onMoveIsWhite = pm.onMoveIsWhite();
		isDraw = false;
		passedPawnPresent = bd.isPassedPawnPresent();
		score = 0;
		midgameScore = 0;
		endgameScore = 0;
		return internalCrudeEval();
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
			// Add a penalty based on the value of the piece not defended, 50% of piece value
			threatScore -= Math.abs((Piece.PIECE_TO_MATERIAL_LUT[0][bd.getPieceAtSquare(scratchBitBoard)] / 2));
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
	
	public static final int FUTILITY_MARGIN_BY_PIECE[] = new int[8];
    static {
    	FUTILITY_MARGIN_BY_PIECE[Piece.QUEEN] = 175;
    	FUTILITY_MARGIN_BY_PIECE[Piece.ROOK] = 150;
    	FUTILITY_MARGIN_BY_PIECE[Piece.BISHOP] = 130;
    	FUTILITY_MARGIN_BY_PIECE[Piece.KNIGHT] = 175;
    	FUTILITY_MARGIN_BY_PIECE[Piece.KING] = 150;
    	FUTILITY_MARGIN_BY_PIECE[Piece.PAWN] = 125;
    }
	
	public int estimateMovePositionalContribution(int move) {
		int originPiece = Move.getOriginPiece(move);
		int originNoColour = originPiece & Piece.PIECE_NO_COLOUR_MASK;
		int futility = FUTILITY_MARGIN_BY_PIECE[originNoColour];
		
		if (originNoColour == Piece.PAWN) {
			int pawnIsAt = Move.getOriginPosition(move);
			long pawnMask = 1L << pawnIsAt;
			long pp = pm.getTheBoard().getPassedPawns();
			if ((pp & pawnMask) != 0L) {
				/* If the moving pawn is already passed, inflate futility. */
				futility += 100;
			} else {
				int pawnWillBeAt = Move.getTargetPosition(move);
				if (bd.isPassedPawn(pawnWillBeAt, pawnMask)) {
					/* If the moving pawn is becoming passed, inflate futility. */
					futility += 125;
				}
			}
			
		} 
		return futility;
	}
	
	public int getStaticEvaluation() {
		int evaluation = 0;
		if (pm.getTheBoard().getPassedPawns() != 0L || isKingExposed()) {
			// No point checking for draws, because we terminate search as soon as a likely draw is detected
			// and return draw score, so we can't get here if the position is a likely draw, the check would
			// be redundant
			evaluation = getFullEvalNotCheckingForDraws(); 
		} else {
			evaluation = getCrudeEvalNotCheckingForDraws();
		}
		return evaluation;
	}
}
