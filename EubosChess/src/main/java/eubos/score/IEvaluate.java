package eubos.score;

import eubos.search.Score;

public interface IEvaluate {
	Score evaluatePosition();
	boolean isQuiescent();
	short getScoreForStalemate();
	//void invalidatePawnCache();
}