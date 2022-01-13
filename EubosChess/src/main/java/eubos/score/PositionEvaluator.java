package eubos.score;

import com.fluxchess.jcpi.models.IntFile;

import eubos.board.Board;
import eubos.board.IForEachPieceCallback;
import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.position.IPositionAccessors;
import eubos.position.Position;
import eubos.search.SearchContext;
import eubos.search.SearchContext.SearchContextEvaluation;

public class PositionEvaluator implements IEvaluate, IForEachPieceCallback {

	IPositionAccessors pm;
	private SearchContext sc;
	
	public static final int DOUBLED_PAWN_HANDICAP = 20;
	public static final int ISOLATED_PAWN_HANDICAP = 33;
	public static final int BACKWARD_PAWN_HANDICAP = 12;
	
	public static final int PASSED_PAWN_BOOST = 18;
	public static final int ROOK_FILE_PASSED_PAWN_BOOST = 12;
	
	public static final int CONNECTED_PASSED_PAWN_BOOST = 75;
	
	public static final boolean ENABLE_PAWN_EVALUATION = true;
	public static final boolean ENABLE_KING_SAFETY_EVALUATION = true;
	public static final boolean ENABLE_DYNAMIC_POSITIONAL_EVALUATION = true;
	
	public PositionEvaluator(IPositionAccessors pm, ReferenceScore refScore) {	
		this.pm = pm;
		Board bd = pm.getTheBoard();
		PiecewiseEvaluation mat = bd.me;
		if (mat == null) {
			mat = pm.getTheBoard().evaluateMaterial();
			mat.clearDynamicPosition();
			if (PositionEvaluator.ENABLE_DYNAMIC_POSITIONAL_EVALUATION && !bd.isEndgame) {
				bd.calculateDynamicMobility(mat);
			}
		}
		sc = new SearchContext(pm, mat, refScore);
	}
	
	public int evaluatePosition() {
		Board bd = pm.getTheBoard();
		bd.me.clearDynamicPosition();
		bd.evaluateMaterial();
		return getFullEvaluation();
	}
	
	public int getCrudeEvaluation() {
		Board bd = pm.getTheBoard();
		SearchContextEvaluation eval = sc.computeSearchGoalBonus(bd.me);
		if (!eval.isDraw) {
			eval.score += pm.onMoveIsWhite() ? bd.me.getDelta() : -bd.me.getDelta();
		}
		return eval.score;
	}
	
	public int getFullEvaluation() {
		Board bd = pm.getTheBoard();
		PiecewiseEvaluation me = bd.me;
		if (PositionEvaluator.ENABLE_DYNAMIC_POSITIONAL_EVALUATION && !bd.isEndgame) {
			bd.calculateDynamicMobility(me);
		}
		SearchContextEvaluation eval = sc.computeSearchGoalBonus(me);
		if (!eval.isDraw) {
			eval.score += pm.onMoveIsWhite() ? me.getDelta() : -me.getDelta();
			if (ENABLE_PAWN_EVALUATION) {
				eval.score += evaluatePawnStructure();
			}
			if (ENABLE_KING_SAFETY_EVALUATION && !sc.isTryForMate()) {
				eval.score += evaluateKingSafety();
			}
		}
		return eval.score;
	}
	
	int evaluatePawnStructure() {
		Board bd = pm.getTheBoard();
		int pawnEvaluationScore = 0;
		long pawnsToTest = pm.onMoveIsWhite() ? bd.getWhitePawns() : bd.getBlackPawns();
		if (pawnsToTest != 0x0) {
			pawnEvaluationScore = evaluatePawnsForColour(pm.getOnMove());
		}
		pawnsToTest = (!pm.onMoveIsWhite()) ? bd.getWhitePawns() : bd.getBlackPawns();
		if (pawnsToTest != 0x0) {
			pawnEvaluationScore -= evaluatePawnsForColour(Colour.getOpposite(pm.getOnMove()));
		}
		return pawnEvaluationScore;
	}
	
	int evaluateKingSafety() {
		int kingSafetyScore = 0;
		kingSafetyScore = pm.getTheBoard().evaluateKingSafety(pm.getOnMove());
		kingSafetyScore -= pm.getTheBoard().evaluateKingSafety(Piece.Colour.getOpposite(pm.getOnMove()));
		return kingSafetyScore;
	}
	
	Colour onMoveIs;
	int individualPawnEval = 0;
	
	@Override
	public void callback(int piece, int atPos) {
		if (pm.getTheBoard().isPassedPawn(atPos, onMoveIs)) {
			int weighting = 1;
			if (Piece.isBlack(piece)) {
				weighting = 7-Position.getRank(atPos);
			} else {
				weighting = Position.getRank(atPos);
			}
			if (Position.getFile(atPos) == IntFile.Fa || Position.getFile(atPos) == IntFile.Fh) {
				individualPawnEval += weighting*ROOK_FILE_PASSED_PAWN_BOOST;
			} else {
				individualPawnEval += weighting*PASSED_PAWN_BOOST;
			}
		}
		if (pm.getTheBoard().isIsolatedPawn(atPos, onMoveIs)) {
			individualPawnEval -= ISOLATED_PAWN_HANDICAP;
		} else if (pm.getTheBoard().isBackwardsPawn(atPos, onMoveIs)) {
			individualPawnEval -= BACKWARD_PAWN_HANDICAP;
		}
	}
	
	private int evaluatePawnsForColour(Colour onMove) {
		Board board = pm.getTheBoard();
		this.onMoveIs = onMove;
		this.individualPawnEval = 0;
		int pawnHandicap = -board.countDoubledPawnsForSide(onMove)*DOUBLED_PAWN_HANDICAP;
		board.forEachPawnOfSide(this, Colour.isBlack(onMove));
		return pawnHandicap + individualPawnEval;
	}
	
	public SearchContext getSearchContext() {
		return this.sc;
	}

	@Override
	public short getScoreForStalemate() {
		return sc.getScoreForStalemate();
	}

	@Override
	public String getGoal() {
		return sc.getGoal();
	}
}
