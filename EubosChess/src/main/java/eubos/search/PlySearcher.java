package eubos.search;

import java.util.Iterator;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.board.pieces.King;
import eubos.board.pieces.Piece.Colour;
import eubos.position.IChangePosition;
import eubos.position.IGenerateMoveList;
import eubos.position.IPositionAccessors;
import eubos.position.IScoreMate;
import eubos.position.Transposition;
import eubos.position.Transposition.ScoreType;
import eubos.search.TranspositionTableAccessor.TranspositionEval;
import eubos.position.IEvaluate;

public class PlySearcher {
	
	public static final int PLIES_PER_MOVE = 2;
	public static final boolean ENABLE_SEARCH_EXTENSION_FOR_RECAPTURES = false;
	
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
	private int searchDepthPly;
	private TranspositionTableAccessor tt;
	
	int currPly = 0;
	
	PlySearcher(
			FixedSizeTranspositionTable hashMap,
			IEvaluate pe,
			IScoreMate sg,
			PrincipalContinuation pc,
			SearchMetrics sm,
			SearchMetricsReporter sr,
			int searchDepthPly,
			IChangePosition pm,
			IGenerateMoveList mlgen,
			IPositionAccessors pos,
			List<GenericMove> lastPc) {
		currPly = 0;
		this.pe = pe;
		this.sg = sg;
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
		this.st = new ScoreTracker(searchDepthPly, initialOnMove == Colour.white);
		this.tt = new TranspositionTableAccessor(hashMap, pos, st, pc, lastPc);
	}
	
	synchronized void terminateFindMove() { terminate = true; }
	private synchronized boolean isTerminated() { return terminate; }	
	
	int searchPly() throws InvalidPieceException {
		SearchDebugAgent.printSearchPly(currPly, pos.getOnMove());
		
		int depthRequiredPly = (searchDepthPly - currPly);
		st.setProvisionalScoreAtPly(currPly);
		TranspositionEval eval = tt.evaluateTranspositionData(currPly, depthRequiredPly);
		switch (eval.status) {
		
		case sufficientTerminalNode:
			SearchDebugAgent.printHashIsTerminalNode(currPly, eval.trans.getBestMove(), eval.trans.getScore());
			st.setBackedUpScoreAtPly(currPly, eval.trans.getScore());
			pc.update(currPly, eval.trans.getBestMove());
			break;
			
		case sufficientRefutation:
			SearchDebugAgent.printHashIsRefutation(currPly);
			break;
			
		case sufficientSeedMoveList:
			SearchDebugAgent.printHashIsSeedMoveList(currPly, eval.trans.getBestMove());
			seedMoveList(eval);
			// Intentional drop through
		case insufficientNoData:
			searchMoves(getMoveList(), eval.trans);
			break;
			
		default:
			break;
		}
		
		return st.getBackedUpScoreAtPly(currPly);
	}

	void searchMoves(List<GenericMove> ml, Transposition trans) throws InvalidPieceException {
		if (isMateOccurred(ml)) {
			int mateScore = sg.scoreMate(currPly, (pos.getOnMove() == Colour.white), initialOnMove);
			st.setBackedUpScoreAtPly(currPly, mateScore);
		} else {
			boolean refutation_found = false;
			int provisionalScoreAtPly = st.getProvisionalScoreAtPly(currPly);
			Iterator<GenericMove> move_iter = ml.iterator();
			
			while(move_iter.hasNext() && !isTerminated()) {
				GenericMove currMove = move_iter.next();
				if (currPly == 0)
					reportMove(currMove);
				
				int positionScore = applyMoveAndScore(currMove);
					
				if (st.isBackUpRequired(currPly, positionScore)) {
					doScoreBackup(currMove, positionScore);
				} else if (st.isAlphaBetaCutOff( currPly, provisionalScoreAtPly, positionScore )) {
					refutation_found = true;	
				}
				
				updateTranspositionTable(move_iter, positionScore, trans);
				
				if (refutation_found) {
					SearchDebugAgent.printRefutationFound(currPly);
					break;
				}
			}
		}
	}

	protected void doScoreBackup(GenericMove currMove, int positionScore) {
		// New best score found at this node, back up and update the principal continuation.
		st.setBackedUpScoreAtPly(currPly, positionScore);
		pc.update(currPly, currMove);
		if (currPly == 0)
			new PrincipalContinuationUpdateHelper(positionScore).report();
	}

