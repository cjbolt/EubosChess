package eubos.search;

import java.util.Iterator;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.MoveList;
import eubos.position.PositionManager;
import eubos.score.IEvaluate;
import eubos.score.IScoreMate;
import eubos.score.MateScoreGenerator;
import eubos.search.transposition.ITranspositionAccessor;
import eubos.search.transposition.Transposition;
import eubos.search.transposition.TranspositionEvaluation;
import eubos.search.transposition.Transposition.ScoreType;

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
	private byte searchDepthPly;
	private ITranspositionAccessor tt;
	private PrincipalContinuationUpdateHelper pcUpdater;
	
	byte currPly = 0;
	byte depthSearchedPly = 0;
	private byte originalDepthRequested = 0;
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
		depthSearchedPly = 0;
		
		this.pc = pc;
		this.sm = sm;
		this.sr = sr;
		this.pm = pm;
		this.pos = pos;
		this.pe = pe;
		this.lastPc = lastPc;
		this.searchDepthPly = searchDepthPly;
		originalDepthRequested = searchDepthPly;
		
		this.st = st;
		this.tt = hashMap;
		this.sg = new MateScoreGenerator(pos);
		this.pcUpdater = new PrincipalContinuationUpdateHelper(pos.getOnMove(), pc, sm, sr);
	}
	
	private boolean atRootNode() { return currPly == 0; }
	
	public synchronized void terminateFindMove() { terminate = true; }
	private synchronized boolean isTerminated() { return terminate; }	
	
	public short searchPly() throws InvalidPieceException {
		if (isTerminated())
			return 0;
		
		MoveList ml = null;
		byte depthRequiredPly = initialiseSearchAtPly();
		
		TranspositionEvaluation eval = tt.getTransposition(currPly, depthRequiredPly);
		switch (eval.status) {
		case sufficientTerminalNode:
		case sufficientRefutation:
			treatAsTerminalNode(eval.trans);
			break;
		case sufficientTerminalNodeInExtendedSearch:
			if (isInExtendedSearch()) {
				  treatAsTerminalNode(eval.trans);
				  break;
			}
			// else intentional drop through
		case sufficientSeedMoveList:
			SearchDebugAgent.printHashIsSeedMoveList(currPly, eval.trans.getBestMove(), pos.getHash());
			ml = eval.trans.getMoveList();
			// intentional drop through
		case insufficientNoData:
			if (ml == null)
				ml = getMoveList();
			searchMoves( ml, eval.trans);
			break;	
		default:
			break;
		}
		handleEarlyTermination();
		clearUpSearchAtPly();
		
		return st.getBackedUpScoreAtPly(currPly);
	}

	private void searchMoves(MoveList ml, Transposition trans) throws InvalidPieceException {
        if (ml.isMateOccurred()) {
            short mateScore = sg.scoreMate(currPly);
            st.setBackedUpScoreAtPly(currPly, mateScore);
        } else {
    		Iterator<GenericMove> move_iter = ml.getIterator(isInExtendedSearch());
    		if (isSearchRequired(ml, move_iter)) {
    			actuallySearchMoves(ml, move_iter, trans);
    		}
        }
    }

	private void actuallySearchMoves(MoveList ml, Iterator<GenericMove> move_iter, Transposition trans) throws InvalidPieceException {
		if (!move_iter.hasNext())
			return;
		
		boolean everBackedUp = false;
		boolean refutationFound = false;
		ScoreType plyBound = (pos.onMoveIsWhite()) ? ScoreType.lowerBound : ScoreType.upperBound;
		short plyScore = (plyBound == ScoreType.lowerBound) ? Short.MIN_VALUE : Short.MAX_VALUE;
		GenericMove currMove = move_iter.next();
		
		pc.update(currPly, currMove);
		while(!isTerminated()) {
		    rootNodeInitAndReportingActions(currMove);

	        short positionScore = applyMoveAndScore(currMove);
	        if (!isTerminated()) {
	        	if(isInNormalSearch()) {
	        		// Rationale; this is when a score was backed up - at this instant update the depth searched
	        		depthSearchedPly = (byte)(searchDepthPly - currPly);
	        	}
	            if (doScoreBackup(positionScore)) {
	                everBackedUp = true;
                    plyScore = positionScore;
                    trans = tt.setTransposition(sm, currPly, trans,
                                new Transposition(getTransDepth(), positionScore, plyBound, ml, currMove));
                    updatePrincipalContinuation(currMove, positionScore, false);
	            } else {
	                // Always clear the principal continuation when we didn't back up the score
	                pc.clearRowsBeyondPly(currPly);
	                // Update the position hash if the move is better than that previously stored at this position
	                if (shouldUpdatePositionBoundScoreAndBestMove(plyBound, plyScore, positionScore)) {
	                    plyScore = positionScore;
	                    trans = tt.setTransposition(sm, currPly, trans,
	                            new Transposition(getTransDepth(), plyScore, plyBound, ml, currMove));
	                }
	            }
	        
	            if (st.isAlphaBetaCutOff(currPly, positionScore)) {
	                refutationFound = true;
	                SearchDebugAgent.printRefutationFound(currPly);
	                break;    
	            }
	        }
			if (move_iter.hasNext()) {
				currMove = move_iter.next();
			} else {
				break;
			}
		}
		if (!isTerminated() && isInNormalSearch()) {
		    if (everBackedUp && !refutationFound && trans != null) {
		        trans.setScoreType(ScoreType.exact);
		    }
		}
	}

	private boolean isSearchRequired(MoveList ml,
			Iterator<GenericMove> move_iter) throws InvalidPieceException {
		boolean searchIsNeeded = true;
		if (isInExtendedSearch() && !move_iter.hasNext()) {
			// Evaluate material to deduce score, this rules out optimistic appraisal, don't use normal move list.
	    	doScoreBackup(pe.evaluatePosition());
	    	searchIsNeeded = false;
	    }
		return searchIsNeeded;
	}
	
	private void treatAsTerminalNode(Transposition trans)
			throws InvalidPieceException {
		short score = trans.getScore();
		pc.clearTreeBeyondPly(currPly);
		if (doScoreBackup(score)) {
			updatePrincipalContinuation(trans.getBestMove(), score, true);
		}
		sm.incrementNodesSearched();
	}
	
	private void updatePrincipalContinuation(
			GenericMove currMove, short positionScore, boolean treatHashHitAsTerminalNode)
			throws InvalidPieceException {
		pc.update(currPly, currMove);
		if (atRootNode() && !treatHashHitAsTerminalNode) {
			// If backed up to the root node, report the principal continuation
			tt.createPrincipalContinuation(pc, searchDepthPly, pm);
			pcUpdater.report(positionScore, extendedSearchDeepestPly);
		}
	}
	
	private void handleEarlyTermination() {
		if (atRootNode() && isTerminated()) {
			TranspositionEvaluation eval = tt.getTransposition(currPly, searchDepthPly);
			if (eval != null && eval.trans != null && eval.trans.getBestMove() != null) {
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
		return searchDepthPly > originalDepthRequested;
	}
	
	private boolean isInNormalSearch() {
		return searchDepthPly <= originalDepthRequested;
	}

	private byte getTransDepth() {
		/* By design, extended searches always use depth zero; therefore ensuring partially 
           searched transpositions can only be used for seeding move lists */
		return isInNormalSearch() ? depthSearchedPly: 0;
	}
	
	private void rootNodeInitAndReportingActions(GenericMove currMove) {
		if (atRootNode()) {
			// When we start to search a move at the root node, clear the principal continuation data
			pc.clearRowsBeyondPly(currPly);
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
	
	private byte initialiseSearchAtPly() {
		byte depthRequiredPly = 0;
		if (currPly >= originalDepthRequested) {
			searchDepthPly++;
		}
		if (this.isInExtendedSearch()) {
			depthRequiredPly = originalDepthRequested;
		} else {
			depthRequiredPly = (byte)(searchDepthPly - currPly);
		}
		st.setProvisionalScoreAtPly(currPly);
		SearchDebugAgent.printStartPlyInfo(currPly, depthRequiredPly, st.getBackedUpScoreAtPly(currPly), pos);
		return depthRequiredPly;
	}
	
	private void clearUpSearchAtPly() {
		if (searchDepthPly > originalDepthRequested) {
			searchDepthPly--;
		}
	}

	private boolean doScoreBackup(short positionScore) {
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
	
	private short applyMoveAndScore(GenericMove currMove) throws InvalidPieceException {
		
		doPerformMove(currMove);
		short positionScore = assessNewPosition(currMove);
		doUnperformMove(currMove);
		
		sm.incrementNodesSearched();
		
		return positionScore;
	}
	
	private short assessNewPosition(GenericMove prevMove) throws InvalidPieceException {
		short positionScore = 0;
		if ( isTerminalNode() ) {
			positionScore = pe.evaluatePosition();
			depthSearchedPly = 1; // We applied a move in order to generate this score
		} else {
			positionScore = searchPly();
		}
		return positionScore;
	}
	
	private boolean isTerminalNode() {
		boolean terminalNode = false;
		if (currPly == originalDepthRequested) {
			if (pe.isQuiescent()) {
				terminalNode = true;
			}
		} else if (currPly > originalDepthRequested) {
			if (pe.isQuiescent() || (currPly > (originalDepthRequested*3)-1)) {
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

	private void doPerformMove(GenericMove currMove) throws InvalidPieceException {
		SearchDebugAgent.printPerformMove(currPly, currMove);
		pm.performMove(currMove);
		currPly++;
	}
	
	private void doUnperformMove(GenericMove currMove) throws InvalidPieceException {
		pm.unperformMove();
		currPly--;
		SearchDebugAgent.printUndoMove(currPly, currMove);
	}	
}
