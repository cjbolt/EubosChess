package eubos.score;

public interface IEvaluate {
	int getFullEvaluation();
	int getStaticEvaluation();
	int lazyEvaluation(int alpha, int beta);
	
	void reportLazyStatistics();
	void reportPawnStatistics();
	
	boolean goForMate();
	int estimateMovePositionalContribution(int move);
}