package eubos.score;

public interface IEvaluate {
	int evaluatePosition();
	short getScoreForStalemate();
	String getGoal();
}