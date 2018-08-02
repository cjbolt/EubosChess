package eubos.search;

class ScoreTracker {
	private int[] scores;
	private boolean initialOnMoveIsWhite = false;
	
	private static final int MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF = 2;

	ScoreTracker(int searchDepth, boolean isWhite) {
		scores = new int[searchDepth];
		initialOnMoveIsWhite = isWhite;
	}
	
	void bringDownAlphaBetaCutOff(int currPly) {
		scores[currPly] = scores[currPly-MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF];
	}

	void initialiseWithWorstPossibleScore(int currPly) {
		if (onMoveIsWhite(currPly)) {
			scores[currPly] = Integer.MIN_VALUE;
		} else {
			scores[currPly] = Integer.MAX_VALUE;
		}
	}
	
	boolean onMoveIsWhite(int currPly) {
		return ((currPly % 2) == 0) ? initialOnMoveIsWhite : !initialOnMoveIsWhite;
	}
	
	void setBackedUpScoreAtPly(int currPly, int positionScore) {
		scores[currPly]=positionScore;
	}
	
	
	int getBackedUpScoreAtPly(int currPly) {
		return scores[currPly];
	}
	
	void setProvisionalScoreAtPly(int currPly) {
		if (currPly<MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF) {
			initialiseWithWorstPossibleScore(currPly);
		} else {
			bringDownAlphaBetaCutOff(currPly);
		}
	}	
	
	int getProvisionalScoreAtPly(int currPly) {
		return scores[currPly];
	}	
}