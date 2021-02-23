package eubos.score;

import eubos.position.IPositionAccessors;

public class MateScoreGenerator implements IScoreMate {
	
	private IPositionAccessors pos;
	private IEvaluate pe;

	public static final int PLIES_PER_MOVE = 2;
	
	public MateScoreGenerator(IPositionAccessors pos, IEvaluate pe) {
		this.pos = pos;
		this.pe = pe;
	}
	
	public short scoreMate(byte currPly) {
		short mateScore = 0;
		if (pos.isKingInCheck()) {
			mateScore = (short) (Short.MIN_VALUE+1+currPly);
		} else {
			// Stalemate
			mateScore = pe.getScoreForStalemate();
		}
		return mateScore;
	}
}