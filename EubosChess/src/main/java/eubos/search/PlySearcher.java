package eubos.search;

import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.MoveListIterator;
import eubos.score.IEvaluate;
//import eubos.score.PositionEvaluator;
import eubos.search.transposition.ITranspositionAccessor;
import eubos.search.transposition.Transposition;

public class PlySearcher {
	
	private static final int [] ASPIRATION_WINDOW_FALLBACK = 
		{ Piece.MATERIAL_VALUE_PAWN/4, 2*Piece.MATERIAL_VALUE_PAWN, Piece.MATERIAL_VALUE_ROOK };

	class SearchState {
		int plyScore;
		int alpha;
		int beta;
		int alphaOriginal;
		int prevBestMove;
		boolean isCutOff;
		int hashScore;
		int moveNumber;
		boolean inCheck; // not initialised here for reasons of optimisation
		
		void initialise(int ply, int alpha, int beta) {
			hashScore = plyScore = Score.PROVISIONAL_ALPHA;
			alphaOriginal = this.alpha = alpha;
			this.beta = beta;
			isCutOff = false;
			moveNumber = 0;
			// This move is only valid for the principal continuation, for the rest of the search, it is invalid. It can also be misleading in iterative deepening?
			// It will deviate from the hash move when we start updating the hash during iterative deepening.
			prevBestMove = Move.clearBest(pc.getBestMove((byte)ply));
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
	@SuppressWarnings("unused")
	private short refScore;
	public long rootTransposition = 0L;
	
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
		state = new SearchState[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
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
		this.refScore = refScore;
		originalSearchDepthRequiredInPly = searchDepthPly;
		
		tt = hashMap;
		this.killers = killers;
		this.ml = ml;
		
		rootTransposition = tt.getTransposition(pos.getHash());
	}
	
	public void reinitialise(byte searchDepthPly, SearchMetricsReporter sr, short refScore) {
		this.sr = sr;
		this.refScore = refScore;
		originalSearchDepthRequiredInPly = searchDepthPly;
		
		hasSearchedPv = false;
		lastAspirationFailed = false;
		terminate = false;
		
		// Back up the root transposition, because it can be lost in the search
		// if it is overwritten and that is very costly for performance.
		rootTransposition = tt.getTransposition(pos.getHash());
	}

	public void terminateFindMove() { 
		EubosEngineMain.logger.info("Terminating PlySearcher");
		terminate = true;
	}
	private boolean isTerminated() { return terminate; }	
	
	public int searchPly(short lastScore)  {
		currPly = 0;
		extendedSearchDeepestPly = 0;
		short score = 0;
		state[0].update();
		
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
				score = (short) searchRoot(originalSearchDepthRequiredInPly, alpha, beta);
		
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
			score = (short) searchRoot(originalSearchDepthRequiredInPly, Score.PROVISIONAL_ALPHA, Score.PROVISIONAL_BETA);
		}
		return score;
	}
		
