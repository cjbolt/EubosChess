package eubos.search;

import java.util.Iterator;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.board.pieces.Piece.Colour;
import eubos.position.IChangePosition;
import eubos.position.IGenerateMoveList;
import eubos.position.IPositionAccessors;
import eubos.position.IScoreMate;
import eubos.position.MateScoreGenerator;
import eubos.search.Transposition.ScoreType;
import eubos.search.TranspositionEvaluation;
import eubos.position.IEvaluate;

public class PlySearcher {

	private IChangePosition pm;
	private IGenerateMoveList mlgen;
	IPositionAccessors pos;
	
	ScoreTracker st;
	private IEvaluate pe;
	private IScoreMate sg;
	PrincipalContinuation pc;
	private SearchMetrics sm;
	private SearchMetricsReporter sr;
	
	private boolean terminate = false;
	
	private Colour initialOnMove;	
	private List<GenericMove> lastPc;
	private byte searchDepthPly;
	private ITranspositionAccessor tt;
	private PrincipalContinuationUpdateHelper pcUpdater;
	
	byte currPly = 0;
	byte depthSearchedPly = 0;
	private byte originalDepthRequested = 0;
	
	PlySearcher(
			ITranspositionAccessor hashMap,
			ScoreTracker st,
			PrincipalContinuation pc,
			SearchMetrics sm,
			SearchMetricsReporter sr,
			byte searchDepthPly,
			IChangePosition pm,
			IGenerateMoveList mlgen,
			IPositionAccessors pos,
			List<GenericMove> lastPc,
			IEvaluate pe) {
		currPly = 0;
		depthSearchedPly = 0;
		initialOnMove = pos.getOnMove();
		
		this.pc = pc;
		this.sm = sm;
		this.sr = sr;
		this.pm = pm;
		this.pos = pos;
		this.mlgen = mlgen;
		this.pe = pe;
		this.lastPc = lastPc;
		this.searchDepthPly = searchDepthPly;
		originalDepthRequested = searchDepthPly;
		
		this.st = st;
		this.tt = hashMap;
		this.sg = new MateScoreGenerator(pos);
		this.pcUpdater = new PrincipalContinuationUpdateHelper(initialOnMove, pc, sm, sr);
	}
	
	private boolean atRootNode() { return currPly == 0; }
	
	public synchronized void terminateFindMove() { terminate = true; }
	private synchronized boolean isTerminated() { return terminate; }	
	
	short searchPly() throws InvalidPieceException {
		if (isTerminated())
			return 0;
		
		List<GenericMove> ml = null;
		byte depthRequiredPly = initialiseSearchAtPly();
		
		TranspositionEvaluation eval = tt.getTransposition(currPly, depthRequiredPly);
		switch (eval.status) {
		
		case sufficientTerminalNode:
		case sufficientRefutation:
			if (searchDepthPly <= originalDepthRequested) {
				depthSearchedPly = eval.trans.getDepthSearchedInPly();
				pc.clearTreeBeyondPly(currPly);
				if (doScoreBackup(eval.trans.getScore())) {
					doPrincipalContinuationUpdateOnScoreBackup(eval.trans.getBestMove(), eval.trans.getScore());
				}
				sm.incrementNodesSearched();
				break;
			}
			
		case sufficientSeedMoveList:
			SearchDebugAgent.printHashIsSeedMoveList(currPly, eval.trans.getBestMove(), pos.getHash());
			ml = eval.trans.getMoveList();
			searchMoves( ml, eval.trans);
			break;
			
		case insufficientNoData:
			ml = getMoveList();
			if (searchDepthPly > originalDepthRequested) {
				ScoreType plyBound = (pos.getOnMove().equals(Colour.white)) ? ScoreType.lowerBound : ScoreType.upperBound;
				short plyScore = (plyBound == ScoreType.lowerBound) ? Short.MIN_VALUE : Short.MAX_VALUE;	
				// In order to store the move list in extended searches
				if (ml.size() != 0) {
					Transposition newTrans = new Transposition((byte)0, plyScore, plyBound, ml, ml.get(0));
					tt.setTransposition(sm, currPly, null, newTrans);
				}
			}
			searchMoves( ml, eval.trans);
			break;
			
		default:
			break;
		}
		handleEarlyTermination();
		clearUpSearchAtPly();
		
		return st.getBackedUpScoreAtPly(currPly);
	}

