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
		Colour onMove = pos.getOnMove();
		SearchDebugAgent.printSearchPly(currPly,onMove);
		
		st.setProvisionalScoreAtPly(currPly);
		int depthRequiredPly = (searchDepthPly - currPly);
		TranspositionEval ret = tt.evaluateTranspositionData(currPly, depthRequiredPly);
		switch (ret.status) {
		
		case sufficientTerminalNode:
			SearchDebugAgent.printHashIsTerminalNode(currPly,ret.trans.getBestMove(), ret.trans.getScore());
			st.setBackedUpScoreAtPly(currPly, ret.trans.getScore());
			pc.update(currPly, ret.trans.getBestMove());
			break;
			
		case sufficientRefutation:
			SearchDebugAgent.printHashIsRefutation(currPly);
			break;
			
		case sufficientSeedMoveList:
			SearchDebugAgent.printHashIsSeedMoveList(currPly, ret.trans.getBestMove());
			seedMoveList(ret);
			// Intentional drop through
		case insufficientNoData:
			List<GenericMove> ml = getMoveList();
			if (!isMateOccurred(ml)) {
				searchMoves(ml);
				
				// Only update transposition if it wasn't a mate!
				int depthSearchedPly = searchDepthPly - currPly;
			    GenericMove bestMove = (depthSearchedPly == 0) ? null : pc.getBestMove(currPly);
				tt.storeTranspositionScore(depthSearchedPly, bestMove, st.getBackedUpScoreAtPly(currPly), ScoreType.exact);
			} else {
				boolean isWhite = (onMove == Colour.white);
				int mateScore = sg.scoreMate(currPly, isWhite, initialOnMove);
				st.setBackedUpScoreAtPly(currPly, mateScore);			
			}
			break;
			
		default:
			break;
		}
		
		return st.getBackedUpScoreAtPly(currPly);
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

	void searchMoves(List<GenericMove> ml) throws InvalidPieceException {
		int alphaBetaCutOff = st.getProvisionalScoreAtPly(currPly);
		Iterator<GenericMove> move_iter = ml.iterator();
		
		while(move_iter.hasNext() && !isTerminated()) {
			GenericMove currMove = move_iter.next();
			if (currPly == 0)
				reportMove(currMove);
			
			// Recurse and evaluate as required according to ply depth
			int positionScore = applyMoveAndScore(currMove);
			sm.incrementNodesSearched();
				
			if (st.isBackUpRequired(currPly, positionScore)) {
				// New best score found at this node, back up and update the principal continuation.
				st.setBackedUpScoreAtPly(currPly, positionScore);
				pc.update(currPly, currMove);
				if (currPly == 0)
					new PrincipalContinuationUpdateHelper(positionScore).report();
			} else if (st.isAlphaBetaCutOff( currPly, alphaBetaCutOff, positionScore )) {
				SearchDebugAgent.printRefutationFound(currPly);
				// Refutation of the move leading to this position was found, cut off search.
				break;	
			} else {
				// move didn't merit backing up and was not refutation, continue search...
			}
			
			// Save the backed up score in the Transposition table
			ScoreType bound = (pos.getOnMove() == Colour.white) ? ScoreType.lowerBound : ScoreType.upperBound;
			int depthPositionSearchedPly = (searchDepthPly - currPly);
		    GenericMove bestMove = (depthPositionSearchedPly == 0) ? null : pc.getBestMove(currPly);
			tt.storeTranspositionScore(depthPositionSearchedPly, bestMove, positionScore, bound);
		}
	}
	
	void reportMove(GenericMove currMove) {
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
	
	private int applyMoveAndScore(GenericMove currMove) throws InvalidPieceException {
		
		doPerformMove(currMove);
		int positionScore = assessNewPosition(currMove);
		doUnperformMove(currMove);
		
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
		/*
		Transposition trans = hashMap.getTransposition(hash.hashCode);
		if ((trans != null) && (trans.getScoreType() == Transposition.ScoreType.exact)) { 
			// don't need to score, can use previous score. 
			positionScore = trans.getScore(); 
		} else { 
			positionScore = pe.evaluatePosition(pos);
			// Store, as it could prevent having to score the position if encountered again
		    storeTranspositionScore(positionScore); 
		}
		*/
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
