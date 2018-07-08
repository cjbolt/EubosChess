package eubos.search;

import eubos.board.PositionManager;
import eubos.board.pieces.Piece.Colour;
import eubos.search.MaterialEvaluator;

public class ScoreGenerator {
	
	int searchDepth;
	MaterialEvaluator me;
	
	static final int PLIES_PER_MOVE = 2;
	
	ScoreGenerator(int searchDepth) {
		this.searchDepth = searchDepth;	
		this.me = new MaterialEvaluator();
	}
	
	int getScoreForStalemate() {
		// Avoid stalemates by giving them a large penalty score.
		return -MaterialEvaluator.KING_VALUE;
	}
	
	int generateScoreForCheckmate(int currPly) {
		// Favour earlier mates (i.e. Mate-in-one over mate-in-three) by giving them a larger score.
		int totalMovesSearched = searchDepth/PLIES_PER_MOVE;
		int mateMoveNum = (currPly-1)/PLIES_PER_MOVE; // currPly-1 because mate was caused by the move from the previousPly
		int multiplier = totalMovesSearched-mateMoveNum;
		return multiplier*MaterialEvaluator.KING_VALUE;
	}
	
	int generateScoreForPosition(PositionManager pm) {
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
