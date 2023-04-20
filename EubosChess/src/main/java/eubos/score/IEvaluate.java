package eubos.score;

public interface IEvaluate {
	int getCrudeEvaluation();
	int getFullEvaluation();
	int lazyEvaluation(int alpha, int beta);
	void reportLazyStatistics();
	void reportPawnStatistics();
	boolean goForMate();
	int estimateMovePositionalContribution(int move);
	public int getFullEvalNotCheckingForDraws();
}