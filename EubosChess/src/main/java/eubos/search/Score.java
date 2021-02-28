package eubos.search;

public final class Score {
	public static final byte typeUnknown = 0;
	public static final byte exact = 1;
	public static final byte upperBound = 2;
	public static final byte lowerBound = 3;
	
	public static final int PROVISIONAL_ALPHA = (Short.MIN_VALUE + 1);
	public static final int PROVISIONAL_BETA = Short.MAX_VALUE;
	
	public static boolean isMate(short score) {
		return (!isProvisional(score) && Math.abs(score) > Short.MAX_VALUE-200);
	}
	
	public static String toString(short score) {
		String scoreString;
		if (Score.isMate(score)) {
			int matePly = (score > 0) ? PROVISIONAL_BETA - score + 1 : PROVISIONAL_ALPHA - score;
			int mateMove = matePly / 2;
			scoreString = String.format("mateIn%d", mateMove);
		} else {
			scoreString = Short.toString(score);
		}
		return scoreString;
	}
	
	public static boolean isProvisional(int score) {
		return (score >= PROVISIONAL_BETA || score <= PROVISIONAL_ALPHA);
	}
}