	int searchRoot(int depth, int alpha, int beta) {
		
		hasSearchedPv = false;
		state[0].initialise(0, alpha, beta);
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(0);
		
		if (SearchDebugAgent.DEBUG_ENABLED) {
			sda.printStartPlyInfo(pos, originalSearchDepthRequiredInPly);
			sda.printNormalSearch(state[0].alpha, state[0].beta);
		}
		
		// Extend search for in-check scenarios, treated outside of quiescence search
		if (state[0].inCheck) {
			++depth;
		}
		
		long trans = tt.getTransposition(pos.getHash());
		if (trans == 0L) {
			trans = rootTransposition;
		}
		if (trans != 0L) {
			evaluateTransposition(trans, depth);
			if (state[0].isCutOff) {
				sm.setPrincipalVariationDataFromHash(0, (short)state[0].hashScore);
				if (sr != null)
					sr.reportPrincipalVariation(sm);
				return state[0].hashScore;
			}
		}
			
		// Main search loop for root ply
		int bestMove = Move.NULL_MOVE;
		int currMove = Move.NULL_MOVE;
		int positionScore = state[0].plyScore;
		int quietOffset = 0;
		boolean refuted = false;
		ml.initialiseAtPly(state[0].prevBestMove, killers.getMoves(0), state[0].inCheck, false, 0);
		do {
			MoveListIterator move_iter = ml.getNextMovesAtPly(0);
			if (!move_iter.hasNext()) {
				if (state[0].moveNumber == 0) {
					// No moves at this point means either a stalemate or checkmate has occurred
					return state[0].inCheck ? Score.getMateScore(0) : 0;
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
				state[0].moveNumber += 1;
				if (state[0].moveNumber == 1) {
					pc.initialise(0, currMove);
					bestMove = currMove;
				}
				if (!Move.isRegular(currMove)) {
					quietOffset = state[0].moveNumber;
				}
				if (EubosEngineMain.ENABLE_UCI_MOVE_NUMBER) {
					sm.setCurrentMove(currMove, state[0].moveNumber);
					if (originalSearchDepthRequiredInPly > 8)
						sr.reportCurrentMove();
				}
				
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printNormalSearch(state[0].alpha, state[0].beta);
				if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(0);
				
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printPerformMove(currMove);
				if (SearchDebugAgent.DEBUG_ENABLED) sda.nextPly();
				currPly++;
				pm.performMove(currMove);
				
				positionScore = doLateMoveReductionSubTreeSearch(depth, currMove, (state[0].moveNumber - quietOffset));
				
				pm.unperformMove();
				currPly--;
				if (SearchDebugAgent.DEBUG_ENABLED) sda.prevPly();
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printUndoMove(currMove, positionScore);
				
				if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
				
				if (isTerminated()) { return 0;	} // don't update PV if out of time for search, instead return last fully searched PV.
				
				// Handle score backed up to this node
				if (positionScore > state[0].alpha) {
					state[0].alpha = state[0].plyScore = positionScore;
					bestMove = currMove;
					pc.update(0, bestMove);
					if (state[0].alpha >= state[0].beta) {
						state[0].plyScore = state[0].beta; // fail hard
						killers.addMove(0, bestMove);
						// Don't report a beta failure PV at the root as this means an aspiration window failure
						// and we don't want to spoil the PV in the SMR with the aspiration fail line
						if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(state[0].plyScore);
						refuted = true;
						break;
					}
					trans = updateTranspositionTable(trans, (byte) depth, bestMove, (short) state[0].alpha, Score.upperBound);
					reportPv((short) state[0].alpha);
				} 
				else if (positionScore > state[0].plyScore) {
					bestMove = currMove;
					state[0].plyScore = positionScore;
				}
				
				hasSearchedPv = true;
				
			} while (move_iter.hasNext());
		} while (!isTerminated() && !refuted);
		
		if (!isTerminated()) {
			trans = updateTranspositionTable(trans, (byte) depth, bestMove, (short) state[0].plyScore);
			rootTransposition = trans;
		}
		
		// fail hard, so don't return plyScore
		return state[0].alpha;
	}
	
	int search(int depth, int alpha, int beta)  {
		return search(depth, true, alpha, beta);
	}
	
	@SuppressWarnings("unused")
	int search(int depth, boolean nullCheckEnabled, int alpha, int beta)  {
		
		state[currPly].initialise(currPly, alpha, beta);
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
		
		// Check for draws by three-fold repetition
		if (pos.isThreefoldRepetitionPossible()) return 0;
		
		// Mate distance pruning
		int mating_value = Score.PROVISIONAL_BETA - currPly;
		if (mating_value < state[currPly].beta) {
			state[currPly].beta = mating_value;
		    if (state[currPly].alpha >= mating_value) return mating_value;
		}
		mating_value = Score.PROVISIONAL_ALPHA + currPly;
		if (mating_value > state[currPly].alpha) {
		    state[currPly].alpha = mating_value;
		    if (state[currPly].beta <= mating_value) return mating_value;
		}

		// Absolute depth limit
		if (currPly >= EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
			return pe.getFullEvaluation();
		}
		
		if (SearchDebugAgent.DEBUG_ENABLED) {
			sda.printStartPlyInfo(pos, originalSearchDepthRequiredInPly);
			sda.printNormalSearch(state[currPly].alpha, state[currPly].beta);
		}
		
		// Extend search for in-check scenarios, treated outside of quiescence search
		if (state[currPly].inCheck) {
			++depth;
		}
		
		if (depth <= 0) {
			return extendedSearch(state[currPly].alpha, state[currPly].beta);
		}
		
		long trans = tt.getTransposition(pos.getHash());
		if (trans != 0L) {
			evaluateTransposition(trans, depth);
			if (state[currPly].isCutOff) {
				return state[currPly].hashScore;
			}
		}
		
		// Null move pruning
		if (EubosEngineMain.ENABLE_NULL_MOVE_PRUNING &&
			!isTerminated() &&
			depth > 2 &&
			nullCheckEnabled &&
			hasSearchedPv && 
			!pos.getTheBoard().me.isEndgame() &&
			!state[currPly].inCheck &&
			!(Score.isMate((short)state[currPly].beta) || Score.isMate((short)state[currPly].alpha)) && 
			//(state[currPly].hashScore >= state[currPly].beta ||
			//pe.lazyEvaluation(state[currPly].alpha, state[currPly].beta) > state[currPly].beta)) {
			pe.getCrudeEvaluation() >= state[currPly].beta) {
			
			state[currPly].plyScore = doNullMoveSubTreeSearch(depth);
			if (isTerminated()) { return 0; }
			
			if (state[currPly].plyScore >= state[currPly].beta) {
				return state[currPly].beta;
			} else {
				state[currPly].plyScore = Score.PROVISIONAL_ALPHA;
			}
		}
		
		// Main search loop for this ply
		int bestMove = Move.NULL_MOVE;
		int currMove = Move.NULL_MOVE;
		int positionScore = state[currPly].plyScore;
		int quietOffset = 0;
		boolean refuted = false;
		ml.initialiseAtPly(state[currPly].prevBestMove, killers.getMoves(currPly), state[currPly].inCheck, false, currPly);
		do {
			MoveListIterator move_iter = ml.getNextMovesAtPly(currPly);
			if (!move_iter.hasNext()) {
				if (state[currPly].moveNumber == 0) {
					// No moves at this point means either a stalemate or checkmate has occurred
					return state[currPly].inCheck ? Score.getMateScore(currPly) : 0;
				} else {
					// As soon as there are no more moves returned from staged move generation, break out, if we already searched a move
					break;
				}
			}
			do {
				currMove = move_iter.nextInt();
				state[currPly].moveNumber += 1;
				if (EubosEngineMain.ENABLE_ASSERTS) {
					assert currMove != Move.NULL_MOVE: "Null move found in MoveList";
				}
				if (state[currPly].moveNumber == 1) {
					pc.initialise(currPly, currMove);
					bestMove = currMove;
				}
				if (!Move.isRegular(currMove)) {
					quietOffset = state[currPly].moveNumber;
				}
				
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printNormalSearch(state[currPly].alpha, state[currPly].beta);
				if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
				
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printPerformMove(currMove);
				if (SearchDebugAgent.DEBUG_ENABLED) sda.nextPly();
				currPly++;
				pm.performMove(currMove);
				
				positionScore = doLateMoveReductionSubTreeSearch(depth, currMove, (state[currPly-1].moveNumber - quietOffset));
				
				pm.unperformMove();
				currPly--;
				if (SearchDebugAgent.DEBUG_ENABLED) sda.prevPly();
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printUndoMove(currMove, positionScore);
				
				if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
				
				if (isTerminated()) { return 0;	} // don't update PV if out of time for search, instead return last fully searched PV.
				
				// Handle score backed up to this node
				if (positionScore > state[currPly].alpha) {
					state[currPly].alpha = state[currPly].plyScore = positionScore;
					bestMove = currMove;
					if (state[currPly].alpha >= state[currPly].beta) {
						state[currPly].plyScore = state[currPly].beta; // fail hard
						killers.addMove(currPly, bestMove);
						if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(state[currPly].plyScore);
						refuted = true;
						break;
					}
					pc.update(currPly, bestMove);
				} 
				else if (positionScore > state[currPly].plyScore) {
					bestMove = currMove;
					state[currPly].plyScore = positionScore;
				}
			} while (move_iter.hasNext());
		} while (!isTerminated() && !refuted);
		
		if (!isTerminated()) {
			trans = updateTranspositionTable(trans, (byte) depth, bestMove, (short) state[currPly].plyScore);
		}
		
		// fail hard, so don't return plyScore
		return state[currPly].alpha;
	}
	
	@SuppressWarnings("unused")
	private int extendedSearch(int alpha, int beta)  {
		
		if (SearchDebugAgent.DEBUG_ENABLED) sda.printExtSearch(alpha, beta);
		if (currPly > extendedSearchDeepestPly) {
			extendedSearchDeepestPly = currPly;
		}
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
		
		state[currPly].plyScore = pe.lazyEvaluation(alpha, beta);
		if (state[currPly].plyScore == Short.MIN_VALUE) {
			// We are standing PAT, so values less than alpha can be increased, that is why 
			// this threashold is not plyScore <= alpha!
			return alpha;
		}
		if (state[currPly].plyScore >= beta) {
			// There is no move to put in the killer table when we stand Pat
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(state[currPly].plyScore);
			return beta;
		}
		if (currPly >= EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
			// Absolute depth limit, return full eval
			return state[currPly].plyScore;
		}
		
		if (state[currPly].plyScore > alpha) {
			// Null move hypothesis
			alpha = state[currPly].plyScore;
		}
		
		long trans = tt.getTransposition(pos.getHash());
		int prevBestMove = Move.NULL_MOVE;
		if (trans != 0L) {
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsSeedMoveList(pos.getHash(), trans);
			prevBestMove = Transposition.getBestMove(trans);
		}
		
		int currMove = Move.NULL_MOVE;
		int positionScore = state[currPly].plyScore;
		ml.initialiseAtPly(prevBestMove, null, state[currPly].inCheck, true, currPly);
		do {
			MoveListIterator move_iter = ml.getNextMovesAtPly(currPly);
			if (!move_iter.hasNext()) {
				if (SearchDebugAgent.DEBUG_ENABLED && state[currPly].moveNumber == 0) sda.printExtSearchNoMoves(alpha);
				// As soon as there are no more moves returned from staged move generation, break out in extended search
				return alpha;
			}
			do {
				currMove = move_iter.nextInt();
				state[currPly].moveNumber += 1;
				if (EubosEngineMain.ENABLE_ASSERTS) {
					assert currMove != Move.NULL_MOVE: "Null move found in MoveList";
				}
				if (state[currPly].moveNumber == 1) {
					pc.initialise(currPly, currMove);
				}

				if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printPerformMove(currMove);
				if (SearchDebugAgent.DEBUG_ENABLED) sda.nextPly();
				currPly++;
				pm.performMove(currMove);
				
				state[currPly].update();
				positionScore = (short) -extendedSearch(-beta, -alpha);
				
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
	
	void evaluateTransposition(long trans, int depth) {
		boolean override_trans_move = false;
		
		if (depth <= Transposition.getDepthSearchedInPly(trans)) {
			int type = Transposition.getType(trans);
			state[currPly].isCutOff = false;
			override_trans_move = checkForRepetitionDueToPositionInSearchTree(Transposition.getBestMove(trans));
			
			if (!override_trans_move || (override_trans_move && type != Score.exact)) {
				boolean check_for_refutation = false;
				
				// If the hashed data is now drawing, due to the position in the search tree, score it accordingly, but still check
				// if it is good enough for a refutation.
				state[currPly].hashScore = !override_trans_move ? convertMateScoreForPositionInSearchTree(Transposition.getScore(trans)) : 0;
				switch(type) {
				case Score.exact:
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsTerminalNode(trans, pos.getHash());
					state[currPly].isCutOff = true;
					break;
				case Score.upperBound:
					state[currPly].beta = Math.min(state[currPly].beta, state[currPly].hashScore);
					check_for_refutation = true;
					break;
				case Score.lowerBound:
					state[currPly].alpha = Math.max(state[currPly].alpha, state[currPly].hashScore);
					state[currPly].alphaOriginal = state[currPly].alpha;
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
					if (state[currPly].alpha >= state[currPly].beta) {
						if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsRefutation(pos.getHash(), trans);
						killers.addMove(currPly, Transposition.getBestMove(trans));
						state[currPly].isCutOff = true;
					}
				}
				if (state[currPly].isCutOff) {
					// Refutation or exact score already known to require search depth, cut off the Search
					pc.set(currPly, Transposition.getBestMove(trans));
				    if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
				    if (SearchDebugAgent.DEBUG_ENABLED) sda.printCutOffWithScore(state[currPly].hashScore);
				}
			}
		}
		// Transposition may still be useful to seed the move list, if not drawing.
		if (!override_trans_move || (override_trans_move && state[currPly].prevBestMove == Move.NULL_MOVE)) {
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsSeedMoveList(pos.getHash(), trans);
			state[currPly].prevBestMove = Transposition.getBestMove(trans);
		}
	}
	
	private long updateTranspositionTable(long trans, byte depth, int currMove, short plyScore) {
		byte plyBound = Score.typeUnknown;
		if (plyScore <= state[currPly].alphaOriginal) {
			// Didn't raise alpha
			plyBound = Score.upperBound;
		} else if (plyScore >= state[currPly].beta) {
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
		trans = tt.setTransposition(pos.getHash(), trans, depth, scoreFromDownTree, plyBound, currMove, pos.getMoveNumber());
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
	
	private int doNullMoveSubTreeSearch(int depth) {
		int plyScore;
		int R = 2;
		if (depth > 6) R = 3;
		currPly++;
		pm.performNullMove();
		
		state[currPly].inCheck = state[currPly-1].inCheck;
		plyScore = -search(depth-1-R, false, -state[currPly-1].beta, -state[currPly-1].beta+1);
		
		pm.unperformNullMove();
		currPly--;
		return plyScore;
	}
	
	private int doLateMoveReductionSubTreeSearch(int depth, int currMove, int moveNumber) {
		int positionScore = 0;
		boolean passedLmr = false;
		state[currPly].update(); /* Update inCheck at this ply and static evaluation. */
		
		if (EubosEngineMain.ENABLE_LATE_MOVE_REDUCTION &&
			moveNumber > 1 && /* Search at least one quiet move */
			!pe.goForMate() && /* Ignore reductions in a mate search */
			depth > 2 &&
		    !state[currPly-1].inCheck && /* Neither king is in check */ 
			!state[currPly].inCheck &&
			!(Move.isPawnMove(currMove) &&  /* Not a passed pawn move or a pawn move in endgame */
					(pos.getTheBoard().me.isEndgame() ||
					(pos.getTheBoard().getPassedPawns() & (1L << Move.getOriginPosition(currMove))) != 0L))) {		
			
			// Calculate reduction, 1 for the first 6 moves, then the closer to the root node, the more severe the reduction
			int lmr = (moveNumber < 6) ? 1 : Math.max(1, depth/4);
			
			// Decrease reduction if we searched more moves at the previous ply
			//if (state[currPly-1].moveNumber > 6) {
				//lmr -= 1;
			//}
			//if ((((currPly-1) & 0x1) == 0) && (pe.getCrudeEvaluation() > refScore) && lmr > 1) {
			//	lmr -= 1;
			//}
			//if (!pos.getTheBoard().isPassedPawnPresent()) {
			//	lmr += 1;
			//}
			if (lmr > 0) {
				positionScore = -search(depth-1-lmr, -state[currPly-1].beta, -state[currPly-1].alpha);
				if (positionScore <= state[currPly-1].alpha) {
					passedLmr = true;
				}
			}
		}
		if (!passedLmr) {
			// Re-search if the reduced search increased alpha 
			positionScore = -search(depth-1, -state[currPly-1].beta, -state[currPly-1].alpha);
		}
		return positionScore;
	}

	public boolean lastAspirationFailed() {
		return lastAspirationFailed;
	}
}
