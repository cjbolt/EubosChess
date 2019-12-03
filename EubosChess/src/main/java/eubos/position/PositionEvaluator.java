package eubos.position;

import java.util.Iterator;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.SquareAttackEvaluator;
import eubos.board.pieces.Piece.Colour;
import eubos.board.pieces.Piece.PieceType;
import eubos.position.CaptureData;
import eubos.search.DrawChecker;
import eubos.search.SearchContext;

public class PositionEvaluator implements IEvaluate {

	PositionManager pm;
	private SearchContext sc;
	
	public static final int HAS_CASTLED_BOOST_CENTIPAWNS = 150;
	public static final int DOUBLED_PAWN_HANDICAP = 50;
	public static final int PASSED_PAWN_BOOST = 30;
	public static final int ROOK_FILE_PASSED_PAWN_BOOST = 20;
	
	public PositionEvaluator(PositionManager pm, DrawChecker dc) {	
		this.pm = pm;
		sc = new SearchContext(pm, MaterialEvaluator.evaluate(pm.getTheBoard()), dc);
	}
	
	public boolean isQuiescent() {
		if (pm.isKingInCheck(pm.getOnMove())) {
			// In order to check for mates
			return false;
		} else if (pm.lastMoveWasCapture()) {
			// we could keep a capture list, so we know where we are in the exchange series?
			// we can get access to the captured piece in the current codebase, but we need to know the whole capture sequence to do swap off?
			CaptureData captured = pm.getCapturedPiece();
			if (captured != null)
			{
				if (SquareAttackEvaluator.isAttacked(
						pm.getTheBoard(),
						captured.square,
						Colour.getOpposite(pm.getOnMove())))
					return false;
			}
		}
		return true;
	}
	
	public short evaluatePosition() {
		MaterialEvaluation mat = MaterialEvaluator.evaluate(pm.getTheBoard());
		short score = mat.getDelta();
		score += sc.computeSearchGoalBonus(mat);
		score += encourageCastling();
		score += evaluatePawnStructure();
		return score;
	}
	
	int encourageCastling() {
		int castleScoreBoost = 0;
		Colour onMoveWas = Colour.getOpposite(pm.getOnMove());
		if (pm.hasCastled(onMoveWas)) {
			castleScoreBoost = HAS_CASTLED_BOOST_CENTIPAWNS;
		}
		if (onMoveWas == Colour.black) {
			castleScoreBoost = -castleScoreBoost;
		}
		return castleScoreBoost;
	}
	
	int evaluatePawnStructure() {
		int pawnEvaluationScore = evaluatePawnsForColour(pm.getOnMove());
		pawnEvaluationScore += evaluatePawnsForColour(Colour.getOpposite(pm.getOnMove()));
		return pawnEvaluationScore;
	}

	private int evaluatePawnsForColour(Colour onMoveWas) {
		Board board = pm.getTheBoard();
		int passedPawnBoost = 0;
		int pawnHandicap = -board.countDoubledPawnsForSide(onMoveWas)*DOUBLED_PAWN_HANDICAP;
		PieceType ownPawns = (onMoveWas==Colour.white) ? PieceType.WhitePawn : PieceType.BlackPawn;
		Iterator<GenericPosition> iter = board.iterateType(ownPawns);
		while (iter.hasNext()) {
			GenericPosition pawn = iter.next();
			if (board.isPassedPawn(pawn, onMoveWas)) {
				if (pawn.file == GenericFile.Fa || pawn.file == GenericFile.Fh) {
					passedPawnBoost += ROOK_FILE_PASSED_PAWN_BOOST;
				} else {
					passedPawnBoost += PASSED_PAWN_BOOST;
				}
			}
		}
		if (onMoveWas == Colour.black) {
			pawnHandicap = -pawnHandicap;
			passedPawnBoost = -passedPawnBoost;
		}
		return pawnHandicap + passedPawnBoost;
	}
}
