package eubos.search;

import eubos.main.EubosEngineMain;

public class ScoreTracker {
	private Score[] scores;
	private boolean initialOnMoveIsWhite = false;
	
	private static final int MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF = 2;

	public ScoreTracker(int searchDepth, boolean isWhite) {
		scores = new Score[searchDepth];
		initialOnMoveIsWhite = isWhite;
	}
	
	private void bringDownAlphaBetaCutOff(byte currPly) {
		scores[currPly] = new Score(
				scores[currPly-MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF].getScore(), 
				onMoveIsWhite(currPly) ? Score.lowerBound : Score.upperBound);
	}

	private void initialiseWithWorstPossibleScore(byte currPly) {
		if (onMoveIsWhite(currPly)) {
			scores[currPly] = new Score(Short.MIN_VALUE, Score.lowerBound);
		} else {
			scores[currPly] = new Score(Short.MAX_VALUE, Score.upperBound);
		}
	}
	
	boolean onMoveIsWhite(byte currPly) {
		return ((currPly % 2) == 0) ? initialOnMoveIsWhite : !initialOnMoveIsWhite;
	}
	
	void setBackedUpScoreAtPly(byte currPly, Score positionScore) {
		if (scores[currPly] != null) {
			SearchDebugAgent.printBackUpScore(currPly, scores[currPly].getScore(), positionScore.getScore());
		}
		scores[currPly]=new Score(positionScore.getScore(), positionScore.getType());
	}
	
	
	Score getBackedUpScoreAtPly(byte currPly) {
		return scores[currPly];
	}
	
	void setProvisionalScoreAtPly(byte currPly) {
		if (currPly<MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF) {
			initialiseWithWorstPossibleScore(currPly);
		} else {
			bringDownAlphaBetaCutOff(currPly);
		}
	}	

	boolean isBackUpRequired(byte currPly, Score positionScore) {
		boolean backUpScore = false;
		if (onMoveIsWhite(currPly)) {
			// if white, maximise score
			if (positionScore.getScore() > getBackedUpScoreAtPly(currPly).getScore() && positionScore.getScore() != Short.MAX_VALUE)
				backUpScore = true;
		} else {
			// if black, minimise score 
			if (positionScore.getScore() < getBackedUpScoreAtPly(currPly).getScore() && positionScore.getScore() != Short.MIN_VALUE)
				backUpScore = true;
		}
		return backUpScore;
	}
	
	public boolean isAlphaBetaCutOff(byte currPly, Score scoreBackedUpToNode) {
		boolean isAlphaBetaCutOff = false;
		if (currPly > 0) {
			Score prevPlyScore = scores[(byte)(currPly-1)];
			if (EubosEngineMain.ASSERTS_ENABLED)
				assert prevPlyScore != null;
			if (onMoveIsWhite(currPly)) {
				/* A note about these score comparisons: 
				 * 
				 *  The prevPlyScore is the best score backed up for the opponent of the side now on Move. If we have 
				 *  now backed up a score to the current position (either through a hash hit or tree search) which is
				 *  worse for the opponent, then we have discovered a refutation of THEIR last move.
				 *  
				 *  This isn't the same as the test to back up a score. That is the reason for unexpected comparison
				 *  (wrt. the usual backing up operation). This comparison is specific to alpha/beta pruning.
				 */
				if (scoreBackedUpToNode.getScore() >= prevPlyScore.getScore()) isAlphaBetaCutOff = true;
			} else {
				if (scoreBackedUpToNode.getScore() <= prevPlyScore.getScore()) isAlphaBetaCutOff = true;
			}
			if (isAlphaBetaCutOff) {
				SearchDebugAgent.printAlphaBetaComparison(prevPlyScore.getScore(), scoreBackedUpToNode.getScore());
			}
		}
		return isAlphaBetaCutOff;
	}	
	
	public short adjustHashTableMateInXScore(byte currPly, short score) {
		if (currPly != 0) {
			if (Math.abs(score) > Short.MAX_VALUE-100) {
				// Indicates hash table score was mate-in-X, adjust the score according to this depth position in the search tree
				int move_num = (currPly+1)/2;
				score = (short) ((score < 0 ) ? score+move_num : score-move_num);
			}
		}
		return score;
	}

	public boolean isAlphaBetaCutOffForHash(byte currPly, short hashScore) {
		boolean isAlphaBetaCutOff = false;
		short adjustedHashScore = adjustHashTableMateInXScore(currPly, hashScore);
		Score broughtDownScore = scores[currPly];
		/*  
		 * This uses a complete different scheme for when we are working out if a hashed move is a refutation as opposed
		 * to that from a regular backup.
		 */
		if (onMoveIsWhite(currPly)) {
			//if (adjustedHashScore >= broughtDownScore.getScore()) isAlphaBetaCutOff = true;
			if (adjustedHashScore <= broughtDownScore.getScore()) isAlphaBetaCutOff = true;
		} else {
			//if (adjustedHashScore <= broughtDownScore.getScore()) isAlphaBetaCutOff = true;
			if (adjustedHashScore >= broughtDownScore.getScore()) isAlphaBetaCutOff = true;
		}
		if (isAlphaBetaCutOff) {
			SearchDebugAgent.printAlphaBetaComparison(broughtDownScore.getScore(), adjustedHashScore);
		}
		return isAlphaBetaCutOff;
	}
}