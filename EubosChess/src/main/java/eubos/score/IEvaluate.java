package eubos.score;

public interface IEvaluate {
	int getCrudeEvaluation();
	int getFullEvaluation();
	int lazyEvaluation(int crudeEval, int alpha, int beta);
	void reportLazyStatistics();
	boolean goForMate();
}