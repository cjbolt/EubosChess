package eubos.search;

public class Score {
	public static final byte exact = 1;
	public static final byte upperBound = 2;
	public static final byte lowerBound = 3;
	
	short score;
	byte type;
	int mateDistanceInPly;
	
	public Score() {
		score = 0;
		mateDistanceInPly = 0;
		type = Score.exact;
	}
	
	public Score(short theScore, byte theType) {
		score = theScore;
		type = theType;
	}
	
	public Score(short theScore, byte theType, int distance) {
		score = theScore;
		type = theType;
		mateDistanceInPly = distance;		
	}

	public Score(byte plyBound) {
		score = (plyBound == Score.lowerBound) ? Short.MIN_VALUE : Short.MAX_VALUE;
		type = plyBound;
	}

	public short getScore() {
		return score;
	}

	public byte getType() {
		return type;
	}

	public void setExact() {
		type = Score.exact;		
	}
	
	public boolean isMate() {
		int abs = Math.abs(score);
		int thresh = Short.MAX_VALUE-200;
		return (abs > thresh) /*&& (type == exact)*/;
	}
	
	public int getMateDistance() {
		return mateDistanceInPly;
	}
}
