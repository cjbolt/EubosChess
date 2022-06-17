package eubos.search;

import java.util.IntSummaryStatistics;
import java.util.Arrays;

import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.MoveListIterator;
import eubos.score.IEvaluate;
import eubos.search.transposition.ITranspositionAccessor;
import eubos.search.transposition.Transposition;

public class PlySearcher {
	
	private static final int [] ASPIRATION_WINDOW_FALLBACK = 
		{ Piece.MATERIAL_VALUE_PAWN/4, 2*Piece.MATERIAL_VALUE_PAWN, Piece.MATERIAL_VALUE_ROOK };
	
	/* The threshold for lazy evaluation was tuned by empirical evidence collected from
	running with the logging in TUNE_LAZY_EVAL for Eubos2.8 and post processing the logs.
	It will need to be re-tuned if the evaluation function is altered significantly. */
	private static final int LAZY_EVAL_THRESHOLD_IN_CP = 250;
	private static final boolean TUNE_LAZY_EVAL = false;

	private static final boolean ENABLE_EXTRA_EXTENSIONS = false;
	
	private class LazyEvalStatistics {
		
		static final int MAX_DELTA = Piece.MATERIAL_VALUE_QUEEN -LAZY_EVAL_THRESHOLD_IN_CP; 
		long lazySavedCountAlpha;
		long lazySavedCountBeta;
		long nodeCount;
		int lazyThreshFailedCount[];
		int maxFailure;
		int maxFailureCount;
		
