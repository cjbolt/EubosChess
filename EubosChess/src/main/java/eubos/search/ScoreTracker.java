package eubos.search;

class ScoreTracker {
	private int[] scores;

	ScoreTracker(int searchDepth) {
		scores = new int[searchDepth];
	}
	
	int initNodeScoreAlphaBeta(int currPly, boolean white) {
		// Initialise score at this node
		if (currPly==0 || currPly==1) {
			if (white) {
				scores[currPly] = Integer.MIN_VALUE;
			} else {
				scores[currPly] = Integer.MAX_VALUE;
			}
		} else {
			// alpha beta algorithm: bring down score from 2 levels up tree
			// TODO: debug and logging?
			//debug.printAlphaBetaCutOffLimit(currPly, score.scores[currPly-2]);
			scores[currPly] = scores[currPly-2];
		}
		return scores[currPly];
	}
	
	void backupScore(int currPly, int positionScore) {
		scores[currPly]=positionScore;
	}
	
	
	int getBestScoreAtPly(int currPly) {
		return scores[currPly];
	}
}