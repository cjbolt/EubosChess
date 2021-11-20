package eubos.search;

import java.util.Iterator;
import java.util.List;


import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.PositionManager;
import eubos.score.IEvaluate;
import eubos.score.IScoreMate;
import eubos.score.MateScoreGenerator;
import eubos.search.generators.MiniMaxMoveGenerator;
import eubos.search.transposition.ITranspositionAccessor;
import eubos.search.transposition.ITransposition;

public class PlySearcher {
	
	private static final boolean ENABLE_MATE_CHECK_IN_EXTENDED_SEARCH = true;
	
	private IChangePosition pm;
	private IPositionAccessors pos;
	private IEvaluate pe;
	private IScoreMate sg;
	
	private PrincipalContinuation pc;
	private SearchMetrics sm;
	private SearchDebugAgent sda;
	
	private boolean terminate = false;
	
	private List<Integer> lastPc;
	private ITranspositionAccessor tt;
	private SearchMetricsReporter sr;
	private KillerList killers;
	
	private byte currPly = 0;
	private byte originalSearchDepthRequiredInPly = 0;
	private byte extendedSearchDeepestPly = 0;
	private byte extendedSearchLimitInPly = 0;
	private int moveListOrdering = 1;
	
	public PlySearcher(
			ITranspositionAccessor hashMap,
			PrincipalContinuation pc,
			SearchMetrics sm,
			SearchMetricsReporter sr,
			byte searchDepthPly,
			IChangePosition pm,
			IPositionAccessors pos,
			List<Integer> lastPc,
			IEvaluate pe,
			KillerList killers,
			SearchDebugAgent sda) {
		currPly = 0;
		
		this.pc = pc;
		this.sm = sm;
		this.pm = pm;
		this.pos = pos;
		this.pe = pe;
		this.sr = sr;
		this.lastPc = lastPc;
		this.sda = sda;
		originalSearchDepthRequiredInPly = searchDepthPly;
		extendedSearchLimitInPly = (byte) (MiniMaxMoveGenerator.EXTENDED_SEARCH_PLY_LIMIT + originalSearchDepthRequiredInPly);
		
		tt = hashMap;
		sg = new MateScoreGenerator(pos, pe);
		this.killers = killers;
	}

	private boolean atRootNode() { return currPly == 0; }
	
	public synchronized void terminateFindMove() { 
		EubosEngineMain.logger.info("Terminating PlySearcher");
		terminate = true;
	}
	private synchronized boolean isTerminated() { return terminate; }	

	public void alternativeMoveListOrdering(int orderingScheme) {
		moveListOrdering = orderingScheme;		
	}
	
	public int searchPly()  {
		currPly = 0;
		extendedSearchDeepestPly = 0;
		return (short) search(Score.PROVISIONAL_ALPHA, Score.PROVISIONAL_BETA, originalSearchDepthRequiredInPly);
	}
	
	public int searchPly(short lastScore)  {
		currPly = 0;
		extendedSearchDeepestPly = 0;	
		boolean exact = false;
		short score = 0;
		
		// Adjust the aspiration window, according to the last score, if searching to sufficient depth
		int alpha = Score.PROVISIONAL_ALPHA;
		int beta = Score.PROVISIONAL_BETA;
		if (originalSearchDepthRequiredInPly >= 5) {
			alpha = Score.isMate(lastScore) ? lastScore-1 : lastScore-25;
			beta = Score.isMate(lastScore) ? lastScore+1 : lastScore+25;
		}

		while (!exact) {
			score = (short) search(alpha, beta, originalSearchDepthRequiredInPly);
	
			if (Score.isProvisional(score)) {
        		// If this is true after the search, it must be an illegal position
        		exact = true;
	            break;
        	} else if (score <= alpha) {
        		// Failed low, adjust window
	            alpha = Score.PROVISIONAL_ALPHA;
	        } else if (score >= beta) {
	        	// Failed high, adjust window
	            beta = Score.PROVISIONAL_BETA;
	        } else {
	        	// Exact score in window returned
	        	exact = true;
	            break;
	        }
		}
		return score;
	}
	
