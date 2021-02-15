package eubos.search;

import eubos.main.EubosEngineMain;

public class ScoreTracker {
	private short[] scores;
	private boolean initialOnMoveIsWhite = false;
	private SearchDebugAgent sda;
	
	private static final int MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF = 2;

	public ScoreTracker(int searchDepth, boolean isWhite, SearchDebugAgent sda) {
		scores = new short[searchDepth];
		initialOnMoveIsWhite = isWhite;
		this.sda = sda;
	}
	
	private void bringDownAlphaBetaCutOff(byte currPly) {
		scores[currPly] = scores[currPly-MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF];
	}

	private void initialiseWithWorstPossibleScore(byte currPly) {
		if (onMoveIsWhite(currPly)) {
			scores[currPly] = Short.MIN_VALUE;
		} else {
			scores[currPly] = Short.MAX_VALUE;
		}
	}
	
	boolean onMoveIsWhite(byte currPly) {
		return ((currPly % 2) == 0) ? initialOnMoveIsWhite : !initialOnMoveIsWhite;
	}
	
	void setBackedUpScoreAtPly(byte currPly, int positionScore) {
		sda.printBackUpScore(currPly, scores[currPly], positionScore);
		scores[currPly] = Score.getScore(positionScore);
	}
	
	void setBackedUpScoreAtPly(byte currPly, short positionScore) {
		sda.printBackUpScore(currPly, scores[currPly], positionScore);
		scores[currPly] = positionScore;
	}	
	
	short getBackedUpScoreAtPly(byte currPly) {
		return scores[currPly];
	}
	
	void setProvisionalScoreAtPly(byte currPly) {
		if (currPly<MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF) {
			initialiseWithWorstPossibleScore(currPly);
		} else {
			bringDownAlphaBetaCutOff(currPly);
		}
	}	

	boolean isBackUpRequired(byte currPly, short positionScore, byte plyBound) {
		boolean backUpScore = false;
		switch(plyBound) {
		case Score.lowerBound:
			backUpScore = (positionScore > scores[currPly] && positionScore != Short.MAX_VALUE);
			break;
		case Score.upperBound:
			backUpScore = (positionScore < scores[currPly] && positionScore != Short.MIN_VALUE);
			break;
		default:
			if (EubosEngineMain.ASSERTS_ENABLED) {
				assert false;
			}
			break;
		}
		return backUpScore;
	}
	
	public short adjustHashTableMateInXScore(byte currPly, short score) {
		if (Score.isMate(score)) {
			// The score stored in the hash table encodes the distance to the mate from the hashed position,
			// not the root node, so adjust for the position in search tree.
			score = (short) ((score < 0 ) ? score+currPly : score-currPly);
		}
		return score;
	}
	
	public boolean isAlphaBetaCutOff(byte currPly, short scoreBackedUpToNode) {
		//if (currPly >= MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF) {
		if (currPly > 0) {
			return isAlphaBeta(currPly, scoreBackedUpToNode);
		}
		return false;
	}

	public boolean isAlphaBetaCutOffForHash(byte currPly, short hashScore) {
		//if (currPly >= MINIMUM_PLY_FOR_ALPHA_BETA_CUT_OFF) {
		if (currPly > 0) {
			short adjustedHashScore = adjustHashTableMateInXScore(currPly, hashScore);			
			return isAlphaBeta(currPly, adjustedHashScore);
		}
		return false;
	}

	private boolean isAlphaBeta(byte currPly, short currScore) {
		boolean isAlphaBetaCutOff = false;
		short prevPlyScore = scores[currPly-1];
		if (prevPlyScore != Short.MAX_VALUE || prevPlyScore != Short.MIN_VALUE) {
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
				if (currScore >= prevPlyScore) isAlphaBetaCutOff = true;
			} else {
				if (currScore <= prevPlyScore) isAlphaBetaCutOff = true;
			}
			if (isAlphaBetaCutOff) {
				sda.printAlphaBetaComparison(prevPlyScore, currScore);
			}
		}
		return isAlphaBetaCutOff;
	}
}