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
	
	private IChangePosition pm;
	private IGenerateMoveList mlgen;
	private IPositionAccessors pos;
	
	private ScoreTracker st;
	private IEvaluate pe;
	private IScoreMate sg;
	PrincipalContinuation pc;
	private SearchMetrics sm;
	private SearchMetricsReporter sr;
	
	private boolean terminate = false;
	
	private Colour initialOnMove;	
	private List<GenericMove> lastPc;
	private int searchDepthPly;
	
	private int currPly = 0;
	
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

	void searchMoves(List<GenericMove> ml) throws InvalidPieceException {
		int alphaBetaCutOff = st.getProvisionalScoreAtPly(currPly);
		Iterator<GenericMove> move_iter = ml.iterator();
		while(move_iter.hasNext() && !isTerminated()) {
			GenericMove currMove = move_iter.next();
			int positionScore = applyMoveAndScore(currMove);
			if (handleBackupOfScore(alphaBetaCutOff, currMove, positionScore)) {
				break;
			}
		}
	}

	private boolean handleBackupOfScore(int alphaBetaCutOff, GenericMove currMove, int positionScore) {
		boolean earlyTerminate = false;
		
		if (isBackUpRequired(positionScore)) {
			doBackUpScore(positionScore);
			doUpdatePrincipalContinuation(currMove, positionScore);
			
		} else if (isAlphaBetaCutOff( alphaBetaCutOff, positionScore )) {
			doRefutation();
			earlyTerminate = true;
		}
		return earlyTerminate;
	}

	private void doRefutation() {
		SearchDebugAgent.printRefutationFound(currPly);
	}

	private void doUpdatePrincipalContinuation(GenericMove currMove, int positionScore) {
		pc.update(currPly, currMove);
		SearchDebugAgent.printPrincipalContinuation(currPly,pc);
		if (currPly == 0) {
			reportPrincipalContinuation(positionScore);
		}
	}

	private void doBackUpScore(int positionScore) {
		st.setBackedUpScoreAtPly(currPly, positionScore);
		SearchDebugAgent.printBackUpScore(currPly, positionScore);
	}

	private int applyMoveAndScore(GenericMove currMove) throws InvalidPieceException {
		int positionScore = 0;
		
		doPerformMove(currMove);
		positionScore = assessNewPosition();
		doUnperformMove(currMove);
		
		sm.incrementNodesSearched();
		return positionScore;
	}

	private int assessNewPosition() throws InvalidPieceException {
		// Either recurse or evaluate a terminal position
		int positionScore;
		if ( isTerminalNode() ) {
			positionScore = pe.evaluatePosition(pos);
		} else {
			currPly++;
			positionScore = searchPly();
			currPly--;
		}
		return positionScore;
	}

	private void doUnperformMove(GenericMove currMove) throws InvalidPieceException {
		SearchDebugAgent.printUndoMove(currPly, currMove);
		pm.unperformMove();
	}

	private void doPerformMove(GenericMove currMove) throws InvalidPieceException {
		reportNextMove(currMove);
		SearchDebugAgent.printPerformMove(currPly, currMove);
		pm.performMove(currMove);
	}

	void reportPrincipalContinuation(int positionScore) {
		assignPrincipalVariationToSearchMetrics(positionScore);
		assignCentipawnScoreToSearchMetrics(positionScore);
		sr.reportPrincipalVariation();
	}

	private void assignPrincipalVariationToSearchMetrics(int positionScore) {
		if (isScoreIndicatesMate(positionScore)) {
			// If the positionScore indicates a mate, truncate the pc accordingly
			int matePly = calculatePlyMateOccurredOn(positionScore);
			pc.clearAfter(matePly);
		}
		sm.setPrincipalVariation(pc.toPvList());
	}

	private void assignCentipawnScoreToSearchMetrics(int positionScore) {
		if (initialOnMove.equals(Colour.black))
			positionScore = -positionScore; // Negated due to UCI spec (from engine pov)
		sm.setCpScore(positionScore);
	}

	private int calculatePlyMateOccurredOn(int positionScore) {
		boolean ownMate = getOwnMate(positionScore); 
		int matePly = Math.abs(positionScore)/King.MATERIAL_VALUE;
		matePly *= PLIES_PER_MOVE;
		matePly = searchDepthPly - matePly;
		if (ownMate) {
			if ((searchDepthPly&1) != 0x1)
				matePly += 1;
		} else {
			if ((searchDepthPly&1) == 0x1)
				matePly -= 1;	
		}
		return matePly;
	}

	private boolean isScoreIndicatesMate(int positionScore) {
		return Math.abs(positionScore) >= King.MATERIAL_VALUE;
	}

	private boolean getOwnMate(int positionScore) {
		boolean ownMate = false;
		if ((initialOnMove==Colour.white && positionScore<0) ||
		    (initialOnMove==Colour.black && positionScore>0)) {
			ownMate = true;
		}
		return ownMate;
	}

	void reportNextMove(GenericMove currMove) {
		if (currPly == 0) {
			sm.setCurrentMove(currMove);
			sm.incrementCurrentMoveNumber();
			sr.reportCurrentMove();
		}
	}

	boolean isBackUpRequired(int positionScore) {
		boolean backUpScore = false;
		if (pos.getOnMove() == Colour.white) {
			// if white, maximise score
			if (positionScore > st.getBackedUpScoreAtPly(currPly))
				backUpScore = true;
		} else {
			// if black, minimise score 
			if (positionScore < st.getBackedUpScoreAtPly(currPly))
				backUpScore = true;
		}
		return backUpScore;
	}
	
	boolean isAlphaBetaCutOff(int cutOffValue, int positionScore) {
		if ((cutOffValue != Integer.MAX_VALUE) && (cutOffValue != Integer.MIN_VALUE)) {
			int prevPlyScore = st.getBackedUpScoreAtPly(currPly-1);
			Colour onMove = pos.getOnMove();
			if ((onMove == Colour.white && positionScore >= prevPlyScore) ||
				(onMove == Colour.black && positionScore <= prevPlyScore)) {
				// Indicates the search passed this node is refuted by an earlier move.
				return true;
			}
		}
		return false;
	}

	private boolean isTerminalNode() {
		boolean isTerminalNode = false;
		if (currPly == (searchDepthPly-1)) {
			isTerminalNode = true;
		}
		return isTerminalNode;
	}
	
	synchronized void terminateFindMove() { terminate = true; }
	private synchronized boolean isTerminated() { return terminate; }	
}
