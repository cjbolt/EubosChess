package eubos.search;

import java.util.List;
import java.util.PrimitiveIterator;

import eubos.board.InvalidPieceException;
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
import eubos.search.transposition.Transposition;
import eubos.search.transposition.TranspositionEvaluation;
import eubos.search.Score.ScoreType;

public class PlySearcher {

	private IChangePosition pm;
	IPositionAccessors pos;
	
	ScoreTracker st;
	private IEvaluate pe;
	private IScoreMate sg;
	PrincipalContinuation pc;
	private SearchMetrics sm;
	private SearchMetricsReporter sr;
	
	private boolean terminate = false;
	
	private List<Integer> lastPc;
	private byte dynamicSearchLevelInPly;
	private ITranspositionAccessor tt;
	private PrincipalContinuationUpdateHelper pcUpdater;
	
	byte currPly = 0;
	byte currDepthSearchedInPly = 0;
	private byte originalSearchDepthRequiredInPly = 0;
	private byte extendedSearchDeepestPly = 0;
	private byte extendedSearchLimitInPly = 0;
	
	public PlySearcher(
			ITranspositionAccessor hashMap,
			ScoreTracker st,
			PrincipalContinuation pc,
			SearchMetrics sm,
			SearchMetricsReporter sr,
			byte searchDepthPly,
			IChangePosition pm,
			IPositionAccessors pos,
			List<Integer> lastPc,
			IEvaluate pe) {
		currPly = 0;
		currDepthSearchedInPly = 0;
		
		this.pc = pc;
		this.sm = sm;
		this.sr = sr;
		this.pm = pm;
		this.pos = pos;
		this.pe = pe;
		this.lastPc = lastPc;
		dynamicSearchLevelInPly = searchDepthPly;
		originalSearchDepthRequiredInPly = searchDepthPly;
		extendedSearchLimitInPly = setExtSearchDepth();
		
		this.st = st;
		tt = hashMap;
		sg = new MateScoreGenerator(pos, pe.getSearchContext());
		pcUpdater = new PrincipalContinuationUpdateHelper(pos.getOnMove(), pc, sm, sr);
	}
	
	private byte setExtSearchDepth() {
		int variableDepthPly = originalSearchDepthRequiredInPly * 4;
		byte extDepthLimitPly = (byte)Math.min(MiniMaxMoveGenerator.EXTENDED_SEARCH_PLY_LIMIT, variableDepthPly);
		extDepthLimitPly += originalSearchDepthRequiredInPly;
		return extDepthLimitPly;
	}
	private boolean atRootNode() { return currPly == 0; }
	
	public synchronized void terminateFindMove() { terminate = true; }
	private synchronized boolean isTerminated() { return terminate; }	
	
	public Score searchPly() throws InvalidPieceException {
		Score theScore = null;
		if (isTerminated())
			return new Score();
		
		MoveList ml = null;
		byte depthRequiredForTerminalNode = initialiseSearchAtPly();
		
		TranspositionEvaluation eval = tt.getTransposition(currPly, depthRequiredForTerminalNode);		
		switch (eval.status) {
		case sufficientTerminalNode:
			theScore = new Score(eval.trans.getScore(), eval.trans.getScoreType());
			pc.update(currPly, eval.trans.getPv());
			sm.incrementNodesSearched();
			break;
		case sufficientRefutation:
			theScore = new Score(eval.trans.getScore(), (pos.onMoveIsWhite()) ? ScoreType.lowerBound : ScoreType.upperBound);
			pc.update(currPly, eval.trans.getPv());
			sm.incrementNodesSearched();
			break;
		case sufficientSeedMoveList:
			SearchDebugAgent.printHashIsSeedMoveList(currPly, eval.trans.getBestMove(), pos.getHash());
			ml = eval.trans.getMoveList();
			// intentional drop through
		case insufficientNoData:
			if (ml == null)
				ml = getMoveList();
			theScore = searchMoves( ml, eval.trans);
			break;	
		default:
			break;
		}
		handleEarlyTermination();
		clearUpSearchAtPly();
		
		return theScore;
	}
	