		public LazyEvalStatistics() {
			lazyThreshFailedCount = new int [MAX_DELTA];
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
					"LazyStats A=%d B=%d nodes=%d failSum=%d exceededCount=%d maxExceeded=%d",
					lazySavedCountAlpha, lazySavedCountBeta, nodeCount, stats.getSum(), max_count, max_threshold));
		}
	}
	
	LazyEvalStatistics lazyStat = null;
	
	int alpha[];
	int beta[];
	int alphaOriginal[];
	int prevBestMove[];
	boolean isCutOff[];
	int hashScore[];
	
	private IChangePosition pm;
	private IPositionAccessors pos;
	private IEvaluate pe;
	
	private PrincipalContinuation pc;
	private SearchMetrics sm;
	private SearchDebugAgent sda;
	
	private volatile boolean terminate = false;
	
	private ITranspositionAccessor tt;
	private SearchMetricsReporter sr;
	private KillerList killers;
	
	private byte currPly = 0;
	private byte originalSearchDepthRequiredInPly = 0;
	private byte extendedSearchDeepestPly = 0;
	private short refScore;
	
	private MoveList ml;
	
	private boolean hasSearchedPv = false;

	private boolean lastAspirationFailed = false;
	
	public PlySearcher(
			ITranspositionAccessor hashMap,
			PrincipalContinuation pc,
			SearchMetrics sm,
			SearchMetricsReporter sr,
			byte searchDepthPly,
			IChangePosition pm,
			IPositionAccessors pos,
			IEvaluate pe,
			KillerList killers,
			SearchDebugAgent sda,
			MoveList ml,
			short refScore) {
		currPly = 0;
		
		this.alpha = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		this.beta = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		this.alphaOriginal = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		this.prevBestMove = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		this.isCutOff = new boolean[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		this.hashScore = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		
		this.pc = pc;
		this.sm = sm;
		this.pm = pm;
		this.pos = pos;
		this.pe = pe;
		this.sr = sr;
		this.sda = sda;
		this.refScore = refScore;
		originalSearchDepthRequiredInPly = searchDepthPly;
		
		tt = hashMap;
		this.killers = killers;
		this.ml = ml;
		if (TUNE_LAZY_EVAL) {
			lazyStat = new LazyEvalStatistics();
		}
	}

	public synchronized void terminateFindMove() { 
		EubosEngineMain.logger.info("Terminating PlySearcher");
		terminate = true;
	}
	private synchronized boolean isTerminated() { return terminate; }	
	
	public int searchPly(short lastScore)  {
		currPly = 0;
		extendedSearchDeepestPly = 0;
		short score = 0;
		
		if (EubosEngineMain.ENABLE_ASPIRATION_WINDOWS) {
			lastAspirationFailed = false;
			int fail_count = 0;
			
			// Adjust the aspiration window, according to the last score, if searching to sufficient depth
			int alpha = Score.PROVISIONAL_ALPHA;
			int beta = Score.PROVISIONAL_BETA;
			if (originalSearchDepthRequiredInPly >= 5) {
				int windowSize = Score.isMate(lastScore) ? 1 : ASPIRATION_WINDOW_FALLBACK[fail_count];
				alpha = lastScore - windowSize;
				beta = lastScore + windowSize;
			}
			
			while (!isTerminated()) {
				this.alpha[0] = alpha;
				this.beta[0] = beta;
				
				score = (short) searchRoot(originalSearchDepthRequiredInPly);
		
				if (Score.isProvisional(score)) {
					lastAspirationFailed = true;
					EubosEngineMain.logger.info("Aspiration Window failed - no score, illegal position");
		            break;
	        	} else if (isTerminated() && score ==0) {
	        		// Early termination, didn't back up a score at the last ply			
	        	} else if (score <= alpha) {
	        		// Failed low, adjust window
	        		lastAspirationFailed = true;
	        		fail_count++;
		        	if (!Score.isMate(lastScore) && fail_count < ASPIRATION_WINDOW_FALLBACK.length-1) {
		        		alpha = lastScore - ASPIRATION_WINDOW_FALLBACK[fail_count];
		        	} else {
		        		alpha = Score.PROVISIONAL_ALPHA;
		        	}
		        } else if (score >= beta) {
		        	// Failed high, adjust window
		        	lastAspirationFailed = true;
		        	fail_count++;
		        	if (!Score.isMate(lastScore) && fail_count < ASPIRATION_WINDOW_FALLBACK.length-1) {
		        		beta = lastScore + ASPIRATION_WINDOW_FALLBACK[fail_count];
		        	} else {
		        		beta = Score.PROVISIONAL_BETA;
		        	}
		        } else {
		        	// Exact score in window returned
		        	lastAspirationFailed = false;
		            break;
		        }
				if (lastAspirationFailed) {
					EubosEngineMain.logger.info(String.format("Aspiration Window failed count=%d score=%d alpha=%d beta=%d depth=%d",
	        				fail_count, score, alpha, beta, originalSearchDepthRequiredInPly));
					if (sr != null)
						sr.resetAfterWindowingFail();
				}
			}
		} else {
			// Not using aspiration windows
			this.alpha[0] = Score.PROVISIONAL_ALPHA;
			this.beta[0] = Score.PROVISIONAL_BETA;
			score = (short) searchRoot(originalSearchDepthRequiredInPly);
		}
		return score;
	}
		
	int searchRoot(int depth) {
		
		int plyScore = Score.PROVISIONAL_ALPHA;
		hasSearchedPv = false;
		isCutOff[0] = false;
		hashScore[0] = plyScore;
		alphaOriginal[0] = alpha[0];
		
		// This move is only valid for the principal continuation, for the rest of the search, it is invalid. It can also be misleading in iterative deepening?
		// It will deviate from the hash move when we start updating the hash during iterative deepening.
		prevBestMove[0] = Move.clearBest(pc.getBestMove((byte)0));
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(0);
		
		if (SearchDebugAgent.DEBUG_ENABLED) {
			sda.printStartPlyInfo(pos, originalSearchDepthRequiredInPly);
			sda.printNormalSearch(alpha[0], beta[0]);
		}
		
		// Extend search for in-check scenarios, treated outside of quiescence search
		boolean needToEscapeCheck = pos.isKingInCheck();
		if (needToEscapeCheck) {
			++depth;
		}
		if (ENABLE_EXTRA_EXTENSIONS) {
			if (currPly < originalSearchDepthRequiredInPly*2) {
				boolean kingThreatened = pos.getTheBoard().kingInDanger(Piece.Colour.isWhite(pos.getOnMove()));
				if (kingThreatened) {
					++depth;
				}
			}
			else if (currPly < originalSearchDepthRequiredInPly*2) {
				if (pos.promotablePawnPresent()) {
					++depth;
				}
			}
		}
		
		long trans = tt.getTransposition();
		if (trans != 0L) {
			evaluateTransposition(trans, depth);
			if (isCutOff[0]) {
				sm.setPrincipalVariationDataFromHash(0, (short)hashScore[0]);
				if (sr != null)
					sr.reportPrincipalVariation(sm);
				return hashScore[0];
			}
		}
			
		// Main search loop for root ply
		int bestMove = Move.NULL_MOVE;
		int currMove = Move.NULL_MOVE;
		int positionScore = plyScore;
		int moveNumber = 0;
		int quietOffset = 0;
		boolean refuted = false;
		ml.initialiseAtPly(prevBestMove[0], killers.getMoves(0), needToEscapeCheck, false, 0);
		do {
			MoveListIterator move_iter = ml.getNextMovesAtPly(0);
			if (!move_iter.hasNext()) {
				if (moveNumber == 0) {
					// No moves at this point means either a stalemate or checkmate has occurred
					return needToEscapeCheck ? Score.getMateScore(0) : 0;
				} else {
					// As soon as there are no more moves returned from staged move generation, break out, if we already searched a move
					break;
				}
			}
			do {
				currMove = move_iter.nextInt();
				if (EubosEngineMain.ENABLE_ASSERTS) {
					assert currMove != Move.NULL_MOVE: "Null move found in MoveList";
				}
				moveNumber += 1;
				if (moveNumber == 1) {
					pc.initialise(0, currMove);
					bestMove = currMove;
				}
				if (!Move.isRegular(currMove)) {
					quietOffset = moveNumber;
				}
				if (EubosEngineMain.ENABLE_UCI_MOVE_NUMBER) {
					sm.setCurrentMove(currMove, moveNumber);
					if (originalSearchDepthRequiredInPly > 8)
						sr.reportCurrentMove();
				}
				
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printNormalSearch(alpha[0], beta[0]);
				if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(0);
				
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printPerformMove(currMove);
				if (SearchDebugAgent.DEBUG_ENABLED) sda.nextPly();
				currPly++;
				pm.performMove(currMove);
				
				positionScore = doLateMoveReductionSubTreeSearch(depth, needToEscapeCheck, currMove, (moveNumber - quietOffset));
				
				pm.unperformMove();
				currPly--;
				if (SearchDebugAgent.DEBUG_ENABLED) sda.prevPly();
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printUndoMove(currMove, positionScore);
				
				if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
				
				if (isTerminated()) { return 0;	} // don't update PV if out of time for search, instead return last fully searched PV.
				
				// Handle score backed up to this node
				if (positionScore > alpha[0]) {
					alpha[0] = plyScore = positionScore;
					bestMove = currMove;
					pc.update(0, bestMove);
					if (alpha[0] >= beta[0]) {
						plyScore = beta[0]; // fail hard
						killers.addMove(0, bestMove);
						reportPv((short) beta[0]);
						if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(plyScore);
						refuted = true;
						break;
					}
					trans = updateTranspositionTable(trans, (byte) depth, bestMove, (short) alpha[0], Score.upperBound);
					reportPv((short) alpha[0]);
				} 
				else if (positionScore > plyScore) {
					bestMove = currMove;
					plyScore = positionScore;
				}
				
				hasSearchedPv = true;
				
			} while (move_iter.hasNext());
		} while (!isTerminated() && !refuted);
		
		if (!isTerminated()) {
			trans = updateTranspositionTable(trans, (byte) depth, bestMove, (short) plyScore);
		}
		
		// fail hard, so don't return plyScore
		return alpha[0];
	}
	
	int search(int depth)  {
		return search(depth, true);
	}
	
	int search(int depth, boolean nullCheckEnabled)  {
		
		int plyScore = Score.PROVISIONAL_ALPHA;
		alphaOriginal[currPly] = alpha[currPly];
		isCutOff[currPly] = false;
		hashScore[currPly] = plyScore;
						
		// This move is only valid for the principal continuation, for the rest of the search, it is invalid. It can also be misleading in iterative deepening?
		// It will deviate from the hash move when we start updating the hash during iterative deepening.
		prevBestMove[currPly] = Move.clearBest(pc.getBestMove(currPly));
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
		
		// Check for draws by three-fold repetition
		if (pos.isThreefoldRepetitionPossible()) return 0;
		
		// Mate distance pruning
		int mating_value = Score.PROVISIONAL_BETA - currPly;
		if (mating_value < beta[currPly]) {
			beta[currPly] = mating_value;
		    if (alpha[currPly] >= mating_value) return mating_value;
		}
		mating_value = Score.PROVISIONAL_ALPHA + currPly;
		if (mating_value > alpha[currPly]) {
		    alpha[currPly] = mating_value;
		    if (beta[currPly] <= mating_value) return mating_value;
		}

		// Absolute depth limit
		if (currPly >= EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
			return pe.getFullEvaluation();
		}
		
		if (SearchDebugAgent.DEBUG_ENABLED) {
			sda.printStartPlyInfo(pos, originalSearchDepthRequiredInPly);
			sda.printNormalSearch(alpha[currPly], beta[currPly]);
		}
		
		// Extend search for in-check scenarios, treated outside of quiescence search
		boolean needToEscapeCheck = pos.isKingInCheck();
		if (needToEscapeCheck) {
			++depth;
		}
		if (ENABLE_EXTRA_EXTENSIONS) {
			if (currPly < originalSearchDepthRequiredInPly*2) {
				boolean kingThreatened = pos.getTheBoard().kingInDanger(Piece.Colour.isWhite(pos.getOnMove()));
				if (kingThreatened) {
					++depth;
				}
			}
			else if (currPly < originalSearchDepthRequiredInPly*2) {
				if (pos.promotablePawnPresent()) {
					++depth;
				}
			}
		}
		
		if (depth <= 0) {
			return extendedSearch(alpha[currPly], beta[currPly], needToEscapeCheck);
		}
		
		long trans = tt.getTransposition();
		if (trans != 0L) {
			evaluateTransposition(trans, depth);
			if (isCutOff[currPly]) {
				return hashScore[currPly];
			}
		}
		
		// Null move pruning
		if (EubosEngineMain.ENABLE_NULL_MOVE_PRUNING &&
			!isTerminated() &&
			depth > 2 &&
			nullCheckEnabled &&
			hasSearchedPv && 
			!pos.getTheBoard().me.isEndgame() &&
			!needToEscapeCheck &&
			!(Score.isMate((short)beta[currPly]) || Score.isMate((short)alpha[currPly])) && 
			pe.getCrudeEvaluation()+LAZY_EVAL_THRESHOLD_IN_CP > beta[currPly]) {
			
			plyScore = doNullMoveSubTreeSearch(depth);
			if (isTerminated()) { return 0; }
			
			if (plyScore >= beta[currPly]) {
				return beta[currPly];
			} else {
				plyScore = Score.PROVISIONAL_ALPHA;
			}
		}
		
		// Main search loop for this ply
		int bestMove = Move.NULL_MOVE;
		int currMove = Move.NULL_MOVE;
		int positionScore = plyScore;
		int moveNumber = 0;
		int quietOffset = 0;
		boolean refuted = false;
		ml.initialiseAtPly(prevBestMove[currPly], killers.getMoves(currPly), needToEscapeCheck, false, currPly);
		do {
			MoveListIterator move_iter = ml.getNextMovesAtPly(currPly);
			if (!move_iter.hasNext()) {
				if (moveNumber == 0) {
					// No moves at this point means either a stalemate or checkmate has occurred
					return needToEscapeCheck ? Score.getMateScore(currPly) : 0;
				} else {
					// As soon as there are no more moves returned from staged move generation, break out, if we already searched a move
					break;
				}
			}
			do {
				currMove = move_iter.nextInt();
				moveNumber += 1;
				if (EubosEngineMain.ENABLE_ASSERTS) {
					assert currMove != Move.NULL_MOVE: "Null move found in MoveList";
				}
				if (moveNumber == 1) {
					pc.initialise(currPly, currMove);
					bestMove = currMove;
				}
				if (!Move.isRegular(currMove)) {
					quietOffset = moveNumber;
				}
				
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printNormalSearch(alpha[currPly], beta[currPly]);
				if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
				
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printPerformMove(currMove);
				if (SearchDebugAgent.DEBUG_ENABLED) sda.nextPly();
				currPly++;
				pm.performMove(currMove);
				
				positionScore = doLateMoveReductionSubTreeSearch(depth, needToEscapeCheck, currMove, (moveNumber - quietOffset));
				
				pm.unperformMove();
				currPly--;
				if (SearchDebugAgent.DEBUG_ENABLED) sda.prevPly();
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printUndoMove(currMove, positionScore);
				
				if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
				
				if (isTerminated()) { return 0;	} // don't update PV if out of time for search, instead return last fully searched PV.
				
				// Handle score backed up to this node
				if (positionScore > alpha[currPly]) {
					alpha[currPly] = plyScore = positionScore;
					bestMove = currMove;
					if (alpha[currPly] >= beta[currPly]) {
						plyScore = beta[currPly]; // fail hard
						killers.addMove(currPly, bestMove);
						if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(plyScore);
						refuted = true;
						break;
					}
					pc.update(currPly, bestMove);
				} 
				else if (positionScore > plyScore) {
					bestMove = currMove;
					plyScore = positionScore;
				}
			} while (move_iter.hasNext());
		} while (!isTerminated() && !refuted);
		
		if (!isTerminated()) {
			trans = updateTranspositionTable(trans, (byte) depth, bestMove, (short) plyScore);
		}
		
		// fail hard, so don't return plyScore
		return alpha[currPly];
	}
	
	@SuppressWarnings("unused")
	private int extendedSearch(int alpha, int beta, boolean needToEscapeCheck)  {
		if (SearchDebugAgent.DEBUG_ENABLED) sda.printExtSearch(alpha, beta);
		if (currPly > extendedSearchDeepestPly) {
			extendedSearchDeepestPly = currPly;
		}
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
		
		int prevBestMove = Move.NULL_MOVE;
		
		// Stand Pat in extended search
		short plyScore = (short) 0;
		if (EubosEngineMain.ENABLE_LAZY_EVALUATION && !pos.getTheBoard().me.isEndgame()) {
			// Phase 1 - crude evaluation
			plyScore = (short) pe.getCrudeEvaluation();
			if (TUNE_LAZY_EVAL) {
				lazyStat.nodeCount++;
			}
			if (plyScore-LAZY_EVAL_THRESHOLD_IN_CP >= beta) {
				// There is no move to put in the killer table when we stand Pat
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(plyScore);
				// According to lazy eval, we probably can't reach beta
				if (TUNE_LAZY_EVAL) {
					lazyStat.lazySavedCountBeta++;
					updateLazyStatistics(plyScore);
				}
				return beta;
			}
			/* Note call to quiescence check is last as it could be very computationally heavy! */
			if (plyScore+LAZY_EVAL_THRESHOLD_IN_CP <= alpha && pos.isQuiescent()) {
				// According to lazy eval, we probably can't increase alpha
				if (TUNE_LAZY_EVAL) {
					lazyStat.lazySavedCountAlpha++;
					updateLazyStatistics(plyScore);
				}
				return alpha;
			}
		}	
		// Phase 2 full evaluation
		plyScore = (short) pe.getFullEvaluation();
		if (plyScore >= beta) {
			// There is no move to put in the killer table when we stand Pat
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(plyScore);
			return beta;
		}
		if (currPly >= EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
			// Absolute depth limit, return full eval
			return plyScore;
		}
		
		if (plyScore > alpha) {
			// Null move hypothesis
			alpha = plyScore;
		}
		
		long trans = tt.getTransposition();
		if (trans != 0L) {
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsSeedMoveList(pos.getHash(), trans);
			prevBestMove = Transposition.getBestMove(trans);
		}
		
		int currMove = Move.NULL_MOVE;
		int positionScore = plyScore;
		int moveNumber = 0;
		ml.initialiseAtPly(prevBestMove, null, needToEscapeCheck, true, currPly);
		do {
			MoveListIterator move_iter = ml.getNextMovesAtPly(currPly);
			if (!move_iter.hasNext()) {
				if (SearchDebugAgent.DEBUG_ENABLED && moveNumber == 0) sda.printExtSearchNoMoves(alpha);
				// As soon as there are no more moves returned from staged move generation, break out in extended search
				return alpha;
			}
			do {
				currMove = move_iter.nextInt();
				moveNumber += 1;
				if (EubosEngineMain.ENABLE_ASSERTS) {
					assert currMove != Move.NULL_MOVE: "Null move found in MoveList";
				}
				if (moveNumber == 1) {
					pc.initialise(currPly, currMove);
				}

				if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printPerformMove(currMove);
				if (SearchDebugAgent.DEBUG_ENABLED) sda.nextPly();
				currPly++;
				pm.performMove(currMove);
				
				positionScore = (short) -extendedSearch(-beta, -alpha, pos.isKingInCheck());
				
				pm.unperformMove();
				currPly--;
				if (SearchDebugAgent.DEBUG_ENABLED) sda.prevPly();
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printUndoMove(currMove, positionScore);
				if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
				
				if (isTerminated()) { return 0;	} // don't update PV if out of time for search, instead return last fully searched PV.
				
				// Handle score backed up to this node
				if (positionScore > alpha) {
					if (positionScore >= beta) {
						if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(positionScore);
						return beta;
					}
					alpha = positionScore;
					pc.update(currPly, currMove);
				}
			} while (move_iter.hasNext());
		} while (!isTerminated());

		return alpha;
	}
	
	private void updateLazyStatistics(short plyScore) {
		int delta = Math.abs(plyScore-pe.getFullEvaluation());
		if (delta > LAZY_EVAL_THRESHOLD_IN_CP) {
			delta -= LAZY_EVAL_THRESHOLD_IN_CP;
			if (delta < LazyEvalStatistics.MAX_DELTA) {
				lazyStat.lazyThreshFailedCount[delta]++;
			} else {
				lazyStat.maxFailureCount++;
				lazyStat.maxFailure = Math.max(delta, lazyStat.maxFailure);
			}
		}
	}
	
	void evaluateTransposition(long trans, int depth) {
		boolean override_trans_move = false;
		
		if (depth <= Transposition.getDepthSearchedInPly(trans)) {
			int type = Transposition.getType(trans);
			isCutOff[currPly] = false;
			override_trans_move = checkForRepetitionDueToPositionInSearchTree(Transposition.getBestMove(trans));
			
			if (!override_trans_move || (override_trans_move && type != Score.exact)) {
				boolean check_for_refutation = false;
				
				// If the hashed data is now drawing, due to the position in the search tree, score it accordingly, but still check
				// if it is good enough for a refutation.
				hashScore[currPly] = !override_trans_move ? convertMateScoreForPositionInSearchTree(Transposition.getScore(trans)) : 0;
				switch(type) {
				case Score.exact:
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsTerminalNode(trans, pos.getHash());
					isCutOff[currPly] = true;
					break;
				case Score.upperBound:
					this.beta[currPly] = Math.min(this.beta[currPly], hashScore[currPly]);
					check_for_refutation = true;
					break;
				case Score.lowerBound:
					this.alpha[currPly] = Math.max(this.alpha[currPly], hashScore[currPly]);
					this.alphaOriginal[currPly] = this.alpha[currPly];
					check_for_refutation = true;
					break;
				case Score.typeUnknown:
				default:
					if (EubosEngineMain.ENABLE_ASSERTS) assert false;
					break;
				}
				
				if (check_for_refutation) {
					// Determine if good enough for a refutation...
					if (this.alpha[currPly] >= this.beta[currPly]) {
						if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsRefutation(pos.getHash(), trans);
						killers.addMove(currPly, Transposition.getBestMove(trans));
						isCutOff[currPly] = true;
					}
				}
				if (isCutOff[currPly]) {
					// Refutation or exact score already known to require search depth, cut off the Search
					pc.set(currPly, Transposition.getBestMove(trans));
				    if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
				    if (SearchDebugAgent.DEBUG_ENABLED) sda.printCutOffWithScore(hashScore[currPly]);
				}
			}
		}
		// Transposition may still be useful to seed the move list, if not drawing.
		if (!override_trans_move || (override_trans_move && prevBestMove[currPly] == Move.NULL_MOVE)) {
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsSeedMoveList(pos.getHash(), trans);
			prevBestMove[currPly] = Transposition.getBestMove(trans);
		}
	}
	
	private long updateTranspositionTable(long trans, byte depth, int currMove, short plyScore) {
		byte plyBound = Score.typeUnknown;
		if (plyScore <= alphaOriginal[currPly]) {
			// Didn't raise alpha
			plyBound = Score.upperBound;
		} else if (plyScore >= this.beta[currPly]) {
			// A beta cut-off, alpha raise was 'too good'
			plyBound = Score.lowerBound;
		} else {
			// because of LMR we can't be sure about depth for a non-PV node, so keep it as upper bound
			plyBound = Score.upperBound;
		}
		return updateTranspositionTable(trans, depth, currMove, plyScore, plyBound);
	}
	
	private long updateTranspositionTable(long trans, byte depth, int currMove, short plyScore, byte plyBound) {
		// Modify mate score (which is expressed in distance from the root node, in ply) to
		// the distance from leaf node (which is what needs to be stored in the hash table).
		short scoreFromDownTree = plyScore;
		if (Score.isMate(plyScore)) {
			scoreFromDownTree = (short) ((plyScore < 0) ? plyScore - currPly : plyScore + currPly);
		}
		trans = tt.setTransposition(trans, depth, scoreFromDownTree, plyBound, currMove);
		return trans;
	}
	
	private int convertMateScoreForPositionInSearchTree(short trans_score)  {	
		short adjustedScoreForThisPositionInTree = trans_score;
		if (Score.isMate(trans_score)) {
			// The score stored in the hash table encodes the distance to the mate from the hashed position,
			// not the root node, so adjust for the position in search tree.
			adjustedScoreForThisPositionInTree = (short) ((trans_score < 0 ) ? trans_score+currPly : trans_score-currPly);
		}
		return adjustedScoreForThisPositionInTree;
	}

	private boolean checkForRepetitionDueToPositionInSearchTree(int move)  {
		boolean retVal = false;
		if (pos.moveLeadsToThreefold(move)) {
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printRepeatedPositionHash(pos.getHash(), pos.getFen());
			retVal = true;
		}
		return retVal;
	}

	private void reportPv(short positionScore) {
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) {
			sm.setPrincipalVariationData(extendedSearchDeepestPly, pc.toPvList(0), pc.length[0], positionScore);
			sr.reportPrincipalVariation(sm);
			extendedSearchDeepestPly = 0;
		}
	}
	
	public void reportLazyStatistics() {
		if (TUNE_LAZY_EVAL) {
			lazyStat.report();
		}
	}
	
	private void setAlphaBeta() {
		alpha[currPly] = -beta[currPly-1];
		beta[currPly] = -alpha[currPly-1];
	}
	
	private int doNullMoveSubTreeSearch(int depth) {
		int plyScore;
		int R = 2;
		if (depth > 6) R = 3;
		currPly++;
		pm.performNullMove();
		alpha[currPly] = -beta[currPly-1];
		beta[currPly] = -beta[currPly-1]+1;
		plyScore = -search(depth-1-R, false);
		pm.unperformNullMove();
		currPly--;
		return plyScore;
	}
	
	private int doLateMoveReductionSubTreeSearch(int depth, boolean needToEscapeCheck, int currMove, int moveNumber) {
		int positionScore = 0;
		boolean passedLmr = false;
		if (EubosEngineMain.ENABLE_LATE_MOVE_REDUCTION &&
			moveNumber > 1 && /* Search at least one quiet move */
			!pe.goForMate() &&
			depth > 3  &&
		    !needToEscapeCheck && 
			!(Move.isPawnMove(currMove) && 
					(pos.getTheBoard().me.isEndgame() ||
					 pos.getTheBoard().isPassedPawn(
							 Move.getOriginPosition(currMove), 
							 Piece.isWhite(Move.getOriginPiece(currMove)) ? Colour.white : Colour.black))) &&
			!pos.isKingInCheck()) {
			
			// Calculate reduction, 1 for the first 6 moves, then the closer to the root node, the more severe the reduction
			int lmr = (moveNumber < 6) ? 1 : depth/3;
			if ((((currPly-1) & 0x1) == 0) && (pe.getCrudeEvaluation() > refScore) && lmr > 1) {
				lmr -= 1;
			}
			if (lmr > 0) {
				setAlphaBeta();
				positionScore = -search(depth-1-lmr);
				if (positionScore <= alpha[currPly-1]) {
					passedLmr = true;
				}
			}
		}
		if (!passedLmr) {
			// Re-search if the reduced search increased alpha 
			setAlphaBeta();
			positionScore = -search(depth-1);
		}
		return positionScore;
	}

	public boolean lastAspirationFailed() {
		return lastAspirationFailed ;
	}
}
