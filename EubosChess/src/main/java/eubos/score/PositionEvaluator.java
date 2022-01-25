package eubos.score;

import com.fluxchess.jcpi.models.IntFile;

import eubos.board.Board;
import eubos.board.IForEachPieceCallback;
import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.position.IPositionAccessors;
import eubos.position.Position;

public class PositionEvaluator implements IEvaluate, IForEachPieceCallback {

	IPositionAccessors pm;
	
	public static final int DOUBLED_PAWN_HANDICAP = 12;
	public static final int ISOLATED_PAWN_HANDICAP = 33;
	public static final int BACKWARD_PAWN_HANDICAP = 12;
	
	public static final int PASSED_PAWN_BOOST = 10;
	public static final int ROOK_FILE_PASSED_PAWN_BOOST = 6;
	
	public static final int CONNECTED_PASSED_PAWN_BOOST = 75;
	
	public static final boolean ENABLE_PAWN_EVALUATION = true;
	public static final boolean ENABLE_KING_SAFETY_EVALUATION = true;
	public static final boolean ENABLE_DYNAMIC_POSITIONAL_EVALUATION = true;
	
	public boolean isDraw;
	public short score;
	public Board bd;
	
	public PositionEvaluator(IPositionAccessors pm) {	
		this.pm = pm;
		bd = pm.getTheBoard();
	}
	
	private void initialise() {
		boolean threeFold = pm.isThreefoldRepetitionPossible();
		boolean insufficient = bd.isInsufficientMaterial();
		
		isDraw = (threeFold || insufficient);
		score = 0;
	}
	
	public int getCrudeEvaluation() {
		initialise();
		if (!isDraw) {
			score += pm.onMoveIsWhite() ? bd.me.getDelta() : -bd.me.getDelta();
			score += pm.onMoveIsWhite() ? bd.me.getPosition() : -bd.me.getPosition();
		}
		return score;
	}
	
	public int getFullEvaluation() {
		initialise();
		PiecewiseEvaluation me = bd.me;
		if (PositionEvaluator.ENABLE_DYNAMIC_POSITIONAL_EVALUATION) {
			bd.calculateDynamicMobility(me);
		}
		if (!isDraw) {
			score += pm.onMoveIsWhite() ? me.getDelta() : -me.getDelta();
			score += pm.onMoveIsWhite() ? bd.me.getPosition() : -bd.me.getPosition();
			if (ENABLE_PAWN_EVALUATION) {
				score += evaluatePawnStructure();
			}
			if (ENABLE_KING_SAFETY_EVALUATION && !bd.isEndgame) {
				score += evaluateKingSafety();
			}
		}
		return score;
	}
	
	int evaluatePawnStructure() {
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
		if (bd.isPassedPawn(atPos, onMoveIs)) {
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
		if (bd.isIsolatedPawn(atPos, onMoveIs)) {
			individualPawnEval -= ISOLATED_PAWN_HANDICAP;
		} else if (bd.isBackwardsPawn(atPos, onMoveIs)) {
			individualPawnEval -= BACKWARD_PAWN_HANDICAP;
		}
	}
	
	private int evaluatePawnsForColour(Colour onMove) {
		this.onMoveIs = onMove;
		this.individualPawnEval = 0;
		int pawnHandicap = -bd.countDoubledPawnsForSide(onMove)*DOUBLED_PAWN_HANDICAP;
		bd.forEachPawnOfSide(this, Colour.isBlack(onMove));
		return pawnHandicap + individualPawnEval;
	}
}
