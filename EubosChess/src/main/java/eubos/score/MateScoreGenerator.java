package eubos.score;

import eubos.position.IPositionAccessors;
import eubos.search.Score;

public class MateScoreGenerator implements IScoreMate {
	
	private IPositionAccessors pos;

	public static final int PLIES_PER_MOVE = 2;
	
	public MateScoreGenerator(IPositionAccessors pos, IEvaluate pe) {
		this.pos = pos;
	}
	
	public short scoreMate(byte currPly) {
		short mateScore = 0;
		if (pos.isKingInCheck()) {
			mateScore = (short) (Score.PROVISIONAL_ALPHA+currPly);
		}
		return mateScore;
	}
}