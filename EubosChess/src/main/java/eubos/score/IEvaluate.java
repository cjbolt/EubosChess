package eubos.score;

public interface IEvaluate {
	int getCrudeEvaluation();
	int getFullEvaluation();
	short getScoreForStalemate();
	String getGoal();
}