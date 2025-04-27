package eubos.search;

import eubos.board.Piece;
import eubos.evaluation.IEvaluate;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.MoveListIterator;
import eubos.search.transposition.ITranspositionAccessor;
import eubos.search.transposition.Transposition;

public class PlySearcher {
	
	private static final int [] ASPIRATION_WINDOW_FALLBACK = 
		//{ Piece.MATERIAL_VALUE_PAWN/6, (3*Piece.MATERIAL_VALUE_PAWN)/2, Piece.MATERIAL_VALUE_KNIGHT };
		{ Piece.MATERIAL_VALUE_PAWN/4, 2*Piece.MATERIAL_VALUE_PAWN, Piece.MATERIAL_VALUE_ROOK };

	private static final int [] ASPIRATION_WINDOW_MATE_FALLBACK = 
		{ 1, 10, 25 };
	
	public static final int FUTILITY_THRESHOLD = 200;
	
	int numAlphaFails = 0;
	int numBetaFails = 0;
	
	class SearchState {
		int bestScore;
		int alpha;
		int beta;
		int adaptiveBeta;
		int prevBestMove;
		boolean isCutOff;
		int hashScore;
		boolean isHashScoreValid;
		int moveNumber;
		boolean inCheck; // not initialised here for reasons of optimisation
		short staticEval;
		boolean isStaticValid;
		boolean isImproving;
		
		void initialise(int ply, int alpha, int beta) {
			hashScore = bestScore = Score.PROVISIONAL_ALPHA;
			this.alpha = alpha;
			adaptiveBeta = this.beta = beta;
			isCutOff = false;
			moveNumber = 0;
			staticEval = 0;
			isHashScoreValid = isStaticValid = isImproving = false;
			// This move is only valid for the principal continuation, for the rest of the search, it is invalid. It can also be misleading in iterative deepening?
			// It will deviate from the hash move when we start updating the hash during iterative deepening.
			prevBestMove = Move.clearBest(pc.getBestMove((byte)ply));
		}
		
//		void reinitialise(int alpha, int beta) {
//			bestScore = Score.PROVISIONAL_ALPHA;
//			this.alpha = alpha;
//			adaptiveBeta = this.beta = beta;
//			moveNumber = 0;
//		}
		
		void update() {
			inCheck = pos.isKingInCheck();
		}
	};
	
	private SearchState state[];
	
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
	public long rootTransposition = 0L;
	
	private MoveList ml;
	