	private void doPrincipalContinuationUpdateOnScoreBackup(
			GenericMove currMove, short positionScore)
			throws InvalidPieceException {
		pc.update(currPly, currMove);
		if (atRootNode()) {
			// If backed up to the root node, report the principal continuation
			tt.createPrincipalContinuation(pc, searchDepthPly, pm);
			pcUpdater.report(positionScore, searchDepthPly);
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

	private void searchMoves(List<GenericMove> ml, Transposition trans) throws InvalidPieceException {
		if (isMateOccurred(ml)) {
			short mateScore = sg.scoreMate(currPly, initialOnMove);
			st.setBackedUpScoreAtPly(currPly, mateScore);
		} else if (searchDepthPly > originalDepthRequested) {
			Iterator<GenericMove> move_iter = ml.iterator();
			boolean isCheckCaptureOrPromotionMove = false;
			boolean isNoMovesSearched = true;
			while(move_iter.hasNext() && !isTerminated()) {
				GenericMove currMove = move_iter.next();
				if (currMove.promotion!=null) {
					isCheckCaptureOrPromotionMove = true;
				} else {
					pm.performMove(currMove);
					isCheckCaptureOrPromotionMove = pos.lastMoveWasCheckOrCapture();
					pm.unperformMove();
				}
				
				if (isCheckCaptureOrPromotionMove || (isNoMovesSearched && !move_iter.hasNext())) { // Need to back up a score, so evaluate as terminal node
					isNoMovesSearched = false;
					short positionScore = applyMoveAndScore(currMove);
					doScoreBackup(positionScore);
					
					if (st.isAlphaBetaCutOff( currPly, positionScore)) {
						SearchDebugAgent.printRefutationFound(currPly);
						break;	
					}
				}
			}
		} else { // normal search
			pc.update(currPly, ml.get(0));
			Iterator<GenericMove> move_iter = ml.iterator();
			
			boolean everBackedUp = false;
			boolean refutationFound = false;
			ScoreType plyBound = (pos.getOnMove().equals(Colour.white)) ? ScoreType.lowerBound : ScoreType.upperBound;
			short plyScore = (plyBound == ScoreType.lowerBound) ? Short.MIN_VALUE : Short.MAX_VALUE;
			
			long currHashAtStart = pos.getHash();
			
			while(move_iter.hasNext() && !isTerminated()) {
				
				debugCheckPositionHashConsistency(currHashAtStart);
				
				GenericMove currMove = move_iter.next();
				rootNodeInitAndReportingActions(currMove);
				
				short positionScore = applyMoveAndScore(currMove);
				if (!isTerminated()) {
					if (doScoreBackup(positionScore)) {
						everBackedUp = true;
						plyScore = positionScore;					
						trans = tt.setTransposition(sm, currPly, trans,
									new Transposition(getTransDepth(), positionScore, plyBound, ml, currMove));
						doPrincipalContinuationUpdateOnScoreBackup(currMove, positionScore);
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
			}
			if (!isTerminated()) {
				if (everBackedUp && !refutationFound && trans != null) {
					trans.setScoreType(ScoreType.exact);
				}
				depthSearchedPly = (byte) (searchDepthPly - currPly);
			}
		}
	}

	private void rootNodeInitAndReportingActions(GenericMove currMove) {
		if (atRootNode()) {
			// When we start to search a move at the root node, clear the principal continuation data
			pc.clearRowsBeyondPly(currPly);
			reportMove(currMove);
		}
	}
	
	private byte getTransDepth() {
		return (byte) Math.max(depthSearchedPly,(searchDepthPly-currPly));
	}

	private void debugCheckPositionHashConsistency(long currHashAtStart) {
		long currHashAtMoveN = pos.getHash();
		assert currHashAtMoveN == currHashAtStart;
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
		if (currPly >= originalDepthRequested) {
			searchDepthPly++;
		}
		byte depthRequiredPly = (byte)(searchDepthPly - currPly);
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
		
	private List<GenericMove> getMoveList() throws InvalidPieceException {
		List<GenericMove> ml = null;
		if ((lastPc != null) && (lastPc.size() > currPly)) {
			// Seeded move list is possible
			ml = mlgen.getMoveList(lastPc.get(currPly));
		} else {
			ml = mlgen.getMoveList();
		}
		return ml;
	}

	private boolean isMateOccurred(List<GenericMove> ml) {
		return ml.isEmpty();
	}
	
	private short applyMoveAndScore(GenericMove currMove) throws InvalidPieceException {
		
		doPerformMove(currMove);
		short positionScore = assessNewPosition(currMove);
		doUnperformMove(currMove);
		
		sm.incrementNodesSearched();
		
		return positionScore;
	}

	enum SearchState {
		normalSearchTerminalNode,
		normalSearchNode,
		extendedSearchNode,
		extendedSearchTerminalNode
	};
	
	private short assessNewPosition(GenericMove prevMove) throws InvalidPieceException {
		short positionScore = 0;
		switch ( isTerminalNode() ) {
		case normalSearchTerminalNode:
		case extendedSearchTerminalNode:
			positionScore = scoreTerminalNode();
			depthSearchedPly = 1; // We applied a move in order to generate this score
			break;
		case normalSearchNode:
			positionScore = searchPly();
			break;
		case extendedSearchNode:
			positionScore = searchPly();
			break;
		default:
			break;
		}
		return positionScore;
	}

	private short scoreTerminalNode() {
		return pe.evaluatePosition();
	}
	
	private SearchState isTerminalNode() {
		SearchState nodeState = SearchState.normalSearchNode;
		if (currPly < originalDepthRequested) {
			nodeState = SearchState.normalSearchNode;
		} else if (currPly == originalDepthRequested) {
			if (pe.isQuiescent()) {
				nodeState = SearchState.normalSearchTerminalNode;
			} else {
				nodeState = SearchState.extendedSearchNode;
			}
		} else if (currPly > originalDepthRequested) {
			if (pe.isQuiescent() || (currPly > Math.min((originalDepthRequested + 6), ((originalDepthRequested*3)-1))) /* todo ARBITRARY!!!! */) {
				nodeState = SearchState.extendedSearchTerminalNode;
			} else {
				nodeState = SearchState.extendedSearchNode; 
			}
		}
		return nodeState;
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
