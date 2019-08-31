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
import eubos.position.PositionEvaluator;
import eubos.position.Transposition;
import eubos.position.Transposition.ScoreType;
import eubos.search.TranspositionTableAccessor.TranspositionEval;
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
			List<GenericMove> lastPc) {
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
		this.pe = new PositionEvaluator();
		this.st = new ScoreTracker(searchDepthPly, initialOnMove == Colour.white);
		this.tt = new TranspositionTableAccessor(hashMap, pos, st, lastPc);
		this.sg = new MateScoreGenerator(pos, searchDepthPly);
		this.pcUpdater = new PrincipalContinuationUpdateHelper(initialOnMove, pc, sm, sr);
	}
	
	public synchronized void terminateFindMove() { 
		terminate = true; }
	private synchronized boolean isTerminated() { return terminate; }	
	
	short searchPly() throws InvalidPieceException {
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
				pc.update(currPly, eval.trans.getPrincipalContinuation());
				if (currPly == 0)
					pcUpdater.report(eval.trans.getScore(), depthSearchedPly);
			}
			sm.incrementNodesSearched();
			break;
			
		case sufficientSeedMoveList:
			SearchDebugAgent.printHashIsSeedMoveList(currPly, eval.trans.getBestMove(),pos.getHash());
			ml = eval.trans.getMoveList();
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
			
			while(move_iter.hasNext() && !isTerminated()) {
				GenericMove currMove = move_iter.next();
				if (currPly == 0) {
					pc.clearRowsBeyondPly(currPly);
					reportMove(currMove);
				}
				
				short positionScore = applyMoveAndScore(currMove);
				
				if (doScoreBackup(positionScore)) {
					everBackedUp = true;
					pc.update(currPly, currMove);
					if (currPly == 0)
						pcUpdater.report(positionScore, searchDepthPly);
					ScoreType bound = (pos.getOnMove().equals(Colour.white)) ? ScoreType.lowerBound : ScoreType.upperBound;
					trans = updateTranspositionTable(bound, ml, st.getBackedUpScoreAtPly(currPly), trans);
				} else {
					// Consequence of this change is that we won't store hashes for nodes we didn't back up. 
					pc.clearRowsBeyondPly(currPly);
					// cause it to bring down the previous ply continuation, to clear it.
					//pc.update(currPly, pc.getBestMove(currPly));
					// if score was better than the previous in this node maybe we could store some info?
					//ScoreType bound = (pos.getOnMove().equals(Colour.white)) ? ScoreType.lowerBound : ScoreType.upperBound;
					//trans = updateTranspositionTable(bound, ml, positionScore, trans);
				}
				
				if (st.isAlphaBetaCutOff( currPly, provisionalScoreAtPly, positionScore)) {
					SearchDebugAgent.printRefutationFound(currPly);
					break;	
				}
			}
			if (everBackedUp) {
				// Needed to set exact score instead of upper/lower bound score now we finished search at this ply
				trans = updateTranspositionTable(getBound(move_iter), ml, st.getBackedUpScoreAtPly(currPly), trans);
			}
			depthSearchedPly++; // backing up, increment search depth
		}
	}
	
	private ScoreType getBound(Iterator<GenericMove> move_iter) {
		ScoreType bound = ScoreType.exact;
		if (move_iter.hasNext()) {
			// We haven't searched all the moves yet so this is a bound score
			bound = (pos.getOnMove().equals(Colour.white)) ? ScoreType.lowerBound : ScoreType.upperBound;
		}
		return bound;
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

	private void checkContinuationValidForPosition() {
		System.out.println("Pc to test: "+pc.toPvList(currPly));
		Iterator<GenericMove> move_iter = pc.toPvList(currPly).iterator();
		System.out.println("Original position: "+pos.getFen());
		/* Even this isn't a good enough test, because it doesn't check that the move is valid
		 *  for the piece, just that there is a piece on the square to be moved :(
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
	
	private Transposition updateTranspositionTable(ScoreType bound, List<GenericMove> ml, short positionScore, Transposition trans) {
		//checkContinuationValidForPosition();
		Transposition new_trans = new Transposition(depthSearchedPly, positionScore, bound, ml, pc.toPvList(currPly));
		if (trans != null) {
			trans = tt.checkForUpdateTrans(currPly, new_trans, trans);
		} else {
			trans = tt.getTransCreateIfNew(currPly, new_trans);
			sm.setHashFull(tt.getHashUtilisation());
		}
		return trans;
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

	private short assessNewPosition(GenericMove prevMove) throws InvalidPieceException {
		short positionScore;
		// Either recurse or evaluate a terminal position
		if ( isTerminalNode() ) {
			positionScore = scoreTerminalNode();
			depthSearchedPly = 1; // We searched to find this score
		} else {
			positionScore = searchPly();
		}
		return positionScore;
	}

	private short scoreTerminalNode() {
		return pe.evaluatePosition(pos);
	}
	
	private boolean isTerminalNode() {
		boolean isTerminalNode = false;
		if (currPly == searchDepthPly) {
			isTerminalNode = true;
		}
		return isTerminalNode;
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
