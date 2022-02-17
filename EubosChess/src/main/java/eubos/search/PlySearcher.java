package eubos.search;

import java.util.List;

import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.MoveListIterator;
import eubos.score.IEvaluate;
import eubos.search.transposition.ITranspositionAccessor;
import eubos.search.transposition.ITransposition;

public class PlySearcher {
	
	private static final boolean TUNE_LAZY_EVAL = false;
	private static int count;  // Only used for tuning lazy evaluation
	
	private IChangePosition pm;
	private IPositionAccessors pos;
	private IEvaluate pe;
	
	private PrincipalContinuation pc;
	private SearchMetrics sm;
	private SearchDebugAgent sda;
	
	private volatile boolean terminate = false;
	
	private List<Integer> lastPc;
	private ITranspositionAccessor tt;
	private SearchMetricsReporter sr;
	private KillerList killers;
	
	private byte currPly = 0;
	private byte originalSearchDepthRequiredInPly = 0;
	private byte extendedSearchDeepestPly = 0;
	
	private MoveList ml;
	
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
		
		this.pc = pc;
		this.sm = sm;
		this.pm = pm;
		this.pos = pos;
		this.pe = pe;
		this.sr = sr;
		this.lastPc = pc.toPvList(0);
		this.sda = sda;
		originalSearchDepthRequiredInPly = searchDepthPly;
		
