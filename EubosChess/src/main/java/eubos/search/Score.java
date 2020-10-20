package eubos.search;

public class Score {
	public static final byte exact = 1;
	public static final byte upperBound = 2;
	public static final byte lowerBound = 3;
	
	short score;
	byte type;
	
	public Score() {
		score = 0;
		type = Score.exact;
	}
	
	public Score(short theScore, byte theType) {
		score = theScore;
		type = theType;
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
		return (Math.abs(score) > Short.MAX_VALUE-200);
	}
}