	int search(int alpha, int beta, int depth)  {
		int alphaOriginal = alpha;
		int plyScore = Score.PROVISIONAL_ALPHA;
		int prevBestMove = ((lastPc != null) && (lastPc.size() > currPly)) ? lastPc.get(currPly) : Move.NULL_MOVE;
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
		
		// Handle draws by three-fold repetition
		if (!atRootNode() && pos.isThreefoldRepetitionPossible()) {
			return 0;
		}
		// Absolute depth limit
		if (currPly >= extendedSearchLimitInPly - 1) {
			return pe.evaluatePosition();
		}
		// Extend search for in-check scenarios, treated outside of quiescence search
		boolean needToEscapeCheck = pos.isKingInCheck();
		if (needToEscapeCheck) {
			++depth;
		}
		
		if (SearchDebugAgent.DEBUG_ENABLED) sda.printStartPlyInfo(pos, originalSearchDepthRequiredInPly);
		if (SearchDebugAgent.DEBUG_ENABLED) sda.printNormalSearch(alpha, beta);
		
		if (depth == 0) {
			return extendedSearch(alpha, beta, needToEscapeCheck);
		}
		
		ITransposition trans = tt.getTransposition();
		if (trans != null) {
			if (depth <= trans.getDepthSearchedInPly()) {
				int type = trans.getType();
				boolean isCutOff = false;
				int hashScore = handleRefutationOrTerminalNodeFromHash(trans);
				if (hashScore == 0) {
					// Downgrade transposition status if the position could be drawn.
					type = Score.lowerBound;
				}
				if (type == Score.exact) {
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsTerminalNode(trans, pos.getHash());
					isCutOff = true;
				} else {
					// Update alpha/beta bound score according to transposition data
					if (type == Score.upperBound) {
						beta = Math.min(beta, hashScore);
					} else if (type == Score.lowerBound) {
						alpha = Math.max(alpha, hashScore);
					}
					// Determine if good enough for a refutation...
					if (alpha >= beta) {
						killers.addMove(currPly, trans.getBestMove());
						if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsRefutation(pos.getHash(), trans);
						isCutOff = true;
					}
				}
				if (isCutOff) {
				    if (ITranspositionAccessor.USE_PRINCIPAL_VARIATION_TRANSPOSITIONS) {
						pc.update(currPly, trans.getPv());
					} else {
						pc.set(currPly, trans.getBestMove());
					}
					if (EubosEngineMain.ENABLE_UCI_INFO_SENDING && atRootNode() && sr != null) {
						sm.setPrincipalVariationData(0, pc.toPvList(0), (short)hashScore);
						sr.reportPrincipalVariation(sm);
					}
				    if (SearchDebugAgent.DEBUG_ENABLED) sda.printCutOffWithScore(hashScore);
					return hashScore;
				}
			}
			// Transposition still useful to seed the move list
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsSeedMoveList(pos.getHash(), trans);
			prevBestMove = trans.getBestMove();
		}
		
		MoveList ml = new MoveList((PositionManager) pm, prevBestMove, killers.getMoves(currPly), moveListOrdering, false, needToEscapeCheck);
		Iterator<Integer> move_iter = ml.iterator();
		if (!move_iter.hasNext()) {
			return sg.scoreMate(currPly);
		}
		
		int currMove = move_iter.next();
		pc.initialise(currPly, currMove);
		
		int moveNumber = 1;
		while (!isTerminated()) {
			if (atRootNode()) {
				sm.setCurrentMove(Move.toGenericMove(currMove), moveNumber);
			}
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
			// Apply move and score
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printPerformMove(currMove);
			if (SearchDebugAgent.DEBUG_ENABLED) sda.nextPly();
			currPly++;
			pm.performMove(currMove);
			int positionScore = -search(-beta, -alpha, depth-1);
			pm.unperformMove();
			currPly--;
			if (SearchDebugAgent.DEBUG_ENABLED) sda.prevPly();
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printUndoMove(currMove, positionScore);
			
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
			
			if (isTerminated()) {
				// don't update PV if out of time for search, instead return last fully searched PV.
				return 0;
			}
			
			// Handle score backed up to this node
			if (positionScore > alpha) {					
				if (positionScore >= beta) {
					killers.addMove(currPly, currMove);
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(positionScore);
					trans = updateTranspositionTable(trans, (byte) depth, currMove, (short) beta, Score.lowerBound);
					reportPv((short) beta);
					return beta;
				}
				
				alpha = positionScore;
				plyScore = positionScore;
				trans = updateTranspositionTable(trans, (byte) depth, currMove, (short) alpha, Score.upperBound);
				pc.update(currPly, currMove);
				reportPv((short) alpha);
				
			} else if (positionScore > plyScore) {
				if (atRootNode() && plyScore == Score.PROVISIONAL_ALPHA) {
					pc.update(currPly, currMove);
					reportPv((short) alpha);
				}
				plyScore = positionScore;
				trans = updateTranspositionTable(trans, (byte) depth, currMove, (short) plyScore, Score.upperBound);
			}
			
			// Break-out when out of moves
			if (move_iter.hasNext()) {
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printNormalSearch(alpha, beta);
				currMove = move_iter.next();
				moveNumber += 1;
			} else {
				break;
			}
		}

		if (!isTerminated() && trans != null && (alpha > alphaOriginal && alpha < beta)) {
			if (trans.checkUpdateToExact((byte) depth)) {
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printExactTrans(pos.getHash(), trans);			
			}
		}
		return alpha;
	}
	
