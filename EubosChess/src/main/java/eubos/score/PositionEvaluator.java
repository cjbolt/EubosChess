package eubos.score;

import java.util.Iterator;

import com.fluxchess.jcpi.models.IntFile;

import eubos.board.Board;
import eubos.board.SquareAttackEvaluator;
import eubos.board.Piece.Colour;
import eubos.board.Piece.PieceType;
import eubos.position.CaptureData;
import eubos.position.Position;
import eubos.position.PositionManager;
import eubos.search.DrawChecker;
import eubos.search.SearchContext;

public class PositionEvaluator implements IEvaluate {

	PositionManager pm;
	private SearchContext sc;
	private DrawChecker dc;
	
	public static final int HAS_CASTLED_BOOST_CENTIPAWNS = 50;
	public static final int DOUBLED_PAWN_HANDICAP = 50;
	public static final int PASSED_PAWN_BOOST = 30;
	public static final int ROOK_FILE_PASSED_PAWN_BOOST = 20;
	
	public PositionEvaluator(PositionManager pm, DrawChecker dc) {	
		this.pm = pm;
		sc = new SearchContext(pm, MaterialEvaluator.evaluate(pm.getTheBoard()), dc);
		this.dc = dc;
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
						Position.valueOf(captured.getSquare()),
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
		if (Colour.isBlack(onMoveWas)) {
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
		PieceType ownPawns = Colour.isWhite(onMoveWas) ? PieceType.WhitePawn : PieceType.BlackPawn;
		Iterator<Integer> iter = board.iterateType(ownPawns);
		while (iter.hasNext()) {
			int pawn = iter.next();
			if (board.isPassedPawn(pawn, onMoveWas)) {
				if (Position.getFile(pawn) == IntFile.Fa || Position.getFile(pawn) == IntFile.Fh) {
					passedPawnBoost += ROOK_FILE_PASSED_PAWN_BOOST;
				} else {
					passedPawnBoost += PASSED_PAWN_BOOST;
				}
			}
		}
		if (Colour.isBlack(onMoveWas)) {
			pawnHandicap = -pawnHandicap;
			passedPawnBoost = -passedPawnBoost;
		}
		return pawnHandicap + passedPawnBoost;
	}

	public MaterialEvaluation getMaterialEvaluation() {
		return MaterialEvaluator.evaluate(pm.getTheBoard());
	}

	@Override
	public boolean isThreeFoldRepetition(Long hashCode) {
		return dc.isPositionDraw(hashCode);
	}
	
	public SearchContext getSearchContext() {
		return this.sc;
	}
}
