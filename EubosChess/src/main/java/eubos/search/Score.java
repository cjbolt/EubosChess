package eubos.search;

import eubos.main.EubosEngineMain;

public final class Score {
	public static final byte exact = 1;
	public static final byte upperBound = 2;
	public static final byte lowerBound = 3;
	
	private static final int SCORE_SHIFT = 0;
	private static final int SCORE_MASK = ((1<<Short.SIZE)-1) << SCORE_SHIFT;
	
	private static final int BOUND_SHIFT = SCORE_SHIFT + Short.SIZE;
	private static final int BOUND_MASK = 0x3 << BOUND_SHIFT;

	public static short getScore(int score) {
		return (short)(score & SCORE_MASK);
	}
	
	public static int setScore(int score, short new_score) {
		score &= ~SCORE_MASK;
		score |= new_score;
		return score;
	}

	public static byte getType(int score) {
		return (byte)(score >>> BOUND_SHIFT);
	}
	
	public static int setType(int score, int type) {
		score &= ~BOUND_MASK;
		score |= (type << BOUND_SHIFT);	
		return score;
	}

	public static boolean isExact(int score) {	
		return ((score & ~BOUND_MASK) == (Score.exact << BOUND_SHIFT));
	}
	
	public static boolean isMate(short score) {
		return (Math.abs(score) > Short.MAX_VALUE-200);
	}
	
	public static int valueOf(short score, byte bound) {
		int theScore = score;
		theScore &= SCORE_MASK;
		if (EubosEngineMain.ASSERTS_ENABLED) {
			assert bound == Score.exact || bound == Score.upperBound || bound == Score.lowerBound;
		}
		theScore |= bound << BOUND_SHIFT;
		return theScore;
	}
	
	public static String toString(int score) {
		StringBuilder string = new StringBuilder();
		char the_type = 'E';
		switch(getType(score))
		{
		case Score.exact:
			the_type='X';
			break;
		case Score.upperBound:
			the_type='U';
			break;
		case Score.lowerBound:
			the_type='L';
			break;
		default:
			if (EubosEngineMain.ASSERTS_ENABLED) {
				assert false;
			}
			break;
		}
		string.append(the_type);
		string.append(":");
		string.append(getScore(score));
		return string.toString();
	}
}
