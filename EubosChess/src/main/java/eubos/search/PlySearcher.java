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
		this.st = new ScoreTracker(searchDepthPly);
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
	}
	
	int searchPly(int currPly) throws InvalidPieceException {
		Colour onMove = pos.getOnMove();
		boolean isWhite = (onMove == Colour.white);
		SearchDebugAgent.printSearchPly(currPly,onMove);
		// Generate the move list
		List<GenericMove> ml = getMoveList(currPly);
		if (isMateOccurred(ml)) {
			int mateScore = sg.scoreMate(currPly, isWhite, initialOnMove);
			st.backupScore(currPly, mateScore);
		} else {
			// Initialise the score for this node and analyse the move list
			searchMoves(currPly, st.initScore(currPly,isWhite), ml);
		}
		return st.getBackedUpScore(currPly);
	}

	private List<GenericMove> getMoveList(int currPly)
			throws InvalidPieceException {
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

	void searchMoves(int currPly, int alphaBetaCutOff, List<GenericMove> ml) 
			throws InvalidPieceException {
		Iterator<GenericMove> move_iter = ml.iterator();
		while(move_iter.hasNext() && !isTerminated()) {
			GenericMove currMove = move_iter.next();
			int positionScore = applyMoveAndScore(currPly, currMove);
			if (handleBackupOfScore(currPly, alphaBetaCutOff, currMove, positionScore)) {
				break;
			}
		}
	}

	private boolean handleBackupOfScore(int currPly, int alphaBetaCutOff,
			GenericMove currMove, int positionScore) {
		boolean earlyTerminate = false;
		// 4) Evaluate the score
		if (isBackUpRequired(currPly, positionScore)) {
			// 4a) Back-up the position score and update the principal continuation...
			st.backupScore(currPly, positionScore);
			SearchDebugAgent.printBackUpScore(currPly, positionScore);
			pc.update(currPly, currMove);
			SearchDebugAgent.printPrincipalContinuation(currPly,pc);
			if (currPly == 0) {
				reportPrincipalContinuation(positionScore);
			}
		} else if (isAlphaBetaCutOff( alphaBetaCutOff, positionScore, currPly )) {
			// 4b) Perform an Alpha Beta algorithm cut-off
			SearchDebugAgent.printRefutationFound(currPly);
			earlyTerminate = true;
		}
		return earlyTerminate;
	}

	private int applyMoveAndScore(int currPly, GenericMove currMove)
			throws InvalidPieceException {
		int positionScore;
		reportNextMove(currPly, currMove);
		SearchDebugAgent.printPerformMove(currPly, currMove);
		pm.performMove(currMove);
		// Either recurse or evaluate a terminal position
		if ( isTerminalNode(currPly) ) {
			positionScore = pe.evaluatePosition(pos);
		} else {
			positionScore = searchPly(currPly+1);
		}
		SearchDebugAgent.printUndoMove(currPly, currMove);
		pm.unperformMove();
		sm.incrementNodesSearched();
		return positionScore;
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

	void reportNextMove(int currPly, GenericMove currMove) {
		if (currPly == 0) {
			sm.setCurrentMove(currMove);
			sm.incrementCurrentMoveNumber();
			sr.reportCurrentMove();
		}
	}

	boolean isBackUpRequired(int currPly, int positionScore) {
		boolean backUpScore = false;
		if (pos.getOnMove() == Colour.white) {
			// if white, maximise score
			if (positionScore > st.getBackedUpScore(currPly))
				backUpScore = true;
		} else {
			// if black, minimise score 
			if (positionScore < st.getBackedUpScore(currPly))
				backUpScore = true;
		}
		return backUpScore;
	}
	
	boolean isAlphaBetaCutOff(int cutOffValue, int positionScore, int currPly) {
		if ((cutOffValue != Integer.MAX_VALUE) && (cutOffValue != Integer.MIN_VALUE)) {
			int prevPlyScore = st.getBackedUpScore(currPly-1);
			Colour onMove = pos.getOnMove();
			if ((onMove == Colour.white && positionScore >= prevPlyScore) ||
				(onMove == Colour.black && positionScore <= prevPlyScore)) {
				// Indicates the search passed this node is refuted by an earlier move.
				return true;
			}
		}
		return false;
	}

	private boolean isTerminalNode(int currPly) {
		boolean isTerminalNode = false;
		if (currPly == (searchDepthPly-1)) {
			isTerminalNode = true;
		}
		return isTerminalNode;
	}
	
	synchronized void terminateFindMove() { terminate = true; }
	private synchronized boolean isTerminated() { return terminate; }	
}
