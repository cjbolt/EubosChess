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
	
	int currPly = 0;
	
	PlySearcher(
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
	}
	
	synchronized void terminateFindMove() { terminate = true; }
	private synchronized boolean isTerminated() { return terminate; }	
	
	int searchPly() throws InvalidPieceException {
		Colour onMove = pos.getOnMove();
		SearchDebugAgent.printSearchPly(currPly,onMove);

		List<GenericMove> ml = getMoveList();
		if (!isMateOccurred(ml)) {
			st.setProvisionalScoreAtPly(currPly);
			searchMoves(ml);
		} else {
			boolean isWhite = (onMove == Colour.white);
			int mateScore = sg.scoreMate(currPly, isWhite, initialOnMove);
			st.setBackedUpScoreAtPly(currPly, mateScore);			
		}
		
		return st.getBackedUpScoreAtPly(currPly);
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
		int positionScore = 0;
		
		doPerformMove(currMove);
		positionScore = assessNewPosition(currMove);
		doUnperformMove(currMove);
		
		return positionScore;
	}

	private int assessNewPosition(GenericMove prevMove) throws InvalidPieceException {
		// Either recurse or evaluate a terminal position
		int positionScore;
		if ( isTerminalNode() ) {
			positionScore = pe.evaluatePosition(pos);
			positionScore = testSearchExtensionForRecaptures(prevMove, positionScore);
		} else {
			currPly++;
			positionScore = searchPly();
			currPly--;
		}
		return positionScore;
	}

	protected int testSearchExtensionForRecaptures(GenericMove prevMove,
			int positionScore) throws InvalidPieceException {
		if (ENABLE_SEARCH_EXTENSION_FOR_RECAPTURES) {
			if (pos.lastMoveWasCapture()) {
				int nextPositionScore;
				List<GenericMove> extra_ml = mlgen.getMoveList();
				// look to see if there is a recapture possible
				// where a recapture is a capture on target square
				boolean recapturePossible = false;
				GenericMove recaptureMove = null;
				Iterator<GenericMove> move_iter = extra_ml.iterator();
				while(move_iter.hasNext()) {
					GenericMove nextMove = move_iter.next();
					if (nextMove.to.equals(prevMove.to)) {
						recaptureMove = nextMove;
						recapturePossible = true;
					}
				}
				// if so search recapture move only
				if (recapturePossible) {
					doPerformMove(recaptureMove);
					nextPositionScore = pe.evaluatePosition(pos);
					doUnperformMove(recaptureMove);
					// be pessimistic if recapture is possible
					if (pos.getOnMove() == Colour.white) {
						if (nextPositionScore > positionScore) // better for black
							positionScore = nextPositionScore;
					} else {
						if (nextPositionScore < positionScore)
							positionScore = nextPositionScore;
					}
				}
			}
		}
		return positionScore;
	}
	
	private boolean isTerminalNode() {
		boolean isTerminalNode = false;
		if (currPly == (searchDepthPly-1)) {
			isTerminalNode = true;
		}
		return isTerminalNode;
	}	

	private void doPerformMove(GenericMove currMove) throws InvalidPieceException {
		SearchDebugAgent.printPerformMove(currPly, currMove);
		pm.performMove(currMove);
	}
	
	private void doUnperformMove(GenericMove currMove) throws InvalidPieceException {
		SearchDebugAgent.printUndoMove(currPly, currMove);
		pm.unperformMove();
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