	private int extendedSearch(int alpha, int beta, boolean needToEscapeCheck)  {
		if (SearchDebugAgent.DEBUG_ENABLED) sda.printExtSearch(alpha, beta);
		if (currPly > extendedSearchDeepestPly) {
			extendedSearchDeepestPly = currPly;
		}
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
		
		// Stand Pat in extended search
		short plyScore = (short) pe.evaluatePosition();	
		if (currPly >= extendedSearchLimitInPly - 1)
			return plyScore;
		if (plyScore >= beta) {
			// There is no move to put in the killer table when we stand Pat
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(plyScore);
			return beta;
		}
		
		int prevBestMove = Move.NULL_MOVE;
		ITransposition trans = tt.getTransposition();
		if (trans != null) {
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsSeedMoveList(pos.getHash(), trans);
			prevBestMove = trans.getBestMove();
		}
		// Don't use Killer moves as we don't search quiet moves in the extended search
		MoveList ml = new MoveList((PositionManager) pm, prevBestMove, null, moveListOrdering, true, pos.isKingInCheck());
		Iterator<Integer> move_iter = ml.getExtendedIterator();
		if (SearchDebugAgent.DEBUG_ENABLED) sda.printExtendedSearchMoveList(ml);
		if (ENABLE_MATE_CHECK_IN_EXTENDED_SEARCH) {
			if (ml.isMateOccurred()) {
	        	// Ideally we need just one normal move to determine that it isn't mate to
	        	// use stand PAT - this could be optimised, at the moment it is too heavy!
	        	MoveList new_ml = new MoveList((PositionManager) pm, 0); // don't bother to sort the list
	    		if (new_ml.isMateOccurred()) {
	    			short mateScore = sg.scoreMate(currPly);
	    			sda.printMateFound(mateScore);
	        		return mateScore;
	    		}
        	}    		
        } // else we will detect there are no moves and assume there are normal moves and stand Pat
		if (!move_iter.hasNext()) {
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printExtSearchNoMoves(plyScore);
			return plyScore;
		}
		
		if (plyScore > alpha) {
			alpha = plyScore;
			//trans = updateTranspositionTable(trans, (byte) 0, currMove, (short) alpha, Score.upperBound);
		}

		int currMove = move_iter.next();
		pc.initialise(currPly, currMove);
		while(true) {		
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
			// Apply capture and score
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printPerformMove(currMove);			
			if (SearchDebugAgent.DEBUG_ENABLED) sda.nextPly();
			currPly++;
			pm.performMove(currMove);
			short positionScore = (short) -extendedSearch(-beta, -alpha, pos.isKingInCheck());
			pm.unperformMove();
			currPly--;
			if (SearchDebugAgent.DEBUG_ENABLED) sda.prevPly();
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printUndoMove(currMove, positionScore);
			
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
			
			if (positionScore > alpha) {
				if (positionScore >= beta) {
					//killers.addMove(currPly, currMove);
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(positionScore);
					trans = updateTranspositionTable(trans, (byte) 0, currMove, (short) beta, Score.lowerBound);
					return beta;
				}
				alpha = positionScore;
				plyScore = positionScore;
				pc.update(currPly, currMove);
				trans = updateTranspositionTable(trans, (byte) 0, currMove, (short) alpha, Score.upperBound);
			} else if (positionScore > plyScore) {
				plyScore = positionScore;
				trans = updateTranspositionTable(trans, (byte) 0, currMove, (short) plyScore, Score.upperBound);
			}
			
			if (move_iter.hasNext()) {
				currMove = move_iter.next();
			} else {
				break;
			}
		}
		return alpha;
	}
	
