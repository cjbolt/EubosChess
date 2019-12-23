package eubos.search;

import eubos.search.Score.ScoreType;

public class ScoreTracker {
	private short[] scores;
	private ScoreType[] type;
	private boolean initialOnMoveIsWhite = false;
	
	private static final int MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF = 2;

	public ScoreTracker(int searchDepth, boolean isWhite) {
		scores = new short[searchDepth];
		type = new ScoreType[searchDepth];
		initialOnMoveIsWhite = isWhite;
	}
	
	private void bringDownAlphaBetaCutOff(byte currPly) {
		scores[currPly] = scores[currPly-MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF];
		type[currPly] = onMoveIsWhite(currPly) ? ScoreType.lowerBound : ScoreType.upperBound;
	}

	private void initialiseWithWorstPossibleScore(byte currPly) {
		if (onMoveIsWhite(currPly)) {
			scores[currPly] = Short.MIN_VALUE;
			type[currPly] = ScoreType.lowerBound;
		} else {
			scores[currPly] = Short.MAX_VALUE;
			type[currPly] = ScoreType.upperBound;
		}
	}
	
	boolean onMoveIsWhite(byte currPly) {
		return ((currPly % 2) == 0) ? initialOnMoveIsWhite : !initialOnMoveIsWhite;
	}
	
	void setBackedUpScoreAtPly(byte currPly, short positionScore, ScoreType scoreType) {
		SearchDebugAgent.printBackUpScore(currPly, scores[currPly], positionScore);
		scores[currPly]=positionScore;
		type[currPly]=scoreType;
	}
	
	
	Score getBackedUpScoreAtPly(byte currPly) {
		return new Score(scores[currPly],type[currPly]);
	}
	
	void setProvisionalScoreAtPly(byte currPly) {
		if (currPly<MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF) {
			initialiseWithWorstPossibleScore(currPly);
		} else {
			bringDownAlphaBetaCutOff(currPly);
		}
	}	

	boolean isBackUpRequired(byte currPly, short positionScore) {
		boolean backUpScore = false;
		if (onMoveIsWhite(currPly)) {
			// if white, maximise score
			if (positionScore > getBackedUpScoreAtPly(currPly).getScore() && positionScore != Short.MAX_VALUE)
				backUpScore = true;
		} else {
			// if black, minimise score 
			if (positionScore < getBackedUpScoreAtPly(currPly).getScore() && positionScore != Short.MIN_VALUE)
				backUpScore = true;
		}
		return backUpScore;
	}
	
	public boolean isAlphaBetaCutOff(byte currPly, short positionScore) {
		boolean isAlphaBetaCutOff = false;
		if ((scores[currPly] != Short.MAX_VALUE) && (scores[currPly] != Short.MIN_VALUE)) {
			if (currPly > 0) {
				short prevPlyScore = scores[(byte)(currPly-1)];
				if (onMoveIsWhite(currPly)) {
					/* A note about these score comparisons: 
					 * 
					 *  The prevPlyScore is for the opponent. If we have backed up a score to the current position
					 *  which is worse for the opponent, then we have discovered a refutation of THEIR last move.
					 *  This isn't the same as the test to back up a score. That is the reason for unexpected comparison
					 *  (wrt. the usual backing up operation). This comparison is specific to alpha/beta pruning.
					 */
					if (positionScore >= prevPlyScore) isAlphaBetaCutOff = true;
				} else {
					if (positionScore <= prevPlyScore) isAlphaBetaCutOff = true;
				}
				if (isAlphaBetaCutOff) {
					SearchDebugAgent.printAlphaBetaComparison(currPly, prevPlyScore, positionScore);
				}
			}
		}
		return isAlphaBetaCutOff;
	}

	public Object getBackedUpScoreType() {
		// TODO Auto-generated method stub
		return null;
	}	
}