	private byte initialiseSearchAtPly() {
		byte transDepthRequiredForTerminalNode = 0;
		if (currPly >= originalSearchDepthRequiredInPly) {
			dynamicSearchLevelInPly++;
		}
		if (this.isInExtendedSearch()) {
			transDepthRequiredForTerminalNode = extendedSearchLimitInPly;
		} else {
			transDepthRequiredForTerminalNode = (byte)(originalSearchDepthRequiredInPly - currPly);
		}
		st.setProvisionalScoreAtPly(currPly);
		SearchDebugAgent.printStartPlyInfo(currPly, transDepthRequiredForTerminalNode, st.getBackedUpScoreAtPly(currPly).getScore(), pos);
		return transDepthRequiredForTerminalNode;
	}
	
	private void clearUpSearchAtPly() {
		if (dynamicSearchLevelInPly > originalSearchDepthRequiredInPly) {
			dynamicSearchLevelInPly--;
		}
	}
	
	private Score searchMoves(MoveList ml, Transposition trans) throws InvalidPieceException {
		Score theScore = null;
        if (ml.isMateOccurred()) {
            theScore = new Score(sg.scoreMate(currPly), ScoreType.exact);
            st.setBackedUpScoreAtPly(currPly, theScore);
            // We will now de-recurse, so should make sure the depth searched is correct
            setDepthSearchedInPly();
			trans = tt.setTransposition(sm, currPly, trans, getTransDepth(), theScore.getScore(), theScore.getType(), ml, Move.NULL_MOVE, pc.toPvList(currPly));
        } else {
    		PrimitiveIterator.OfInt move_iter = ml.getIterator(isInExtendedSearch());
    		if (move_iter.hasNext()) {
    			theScore = actuallySearchMoves(ml, move_iter, trans);
    		} else {
    			// It is effectively a terminal node in extended search, so update the trans with null best move
    			// and return an exact position score back down the tree.			
    			theScore = applyBestNormalMoveAndScore(ml);
    			SearchDebugAgent.printExtSearchNoMoves(currPly, theScore);
    			trans = tt.setTransposition(sm, currPly, trans, (byte)0, theScore.getScore(), theScore.getType(), ml, Move.NULL_MOVE, pc.toPvList(currPly));
    		}
        }
        return theScore;
    }