	private ITransposition updateTranspositionTable(ITransposition trans, byte depth, int currMove, short plyScore, byte plyBound) {
		// Modify mate score (which is expressed in distance from the root node, in ply) to
		// the distance from leaf node (which is what needs to be stored in the hash table).
		short scoreFromDownTree = plyScore;
		if (Score.isMate(plyScore)) {
			scoreFromDownTree = (short) ((plyScore < 0) ? plyScore - currPly : plyScore + currPly);
		}
		if (ITranspositionAccessor.USE_PRINCIPAL_VARIATION_TRANSPOSITIONS) {
			trans = tt.setTransposition(trans, depth, scoreFromDownTree, plyBound, currMove, pc.toPvList(currPly));
		} else {
			trans = tt.setTransposition(trans, depth, scoreFromDownTree, plyBound, currMove);
		}
		return trans;
	}
	
	private int handleRefutationOrTerminalNodeFromHash(ITransposition trans)  {
		int trans_move;
		int theScore = 0;
		short trans_score;
		synchronized (trans) {
			trans_move = trans.getBestMove();
			trans_score = trans.getScore();
		}
		// Check score for hashed position causing a search cut-off is still valid (i.e. best move doesn't lead to a draw)
		// If hashed score is a draw score, check it is still a draw, if not, search position
		boolean isThreefold = checkForRepetitionDueToPositionInSearchTree(trans_move);
		if (isThreefold || (!isThreefold && (trans_score == 0))) {
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsSeedMoveList(pos.getHash(), trans);
		} else {
			short adjustedScoreForThisPositionInTree = trans_score;
			if (Score.isMate(trans_score)) {
				// The score stored in the hash table encodes the distance to the mate from the hashed position,
				// not the root node, so adjust for the position in search tree.
				adjustedScoreForThisPositionInTree = (short) ((trans_score < 0 ) ? trans_score+currPly : trans_score-currPly);
			}
			theScore = adjustedScoreForThisPositionInTree;
		}
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sm.incrementNodesSearched();
		return theScore;
	}

	private boolean checkForRepetitionDueToPositionInSearchTree(int move)  {
		boolean retVal = false;
		if (move != Move.NULL_MOVE) {
			pm.performMove(move);
			if (SearchDebugAgent.DEBUG_ENABLED) sda.nextPly();
			if (pos.isThreefoldRepetitionPossible()) {
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printRepeatedPositionHash(pos.getHash(), pos.getFen());
				retVal = true;
			}
			pm.unperformMove();
			if (SearchDebugAgent.DEBUG_ENABLED) sda.prevPly();
		}
		return retVal;
	}

	private void reportPv(short positionScore) {
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING && atRootNode() && sr != null) {
			sm.setPrincipalVariationData(extendedSearchDeepestPly, pc.toPvList(0), positionScore);
			sr.reportPrincipalVariation(sm);
			extendedSearchDeepestPly = 0;
		}
	}
}
