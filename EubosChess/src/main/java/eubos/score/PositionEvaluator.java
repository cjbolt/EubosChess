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
	
	public PositionEvaluator(IPositionAccessors pm) {	
		this.pm = pm;
		sc = new SearchContext(pm, pm.getTheBoard().evaluateMaterial());
	}
	
	public boolean isQuiescent(int currMove) {
		if (DISABLE_QUIESCENCE_CHECK)
			return true;
		if (Move.isCheck(currMove)) {
			return false;
		} else if (Move.isPromotion(currMove) || pm.isPromotionPossible()) {
			return false;
		} else if (Move.isCapture(currMove)) {
			// we could keep a capture list, so we know where we are in the exchange series?
			// we can get access to the captured piece in the current codebase, but we need to know the whole capture sequence to do swap off?
			if (Move.getTargetPiece(currMove) != Piece.NONE)
			{
				if (SquareAttackEvaluator.isAttacked(
						pm.getTheBoard(),
						Move.getTargetPosition(currMove),
						pm.getOnMove()))
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
		}
		return new Score(eval.score, Score.exact);
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