		tt = hashMap;
		this.killers = killers;
		this.ml = ml;
	}

	private boolean atRootNode() { return currPly == 0; }
	
	public synchronized void terminateFindMove() { 
		EubosEngineMain.logger.info("Terminating PlySearcher");
		terminate = true;
	}
	private synchronized boolean isTerminated() { return terminate; }	
	
	public int searchPly()  {
		currPly = 0;
		extendedSearchDeepestPly = 0;
		return (short) search(Score.PROVISIONAL_ALPHA, Score.PROVISIONAL_BETA, originalSearchDepthRequiredInPly);
	}
	
	public int searchPly(short lastScore)  {
		currPly = 0;
		extendedSearchDeepestPly = 0;	
		short score = 0;
		
		// Adjust the aspiration window, according to the last score, if searching to sufficient depth
		int alpha = Score.PROVISIONAL_ALPHA;
		int beta = Score.PROVISIONAL_BETA;
		if (originalSearchDepthRequiredInPly >= 5) {
			int windowSize = Score.isMate(lastScore) ? 1 : Piece.MATERIAL_VALUE_PAWN/4;
			alpha = lastScore - windowSize;
			beta = lastScore + windowSize;
		}
		
		while (!isTerminated()) {
			boolean windowFailed = false;

			score = (short) search(alpha, beta, originalSearchDepthRequiredInPly);
	
			if (Score.isProvisional(score)) {
				EubosEngineMain.logger.info("Aspiration Window failed - no score, illegal position");
	            break;
        	} else if (isTerminated() && score ==0) {
        		// Early termination, didn't back up a score at the last ply			
        	} else if (score <= alpha) {
        		// Failed low, adjust window
        		windowFailed = true;
	            alpha = Score.PROVISIONAL_ALPHA;
	        } else if (score >= beta) {
	        	// Failed high, adjust window
	        	windowFailed = true;
	            beta = Score.PROVISIONAL_BETA;
	        } else {
	        	// Exact score in window returned
	            break;
	        }
			if (windowFailed) {
				EubosEngineMain.logger.info(String.format("Aspiration Window failed score=%d alpha=%d beta=%d depth=%d",
        				score, alpha, beta, originalSearchDepthRequiredInPly));
				sr.resetAfterWindowingFail();
				windowFailed = false;
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
		if (currPly >= EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
			return pe.getFullEvaluation();
		}
		
		if (SearchDebugAgent.DEBUG_ENABLED) {
			sda.printStartPlyInfo(pos, originalSearchDepthRequiredInPly);
			sda.printNormalSearch(alpha, beta);
		}
		
		// Extend search for in-check scenarios, treated outside of quiescence search
		boolean needToEscapeCheck = pos.isKingInCheck();
		if (needToEscapeCheck) {
			++depth;
		}
		
		if (depth == 0) {
			return extendedSearch(alpha, beta, needToEscapeCheck);
		}
		
		ITransposition trans = tt.getTransposition();
		if (trans != null) {
			boolean override_trans_move = false;
			
			if (depth <= trans.getDepthSearchedInPly()) {
				int type = trans.getType();
				boolean isCutOff = false;
				override_trans_move = checkForRepetitionDueToPositionInSearchTree(trans.getBestMove(pos.getTheBoard()));
				
				if (!override_trans_move || (override_trans_move && type != Score.exact)) {
					boolean check_for_refutation = false;
					
					// If the hashed data is now drawing, due to the position in the search tree, score it accordingly, but still check
					// if it is good enough for a refutation.
					int hashScore = !override_trans_move ? convertMateScoreForPositionInSearchTree(trans.getScore()) : 0;
					switch(type) {
					case Score.exact:
						if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsTerminalNode(trans, pos.getHash());
						isCutOff = true;
						break;
					case Score.upperBound:
						beta = Math.min(beta, hashScore);
						check_for_refutation = true;
						break;
					case Score.lowerBound:
						alpha = Math.max(alpha, hashScore);
						check_for_refutation = true;
						break;
					case Score.typeUnknown:
					default:
						if (EubosEngineMain.ENABLE_ASSERTS) assert false;
						break;
					}
					
					if (check_for_refutation) {
						// Determine if good enough for a refutation...
						if (alpha >= beta) {
							if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsRefutation(pos.getHash(), trans);
							killers.addMove(currPly, trans.getBestMove(pos.getTheBoard()));
							isCutOff = true;
						}
					}
					if (isCutOff) {
						// Refutation or exact score already known to require search depth, cut off the Search
					    if (ITranspositionAccessor.USE_PRINCIPAL_VARIATION_TRANSPOSITIONS) {
							pc.update(currPly, trans.getPv());
						} else {
							pc.set(currPly, trans.getBestMove(pos.getTheBoard()));
						}
					    if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) {
					    	sm.incrementNodesSearched();
							if (atRootNode()) {
								sm.setPrincipalVariationDataFromHash(0, pc.toPvList(0), (short)hashScore);
								sr.reportPrincipalVariation(sm);
							}
						}
					    if (SearchDebugAgent.DEBUG_ENABLED) sda.printCutOffWithScore(hashScore);
						return hashScore;
					}
				}
			}
			// Transposition may still be useful to seed the move list, if not drawing.
			if (!override_trans_move || (override_trans_move && prevBestMove == Move.NULL_MOVE)) {
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsSeedMoveList(pos.getHash(), trans);
				int trans_move = trans.getBestMove(pos.getTheBoard());
				if (trans_move != Move.NULL_MOVE) {
					if (atRootNode()) {
						EubosEngineMain.logger.info(
								String.format("best move set from trans=%s", Move.toString(trans_move)));
					}
					prevBestMove = trans_move;
				} else {
					EubosEngineMain.logger.severe(
							String.format("hash move is bad, falling back to prevBestMove=%s", Move.toString(prevBestMove)));
				}
			}
		}
		
		MoveListIterator move_iter = ml.createForPly(prevBestMove, killers.getMoves(currPly), false, needToEscapeCheck, currPly);
		if (!move_iter.hasNext()) {
			// No moves at this point means either a stalemate or checkmate has occurred
			return needToEscapeCheck ? Score.getMateScore(currPly) : 0;
		}
		
		int currMove = move_iter.nextInt();
		int bestMove = currMove;
		pc.initialise(currPly, currMove);
		
		int moveNumber = 1;
		while (!isTerminated()) {
			if (EubosEngineMain.ENABLE_UCI_MOVE_NUMBER && atRootNode()) {
				sm.setCurrentMove(Move.toGenericMove(currMove), moveNumber);
				if (originalSearchDepthRequiredInPly > 7)
					sr.reportCurrentMove();
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
				alpha = plyScore = positionScore;
				bestMove = currMove;
				
				if (alpha >= beta) {
					plyScore = beta; // fail hard
					killers.addMove(currPly, bestMove);
					reportPv((short) beta);
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(plyScore);
					break;
				}
				
				pc.update(currPly, bestMove);
				if (atRootNode()) trans = updateTranspositionTable(trans, (byte) depth, bestMove, (short) alpha, Score.upperBound);
				reportPv((short) alpha);
				
			} else if (positionScore > plyScore) {
				if (atRootNode() && plyScore == Score.PROVISIONAL_ALPHA) {
					pc.update(currPly, currMove);
					reportPv((short) alpha);
				}
				bestMove = currMove;
				plyScore = positionScore;
			}
			
			// Break-out when out of moves
			if (move_iter.hasNext()) {
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printNormalSearch(alpha, beta);
				currMove = move_iter.nextInt();
				moveNumber += 1;
				if (EubosEngineMain.ENABLE_ASSERTS) {
					assert currMove != Move.NULL_MOVE: "Null move found in MoveList";
					assert moveNumber <= ml.getList(currPly).size() : "MoveList is too long";
				}
			} else {
				break;
			}
		}
		
		byte plyBound = Score.typeUnknown;
		if (plyScore <= alphaOriginal) {
			// Didn't raise alpha
			plyBound = Score.upperBound;
		} else if (plyScore >= beta) {
			// A beta cut-off, alpha raise was 'too good'
			plyBound = Score.lowerBound;
		} else {
			// In exact window, searched all nodes...
			plyBound = Score.exact;
		}		
		if (!isTerminated()) {
			trans = updateTranspositionTable(trans, (byte) depth, bestMove, (short) plyScore, plyBound);
		}
		
		// fail hard, so don't return plyScore
		return alpha;
	}
	
	@SuppressWarnings("unused")
	private int extendedSearch(int alpha, int beta, boolean needToEscapeCheck)  {
		if (SearchDebugAgent.DEBUG_ENABLED) sda.printExtSearch(alpha, beta);
		if (currPly > extendedSearchDeepestPly) {
			extendedSearchDeepestPly = currPly;
		}
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) pc.clearContinuationBeyondPly(currPly);
		
		int prevBestMove = Move.NULL_MOVE;
		MoveListIterator move_iter;
		
		// Stand Pat in extended search
		short plyScore = (short) 0;
		if (EubosEngineMain.ENABLE_LAZY_EVALUATION && !pos.getTheBoard().me.isEndgame()) {
			// Phase 1 - crude evaluation
			plyScore = (short) pe.getCrudeEvaluation();
			if (plyScore-350 >= beta) {
				// There is no move to put in the killer table when we stand Pat
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(plyScore);
				// According to lazy eval, we probably can't reach beta
				if (TUNE_LAZY_EVAL) {
					if (count == 1024) {
						count = 0;
						EubosEngineMain.logger.info(
							String.format("TUNE LAZY B=%d delta=%d ", beta, plyScore-pe.getFullEvaluation()));
					} else {
						count++;
					}
				}
				return beta;
			}
			/* Note call to quiescence check is last as it could be very computationally heavy! */
			if (plyScore+350 <= alpha && pos.isQuiescent()) {
				// According to lazy eval, we probably can't increase alpha
				if (TUNE_LAZY_EVAL) {
					if (count == 1024) {
						count = 0;
						EubosEngineMain.logger.info(
							String.format("TUNE LAZY A=%d delta=%d ", alpha, plyScore-pe.getFullEvaluation()));
					} else {
						count++;
					}
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
		
		// Create MoveList, computationally very heavy in extended search
		ITransposition trans = tt.getTransposition();
		if (trans != null) {
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printHashIsSeedMoveList(pos.getHash(), trans);
			prevBestMove = trans.getBestMove(pos.getTheBoard());
		}
		move_iter = ml.createForPly(prevBestMove, needToEscapeCheck, currPly);
		if (SearchDebugAgent.DEBUG_ENABLED) sda.printExtendedSearchMoveList(ml);		
		if (!move_iter.hasNext()) {
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printExtSearchNoMoves(plyScore);
			return plyScore;
		}
		
		if (plyScore > alpha) {
			// Null move hypothesis
			alpha = plyScore;
		}

		int currMove = move_iter.nextInt();
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
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printRefutationFound(positionScore);
					return beta;
				}
				alpha = positionScore;
				pc.update(currPly, currMove);
			}
			
			if (move_iter.hasNext()) {
				currMove = move_iter.nextInt();
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
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING && atRootNode()) {
			sm.setPrincipalVariationData(extendedSearchDeepestPly, pc.toPvList(0), positionScore);
			sr.reportPrincipalVariation(sm);
			extendedSearchDeepestPly = 0;
		}
	}
}