	private Score actuallySearchMoves(MoveList ml, PrimitiveIterator.OfInt move_iter, Transposition trans) throws InvalidPieceException {
		boolean everBackedUp = false;
		boolean backedUpScoreWasExact = false;
		boolean refutationFound = false;
		ScoreType plyBound = (pos.onMoveIsWhite()) ? ScoreType.lowerBound : ScoreType.upperBound;
		Score plyScore = new Score((plyBound == ScoreType.lowerBound) ? Short.MIN_VALUE : Short.MAX_VALUE, plyBound);
		
		int currMove = move_iter.nextInt();
		pc.initialise(currPly, currMove);
		
		if (isInExtendedSearch() && !move_iter.hasNext()) {
			/*
			 * The idea is that if we are in an extended search, if there are normal moves available 
			 * and only a single "forced" capture, we shouldn't necessarily be forced into making that capture.
			 * The capture needs to improve the position score to get searched, otherwise it can be treated as terminal.
			 * Note: This is only a problem for the PV search, all others will bring down alpha/beta score and won't 
			 * back up if worse.
			 */
			Score provScore = st.getBackedUpScoreAtPly(currPly);
			boolean isProvisional = (provScore.getScore() == Short.MIN_VALUE || provScore.getScore() == Short.MAX_VALUE);
			if (isProvisional && ml.hasMultipleRegularMoves()) {
				plyScore = applyBestNormalMoveAndScore(ml);
				plyScore.type = plyBound;
				st.setBackedUpScoreAtPly(currPly, plyScore);
			}
		}
		
		while(!isTerminated()) {
		    rootNodeInitAndReportingActions(currMove);

	        Score positionScore = applyMoveAndScore(currMove);
	        if (!isTerminated()) {
	        	// Rationale: this is when a score was backed up - at this instant update the depth searched
	        	setDepthSearchedInPly();
	        	if (doScoreBackup(positionScore)) {
	                everBackedUp = true;
	                backedUpScoreWasExact = (positionScore.getType()==ScoreType.exact);
                    plyScore = positionScore;
                    updatePrincipalContinuation(currMove, positionScore.getScore());
                    trans = tt.setTransposition(sm, currPly, trans, getTransDepth(), positionScore.getScore(), plyBound, ml, currMove, pc.toPvList(currPly));
	            } else {
                    List<Integer> last_pv = pc.toPvList(currPly+1);
                    last_pv.add(0, currMove);
	                // Always clear the principal continuation when we didn't back up the score
	                pc.clearContinuationsBeyondPly(currPly);
	                // Update the position hash if the move is better than that previously stored at this position
	                if (shouldUpdatePositionBoundScoreAndBestMove(plyBound, plyScore.getScore(), positionScore.getScore())) {
	                    plyScore = positionScore;
	                    trans = tt.setTransposition(sm, currPly, trans, getTransDepth(), plyScore.getScore(), plyBound, ml, currMove, last_pv/*(trans != null) ? trans.getPv() : null*/);
	                }
	            }
	        
	            if (st.isAlphaBetaCutOff(currPly, positionScore)) {
	                refutationFound = true;
	                plyScore = new Score(plyScore.getScore(), plyBound);
	                SearchDebugAgent.printRefutationFound(currPly);
	                break;    
	            }
	        }
			if (move_iter.hasNext()) {
				currMove = move_iter.nextInt();
			} else {
				break;
			}
		}
		if (!isTerminated() && isInNormalSearch()) {
		    if (everBackedUp && backedUpScoreWasExact && !refutationFound && trans != null) {
		    	// This is the only way a hash and score can be exact.
		    	if (trans.getDepthSearchedInPly() <= getTransDepth()) {
		    		// however we need to be careful that the depth is appropriate, we don't set exact for wrong depth...
		    		trans.setScoreType(ScoreType.exact);
		    		
			        // found to be needed due to score discrepancies caused by refutations coming out of extended search...
			        trans.setBestMove(pc.getBestMove(currPly));
			        trans.setPv(pc.toPvList(currPly));
			        trans.setScore(plyScore.getScore());
			        
			        SearchDebugAgent.printExactTrans(currPly, pos.getHash(), trans);
		    	}
		    	plyScore.setExact();
		    }
		}
		return plyScore;
	}

	private void setDepthSearchedInPly() {
		if(isInNormalSearch()) {
			currDepthSearchedInPly = (byte)(originalSearchDepthRequiredInPly - currPly);
		} else {
			currDepthSearchedInPly = 1; // it is always 1 in extended search?
		}
	}
	
	private void updatePrincipalContinuation(int currMove, short positionScore)
			throws InvalidPieceException {
		pc.update(currPly, currMove);
		if (atRootNode()) {
			pcUpdater.report(positionScore, extendedSearchDeepestPly);
		}
	}
	
	private void handleEarlyTermination() {
		if (atRootNode() && isTerminated()) {
			TranspositionEvaluation eval = tt.getTransposition(currPly, dynamicSearchLevelInPly);
			if (eval != null && eval.trans != null && eval.trans.getBestMove() != Move.NULL_MOVE) {
				pc.update(0, eval.trans.getBestMove());
			}
			// Set best move to the previous iteration search result
			else if (lastPc != null) {
				pc.update(0, lastPc.get(0));
			} else {
				// Just return pc
			}
		}
	}
	
	private boolean isInExtendedSearch() {
		return dynamicSearchLevelInPly > originalSearchDepthRequiredInPly;
	}
	
	private boolean isInNormalSearch() {
		assert dynamicSearchLevelInPly >= originalSearchDepthRequiredInPly;
		return dynamicSearchLevelInPly == originalSearchDepthRequiredInPly;
	}

	private byte getTransDepth() {
		/* By design, extended searches always use depth zero; therefore ensuring partially 
           searched transpositions can only be used for seeding move lists */
		return isInNormalSearch() ? currDepthSearchedInPly: 0;
	}
	
