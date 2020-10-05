package eubos.score;

import eubos.board.Board;
import eubos.search.Score;

public interface IEvaluate {
	Score evaluatePosition();
	boolean isQuiescent();
	short getScoreForStalemate();
	MaterialEvaluation updateMaterialForDoMove(Board theBoard, int currMove);
	MaterialEvaluation updateMaterialForUndoMove(Board theBoard, int currMove);
}