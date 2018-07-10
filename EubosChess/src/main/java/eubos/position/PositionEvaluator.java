package eubos.position;

import eubos.board.pieces.Piece.Colour;

public class PositionEvaluator {
	
	MaterialEvaluator me;
	
	public PositionEvaluator() {	
		this.me = new MaterialEvaluator();
	}
	
	public int evaluatePosition(IPositionAccessors pos) {
		int score = me.evaluate(pos.getTheBoard());
		score += encourageCastling(pos);
		return score;
	}
	
	private int encourageCastling(IPositionAccessors pos) {
		int castleScoreBoost = 0;
		Colour onMoveWas = (pos.getOnMove() == Colour.black) ? Colour.white : Colour.black;
		if (pos.hasCastled(onMoveWas)) {
			castleScoreBoost = 50;
		}
		if (onMoveWas == Colour.black) {
			castleScoreBoost = -castleScoreBoost;
		}
		return castleScoreBoost;
	}
}
