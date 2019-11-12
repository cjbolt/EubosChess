package eubos.search;

import eubos.board.pieces.Piece;
import eubos.board.pieces.Piece.Colour;
import eubos.position.IPositionAccessors;
import eubos.position.MaterialEvaluation;

public class SearchContext {
	MaterialEvaluation initial;
	IPositionAccessors pos;
	Piece.Colour initialOnMove;
	SearchGoal goal;
	
	static final short SIMPLIFY_THRESHOLD = 100;
	static final short DRAW_THRESHOLD = -200;
	
	static final short SIMPLIFICATION_BONUS = 75;
	
	private enum SearchGoal {
		simplify,
		draw
	};
	
	public SearchContext(IPositionAccessors pos, MaterialEvaluation initialMaterial) {
		this.pos = pos;
		initial = initialMaterial;
		initialOnMove = pos.getOnMove();
		if ((initialOnMove.equals(Colour.white) && initial.getDelta() > SIMPLIFY_THRESHOLD) ||
			(initialOnMove.equals(Colour.black) && Math.abs(initial.getDelta()) > SIMPLIFY_THRESHOLD )) {
			goal = SearchGoal.simplify;
		}
	}
	
	public short computeSearchGoalBonus(MaterialEvaluation current) {
		short bonus = 0;
		if (goal == SearchGoal.simplify) {
			if (isPositionSimplified(current)) {
				bonus += SIMPLIFICATION_BONUS;
			}
		}
		return bonus;
	}
	
	private boolean isPositionSimplified(MaterialEvaluation current) {
		boolean isSimplification = false;
		if (initialOnMove.equals(Colour.white)) {
			if ((initial.getDelta() <= current.getDelta()) && initial.getWhite() > current.getWhite()) {
				isSimplification = true;
			}
		} else {
			if ((initial.getDelta() >= current.getDelta()) && initial.getBlack() > current.getBlack()) {
				isSimplification = true;
			}			
		}
		return isSimplification;
	}
}
