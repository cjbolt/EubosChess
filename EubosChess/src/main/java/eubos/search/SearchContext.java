package eubos.search;

import eubos.board.pieces.Piece;
import eubos.board.pieces.Piece.Colour;
import eubos.position.IPositionAccessors;
import eubos.position.MaterialEvaluation;
import eubos.position.MaterialEvaluator;

public class SearchContext {
	MaterialEvaluation initial;
	IPositionAccessors pos;
	Piece.Colour initialOnMove;
	SearchGoal goal;
	DrawChecker dc;
	
	static final short SIMPLIFY_THRESHOLD = 100;
	static final short DRAW_THRESHOLD = -200;
	
	static final short SIMPLIFICATION_BONUS = 75;
	static final short AVOID_DRAW_HANDICAP = -400;
	
	private enum SearchGoal {
		try_for_win,
		simplify,
		try_for_draw
	};
	
	public SearchContext(IPositionAccessors pos, MaterialEvaluation initialMaterial, DrawChecker dc) {
		this.pos = pos;
		this.dc = dc;
		initial = initialMaterial;
		initialOnMove = pos.getOnMove();
		setGoal();
	}

	private void setGoal() {
		if ((initialOnMove.equals(Colour.white) && initial.getDelta() > SIMPLIFY_THRESHOLD) ||
			(initialOnMove.equals(Colour.black) && initial.getDelta() < -SIMPLIFY_THRESHOLD )) {
			goal = SearchGoal.simplify;
		} else if ((initialOnMove.equals(Colour.white) && initial.getDelta() < DRAW_THRESHOLD) ||
				(initialOnMove.equals(Colour.black) && initial.getDelta() > -DRAW_THRESHOLD )) {
			goal = SearchGoal.try_for_draw;
		} else {
			goal = SearchGoal.try_for_win;
		}
	}
	
	public short computeSearchGoalBonus(MaterialEvaluation current) {
		short bonus = 0;
		// If we just moved, score as according to our game plan
		if (pos.getOnMove().equals(Colour.getOpposite(initialOnMove))) {
			switch(goal) {
			case simplify:
				if (dc.isPositionDraw(pos.getHash())) {
					bonus += AVOID_DRAW_HANDICAP;
				} else if (isPositionSimplified(current)) {
					bonus += SIMPLIFICATION_BONUS;
				}
				break;
			case try_for_win:
				if (dc.isPositionDraw(pos.getHash())) {
					bonus += AVOID_DRAW_HANDICAP;
				}
				break;
			case try_for_draw:
				if (dc.isPositionDraw(pos.getHash())) {
					bonus += MaterialEvaluator.MATERIAL_VALUE_KING/2;
				}
				break;
			default:
				break;
			}
			if (initialOnMove.equals(Colour.black)) {
				bonus = (short)-bonus;
			}
		} else {
			switch(goal) {
			case simplify:
			case try_for_win:
				if (dc.isPositionDraw(pos.getHash())) {
					// Assume opponent wants a draw.
					bonus += MaterialEvaluator.MATERIAL_VALUE_KING/2;
				}
				break;
			case try_for_draw:
				// If we are trying for a draw and evaluating opponents move, score a draw as bad for them
				if (dc.isPositionDraw(pos.getHash())) {
					bonus += AVOID_DRAW_HANDICAP;
				}
				break;
			default:
				break;
			}
			// we are evaluating after the move, so if opponent is black, invert score
			if (pos.getOnMove().equals(Colour.white)) {
				bonus = (short)-bonus;
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
