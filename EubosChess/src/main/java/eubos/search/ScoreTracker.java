package eubos.search;

public class ScoreTracker {
	private int[] scores;
	private boolean initialOnMoveIsWhite = false;
	
	private static final int MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF = 2;

	public ScoreTracker(int searchDepth, boolean isWhite) {
		scores = new int[searchDepth];
		initialOnMoveIsWhite = isWhite;
	}
	
	private void bringDownAlphaBetaCutOff(byte currPly) {
		scores[currPly] = scores[currPly-MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF];
	}

	private void initialiseWithWorstPossibleScore(byte currPly) {
		if (onMoveIsWhite(currPly)) {
			scores[currPly] = Score.valueOf(Score.lowerBound);
		} else {
			scores[currPly] = Score.valueOf(Score.upperBound);
		}
	}
	
	boolean onMoveIsWhite(byte currPly) {
		return ((currPly % 2) == 0) ? initialOnMoveIsWhite : !initialOnMoveIsWhite;
	}
	
	void setBackedUpScoreAtPly(byte currPly, int positionScore) {
		SearchDebugAgent.printBackUpScore(currPly, scores[currPly], positionScore);
		scores[currPly] = positionScore;
	}
	
	
	int getBackedUpScoreAtPly(byte currPly) {
		return scores[currPly];
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
			if (positionScore > Score.getScore(scores[currPly]) && positionScore != Short.MAX_VALUE)
				backUpScore = true;
		} else {
			// if black, minimise score 
			if (positionScore < Score.getScore(scores[currPly]) && positionScore != Short.MIN_VALUE)
				backUpScore = true;
		}
		return backUpScore;
	}
	
	public boolean isAlphaBetaCutOff(byte currPly, short scoreBackedUpToNode) {
		boolean isAlphaBetaCutOff = false;
		if (currPly > 0) {
			short prevPlyScore = Score.getScore(scores[(byte)(currPly-1)]);
			if (onMoveIsWhite(currPly)) {
				/* A note about these score comparisons: 
				 * 
				 *  The prevPlyScore is the best score backed up for the opponent of the side now on Move. If we have 
				 *  now backed up a score to the current position through the tree search which is
				 *  worse for the opponent, then we have discovered a refutation of THEIR last move.
				 *  
				 *  This isn't the same as the test to back up a score. That is the reason for unexpected comparison
				 *  (wrt. the usual backing up operation). This comparison is specific to alpha/beta pruning.
				 */
				if (scoreBackedUpToNode >= prevPlyScore) isAlphaBetaCutOff = true;
			} else {
				if (scoreBackedUpToNode <= prevPlyScore) isAlphaBetaCutOff = true;
			}
			if (isAlphaBetaCutOff) {
				SearchDebugAgent.printAlphaBetaComparison(prevPlyScore, scoreBackedUpToNode);
			}
		}
		return isAlphaBetaCutOff;
	}	
	
	public short adjustHashTableMateInXScore(byte currPly, short score) {
		if (Score.isMate(score)) {
			// The score stored in the hash table encodes the distance to the mate from the hashed position,
			// not the root node, so adjust for the position in search tree.
			score = (short) ((score < 0 ) ? score+currPly : score-currPly);
		}
		return score;
	}

	public boolean isAlphaBetaCutOffForHash(byte currPly, short hashScore) {
		boolean isAlphaBetaCutOff = false;
		if (currPly > 0) {
			
			short adjustedHashScore = adjustHashTableMateInXScore(currPly, hashScore);			
			short prevPlyScore = Score.getScore(scores[(byte)(currPly-1)]);
			if (onMoveIsWhite(currPly)) {
				/* A note about these score comparisons: 
				 * 
				 *  The prevPlyScore is the best score backed up for the opponent of the side now on Move. 
				 *  If the hash table for this position has a best score which is worse for the opponent, 
				 *  then we have discovered a refutation of THEIR last move.
				 *  
				 *  This isn't the same as the test to back up a score. That is the reason for unexpected comparison
				 *  (wrt. the usual backing up operation). This comparison is specific to alpha/beta pruning.
				 */
				if (adjustedHashScore >= prevPlyScore) isAlphaBetaCutOff = true;
			} else {
				if (adjustedHashScore <= prevPlyScore) isAlphaBetaCutOff = true;
			}
			
			if (isAlphaBetaCutOff) {
				SearchDebugAgent.printAlphaBetaComparison(hashScore, adjustedHashScore);
			}
		}
		return isAlphaBetaCutOff;
	}
}