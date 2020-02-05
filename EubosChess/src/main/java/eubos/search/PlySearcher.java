package eubos.search;

import java.util.List;
import java.util.PrimitiveIterator;

import com.fluxchess.jcpi.models.GenericMove;

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
	
	private List<GenericMove> lastPc;
	private byte dynamicSearchLevelInPly;
	private ITranspositionAccessor tt;
	private PrincipalContinuationUpdateHelper pcUpdater;
	
	byte currPly = 0;
	byte currDepthSearchedInPly = 0;
	private byte originalSearchDepthRequiredInPly = 0;
	private byte extendedSearchDeepestPly = 0;
	
	public PlySearcher(
			ITranspositionAccessor hashMap,
			ScoreTracker st,
			PrincipalContinuation pc,
			SearchMetrics sm,
			SearchMetricsReporter sr,
			byte searchDepthPly,
			IChangePosition pm,
			IPositionAccessors pos,
			List<GenericMove> lastPc,
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
		
		this.st = st;
		tt = hashMap;
		sg = new MateScoreGenerator(pos, pe.getSearchContext());
		pcUpdater = new PrincipalContinuationUpdateHelper(pos.getOnMove(), pc, sm, sr);
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
			pc.clearTreeBeyondPly(currPly);
			if (doScoreBackup(theScore)) {
				updatePrincipalContinuation(eval.trans.getBestMoveAsInt(), theScore.getScore(), true);
			}
			sm.incrementNodesSearched();
			break;
		case sufficientRefutation:
			theScore = new Score(eval.trans.getScore(), (pos.onMoveIsWhite()) ? ScoreType.lowerBound : ScoreType.upperBound);
			pc.clearTreeBeyondPly(currPly);
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
			transDepthRequiredForTerminalNode = (byte)Math.max(MiniMaxMoveGenerator.SEARCH_PLY_MULTIPLIER, originalSearchDepthRequiredInPly);
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
			trans = tt.setTransposition(sm, currPly, trans, getTransDepth(), theScore.getScore(), theScore.getType(), ml, Move.NULL_MOVE);
        } else {
    		PrimitiveIterator.OfInt move_iter = ml.getIterator(isInExtendedSearch());
    		if (isSearchRequired(ml, move_iter)) {
    			theScore = actuallySearchMoves(ml, move_iter, trans);
    		} else {
    			// It is effectively a terminal node in extended search, so update the trans with null best move
    			// and return the position score back down the tree. We always back-up, because it is terminal,
    			// and we need to overwrite any alpha/beta provisional score that was brought down.
    			theScore = st.getBackedUpScoreAtPly(currPly);
    			trans = tt.setTransposition(sm, currPly, trans, (byte)0, theScore.getScore(), theScore.getType(), ml, Move.NULL_MOVE);
    		}
        }
        return theScore;
    }

	private Score actuallySearchMoves(MoveList ml, PrimitiveIterator.OfInt move_iter, Transposition trans) throws InvalidPieceException {
		if (!move_iter.hasNext())
			return new Score();
		
		boolean everBackedUp = false;
		boolean backedUpScoreWasExact = false;
		boolean refutationFound = false;
		ScoreType plyBound = (pos.onMoveIsWhite()) ? ScoreType.lowerBound : ScoreType.upperBound;
		Score plyScore = new Score((plyBound == ScoreType.lowerBound) ? Short.MIN_VALUE : Short.MAX_VALUE, plyBound);
		int currMove = move_iter.nextInt();
		
		pc.update(currPly, currMove);
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
                    trans = tt.setTransposition(sm, currPly, trans, getTransDepth(), positionScore.getScore(), plyBound, ml, currMove);
                    updatePrincipalContinuation(currMove, positionScore.getScore(), false);
	            } else {
	                // Always clear the principal continuation when we didn't back up the score
	                pc.clearRowsBeyondPly(currPly);
	                // Update the position hash if the move is better than that previously stored at this position
	                if (shouldUpdatePositionBoundScoreAndBestMove(plyBound, plyScore.getScore(), positionScore.getScore())) {
	                    plyScore = positionScore;
	                    trans = tt.setTransposition(sm, currPly, trans, getTransDepth(), plyScore.getScore(), plyBound, ml, currMove);
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
		    	if (trans.getDepthSearchedInPly() == getTransDepth()) {
		    		// however we need to be careful that the depth is appropriate, we don't set exact for wrong depth...
		    		trans.setScoreType(ScoreType.exact);
		    		
			        // found to be needed due to score discrepancies caused by refutations coming out of extedned search...
			        trans.setBestMove(pc.getBestMoveAsInt(currPly));
			        trans.setScore(plyScore.getScore());
			        
			        SearchDebugAgent.printExactTrans(currPly, pos.getHash());
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

	private boolean isSearchRequired(MoveList ml,
			PrimitiveIterator.OfInt move_iter) throws InvalidPieceException {
		boolean searchIsNeeded = true;
		if (isInExtendedSearch() && !move_iter.hasNext()) {
			// Evaluate material to deduce score, this rules out optimistic appraisal, don't use normal move list.
			st.setBackedUpScoreAtPly(currPly, new Score(pe.evaluatePosition(), ScoreType.exact));
	    	searchIsNeeded = false;
	    }
		return searchIsNeeded;
	}
	
	private void updatePrincipalContinuation(
			int currMove, short positionScore, boolean isATerminalNodeHashHit)
			throws InvalidPieceException {
		pc.update(currPly, currMove);
		if (atRootNode() && !isATerminalNodeHashHit) {
			// If backed up to the root node, report the principal continuation
			tt.createPrincipalContinuation(pc, originalSearchDepthRequiredInPly, pm);
			pcUpdater.report(positionScore, extendedSearchDeepestPly);
		}
	}
	
	private void handleEarlyTermination() {
		if (atRootNode() && isTerminated()) {
			TranspositionEvaluation eval = tt.getTransposition(currPly, dynamicSearchLevelInPly);
			if (eval != null && eval.trans != null && eval.trans.getBestMove() != null) {
				pc.update(0, eval.trans.getBestMoveAsInt());
			}
			// Set best move to the previous iteration search result
			else if (lastPc != null) {
				GenericMove lastMove = lastPc.get(0);
				int type = (lastMove.promotion != null) ?  Move.TYPE_PROMOTION : Move.TYPE_REGULAR;
				pc.update(0, Move.toMove(lastMove, pos.getTheBoard(), type));
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
			pc.clearRowsBeyondPly(currPly);
			reportMove(Move.toGenericMove(currMove));
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
	
	private void reportMove(GenericMove currMove) {
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
			if (currPly > (originalSearchDepthRequiredInPly*MiniMaxMoveGenerator.SEARCH_PLY_MULTIPLIER)-2) {
				// -2 always leaves room for one more move for each side without overflowing array...
				limitReached = true;
			}
		}
		return limitReached;
	}
}
