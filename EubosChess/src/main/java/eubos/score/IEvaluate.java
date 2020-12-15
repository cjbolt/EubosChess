package eubos.score;

public interface IEvaluate {
	int evaluatePosition();
	boolean isQuiescent(int currMove);
	boolean isQuiescent(int currMove, boolean neededToEscapeCheck);
	short getScoreForStalemate();
	String getGoal();
}