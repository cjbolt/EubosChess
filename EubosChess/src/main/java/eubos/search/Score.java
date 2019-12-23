package eubos.search;

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
