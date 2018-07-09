package eubos.position;

import eubos.board.pieces.Piece.Colour;

public class PositionEvaluator {
	
	MaterialEvaluator me;
	
	public PositionEvaluator() {	
		this.me = new MaterialEvaluator();
	}
	
	public int evaluatePosition(PositionManager pm) {
		int score = me.evaluate(pm.getTheBoard());
		score += encourageCastling(pm);
		return score;
	}
	
	private int encourageCastling(PositionManager pm) {
		int castleScoreBoost = 0;
		Colour onMoveWas = (pm.getOnMove() == Colour.black) ? Colour.white : Colour.black;
		if (pm.hasCastled(onMoveWas)) {
			castleScoreBoost = 50;
		}
		if (onMoveWas == Colour.black) {
			castleScoreBoost = -castleScoreBoost;
		}
		return castleScoreBoost;
	}
}
