package eubos.score;

public interface IEvaluate {
	int evaluatePosition();
	boolean isQuiescent(int currMove);
	short getScoreForStalemate();
	String getGoal();
}