	private void rootNodeInitAndReportingActions(int currMove) {
		if (atRootNode()) {
			// When we start to search a move at the root node, clear the principal continuation data
			pc.clearContinuationsBeyondPly(currPly);
			reportMove(currMove);
		}
	}

	private boolean shouldUpdatePositionBoundScoreAndBestMove(
			ScoreType plyBound, short plyScore, short positionScore) {
		boolean doUpdate = false;
		if (plyBound == ScoreType.lowerBound) {
			if (positionScore > plyScore && positionScore != Short.MAX_VALUE)
				doUpdate = true;
		} else {
			if (positionScore < plyScore && positionScore != Short.MIN_VALUE)
				doUpdate = true;
		}
		return doUpdate;
	}

	private boolean doScoreBackup(Score positionScore) {
		boolean backupRequired = false;
		if (st.isBackUpRequired(currPly, positionScore)) {
			st.setBackedUpScoreAtPly(currPly, positionScore);
			backupRequired = true;
		}
		return backupRequired;
	}
	
	private void reportMove(int currMove) {
		sm.setCurrentMove(currMove);
		sm.incrementCurrentMoveNumber();
		sr.reportCurrentMove();
	}
		
	private MoveList getMoveList() throws InvalidPieceException {
		MoveList ml = null;
		if ((lastPc != null) && (lastPc.size() > currPly)) {
			// Seeded move list is possible
			ml = new MoveList((PositionManager) pm, lastPc.get(currPly));
		} else {
			ml = new MoveList((PositionManager) pm);
		}
		return ml;
	}
	
	private Score applyMoveAndScore(int currMove) throws InvalidPieceException {
		SearchDebugAgent.printPerformMove(currPly, currMove);
		pm.performMove(currMove);
		currPly++;
		Score positionScore = assessNewPosition();
		pm.unperformMove();
		currPly--;
		SearchDebugAgent.printUndoMove(currPly, currMove);
		
		sm.incrementNodesSearched();
		return positionScore;
	}
	
	private Score applyBestNormalMoveAndScore(MoveList ml) throws InvalidPieceException {
		int currMove = ml.getBestMove();
		assert currMove != Move.NULL_MOVE;
		SearchDebugAgent.printPerformMove(currPly, currMove);
		pm.performMove(currMove);
		currPly++;
		// exact because it is a terminal node
		Score positionScore = new Score(pe.evaluatePosition(), ScoreType.exact);
		
		pm.unperformMove();
		currPly--;
		SearchDebugAgent.printUndoMove(currPly, currMove);
		
		pc.update(currPly, currMove);
		sm.incrementNodesSearched();
		return positionScore;
	}
	
	private Score assessNewPosition() throws InvalidPieceException {
		Score positionScore = null;
		if ( isTerminalNode() ) {
			positionScore = new Score(pe.evaluatePosition(), ScoreType.exact);
			currDepthSearchedInPly = 1; // We applied a move in order to generate this score
		} else {
			positionScore = searchPly();
		}
		return positionScore;
	}
	
	private boolean isTerminalNode() {
		boolean terminalNode = false;
		if (pe.isThreeFoldRepetition(pos.getHash())) {
			SearchDebugAgent.printRepeatedPositionHash(currPly, pos.getHash());
			terminalNode = true;
		} else if (currPly == originalSearchDepthRequiredInPly) {
			if (pe.isQuiescent()) {
				terminalNode = true;
			}
		} else if (currPly > originalSearchDepthRequiredInPly) {
			if (pe.isQuiescent() || isExtendedSearchLimitReached()) {
				if (currPly > extendedSearchDeepestPly) {
					extendedSearchDeepestPly = currPly;
				}
				terminalNode = true;
			}
		} else {
			// is not a terminal node
		}
		return terminalNode;
	}

	private boolean isExtendedSearchLimitReached() {
		boolean limitReached = false;
		if (currPly%2 == 0) {
			// means that initial onMove side is back on move
			if (currPly > extendedSearchLimitInPly-2) {
				// -2 always leaves room for one more move for each side without overflowing array...
				limitReached = true;
			}
		}
		return limitReached;
	}
}
