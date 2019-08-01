package eubos.search;

class ScoreTracker {
	private int[] scores;
	private boolean initialOnMoveIsWhite = false;
	
	private static final int MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF = 2;

	ScoreTracker(int searchDepth, boolean isWhite) {
		scores = new int[searchDepth];
		initialOnMoveIsWhite = isWhite;
	}
	
	private void bringDownAlphaBetaCutOff(int currPly) {
		scores[currPly] = scores[currPly-MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF];
	}

	private void initialiseWithWorstPossibleScore(int currPly) {
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
		SearchDebugAgent.printBackUpScore(currPly, positionScore);
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

	boolean isBackUpRequired(int currPly, int positionScore) {
		boolean backUpScore = false;
		if (onMoveIsWhite(currPly)) {
			// if white, maximise score
			if (positionScore > getBackedUpScoreAtPly(currPly))
				backUpScore = true;
		} else {
			// if black, minimise score 
			if (positionScore < getBackedUpScoreAtPly(currPly))
				backUpScore = true;
		}
		return backUpScore;
	}
	
	boolean isAlphaBetaCutOff(int currPly, int nodeProvisionalScore, int positionScore) throws IllegalArgumentException {
		if ((nodeProvisionalScore != Integer.MAX_VALUE) && (nodeProvisionalScore != Integer.MIN_VALUE)) {
			if (currPly <= 0) throw new IllegalArgumentException();
			int prevPlyScore = getBackedUpScoreAtPly(currPly-1);
			if (onMoveIsWhite(currPly)) {
				/* A note about these score comparisons: 
				 * 
				 *  The prevPlyScore is for the opponent. If we have backed up a score to the current position
				 *  which is worse for the opponent, then we have discovered a refutation of THEIR last move.
				 *  This isn't the same as the test to back up a score. That is the reason for unexpected comparison
				 *  (wrt. the usual backing up operation). This comparison is specific to alpha/beta pruning.
				 */
				if (positionScore >= prevPlyScore) return true;
			} else {
				if (positionScore <= prevPlyScore) return true;
			}
		}
		return false;
	}	
}