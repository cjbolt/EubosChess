package eubos.search;

import eubos.board.Piece;
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
		//{ Piece.MATERIAL_VALUE_PAWN/6, (3*Piece.MATERIAL_VALUE_PAWN)/2, Piece.MATERIAL_VALUE_KNIGHT };
		{ Piece.MATERIAL_VALUE_PAWN/4, 2*Piece.MATERIAL_VALUE_PAWN, Piece.MATERIAL_VALUE_ROOK };

	private static final int [] ASPIRATION_WINDOW_MATE_FALLBACK = 
		{ 1, 10 };
	
	public static final int FUTILITY_THRESHOLD = 200;
	
	class SearchState {
		int bestScore;
		int alpha;
		int beta;
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
			this.beta = beta;
			isCutOff = false;
			moveNumber = 0;
			staticEval = 0;
			isHashScoreValid = isStaticValid = isImproving = false;
			// This move is only valid for the principal continuation, for the rest of the search, it is invalid. It can also be misleading in iterative deepening?
			// It will deviate from the hash move when we start updating the hash during iterative deepening.
			prevBestMove = Move.clearBest(pc.getBestMove((byte)ply));
		}
		
		void reinitialise(int alpha, int beta) {
			bestScore = Score.PROVISIONAL_ALPHA;
			this.alpha = alpha;
			this.beta = beta;
			moveNumber = 0;
		}
		
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
		
		rootTransposition = tt.getTransposition(pos.getHash());
	}
	
	public void reinitialise(byte searchDepthPly, SearchMetricsReporter sr) {
		this.sr = sr;
		originalSearchDepthRequiredInPly = searchDepthPly;
		
		hasSearchedPv = false;
		lastAspirationFailed = false;
		certain = false;
		terminate = false;
		
		// Back up the root transposition, because it can be lost in the search
		// if it is overwritten and that is very costly for performance.
		rootTransposition = tt.getTransposition(pos.getHash());
	}

	public void terminateFindMove() {
		if (EubosEngineMain.ENABLE_LOGGING) {
			EubosEngineMain.logger.fine("Terminating PlySearcher");
		}
		terminate = true;
	}
	private boolean isTerminated() { return terminate; }	
	
	private int getCoefficientAlpha(int lastScore, int windowSize) {
		int windowOffset = EubosEngineMain.ENABLE_SKEWED_ASPIRATION_WINDOWS ? (lastScore >= 50 ? lastScore/50 : 0) : 0;
		if (windowOffset > windowSize)
			windowSize += windowOffset/2;
		return Math.max(Score.PROVISIONAL_ALPHA, Math.min(Score.PROVISIONAL_BETA-1, lastScore + windowOffset - windowSize));
	}
	
	private int getCoefficientBeta(int lastScore, int windowSize) {
		int windowOffset = EubosEngineMain.ENABLE_SKEWED_ASPIRATION_WINDOWS ? (lastScore <= -50 ? lastScore/50 : 0) : 0;
		if (windowOffset > windowSize)
			windowSize += windowOffset/2;
		return Math.min(Score.PROVISIONAL_BETA, Math.max(Score.PROVISIONAL_ALPHA+1, lastScore + windowOffset + windowSize));
	}
	
	public int searchPly(short lastScore)  {
		currPly = 0;
		extendedSearchDeepestPly = 0;
		int score = lastScore;
		lastAspirationFailed = false;
		SearchState s = state[0];
		s.update();
		boolean doAspiratedSearch = !pe.goForMate() && originalSearchDepthRequiredInPly >= 5;
		boolean doFullWidthSearch = !doAspiratedSearch;

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
					EubosEngineMain.logger.info(String.format("Aspiration Window window=%d score=%d alpha=%d beta=%d depth=%d",
							aspiration_window, score, alpha, beta, originalSearchDepthRequiredInPly));
				}
				
				score = searchRoot(originalSearchDepthRequiredInPly, alpha, beta);
				assert currPly == 0;
				if (Score.isProvisional(score)) {
					EubosEngineMain.logger.severe("Aspiration Window failed - no score, illegal position");
		            return score;
	        	} else if (isTerminated() && score == 0) {
	        		// Early termination, possibly didn't back up a score at the last ply
	        		lastAspirationFailed = false;
	        		certain = false;
	        		break;
	        	} else if ((score > s.alpha && score < s.beta) || isTerminated()) {
		        	// Exact score in window returned
		        	lastAspirationFailed = false;
		        	if (EubosEngineMain.ENABLE_LOGGING) {
						EubosEngineMain.logger.fine(String.format("Aspiration returned window=%d score=%d in alpha=%d beta=%d for depth=%d",
								aspiration_window, score, alpha, beta, originalSearchDepthRequiredInPly));
					}
		        	reportPv((short) score);
		            break;
		        } else {
		        	// Score returned was outside aspiration window
		        	lastAspirationFailed = true;
					certain = false;
					alphaFail = score <= alpha;
					betaFail = score >= beta;
					if (sr != null)
						sr.resetAfterWindowingFail();
		        }
			}
			if (lastAspirationFailed) {
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
		if (trans != 0L) {
			evaluateTransposition(trans, depth);
			if (s.isCutOff) {
				sm.setPrincipalVariationDataFromHash(0, (short)s.hashScore);
				if (sr != null)
					sr.reportPrincipalVariation(sm);
				return s.hashScore;
			}
		}
			
		// Main search loop for root ply
		int bestMove = Move.NULL_MOVE;
		int currMove = Move.NULL_MOVE;
		int positionScore = s.bestScore;
		int quietMoveNumber = 0;
		boolean refuted = false;
		MoveListIterator move_iter = ml.initialiseAtPly(s.prevBestMove, killers.getMoves(0), s.inCheck, false, 0);
		while ((currMove = move_iter.nextInt()) != Move.NULL_MOVE && !isTerminated() && !refuted) {
			// Legal move check	
			if (!pm.performMove(currMove)) {
				continue;
			}
			
			if (EubosEngineMain.ENABLE_ASSERTS) {
				assert !Move.areEqual(currMove, Move.NULL_MOVE): "Null move found in MoveList";
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
				pc.initialise(0, bestMove);
			}
			if (Move.isRegular(currMove)) {
				quietMoveNumber++;
			}
			
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printNormalSearch(s.alpha, s.beta);
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(0);
			
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printPerformMove(currMove);
			if (SearchDebugAgent.DEBUG_ENABLED) sda.nextPly();
			
			currPly++;
			
			positionScore = doLmrSubTreeSearch(depth, currMove, quietMoveNumber, false);
			
			pm.unperformMove();
			currPly--;
			if (SearchDebugAgent.DEBUG_ENABLED) sda.prevPly();
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printUndoMove(currMove, positionScore);
			
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
			
			if (isTerminated()) { return 0;	} // don't update PV if out of time for search, instead return last fully searched PV.
			
			// Handle score backed up to this node
			if (positionScore > s.alpha) {
				s.alpha = s.bestScore = positionScore;
				bestMove = currMove;
				pc.update(0, bestMove);
				if (s.alpha >= s.beta) {
					s.bestScore = s.beta; // fail hard
					killers.addMove(0, bestMove);
					// Don't report a beta failure PV at the root as this means an aspiration window failure
					// and we don't want to spoil the PV in the SMR with the aspiration fail line
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(s.bestScore);
					refuted = true;
		        	if (EubosEngineMain.ENABLE_LOGGING) {
						EubosEngineMain.logger.fine(String.format("BETA FAIL AT ROOT score=%d alpha=%d beta=%d depth=%d move=%s",
								s.bestScore, s.alpha, s.beta, originalSearchDepthRequiredInPly, Move.toString(bestMove)));
					}
					break;
				}
				trans = updateTranspositionTable(trans, (byte) depth, bestMove, (short) s.bestScore, Score.upperBound);
				rootTransposition = trans;
				if (EubosEngineMain.ENABLE_LOGGING) {
					EubosEngineMain.logger.fine(String.format("ALPHA INCREASED AT ROOT score=%d alpha=%d beta=%d depth=%d move=%s",
							s.bestScore, s.alpha, s.beta, originalSearchDepthRequiredInPly, Move.toString(bestMove)));
				}
				reportPv((short) s.alpha);
			} 
			else if (positionScore > s.bestScore) {
				bestMove = currMove;
				s.bestScore = positionScore;
				if (EubosEngineMain.ENABLE_LOGGING) {
					EubosEngineMain.logger.info(String.format("BEST_SCORE INCREASED AT ROOT score=%d alpha=%d beta=%d depth=%d move=%s",
							s.bestScore, s.alpha, s.beta, originalSearchDepthRequiredInPly, Move.toString(bestMove)));
				}
			}
			
			hasSearchedPv = true;
		}
		
		if (!isTerminated()) {
			if (s.moveNumber == 0) {
				// No moves at this point means either a stalemate or checkmate has occurred
				return s.inCheck ? Score.getMateScore(0) : 0;
			}
			trans = updateTranspositionTable(trans, (byte) depth, bestMove, (short) s.bestScore, refuted ? Score.lowerBound : Score.upperBound);
			rootTransposition = trans;
			if (EubosEngineMain.ENABLE_LOGGING) {
				EubosEngineMain.logger.info(String.format("TRANSPOSITION SAVED AT ROOT score=%d alpha=%d beta=%d depth=%d move=%s (%s)",
						s.bestScore, s.alpha, s.beta, depth, Move.toString(bestMove), Transposition.report(trans)));
			}
		}
		
		return s.bestScore;
	}
	
	int search(int depth, int alpha, int beta)  {
		return search(depth, true, alpha, beta, true);
	}
	
	@SuppressWarnings("unused")
	int search(int depth, boolean nullCheckEnabled, int alpha, int beta, boolean lmrApplied)  {
		
		SearchState s = state[currPly];
		s.initialise(currPly, alpha, beta);
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
		
		// Check for absolute draws
		if (pos.isThreefoldRepetitionPossible() || pos.isInsufficientMaterial()) return 0;
		
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
		
		if (SearchDebugAgent.DEBUG_ENABLED) {
			sda.printStartPlyInfo(pos, originalSearchDepthRequiredInPly);
			sda.printNormalSearch(s.alpha, s.beta);
		}
		
		// Extend search for in-check scenarios, treated outside of quiescence search
		if (s.inCheck) {
			++depth;
		}
		
		if (depth <= 0) {
			return extendedSearch(s.alpha, s.beta, depth-1);
		}
		
		long trans = tt.getTransposition(pos.getHash());
		if (trans != 0L) {
			evaluateTransposition(trans, depth);
			if (s.isCutOff) {
				return s.hashScore;
			}
		}
		
		if (!s.inCheck) {
			// Reverse futility pruning
			if (depth < 8 &&
				hasSearchedPv &&
				!pe.goForMate()) {
				if (!s.isStaticValid) {
					setStaticEvaluation(trans);
				}
				if (s.staticEval - 330 * depth >= s.beta) {
					return s.beta;
				}
			}
			
			// Razoring
		    if (EubosEngineMain.ENABLE_RAZORING_ON_QUIESCENCE &&
		    	hasSearchedPv && 
		    	depth <= 5) {
		    	int thresh = s.staticEval + 800 + (150 * depth * depth);
		    	if (thresh < s.alpha) {
		            int value = extendedSearch(s.alpha - 1, s.alpha, depth-1);
		            if (value < s.alpha) {
		                return s.alpha;
		            } else {
		            	s.reinitialise(s.alpha, s.beta);
		            }
		        }
		    }
			
			// Null move pruning
			if (EubosEngineMain.ENABLE_NULL_MOVE_PRUNING &&
				!isTerminated() &&
				depth > 2 &&
				nullCheckEnabled && 
				(pos.getTheBoard().me.phase < 4000 && !pe.goForMate())) {
				
				s.bestScore = doNullMoveSubTreeSearch(depth);
				if (isTerminated()) { return 0; }
				
				if (s.bestScore >= s.beta) {
					return s.bestScore;
				} else {
					s.bestScore = Score.PROVISIONAL_ALPHA;
				}
			}
		}
		
		// Internal Iterative Deepening
		if (EubosEngineMain.ENABLE_ITERATIVE_DEEPENING && 
			s.prevBestMove == Move.NULL_MOVE && 
			depth >= 6 &&
			!isTerminated()) {

			s.update();
			int score = search(depth-3, false, s.alpha, s.beta, true);

		    if (score <= s.alpha) {
		    	score = search(depth-3, false, Score.PROVISIONAL_ALPHA, s.alpha+1, true);
		    }

		    if (EubosEngineMain.ENABLE_ASSERTS) {
			    if (!Score.isMate((short)score) && score != 0 && !isTerminated()) {
				    assert score != Score.PROVISIONAL_ALPHA;
				    assert score != Score.PROVISIONAL_BETA;
			    	//assert pc.getBestMoveAtPly((byte)(currPly)) != Move.NULL_MOVE :
			    	//	String.format("score=%d %s %s next_pc=%s", score, pos.unwindMoveStack(), pos.getFen(), pc.toStringAt(currPly+1));
			    }
		    }

		    /* The rationale for not using alpha and beta on the search stack here is
		       that we already had a hash miss, so can't have reduced the window with
		       a hash hit. */
		    s.reinitialise(alpha, beta);
		    /* Get the suggested best move that was returned and use it as the hash move */		    
		    s.prevBestMove = pc.getBestMoveAtPly((byte)(currPly));
		}
		
		// Main search loop for this ply
		int bestMove = Move.NULL_MOVE;
		int currMove = Move.NULL_MOVE;
		int positionScore = s.bestScore;
		boolean refuted = false;
		int quietMoveNumber = 0;
		MoveListIterator move_iter = ml.initialiseAtPly(s.prevBestMove, killers.getMoves(currPly), s.inCheck, false, currPly, depth == 1);
		while ((currMove = move_iter.nextInt()) != Move.NULL_MOVE && !isTerminated() && !refuted) {
			
			if (EubosEngineMain.ENABLE_FUTILITY_PRUNING) {
				if (quietMoveNumber >= 1) {
					if (neitherAlphaBetaIsMate() && !pe.goForMate() && depth <= 2) {
						if (!s.isStaticValid) {
							setStaticEvaluation(trans);
						}
						if (EubosEngineMain.ENABLE_PER_MOVE_FUTILITY_PRUNING) {
							int threshold = pe.estimateMovePositionalContribution(currMove) + ((depth == 1) ? 0 : 250);
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
			if (EubosEngineMain.ENABLE_ASSERTS) {
				assert !Move.areEqual(currMove, Move.NULL_MOVE): "Null move found in MoveList";
			}
			if (s.moveNumber == 1) {
				pc.initialise(currPly, currMove);
				bestMove = currMove;
			}
			if (EubosEngineMain.ENABLE_FUTILITY_PRUNING_OF_KILLER_MOVES ? Move.isRegularOrKiller(currMove) : Move.isRegular(currMove)) {
				quietMoveNumber++;
			} else {
				if (EubosEngineMain.ENABLE_ASSERTS) {
					//assert quietMoveNumber == 0 : String.format("Out_of_order move %s num=%d quiet=%d best=%s", Move.toString(currMove), s.moveNumber, quietMoveNumber, Move.toString(bestMove));
				}
			}
			
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printNormalSearch(s.alpha, s.beta);
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
			
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printPerformMove(currMove);
			if (SearchDebugAgent.DEBUG_ENABLED) sda.nextPly();
		
			currPly++;
			positionScore = doLmrSubTreeSearch(depth, currMove, quietMoveNumber, lmrApplied);
			
			pm.unperformMove();
			currPly--;
			if (SearchDebugAgent.DEBUG_ENABLED) sda.prevPly();
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printUndoMove(currMove, positionScore);
			
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
			
			if (isTerminated()) { return 0;	} // don't update PV if out of time for search, instead return last fully searched PV.
			
			// Handle score backed up to this node
			if (positionScore > s.alpha) {
				s.alpha = s.bestScore = positionScore;
				bestMove = currMove;
				if (s.alpha >= s.beta) {
					killers.addMove(currPly, bestMove);
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(s.bestScore);
					refuted = true;
					break;
				}
				pc.update(currPly, bestMove);
			} 
			else if (positionScore > s.bestScore) {
				bestMove = currMove;
				s.bestScore = positionScore;
			}
		}
		
		if (!isTerminated()) {
			if (s.moveNumber == 0) {
				// No moves searched at this point means either a stalemate or checkmate has occurred
				return s.inCheck ? Score.getMateScore(currPly) : 0;
			}
			trans = updateTranspositionTable(trans, (byte) depth, bestMove, (short) s.bestScore, refuted ? Score.lowerBound : Score.upperBound);
		}
		
		return s.bestScore;
	}
	
	@SuppressWarnings("unused")
	int extendedSearch(int alpha, int beta, int depth)  {
		
		if (SearchDebugAgent.DEBUG_ENABLED) sda.printExtSearch(alpha, beta);
		if (currPly > extendedSearchDeepestPly) {
			extendedSearchDeepestPly = currPly;
		}
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
		
		SearchState s = state[currPly];
		s.initialise(currPly, alpha, beta);
		pc.initialise(currPly);
		
		// Check for absolute draws
		if (pos.isThreefoldRepetitionPossible() || pos.isInsufficientMaterial()) return 0;
		
		long trans = tt.getTransposition(pos.getHash());
		int prevBestMove = Move.NULL_MOVE;
		if (trans != 0L) {	
			s.isCutOff = false;
			s.hashScore = convertMateScoreForPositionInSearchTree(Transposition.getScore(trans));
			int type = Transposition.getType(trans);
			if (type == Score.exact) {
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsTerminalNode(trans, pos.getHash());
				s.isCutOff = true;
				return s.hashScore;
			} else {
				if (hasSearchedPv && type == (s.hashScore >= beta ? Score.lowerBound : Score.upperBound)) {
					return s.hashScore;
				}
				s.bestScore = Transposition.getStaticEval(trans);
				if (s.bestScore == Short.MAX_VALUE) {
					s.bestScore = pe.lazyEvaluation(alpha, beta);
				}
				byte boundScope = (s.hashScore > s.bestScore) ? Score.lowerBound : Score.upperBound;
				if (type == boundScope) {
					s.bestScore = s.hashScore;
				}
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsSeedMoveList(pos.getHash(), trans);
				prevBestMove = Move.valueOfFromTransposition(trans, pos.getTheBoard());
			}
		} else {
			s.bestScore = pe.lazyEvaluation(alpha, beta);
		}
		
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
		
		int bestMove = Move.NULL_MOVE;
		int currMove = Move.NULL_MOVE;
		int positionScore = s.bestScore;
		boolean refuted = false;
		MoveListIterator move_iter = ml.initialiseAtPly(prevBestMove, null, s.inCheck, true, currPly);
		while ((currMove = move_iter.nextInt()) != Move.NULL_MOVE && !isTerminated()) {
			// Legal move check	
			if (!pm.performMove(currMove)) {
				continue;
			}
			
			s.moveNumber += 1;
			if (EubosEngineMain.ENABLE_ASSERTS) {
				assert currMove != Move.NULL_MOVE: "Null move found in MoveList";
			}

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
			trans = updateTranspositionTable(trans, (byte) depth, bestMove, (short) s.bestScore, refuted ? Score.lowerBound : Score.upperBound);
		}

		return s.bestScore;
	}
	
	void evaluateTransposition(long trans, int depth) {
		SearchState s = state[currPly];
		boolean override_trans_move = false;
		int trans_move = Move.valueOfFromTransposition(trans, pos.getTheBoard());
		short static_eval = Transposition.getStaticEval(trans);
		if (static_eval != Short.MAX_VALUE) {
			s.staticEval = static_eval;
			s.isStaticValid = true;
			isPositionImproving();
		}
		
		if (depth <= Transposition.getDepthSearchedInPly(trans)) {
			int type = Transposition.getType(trans);
			s.isCutOff = false;
			override_trans_move = checkForRepetitionDueToPositionInSearchTree(trans_move);
			boolean check_for_refutation = false;
			
			// If the hash move is drawing due to the position in the search tree, score accordingly, but still check
			// if it is good enough for a refutation.
			s.hashScore = override_trans_move ? 0 : convertMateScoreForPositionInSearchTree(Transposition.getScore(trans));
			switch(type) {
			case Score.exact:
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsTerminalNode(trans, pos.getHash());
				s.isCutOff = !override_trans_move;
				break;
			case Score.upperBound:
				s.beta = Math.min(s.beta, s.hashScore);
	        	if (EubosEngineMain.ENABLE_LOGGING) {
	        		if (currPly == 0)
						EubosEngineMain.logger.fine(String.format("Trans upperBound reducing beta=%d hashScore=%d",
								s.beta, s.hashScore));
				}
				check_for_refutation = true;
				break;
			case Score.lowerBound:
				s.alpha = Math.max(s.alpha, s.hashScore);
	        	if (EubosEngineMain.ENABLE_LOGGING) {
	        		if (currPly == 0)
						EubosEngineMain.logger.fine(String.format("Trans lowerBound increasing alpha=%d hashScore=%d",
								s.alpha, s.hashScore));
				}
				check_for_refutation = true;
				break;
			case Score.typeUnknown:
				break;
			default:
				if (EubosEngineMain.ENABLE_ASSERTS) assert false;
				break;
			}
			
			if (check_for_refutation) {
				// Determine if good enough for a refutation...
				if (s.alpha >= s.beta) {
		        	if (EubosEngineMain.ENABLE_LOGGING) {
		        		if (currPly == 0)
							EubosEngineMain.logger.fine(String.format("Trans cut-off as alpha=%d >= beta=%d",
									s.alpha, s.beta));
					}
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsRefutation(pos.getHash(), trans);
					killers.addMove(currPly, trans_move);
					s.isCutOff = true;
				} else {
					s.isHashScoreValid = true;
				}
			}
			if (s.isCutOff) {
				// Refutation or exact score already known to required search depth, cut off the Search
				pc.set(currPly, trans_move);
			    if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
			    if (SearchDebugAgent.DEBUG_ENABLED) sda.printCutOffWithScore(s.hashScore);
			}
		}
		// Transposition may still be useful to seed the move list, if not drawing.
		if (!override_trans_move || (override_trans_move && s.prevBestMove == Move.NULL_MOVE)) {
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsSeedMoveList(pos.getHash(), trans);
			s.prevBestMove = trans_move;
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
		trans = tt.setTransposition(pos.getHash(), trans, depth, scoreFromDownTree, plyBound, currMove, pos.getMoveNumber(), 
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
			if (sr != null)
				sr.reportPrincipalVariation(sm);
			extendedSearchDeepestPly = 0;
		}
		certain = !(isTerminated() && positionScore == 0);
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
		plyScore = -search(depth-1-R, false, -prev_s.beta, -prev_s.beta+1, false);
		
		pm.unperformNullMove();
		currPly--;
		return plyScore;
	}
	
	private int doLmrSubTreeSearch(int depth, int currMove, int moveNumber, boolean lmrApplied) {
		int positionScore = 0;
		boolean passedLmr = false;
		SearchState prev_s = state[currPly-1];
		SearchState s = state[currPly];
		s.update(); /* Update inCheck */
		if (EubosEngineMain.ENABLE_LATE_MOVE_REDUCTION &&
			moveNumber > 1 && /* Full search for at least one quiet move */
			//!lmrApplied && /* Only apply LMR once per branch of tree */
			!pe.goForMate() && /* Ignore reductions in a mate search */
			depth > 2 &&
			!(Move.isPawnMove(currMove) &&  /* Not a passed pawn move or a pawn move in endgame */
					(pos.getTheBoard().me.isEndgame() ||
					(pos.getTheBoard().getPassedPawns() & (1L << Move.getOriginPosition(currMove))) != 0L))) {		
		
			// Calculate reduction, 1 for the first 6 moves, then the closer to the root node, the more severe the reduction
			int lmr = 0;
			if (prev_s.isImproving) {
				lmr = (moveNumber < (depth * depth)) ? 1 : Math.max(1, depth/5);
			} else {
				lmr = (moveNumber < 6) ? 1 : Math.max(1, depth/4);
			}
			if (s.inCheck) lmr = 1;
			if (lmr > 0) {
				positionScore = -search(depth-1-lmr, -prev_s.beta, -prev_s.alpha);
				if (positionScore <= prev_s.alpha) {
					passedLmr = true;
				}
			}
		}
		if (!passedLmr) {
			// Re-search if the reduced search increased alpha 
			positionScore = -search(depth-1, true, -prev_s.beta, -prev_s.alpha, false);
		}
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
}
