package eubos.search;

import java.util.ArrayList;
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
import eubos.search.TranspositionTableAccessor.TranspositionEval;
import eubos.search.TranspositionTableAccessor.TranspositionTableStatus;
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
	private TranspositionTableAccessor tt;
	private PrincipalContinuationUpdateHelper pcUpdater;
	
	byte currPly = 0;
	byte depthSearchedPly = 0;
	
	PlySearcher(
			FixedSizeTranspositionTable hashMap,
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
		this.pc = pc;
		this.sm = sm;
		this.sr = sr;
		this.pm = pm;
		this.pos = pos;
		this.mlgen = mlgen;
		this.lastPc = lastPc;
		this.searchDepthPly = searchDepthPly;
		// Register initialOnMove
		initialOnMove = pos.getOnMove();
		this.pe = pe;
		this.st = new ScoreTracker(searchDepthPly*3, initialOnMove == Colour.white);
		this.tt = new TranspositionTableAccessor(hashMap, pos, st, lastPc);
		this.sg = new MateScoreGenerator(pos, searchDepthPly*3);
		this.pcUpdater = new PrincipalContinuationUpdateHelper(initialOnMove, pc, sm, sr);
	}
	
	public synchronized void terminateFindMove() { 
		terminate = true; }
	private synchronized boolean isTerminated() { return terminate; }	
	
	short normalSearchPly() throws InvalidPieceException {
		if (isTerminated())
			return 0;
		
		List<GenericMove> ml = null;
		byte depthRequiredPly = initialiseSearchAtPly();
		
		TranspositionEval eval = tt.evaluateTranspositionData(currPly, depthRequiredPly);
		switch (eval.status) {
		
		case sufficientTerminalNode:
		case sufficientRefutation:
			depthSearchedPly = eval.trans.getDepthSearchedInPly();
			pc.clearTreeBeyondPly(currPly);
			if (doScoreBackup(eval.trans.getScore())) {
				pc.update(currPly, eval.trans.getBestMove() /*eval.trans.getPrincipalContinuation()*/);
				if (currPly == 0) {
					//constructPc();
					pcUpdater.report(eval.trans.getScore(), depthSearchedPly);
				}
			}
			sm.incrementNodesSearched();
			break;
			
		case sufficientSeedMoveList:
			SearchDebugAgent.printHashIsSeedMoveList(currPly, eval.trans.getBestMove(), pos.getHash());
			ml = eval.trans.getMoveList();
			//SearchDebugAgent.printTransUpdate(currPly, eval.trans, pos.getHash());
			// Intentional drop through
		case insufficientNoData:
			if (ml == null)
				ml = getMoveList();
			searchMoves( ml, eval.trans);
			break;
			
		default:
			break;
		}
		handleEarlyTermination();
		
		return st.getBackedUpScoreAtPly(currPly);
	}

	private void handleEarlyTermination() {
		if (currPly == 0 && isTerminated()) {
			// Set best move to previous iteration search result.
			if (lastPc != null) {
				pc.update(0, lastPc.get(0));
			}
		}
	}

	private void searchMoves(List<GenericMove> ml, Transposition trans) throws InvalidPieceException {
		if (isMateOccurred(ml)) {
			short mateScore = sg.scoreMate(currPly, (pos.getOnMove() == Colour.white), initialOnMove);
			st.setBackedUpScoreAtPly(currPly, mateScore);
		} else {
			pc.update(currPly, ml.get(0));
			short provisionalScoreAtPly = st.getProvisionalScoreAtPly(currPly);
			Iterator<GenericMove> move_iter = ml.iterator();
			
			boolean everBackedUp = false;
			boolean refutationFound = false;
			ScoreType plyBound = (pos.getOnMove().equals(Colour.white)) ? ScoreType.lowerBound : ScoreType.upperBound;
			short plyScore = (plyBound == ScoreType.lowerBound) ? Short.MIN_VALUE : Short.MAX_VALUE;
			
			while(move_iter.hasNext() && !isTerminated()) {
				GenericMove currMove = move_iter.next();
				if (currPly == 0) {
					pc.clearRowsBeyondPly(currPly);
					reportMove(currMove);
				}
				
				short positionScore = applyMoveAndScore(currMove);
				
				if (doScoreBackup(positionScore)) {
					everBackedUp = true;
					plyScore = positionScore;
					pc.update(currPly, currMove);
					if (currPly == 0) {
						//constructPc();
						pcUpdater.report(positionScore, searchDepthPly);
					}
					Transposition newTrans = new Transposition(depthSearchedPly, st.getBackedUpScoreAtPly(currPly), plyBound, ml, pc.getBestMove(currPly)/*pc.toPvList(currPly)*/);
					trans = tt.updateTranspositionTable(sm, currPly, trans, newTrans);
				} else {
					boolean doUpdate = false;
					if (plyBound == ScoreType.lowerBound) {
						if (positionScore > plyScore)
							doUpdate = true;
					} else {
						if (positionScore < plyScore)
							doUpdate = true;
					}
					pc.clearRowsBeyondPly(currPly);
					if (doUpdate) {
						plyScore = positionScore;
						List<GenericMove> continuation = pc.toPvList(currPly);
						continuation.add(0,currMove);
						Transposition newTrans = new Transposition(depthSearchedPly, positionScore, plyBound, ml, continuation.get(0));
						trans = tt.updateTranspositionTable(sm, currPly, trans, newTrans);
					}
				}
				
				if (st.isAlphaBetaCutOff( currPly, provisionalScoreAtPly, positionScore)) {
					refutationFound = true;
					SearchDebugAgent.printRefutationFound(currPly);
					break;	
				}
			}
			if (everBackedUp && !refutationFound /*&& trans != null*/) {
				// Needed to set exact score instead of upper/lower bound score now we finished search at this ply
				//trans.setScoreType(ScoreType.exact);
				Transposition newTrans = new Transposition(depthSearchedPly, st.getBackedUpScoreAtPly(currPly), ScoreType.exact, ml, pc.getBestMove(currPly));
				trans = tt.updateTranspositionTable(sm, currPly, trans, newTrans);
			}
			depthSearchedPly++; // backing up, increment depth searched
		}
	}
	
	void constructPc() throws InvalidPieceException {
		byte plies = 0;
		int numMoves = 0;
		List<GenericMove> constructed_pc = new ArrayList<GenericMove>(searchDepthPly);
		for (plies = 0; plies < searchDepthPly; plies++) {
			GenericMove pcMove = pc.getBestMove(plies);
			if (pcMove != null) {
				// apply move from principal continuation
				constructed_pc.add(pcMove);
				pm.performMove(pcMove);
				numMoves++;
			} else {
				/* Apply move and find best move from hash */
				TranspositionEval eval = tt.evaluateTranspositionData(plies, 0);
				if (eval.status != TranspositionTableStatus.insufficientNoData && eval != null && eval.trans != null) {
					GenericMove currMove = eval.trans.getBestMove();
					constructed_pc.add(currMove);
					pm.performMove(currMove);
					numMoves++;
				}
			}
		}
		for (plies = 0; plies < numMoves; plies++) {
			pm.unperformMove();
		}
		pc.update(0, constructed_pc);
	}
	
	private byte initialiseSearchAtPly() {
		byte depthRequiredPly = (byte)(searchDepthPly - currPly);
		st.setProvisionalScoreAtPly(currPly);
		SearchDebugAgent.printStartPlyInfo(currPly, depthRequiredPly, st.getProvisionalScoreAtPly(currPly), pos);
		return depthRequiredPly;
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
		short positionScore;
		switch ( isTerminalNode() ) {
		case normalSearchTerminalNode:
			positionScore = scoreTerminalNode();
			depthSearchedPly = 1; // We searched to find this score
			break;
		case normalSearchNode:
			positionScore = normalSearchPly();
			break;
		case extendedSearchNode:
			positionScore = extendedSearchPly();
			depthSearchedPly = 1; // Not sure this is needed
			break;
		case extendedSearchTerminalNode:
			positionScore = scoreTerminalNode();
			depthSearchedPly = 1; // Not sure this is needed
			break;
		default:
			positionScore = 0;
			break;
		}
		return positionScore;
	}

	private short scoreTerminalNode() {
		return pe.evaluatePosition();
	}
	
	private SearchState isTerminalNode() {
		SearchState nodeState = SearchState.normalSearchNode;
		if (currPly < searchDepthPly) {
			nodeState = SearchState.normalSearchNode;
		} else if (currPly == searchDepthPly) {
			if (pe.isQuiescent()) {
				nodeState = SearchState.normalSearchTerminalNode;
			} else {
				nodeState = SearchState.extendedSearchNode; 
			}
		} else { // if (currPly > searchDepthPly) // extended search
			if (pe.isQuiescent() || (currPly > Math.min((searchDepthPly + 4), ((searchDepthPly*3)-1))) /* todo ARBITRARY!!!! */) {
				nodeState = SearchState.extendedSearchTerminalNode;
			} else {
				nodeState = SearchState.extendedSearchNode; 
			}
		}
		return nodeState;
	}
	
	private short extendedSearchPly() throws InvalidPieceException {
		if (isTerminated())
			return 0;
				
		// todo At first, don't use hash map for extended searches
		st.setProvisionalScoreAtPly(currPly);
		List<GenericMove> ml = mlgen.getMoveListOfChecksAndCaptures();
		searchCheckAndCaptureMoves( ml );
			
		return st.getBackedUpScoreAtPly(currPly);
	}
	
	private void searchCheckAndCaptureMoves(List<GenericMove> ml) throws InvalidPieceException {
		if (isMateOccurred(ml)) {
			// Probably wrong!
			short mateScore = sg.scoreMate(currPly, (pos.getOnMove() == Colour.white), initialOnMove);
			st.setBackedUpScoreAtPly(currPly, mateScore);
		} else {
			pc.update(currPly, ml.get(0));
			short provisionalScoreAtPly = st.getProvisionalScoreAtPly(currPly);
			Iterator<GenericMove> move_iter = ml.iterator();
			
			while(move_iter.hasNext() && !isTerminated()) {
				GenericMove currMove = move_iter.next();
				if (currPly == 0) {
					pc.clearRowsBeyondPly(currPly);
					reportMove(currMove);
				}
				
				short positionScore = applyMoveAndScore(currMove);
				
				if (doScoreBackup(positionScore)) {
					pc.update(currPly, currMove);
				}
				
				if (st.isAlphaBetaCutOff( currPly, provisionalScoreAtPly, positionScore)) {
					SearchDebugAgent.printRefutationFound(currPly);
					break;	
				}
			}
			// don't count extended searches in hashing....
			//depthSearchedPly++; // backing up, increment depth searched
		}
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
	
	void checkContinuationValidForPosition() {
		System.out.println("Pc to test: "+pc.toPvList(currPly));
		Iterator<GenericMove> move_iter = pc.toPvList(currPly).iterator();
		System.out.println("Original position: "+pos.getFen());
		/* Even this isn't a good enough test, because it doesn't check that the move is valid
		 *  for the piece, just that there is a piece on the square to be moved :(
		 *  
		 *  To tighten it right up we would need to search the moves in each position and check
		 *  the move list contains the move from the variation (move list moves are legal).
		 */
		while(move_iter.hasNext()) {
			GenericMove move = move_iter.next();
			try {
				pm.performMove(move);
				System.out.println("Test move: " + move);
				System.out.println("Next position: " +pos.getFen());
			} catch (InvalidPieceException e) {
				/* The continuation provided is invalid. Probably because it is from old limbs of
				 * the search tree, stale backed up data. If we didn't back up pc at the node
				 * searched, then we should probably clear the pv at that row of the array,
				 * when we return.
				 */
				System.out.println("Can't move: " + move);
				System.out.println("Error position: "+pos.getFen());
			}
		}
		move_iter = pc.toPvList(currPly).iterator();
		while(move_iter.hasNext()) {
			GenericMove move = move_iter.next();
			try {
				System.out.println("Undo move: " + move);
				pm.unperformMove();
			} catch (InvalidPieceException e) {
				System.out.println("Can't undo move: " + move);
				System.out.println("Error undo position: "+pos.getFen());
			}
		}
	}
}
