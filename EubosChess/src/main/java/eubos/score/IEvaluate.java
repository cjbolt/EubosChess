package eubos.score;

public interface IEvaluate {
	int evaluatePosition();
	int getCrudeEvaluation();
	int getFullEvaluation();
	short getScoreForStalemate();
	String getGoal();
}