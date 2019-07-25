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
import eubos.position.IEvaluate;
import eubos.position.ZobristHashCode;

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
	
	private ZobristHashCode hash;
	private FixedSizeTranspositionTable hashMap;
	
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
		this.hash = new ZobristHashCode(pos);
		this.hashMap = hashMap;
	}
	
	synchronized void terminateFindMove() { terminate = true; }
	private synchronized boolean isTerminated() { return terminate; }	
	
	int searchPly() throws InvalidPieceException {
		Colour onMove = pos.getOnMove();
		SearchDebugAgent.printSearchPly(currPly,onMove);
		
		if (hashIsTranspositionMiss(onMove)) {
			// Do search as usual
			List<GenericMove> ml = getMoveList();
			if (!isMateOccurred(ml)) {
				st.setProvisionalScoreAtPly(currPly);
				searchMoves(ml);
			} else {
				boolean isWhite = (onMove == Colour.white);
				int mateScore = sg.scoreMate(currPly, isWhite, initialOnMove);
				st.setBackedUpScoreAtPly(currPly, mateScore);			
			}
			storeTranspositionScore(st.getBackedUpScoreAtPly(currPly));
		}
		
		return st.getBackedUpScoreAtPly(currPly);
	}

	private boolean hashIsTranspositionMiss(Colour onMove) {
		boolean transpositionHit = false;
		int score = 0;
		Transposition trans = hashMap.getTransposition(hash.hashCode);
		if(trans != null) {
			// evaluate transposition
			int depth = trans.getDepthSearchedInPly();
			score = trans.getScore();
			int alphaBetaCutOff = st.getProvisionalScoreAtPly(currPly);
			if (depth >= (searchDepthPly-currPly)) {
				if ((onMove==Colour.white) && (score > alphaBetaCutOff)) {
					transpositionHit = true;
				} else if ((onMove==Colour.black) && (score < alphaBetaCutOff)) {
					transpositionHit = true;
				}
			}
		}
		if (transpositionHit) {
			st.setBackedUpScoreAtPly(currPly, score);
		}
		return !transpositionHit;
	}
	
	private void storeTranspositionScore(int score) {
		int depthPositionSearched = (searchDepthPly - currPly);
		Transposition trans = hashMap.getTransposition(hash.hashCode);
		if (trans != null) {
			 if (trans.getDepthSearchedInPly() >= depthPositionSearched)
				 return;
		}
		GenericMove move = (depthPositionSearched == 0) ? null : pc.getBestMove(currPly);
		trans = new Transposition(move, depthPositionSearched, score, Transposition.ScoreType.exact);
		hashMap.putTransposition(hash.hashCode, trans);		
	}

	void searchMoves(List<GenericMove> ml) throws InvalidPieceException {
		int alphaBetaCutOff = st.getProvisionalScoreAtPly(currPly);
		Iterator<GenericMove> move_iter = ml.iterator();
		
		while(move_iter.hasNext() && !isTerminated()) {
			GenericMove currMove = move_iter.next();
			
			if (currPly == 0) {
				reportMove(currMove);
			}
			
			int positionScore = applyMoveAndScore(currMove);
			sm.incrementNodesSearched();
			
			if (handleBackupOfScore(alphaBetaCutOff, currMove, positionScore)) {
				break;
			}
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
		pm.performMove(hash, currMove);
		currPly++;
	}
	
	private void doUnperformMove(GenericMove currMove) throws InvalidPieceException {
		SearchDebugAgent.printUndoMove(currPly, currMove);
		pm.unperformMove(hash);
		currPly--;
	}	

	private boolean handleBackupOfScore(int alphaBetaCutOff, GenericMove currMove, int positionScore) {
		boolean earlyTerminate = false;
		
		if (st.isBackUpRequired(currPly, positionScore)) {
			st.setBackedUpScoreAtPly(currPly, positionScore);
			pc.update(currPly, currMove);
			if (currPly == 0) {
				new PrincipalContinuationUpdateHelper(positionScore).report();
			}
		} else if (st.isAlphaBetaCutOff( currPly, alphaBetaCutOff, positionScore )) {
			SearchDebugAgent.printRefutationFound(currPly);
			earlyTerminate = true;
		}
		return earlyTerminate;
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
