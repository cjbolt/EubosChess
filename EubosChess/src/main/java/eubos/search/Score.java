package eubos.search;

import eubos.search.Score.ScoreType;

public class Score {
	short score;
	public enum ScoreType { 
		exact, upperBound, lowerBound;
	};
	ScoreType type;
	
	public Score() {
		score = 0;
		type = ScoreType.exact;
	}
	
	public Score(short theScore, ScoreType theType) {
		score = theScore;
		type = theType;
	}

	public Score(ScoreType plyBound) {
		score = (plyBound == ScoreType.lowerBound) ? Short.MIN_VALUE : Short.MAX_VALUE;
		type = plyBound;
	}

	public short getScore() {
		return score;
	}

	public ScoreType getType() {
		return type;
	}

	public void setExact() {
		type = ScoreType.exact;		
	}
}
