package eubos.score;

import java.util.PrimitiveIterator;

import com.fluxchess.jcpi.models.IntFile;

import eubos.board.Board;
import eubos.board.Piece;
import eubos.board.SquareAttackEvaluator;
import eubos.board.Piece.Colour;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.Position;
import eubos.search.Score;
import eubos.search.SearchContext;
import eubos.search.SearchContext.SearchContextEvaluation;

public class PositionEvaluator implements IEvaluate {

	IPositionAccessors pm;
	private SearchContext sc;
	
	public static final int DOUBLED_PAWN_HANDICAP = 33;
	public static final int PASSED_PAWN_BOOST = 30;
	public static final int ROOK_FILE_PASSED_PAWN_BOOST = 20;
	
	public static final boolean DISABLE_QUIESCENCE_CHECK = false;
	public static final boolean ENABLE_PAWN_EVALUATION = false; 
	
	public PositionEvaluator(IPositionAccessors pm) {	
		this.pm = pm;
		sc = new SearchContext(pm, pm.getTheBoard().evaluateMaterial());
	}
	
	public boolean isQuiescent(int currMove) {
		return isQuiescent(currMove, false);
	}
	
	public boolean isQuiescent(int currMove, boolean neededToEscapeCheck) {
		if (DISABLE_QUIESCENCE_CHECK)
			return true;
		if (Move.isCheck(currMove) || (neededToEscapeCheck && Piece.isKing(Move.getOriginPiece(currMove)))) {
			// This condition indicates a potentially highly forced move, need to check for move the defender type en-prise captures */
			return false;
		} else if (Move.isPromotion(currMove) || pm.isPromotionPossible()) {
			return false;
		} else if (Move.isCapture(currMove)) {
			// It isn't quiescent if a recapture is possible
			if (SquareAttackEvaluator.isAttacked(
					pm.getTheBoard(),
					Move.getTargetPosition(currMove),
					pm.getOnMove())) {
				return false;
			}
		}
		return true;
	}
	
	public Score evaluatePosition() {
		pm.getTheBoard().evaluateMaterial();
		SearchContextEvaluation eval = sc.computeSearchGoalBonus(pm.getTheBoard().me);
		if (!eval.isDraw) {
			eval.score += pm.getTheBoard().me.getDelta();
			if (ENABLE_PAWN_EVALUATION) {
				eval.score += evaluatePawnStructure();
			}
		}
		return new Score(eval.score, Score.exact);
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

	private int evaluatePawnsForColour(Colour onMoveWas) {
		Board board = pm.getTheBoard();
		int passedPawnBoost = 0;
		int pawnHandicap = -board.countDoubledPawnsForSide(onMoveWas)*DOUBLED_PAWN_HANDICAP;
		int ownPawns = Colour.isWhite(onMoveWas) ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
		PrimitiveIterator.OfInt iter = board.iterateType(ownPawns);
		while (iter.hasNext()) {
			int pawn = iter.nextInt();
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
	
	public SearchContext getSearchContext() {
		return this.sc;
	}

	@Override
	public short getScoreForStalemate() {
		return sc.getScoreForStalemate();
	}
}
