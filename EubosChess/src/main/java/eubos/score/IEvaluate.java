package eubos.score;

import eubos.search.Score;

public interface IEvaluate {
	Score evaluatePosition();
	boolean isQuiescent(int currMove);
	short getScoreForStalemate();
	void invalidatePawnCache();
}