	protected void updateTranspositionTable(Iterator<GenericMove> move_iter,
			int positionScore, Transposition trans) {
		int depthPositionSearchedPly = (searchDepthPly - currPly);
		GenericMove bestMove = (depthPositionSearchedPly == 0) ? null : pc.getBestMove(currPly);
		if (move_iter.hasNext()) {
			// We haven't searched all the moves yet so this is a bound score
			ScoreType bound = (pos.getOnMove() == Colour.white) ? ScoreType.lowerBound : ScoreType.upperBound;
			tt.storeTranspositionScore(depthPositionSearchedPly, bestMove, positionScore, bound, trans);
		} else {
			// All moves have been searched so score is exact for this depth.
			tt.storeTranspositionScore(depthPositionSearchedPly, bestMove, positionScore, ScoreType.exact, trans);
		}
	}
	
	void reportMove(GenericMove currMove) {
		sm.setCurrentMove(currMove);
		sm.incrementCurrentMoveNumber();
		sr.reportCurrentMove();
	}
	
	private void seedMoveList(TranspositionEval ret) {
		if (lastPc != null) {
			try {
				lastPc.set(currPly, ret.trans.getBestMove());
			} catch (IndexOutOfBoundsException e) {
				for (int i=lastPc.size(); i < currPly; i++) {
					lastPc.add(i, ret.trans.getBestMove());
				}
			}
		}
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
	
	private int applyMoveAndScore(GenericMove currMove) throws InvalidPieceException {
		
		doPerformMove(currMove);
		int positionScore = assessNewPosition(currMove);
		doUnperformMove(currMove);
		
		sm.incrementNodesSearched();
		
		return positionScore;
	}

	private int assessNewPosition(GenericMove prevMove) throws InvalidPieceException {
		int positionScore;
		// Either recurse or evaluate a terminal position
		if ( isTerminalNode() ) {
			positionScore = scoreTerminalNode();
		} else {
			positionScore = searchPly();
		}
		return positionScore;
	}

	private int scoreTerminalNode() {
		int positionScore = pe.evaluatePosition(pos); 
		Transposition trans = tt.hashMap.getTransposition(pos.getHash().hashCode);
		if ((trans != null) && (trans.getScoreType() == ScoreType.exact)) { 
			// don't need to score, can use previous score. 
			positionScore = trans.getScore(); 
		} else { 
			positionScore = pe.evaluatePosition(pos);
			// Store, as it could prevent having to score the position if encountered again
		    tt.storeTranspositionScore(0, null, positionScore, ScoreType.exact, null); 
		}
		return positionScore;
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
		SearchDebugAgent.printUndoMove(currPly, currMove);
		pm.unperformMove();
		currPly--;
	}	
		
	// Principal continuation focused, search report focused inner class
	class PrincipalContinuationUpdateHelper
	{
		int positionScore;
		
		PrincipalContinuationUpdateHelper(int score) {
			positionScore = score;
		}

		void report() {
			assignPrincipalVariationToSearchMetrics();
			assignCentipawnScoreToSearchMetrics();
			sr.reportPrincipalVariation();
		}	
		
		private void assignPrincipalVariationToSearchMetrics() {
			truncatePrincipalContinuation();
			sm.setPrincipalVariation(pc.toPvList());
		}	

		private void assignCentipawnScoreToSearchMetrics() {
			if (initialOnMove.equals(Colour.black))
				positionScore = -positionScore; // Negated due to UCI spec (from engine pov)
			sm.setCpScore(positionScore);
		}
		
		private void truncatePrincipalContinuation() {
			if (isScoreIndicatesMate()) {
				// If the positionScore indicates a mate, truncate the pc accordingly
				int matePly = calculatePlyMateOccurredOn();
				pc.clearAfter(matePly);
			}
		}
		
		private boolean isScoreIndicatesMate() {
			return Math.abs(positionScore) >= King.MATERIAL_VALUE;
		}
		
		private int calculatePlyMateOccurredOn() {
			int matePly = Math.abs(positionScore)/King.MATERIAL_VALUE;
			matePly *= PLIES_PER_MOVE;
			matePly = searchDepthPly - matePly;
			if (isOwnMate()) {
				if ((searchDepthPly&1) != 0x1)
					matePly += 1;
			} else {
				if ((searchDepthPly&1) == 0x1)
					matePly -= 1;	
			}
			return matePly;
		}
		
		private boolean isOwnMate() {
			return ((initialOnMove==Colour.white && positionScore<0) ||
			        (initialOnMove==Colour.black && positionScore>0));
		}
	}	
}
