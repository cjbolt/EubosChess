package eubos.score;

public interface IEvaluate {
	int getFullEvaluation();
	int getStaticEvaluation();
	int lazyEvaluation(int alpha, int beta);
	boolean goForMate();
}