package eubos.neural_net;

public interface IEvaluate {
	int getFullEvaluation();
	int getStaticEvaluation();
	int lazyEvaluation(int alpha, int beta);
	boolean goForMate();
	int estimateMovePositionalContribution(int move);
}