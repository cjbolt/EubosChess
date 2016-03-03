package eubos.search;

class ScoreTracker {
	private int[] scores;

	ScoreTracker(int searchDepth) {
		scores = new int[searchDepth];
	}
	
	int initScore(int currPly, boolean white) {
		if (currPly==0 || currPly==1) {
			// Will get overwritten immediately...
			if (white) {
				scores[currPly] = Integer.MIN_VALUE;
			} else {
				scores[currPly] = Integer.MAX_VALUE;
			}
		} else {
			// Alpha Beta algorithm: bring down score from 2 levels up tree
			scores[currPly] = scores[currPly-2];
		}
		return scores[currPly];
	}
	
	void backupScore(int currPly, int positionScore) {
		scores[currPly]=positionScore;
	}
	
	
	int getBackedUpScore(int currPly) {
		return scores[currPly];
	}
}