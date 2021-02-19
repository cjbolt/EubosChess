package eubos.score;

import com.fluxchess.jcpi.models.IntFile;

import eubos.board.Board;
import eubos.board.IForEachPieceCallback;
import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.Position;
import eubos.search.Score;
import eubos.search.SearchContext;
import eubos.search.SearchContext.SearchContextEvaluation;

public class PositionEvaluator implements IEvaluate, IForEachPieceCallback {

	IPositionAccessors pm;
	private SearchContext sc;
	
	public static final int DOUBLED_PAWN_HANDICAP = 20;
	public static final int ISOLATED_PAWN_HANDICAP = 33;
	public static final int BACKWARD_PAWN_HANDICAP = 12;
	
	public static final int PASSED_PAWN_BOOST = 33;
	public static final int ROOK_FILE_PASSED_PAWN_BOOST = 20;
	
	public static final int CONNECTED_PASSED_PAWN_BOOST = 75;
	
	public static final boolean ENABLE_QUIESCENCE_CHECK = true;
	public static final boolean ENABLE_PAWN_EVALUATION = true;
	public static final boolean ENABLE_KING_SAFETY_EVALUATION = true;
	public static final boolean ENABLE_DYNAMIC_POSITIONAL_EVALUATION = true;
	
	public PositionEvaluator(IPositionAccessors pm, ReferenceScore refScore) {	
		this.pm = pm;
		PiecewiseEvaluation mat = pm.getTheBoard().me;
		if (mat == null) {
			mat = pm.getTheBoard().evaluateMaterial();
		}
		sc = new SearchContext(pm, mat, refScore);
	}
	
	public boolean isQuiescent(int currMove) {
		if (!ENABLE_QUIESCENCE_CHECK)
			return true;
		if (Move.isPromotion(currMove) || pm.isPromotionPossible()) {
			return false;
		} else if (Move.isCapture(currMove)) {
		    return false;
		}
		return true;
	}
	
	public int evaluatePosition() {
		pm.getTheBoard().evaluateMaterial();
		SearchContextEvaluation eval = sc.computeSearchGoalBonus(pm.getTheBoard().me);
		if (!eval.isDraw) {
			eval.score += pm.getTheBoard().me.getDelta();
			if (ENABLE_PAWN_EVALUATION) {
				eval.score += evaluatePawnStructure();
			}
			if (ENABLE_KING_SAFETY_EVALUATION) {
				eval.score += evaluateKingSafety();
			}
		}
		return Score.valueOf(eval.score, Score.exact);
	}
	
	int evaluatePawnStructure() {
		boolean onMoveIsWhite = Colour.isWhite(pm.getOnMove());
		Board bd = pm.getTheBoard();
		int pawnEvaluationScore = 0;
		long pawnsToTest = onMoveIsWhite ? bd.getWhitePawns() : bd.getBlackPawns();
		if (pawnsToTest != 0x0) {
			pawnEvaluationScore = evaluatePawnsForColour(pm.getOnMove());
		}
		pawnsToTest = (!onMoveIsWhite) ? bd.getWhitePawns() : bd.getBlackPawns();
		if (pawnsToTest != 0x0) {
			pawnEvaluationScore += evaluatePawnsForColour(Colour.getOpposite(pm.getOnMove()));
		}
		return pawnEvaluationScore;
	}
	
	int evaluateKingSafety() {
		int kingSafetyScore = 0;
		boolean ownSideIsWhite = pm.onMoveIsWhite();
		kingSafetyScore = pm.getTheBoard().evaluateKingSafety(ownSideIsWhite);
		kingSafetyScore += pm.getTheBoard().evaluateKingSafety(!ownSideIsWhite);
		return kingSafetyScore;
	}
	
	Colour onMoveWas;
	int individualPawnEval = 0;
	
	@Override
	public void callback(int piece, int atPos) {
		if (pm.getTheBoard().isPassedPawn(atPos, onMoveWas)) {
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
		if (pm.getTheBoard().isIsolatedPawn(atPos, onMoveWas)) {
			individualPawnEval -= ISOLATED_PAWN_HANDICAP;
		} else if (pm.getTheBoard().isBackwardsPawn(atPos, onMoveWas)) {
			individualPawnEval -= BACKWARD_PAWN_HANDICAP;
		}
	}
	
	private int evaluatePawnsForColour(Colour onMoveWas) {
		Board board = pm.getTheBoard();
		this.onMoveWas = onMoveWas;
		this.individualPawnEval = 0;
		int pawnHandicap = -board.countDoubledPawnsForSide(onMoveWas)*DOUBLED_PAWN_HANDICAP;
		board.forEachPawnOfSide(this, Colour.isBlack(onMoveWas));
		if (Colour.isBlack(onMoveWas)) {
			pawnHandicap = -pawnHandicap;
			individualPawnEval = -individualPawnEval;
		}
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
