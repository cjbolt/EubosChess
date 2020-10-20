package eubos.score;

public interface IScoreMate {
	short scoreMate(byte currPly);
	public int getMateDistanceInPly(short score);
}
