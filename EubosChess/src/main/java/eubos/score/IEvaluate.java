package eubos.score;

import eubos.search.Score;

public interface IEvaluate {
	Score evaluatePosition();
	boolean isQuiescent();
	short getScoreForStalemate();
	MaterialEvaluation updateMaterialForDoMove(int currMove);
	MaterialEvaluation updateMaterialForUndoMove(int currMove);
}