	private boolean hasSearchedPv = false;
	private boolean lastAspirationFailed = false;
	public boolean certain = false;
	
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
			MoveList ml) {
		currPly = 0;
		state = new SearchState[EubosEngineMain.SEARCH_DEPTH_IN_PLY+1]; // Lengthened to prevent out by one errors in LMR update
		for (int i=0; i < state.length; i++) {
			state[i] = new SearchState();
		}
		
		this.pc = pc;
		this.sm = sm;
		this.pm = pm;
		this.pos = pos;
		this.pe = pe;
		this.sr = sr;
		this.sda = sda;
		originalSearchDepthRequiredInPly = searchDepthPly;
		
		tt = hashMap;
		this.killers = killers;
		this.ml = ml;
		
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
			rootTransposition = tt.getTransposition(pos.getHash());
		}
	}
	
	public void reinitialise(byte searchDepthPly, SearchMetricsReporter sr) {
		this.sr = sr;
		originalSearchDepthRequiredInPly = searchDepthPly;
		
		hasSearchedPv = false;
		lastAspirationFailed = false;
		//certain = false;
		terminate = false;
		
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
			// Back up the root transposition, because it can be lost in the search
			// if it is overwritten and that is very costly for performance.
			rootTransposition = tt.getTransposition(pos.getHash());
		}
	}

	public void terminateFindMove() {
		if (EubosEngineMain.ENABLE_LOGGING) {
			EubosEngineMain.logger.fine("Terminating PlySearcher");
		}
		terminate = true;
	}
	private boolean isTerminated() { return terminate; }	
	
	private int getCoefficientAlpha(int lastScore, int windowSize) {
		return Math.max(Score.PROVISIONAL_ALPHA, Math.min(Score.PROVISIONAL_BETA-1, lastScore - (windowSize*numAlphaFails)));
	}
	
	private int getCoefficientBeta(int lastScore, int windowSize) {
		return Math.min(Score.PROVISIONAL_BETA, Math.max(Score.PROVISIONAL_ALPHA+1, lastScore + (windowSize*numBetaFails)));
	}
	
	private void log(String debug) {
		EubosEngineMain.logger.fine(debug);
		if (eubos != null) {
			eubos.sendInfoString(debug);
		}
	}
	
	public int searchPly(short lastScore)  {
		currPly = 0;
		extendedSearchDeepestPly = 0;
		int score = lastScore;
		lastAspirationFailed = false;
//		SearchState s = state[0];
//		s.update();
		boolean doAspiratedSearch = false; //!pe.goForMate() && originalSearchDepthRequiredInPly >= 5 && !eubos.generate_training_data;
		boolean doFullWidthSearch = true; //!doAspiratedSearch;

		if (doAspiratedSearch) {
			int [] aspirations = Score.isMate(lastScore) ? ASPIRATION_WINDOW_MATE_FALLBACK : ASPIRATION_WINDOW_FALLBACK;
			int alpha = Score.PROVISIONAL_ALPHA;
			int beta = Score.PROVISIONAL_BETA;
			boolean alphaFail = false;
			boolean betaFail = false;
			boolean initialised = false;
			
			for (int aspiration_window : aspirations) {
				// Adjust the aspiration window, according to the last score, if searching to sufficient depth
				if (!initialised || alphaFail)
					alpha = getCoefficientAlpha(lastScore, aspiration_window);
				if (!initialised || betaFail)
					beta = getCoefficientBeta(lastScore, aspiration_window);
				initialised = true;
				
				if (EubosEngineMain.ENABLE_LOGGING) {
					log(String.format("Aspiration Window window=%d score=%d alpha=%d afails=%d beta=%d bfails=%d depth=%d",
							aspiration_window, score, alpha, numAlphaFails, beta, numBetaFails, originalSearchDepthRequiredInPly));
				}
				
				score = searchRoot(originalSearchDepthRequiredInPly, alpha, beta);
				assert currPly == 0;
				if (isTerminated() && (score == 0 || Score.isProvisional(score))) {
	        		// Early termination, possibly didn't back up a score at the last ply
	        		lastAspirationFailed = false;
	        		break;
	        	} else if ((score > alpha && score < beta) || isTerminated()) {
		        	// Exact score in window returned
		        	lastAspirationFailed = false;
					if (EubosEngineMain.ENABLE_LOGGING) {
						log(String.format("Aspiration returned window=%d score=%d in alpha=%d beta=%d for depth=%d",
									aspiration_window, score, alpha, beta, originalSearchDepthRequiredInPly));
					}
		        	reportPv((short) score);
		            break;
		        } else {
		        	// Score returned was outside aspiration window
		        	lastAspirationFailed = true;
					certain = false;
					if (EubosEngineMain.ENABLE_LOGGING) {
						log(String.format("aspirated search failed score=%d in alpha=%d beta=%d for depth=%d",
									score, alpha, beta, originalSearchDepthRequiredInPly));
					}
					alphaFail = score <= alpha;
					betaFail = score >= beta;
					if (alphaFail) {
						numAlphaFails++;
						reportPvFail((short)alpha, alphaFail);
					}
					if (betaFail) {
						numBetaFails++;
						reportPvFail((short)beta, alphaFail);
					}
					if (sr != null)
						sr.resetAfterWindowingFail();
		        }
			}
			if (lastAspirationFailed) {
				if (EubosEngineMain.ENABLE_LOGGING) {
					log(String.format("searchPly aspirated search failed depth=%d", originalSearchDepthRequiredInPly));
				}
				doFullWidthSearch = true;
			}
		}
		if (doFullWidthSearch) {
			score = searchRoot(originalSearchDepthRequiredInPly, Score.PROVISIONAL_ALPHA, Score.PROVISIONAL_BETA);
			lastAspirationFailed = !certain;
			reportPv((short) score);
		}
		return score;
	}
		
	public int searchRoot(int depth, int alpha, int beta) {
		
		hasSearchedPv = false;
		SearchState s = state[0];
		s.initialise(0, alpha, beta);
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(0);
		
		if (SearchDebugAgent.DEBUG_ENABLED) {
			sda.printStartPlyInfo(pos, originalSearchDepthRequiredInPly);
			sda.printNormalSearch(s.alpha, s.beta);
		}
		
		// Extend search for in-check scenarios, treated outside of quiescence search
		if (s.inCheck) {
			++depth;
		}
		
		long trans = tt.getTransposition(pos.getHash());
		if (trans == 0L) {
			trans = rootTransposition;
		}
		//if (trans != 0L) {
		//	evaluateTransposition(trans, depth);
		//	if (s.isCutOff) {
		//		sm.setPrincipalVariationDataFromHash(0, (short)s.hashScore);
		//		if (sr != null)
		//			sr.reportPrincipalVariation(sm, false, false); /* need to set these booleans somehow */
		//		return s.hashScore;
		//	}
		//}
			
		// Main search loop for root ply
		int bestMove = Move.NULL_MOVE;
		int currMove = Move.NULL_MOVE;
		int positionScore = s.bestScore;
		int quietMoveNumber = 0;
//		if (trans != 0L) {
//			s.prevBestMove = Move.valueOfFromTransposition(trans, pos.getTheBoard());
//		}
		MoveListIterator move_iter = ml.initialiseAtPly(s.prevBestMove, killers.getMoves(0), s.inCheck, false, 0);
		while ((currMove = move_iter.nextInt()) != Move.NULL_MOVE && !isTerminated()) {
			// Legal move check	
			if (!pm.performMove(currMove)) {
				continue;
			}
				
			s.moveNumber += 1;
			if (EubosEngineMain.ENABLE_UCI_MOVE_NUMBER) {
				sm.setCurrentMove(currMove, s.moveNumber);
				if (originalSearchDepthRequiredInPly > 8 && sr != null)
					sr.reportCurrentMove();
			}
			if (s.moveNumber == 1) {
				// First legal move re-initialises the PV 
				bestMove = currMove;
			}
			if (Move.isRegular(currMove)) {
				quietMoveNumber++;
			}
			
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printNormalSearch(s.alpha, s.beta);
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
			
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printPerformMove(currMove);
			if (SearchDebugAgent.DEBUG_ENABLED) sda.nextPly();
			
			positionScore = doLmrSubTreeSearch(depth, currMove, quietMoveNumber, false, s.alpha, s.adaptiveBeta, true);

			if (SearchDebugAgent.DEBUG_ENABLED) sda.prevPly();
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printUndoMove(currMove, positionScore);
			
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
			
			if (isTerminated()) { pm.unperformMove(); return s.bestScore;	} // don't update PV if out of time for search, instead return last fully searched PV.
			
			// Handle score backed up to this node
			if (positionScore > s.bestScore) {
				if (EubosEngineMain.ENABLE_LOGGING) {
					log(String.format("BEST_SCORE INCREASED AT ROOT positionScore=%d score=%d alpha=%d beta=%d depth=%d move=%s",
							positionScore, s.bestScore, s.alpha, s.beta, originalSearchDepthRequiredInPly, Move.toString(bestMove)));
				}
				bestMove = currMove;
				pc.update(0, bestMove);
				if (s.adaptiveBeta == s.beta || depth < 2) {
					s.bestScore = positionScore;
				} else {
					s.bestScore = doLmrSubTreeSearch(depth, currMove, quietMoveNumber, false, positionScore, s.beta, false);
				}
				pm.unperformMove();
				
				if (isTerminated()) { return s.bestScore;	} // could have timed out during research of negascout!
				
				if (s.bestScore > s.alpha) {
					s.alpha = s.bestScore;
					if (EubosEngineMain.ENABLE_LOGGING) {
						log(String.format("ALPHA INCREASED AT ROOT score=%d alpha=%d beta=%d depth=%d move=%s",
								s.bestScore, s.alpha, s.beta, originalSearchDepthRequiredInPly, Move.toString(bestMove)));
					}
					if (s.alpha >= s.beta) {
						killers.addMove(currPly, bestMove);
						ml.history.updateMove(depth, bestMove);
						if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(s.bestScore);
						if (EubosEngineMain.ENABLE_LOGGING) {
							log(String.format("BETA FAIL AT ROOT score=%d alpha=%d beta=%d depth=%d move=%s",
									s.bestScore, s.alpha, s.beta, originalSearchDepthRequiredInPly, Move.toString(bestMove)));
						}
						//trans = updateTranspositionTable(trans, (byte) depth, bestMove, (short) s.bestScore, Score.lowerBound);
						//rootTransposition = trans;
						break;
					}
					//trans = updateTranspositionTable(trans, (byte) depth, bestMove, (short) s.bestScore, Score.upperBound);
					//rootTransposition = trans;
					reportPv((short) s.alpha);
				}
				s.adaptiveBeta = s.alpha + 1;
			} else {
				pm.unperformMove();
			}
			hasSearchedPv = true;
		}
		
		return s.bestScore;
	}
	
	int negaScout(int depth, int alpha, int beta)  {
		return negaScout(depth, true, alpha, beta, true);
	}
	
	@SuppressWarnings("unused")
	int negaScout(int depth, boolean nullCheckEnabled, int alpha, int beta, boolean lmrApplied)  {
		
		// Check for absolute draws
		if (pos.isThreefoldRepetitionPossible() || pos.isInsufficientMaterial()) return 0;
		
		SearchState s = state[currPly];
		s.initialise(currPly, alpha, beta);
		
		// Mate distance pruning
		int mating_value = Score.PROVISIONAL_BETA - currPly;
		if (mating_value < s.beta) {
			s.beta = mating_value;
		    if (s.alpha >= mating_value) return mating_value;
		}
		mating_value = Score.PROVISIONAL_ALPHA + currPly;
		if (mating_value > s.alpha) {
		    s.alpha = mating_value;
		    if (s.beta <= mating_value) return mating_value;
		}

		// Absolute depth limit
		if (currPly >= EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
			return pe.getFullEvaluation();
		}
		
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
		if (SearchDebugAgent.DEBUG_ENABLED) {
			sda.printStartPlyInfo(pos, originalSearchDepthRequiredInPly);
			sda.printNormalSearch(s.alpha, s.beta);
		}
		
		// Extend search for in-check scenarios, treated outside of quiescence search
		if (s.inCheck) {
			++depth;
		}
		
		if (depth <= 0) {
			return extendedSearch(s.alpha, s.beta, depth);
		}
		
		long trans = tt.getTransposition(pos.getHash());
		if (trans != 0L) {
//			evaluateTransposition(trans, depth);
//			if (s.isCutOff) {
//				return s.hashScore;
//			}
		}
		
		if (!s.inCheck && !pe.goForMate()) {
			// Reverse futility pruning
			if (depth < 8 &&
				hasSearchedPv) {
				if (!s.isStaticValid) {
					setStaticEvaluation(trans);
				}
				if (s.staticEval - 330 * depth >= s.beta) {
					return s.beta;
				}
			}
	
			// Null move pruning
			if (EubosEngineMain.ENABLE_NULL_MOVE_PRUNING &&
				!isTerminated() &&
				depth > 2 &&
				nullCheckEnabled &&
				pos.getTheBoard().me.phase < 4000) {
				
				s.bestScore = doNullMoveSubTreeSearch(depth);
				if (isTerminated()) { return 0; }
				
				if (s.bestScore >= s.beta) {
					return s.bestScore;
				} else {
					s.bestScore = Score.PROVISIONAL_ALPHA;
				}
			}
		}
		
		// Main search loop for this ply
		int bestMove = Move.NULL_MOVE;
		int currMove = Move.NULL_MOVE;
		int positionScore = s.bestScore;
		boolean refuted = false;
		int quietMoveNumber = 0;
//		if (trans != 0L) {
//			s.prevBestMove = Move.valueOfFromTransposition(trans, pos.getTheBoard());
//		}
		MoveListIterator move_iter = ml.initialiseAtPly(s.prevBestMove, killers.getMoves(currPly), s.inCheck, false, currPly);
		while ((currMove = move_iter.nextInt()) != Move.NULL_MOVE && !isTerminated()) {
			
			if (EubosEngineMain.ENABLE_FUTILITY_PRUNING) {
				if (quietMoveNumber >= 1) {
					if (neitherAlphaBetaIsMate() && !pe.goForMate() && depth <= 2) {
						if (!s.isStaticValid) {
							setStaticEvaluation(trans);
						}
						if (EubosEngineMain.ENABLE_PER_MOVE_FUTILITY_PRUNING) {
							int threshold = pe.estimateMovePositionalContribution(currMove) + ((depth == 1) ? 0 : 250);
							if (s.isImproving) {
								threshold /= 2;
							}
							if (s.staticEval + threshold < s.alpha) {
								continue;
							}
						} else {
							if (s.staticEval + ((depth == 1) ? 250 : 500) < s.alpha) {
								break;
							}
						}
					}
				}
			}
			
			if (!pm.performMove(currMove)) {
				continue;
			}
			
			s.moveNumber += 1;
			if (s.moveNumber == 1) {
				pc.initialise(currPly, currMove);
				bestMove = currMove;
			}
			if (EubosEngineMain.ENABLE_FUTILITY_PRUNING_OF_KILLER_MOVES ? Move.isRegularOrKiller(currMove) : Move.isRegular(currMove)) {
				quietMoveNumber++;
			} else {
				if (EubosEngineMain.ENABLE_ASSERTS) {
					assert quietMoveNumber == 0 : String.format("Out_of_order move %s num=%d quiet=%d best=%s", 
							Move.toString(currMove), s.moveNumber, quietMoveNumber, Move.toString(bestMove));
				}
			}
			
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printNormalSearch(s.alpha, s.beta);
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
			
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printPerformMove(currMove);
			if (SearchDebugAgent.DEBUG_ENABLED) sda.nextPly();
			positionScore = doLmrSubTreeSearch(depth, currMove, quietMoveNumber, lmrApplied, s.alpha, s.adaptiveBeta, true);
			if (SearchDebugAgent.DEBUG_ENABLED) sda.prevPly();
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printUndoMove(currMove, positionScore);
			
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
			
			if (isTerminated()) { pm.unperformMove(); return 0;	} // don't update PV if out of time for search, instead return last fully searched PV.
			
			// Handle score backed up to this node
			if (positionScore > s.bestScore) {
				bestMove = currMove;
				if (s.adaptiveBeta == s.beta || depth < 2) {
					s.bestScore = positionScore;
				} else {
					s.bestScore = doLmrSubTreeSearch(depth, currMove, quietMoveNumber, lmrApplied, positionScore, s.beta, false);
				}
				if (s.bestScore > s.alpha) {
					s.alpha = s.bestScore;
					pc.update(currPly, bestMove);
				}
				if (s.alpha >= s.beta) {
					killers.addMove(currPly, bestMove);
					ml.history.updateMove(depth, bestMove);
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(s.bestScore);
					refuted = true;
					pm.unperformMove();
					break;
				}
				s.adaptiveBeta = s.alpha + 1;
			}
			pm.unperformMove();
		}
		
		if (!isTerminated()) {
			if (s.moveNumber == 0) {
				// No moves searched at this point means either a stalemate or checkmate has occurred
				return s.inCheck ? Score.getMateScore(currPly) : 0;
			}
			//trans = updateTranspositionTable(trans, (byte) depth, bestMove, (short) s.bestScore, refuted ? Score.lowerBound : Score.upperBound);
		}
		
		return s.bestScore;
	}
	
	@SuppressWarnings("unused")
	int extendedSearch(int alpha, int beta, int depth)  {
		
		if (SearchDebugAgent.DEBUG_ENABLED) sda.printExtSearch(alpha, beta);
		if (currPly > extendedSearchDeepestPly) {
			extendedSearchDeepestPly = currPly;
		}
		pc.initialise(currPly);
		
		// Check for absolute draws
		if (pos.isThreefoldRepetitionPossible() || pos.isInsufficientMaterial()) return 0;
		
		long trans = tt.getTransposition(pos.getHash());
		SearchState s = state[currPly];
		s.initialise(currPly, alpha, beta);
		int prevBestMove = Move.NULL_MOVE;
		//if (trans != 0L) {	
			//s.isCutOff = false;
			//s.hashScore = convertMateScoreForPositionInSearchTree(Transposition.getScore(trans));
//			
//			if (EubosEngineMain.ENABLE_TT_CUT_OFFS_IN_EXTENDED_SEARCH) {
//				int type = Transposition.getType(trans);
//				if (hasSearchedPv && type == (s.hashScore >= beta ? Score.lowerBound : Score.upperBound)) {
//					return s.hashScore;
//				}
//				s.bestScore = Transposition.getStaticEval(trans);
//				if (s.bestScore == Short.MAX_VALUE) {
//					s.bestScore = pe.lazyEvaluation(alpha, beta);
//				}
//				byte boundScope = (s.hashScore > s.bestScore) ? Score.lowerBound : Score.upperBound;
//				if (type == boundScope) {
//					s.bestScore = s.hashScore;
//				}
//			} else {
//				s.bestScore = s.hashScore;
//			}
//			
//			if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsSeedMoveList(pos.getHash(), trans);
		//} else {
			s.bestScore = pe.lazyEvaluation(alpha, beta);
		//}
		
		if (currPly >= EubosEngineMain.SEARCH_DEPTH_IN_PLY || s.bestScore >= beta) {
			// Absolute depth limit, return full eval
			// There is no move to put in the killer table when we stand Pat
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(s.bestScore);
			return s.bestScore;
		}
		if (s.bestScore > alpha) {
			// Null move hypothesis
			alpha = s.bestScore;
		}
		
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
		
		int bestMove = Move.NULL_MOVE;
		int currMove = Move.NULL_MOVE;
		int positionScore = s.bestScore;
		boolean refuted = false;
		if (trans != 0L) {
			prevBestMove = Move.valueOfFromTransposition(trans, pos.getTheBoard());
		}
		MoveListIterator move_iter = ml.initialiseAtPly(prevBestMove, null, s.inCheck, true, currPly);
		while ((currMove = move_iter.nextInt()) != Move.NULL_MOVE && !isTerminated()) {
			// Legal move check	
			if (!pm.performMove(currMove)) {
				continue;
			}			
			s.moveNumber += 1;

			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printPerformMove(currMove);
			if (SearchDebugAgent.DEBUG_ENABLED) sda.nextPly();
			currPly++;
			
			state[currPly].update();
			positionScore = (short) -extendedSearch(-beta, -alpha, depth-1);
			
			pm.unperformMove();
			currPly--;
			if (SearchDebugAgent.DEBUG_ENABLED) sda.prevPly();
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printUndoMove(currMove, positionScore);
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
			
			if (isTerminated()) { return 0;	} // don't update PV if out of time for search, instead return last fully searched PV.
			
			// Handle score backed up to this node
			if (positionScore > alpha) {
				alpha = s.bestScore = positionScore;
				bestMove = currMove;
				if (alpha >= beta) {
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(positionScore);
					refuted = true;
					break;
				}
				pc.update(currPly, bestMove);
			}
		}

		if (!isTerminated() && bestMove != Move.NULL_MOVE) {
			//trans = updateTranspositionTable(trans, (byte) depth, bestMove, (short) s.bestScore, refuted ? Score.lowerBound : Score.upperBound);
		}

		return s.bestScore;
	}
	
	void evaluateTransposition(long trans, int depth) {
		SearchState s = state[currPly];
		short static_eval = Transposition.getStaticEval(trans);
		if (static_eval != Short.MAX_VALUE) {
			s.staticEval = static_eval;
			s.isStaticValid = true;
			isPositionImproving();
		}
		
		s.isCutOff = false;
		if (depth <= Transposition.getDepthSearchedInPly(trans)) {
			int type = Transposition.getType(trans);
			boolean check_for_refutation = false;
			
			// If the hash move is drawing due to the position in the search tree, score accordingly, but still check
			// if it is good enough for a refutation.
			s.hashScore = convertMateScoreForPositionInSearchTree(Transposition.getScore(trans));
			if (type == Score.upperBound) {
				s.adaptiveBeta = s.beta = Math.min(s.beta, s.hashScore);
	        	if (EubosEngineMain.ENABLE_LOGGING) {
	        		if (currPly == 0)
	        			log(String.format("Trans upperBound reducing beta=%d hashScore=%d",
								s.beta, s.hashScore));
				}
				check_for_refutation = true;
			} else if (type == Score.lowerBound) {
				s.alpha = Math.max(s.alpha, s.hashScore);
	        	if (EubosEngineMain.ENABLE_LOGGING) {
	        		if (currPly == 0)
	        			log(String.format("Trans lowerBound increasing alpha=%d hashScore=%d",
								s.alpha, s.hashScore));
				}
				check_for_refutation = true;
			} else {
				// Type unknown or exact
			}
			
			if (check_for_refutation) {
				// Determine if good enough for a refutation...
				if (s.alpha >= s.beta) {
		        	if (EubosEngineMain.ENABLE_LOGGING) {
		        		if (currPly == 0)
		        			log(String.format("Trans cut-off as alpha=%d >= beta=%d",
									s.alpha, s.beta));
					}
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsRefutation(pos.getHash(), trans);
					int trans_move = Move.valueOfFromTransposition(trans, pos.getTheBoard());
					// Refutation at required search depth, cut off the Search
					killers.addMove(currPly, trans_move);
					ml.history.updateMove(depth, trans_move);
					s.isCutOff = true;
					pc.set(currPly, trans_move);
				    if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
				    if (SearchDebugAgent.DEBUG_ENABLED) sda.printCutOffWithScore(s.hashScore);
				} else {
					s.isHashScoreValid = true;
				}
			}
		}
	}
	
	private long updateTranspositionTable(long trans, byte depth, int currMove, short plyScore, byte plyBound) {
		// Modify mate score (which is expressed in distance from the root node, in ply) to
		// the distance from leaf node (which is what needs to be stored in the hash table).
		SearchState s = state[currPly];
		short scoreFromDownTree = plyScore;
		if (Score.isMate(plyScore)) {
			scoreFromDownTree = (short) ((plyScore < 0) ? plyScore - currPly : plyScore + currPly);
		}
		trans = tt.setTransposition(pos.getHash(), trans, depth, scoreFromDownTree, plyBound, (short)currMove, pos.getMoveNumber(), 
				s.isStaticValid ? s.staticEval : Short.MAX_VALUE);
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

	private void reportPv(short positionScore) {
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) {
			sm.setPrincipalVariationData(extendedSearchDeepestPly, pc.toPvList(0), pc.length[0], positionScore);
			if (sr != null)
				sr.reportPrincipalVariation(sm, true, false);
			extendedSearchDeepestPly = 0;
		}
		certain = !(isTerminated() && positionScore == 0);
	}
	
	private void reportPvFail(short positionScore, boolean alpha) {
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) {
			sm.setPrincipalVariationData(extendedSearchDeepestPly, pc.toPvList(0), pc.length[0], positionScore);
			if (sr != null)
				sr.reportPrincipalVariation(sm, false, alpha);
			extendedSearchDeepestPly = 0;
		}
		certain = false;
	}
	
	private int doNullMoveSubTreeSearch(int depth) {
		int plyScore;
		int R = 2;
		if (depth > 6) R = 3;
		
		//if (state[currPly].isImproving) R++;
		
		if (SearchDebugAgent.DEBUG_ENABLED) { sda.printNullMove(R);	}
		
		currPly++;
		pm.performNullMove();
		
		SearchState prev_s = state[currPly-1];
		SearchState s = state[currPly];
		s.inCheck = prev_s.inCheck;
		plyScore = -negaScout(depth-1-R, false, -prev_s.beta, -prev_s.beta+1, false);
		
		pm.unperformNullMove();
		currPly--;
		return plyScore;
	}
	
	private int doLmrSubTreeSearch(int depth, int currMove, int moveNumber, boolean lmrApplied, int alpha, int beta, boolean scout) {
		int positionScore = 0;
		boolean passedLmr = false;
		currPly++;
		SearchState s = state[currPly];
		if (scout) {
			s.update();
		}
		if (EubosEngineMain.ENABLE_LATE_MOVE_REDUCTION &&
			moveNumber > 1 && /* Full search for at least one quiet move */
			!pe.goForMate() && /* Ignore reductions in a mate search */
			depth > 2 &&
			!(Move.isPawnMove(currMove) &&  /* Not a passed pawn move or a pawn move in endgame */
					(pos.getTheBoard().me.isEndgame() ||
					(pos.getTheBoard().getPassedPawns() & (1L << Move.getTargetPosition(currMove))) != 0L))) {
		
			// Calculate reduction, 1 for the first few moves, then the closer to the root node, the more severe the reduction
			int lmr = (moveNumber < depth/2) ? 1 : Math.max(1, depth/4);
			if (s.inCheck) lmr = 1;
			if (lmr > 0) {
				positionScore = -negaScout(depth-1-lmr, -beta, -alpha);
				if (positionScore <= alpha) {
					passedLmr = true;
				}
			}
		}
		if (!passedLmr) {
			// Re-search if the reduced search increased alpha 
			positionScore = -negaScout(depth-1, true, -beta, -alpha, false);
		}
		currPly--;
		return positionScore;
	}
	
	public boolean lastAspirationFailed() {
		return lastAspirationFailed;
	}
	
	private void isPositionImproving() {
		SearchState s = state[currPly];
		if (currPly >= 2) {
			SearchState prev_prev_s = state[currPly-2];
			if (prev_prev_s.isStaticValid && s.isStaticValid) {
				s.isImproving = s.staticEval > (prev_prev_s.staticEval + 100);
			}
		}
	}
	
	void setStaticEvaluation(long trans) {
		SearchState s = state[currPly];
		s.staticEval = (short) pe.getStaticEvaluation();
		refineStaticEvalWithHashScore(trans);
		s.isStaticValid = true;
		isPositionImproving();
	}
	
	private void refineStaticEvalWithHashScore(long trans) {
		SearchState s = state[currPly];
		if (s.isHashScoreValid) {
			// Match the scope for improvement of the static score with the bound type in the hash entry
			byte boundScope = (s.hashScore > s.staticEval) ? Score.lowerBound : Score.upperBound;
			if (Transposition.getType(trans) == boundScope) {
				// If the bound type matches, then we can improve the static evaluation using the has score.
				s.staticEval = (short) s.hashScore;
			}
		}
	}
	
	private boolean neitherAlphaBetaIsMate() {
		return !Score.isMate((short)state[currPly].alpha) && !Score.isMate((short)state[currPly].beta);
	}
	
	EubosEngineMain eubos;
	public void setEubosEngine(EubosEngineMain eubos) {
		this.eubos = eubos;
	}
}
