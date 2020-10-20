package eubos.score;

import eubos.board.Piece.Colour;
import eubos.position.IPositionAccessors;

public class MateScoreGenerator implements IScoreMate {
	
	private IPositionAccessors pos;
	private Colour initialOnMove;
	private IEvaluate pe;

	public static final int PLIES_PER_MOVE = 2;
	
	public MateScoreGenerator(IPositionAccessors pos, IEvaluate pe) {
		this.pos = pos;
		initialOnMove = pos.getOnMove();
		this.pe = pe;
	}
	
	private boolean isInitialOnMove(byte currPly) {
		return (currPly%2) == 0;
	}
	
	private short getWhiteIsMatedScore(short mateMoveNum) {
		return (short) (Short.MIN_VALUE + mateMoveNum);
	}
	
	private short getBlackIsMatedScore(short mateMoveNum) {
		return (short) (Short.MAX_VALUE - mateMoveNum);
	}
	
	public short scoreMate(byte currPly) {
		short mateScore = 0;
		if (pos.isKingInCheck()) {
			// Checkmate
			//short mateMoveNum = (short)(((currPly-1)/PLIES_PER_MOVE)+1); // currPly-1 because mate was caused by the move from the previousPly
			short mateOnPly = (short)currPly;
			short mateMoveNum = mateOnPly;
			if (Colour.isWhite(initialOnMove)) {
				mateScore = isInitialOnMove(currPly) ? getWhiteIsMatedScore(mateMoveNum) : getBlackIsMatedScore(mateMoveNum);
			} else { // initial on move is black
				mateScore = isInitialOnMove(currPly) ? getBlackIsMatedScore(mateMoveNum) : getWhiteIsMatedScore(mateMoveNum);
			}
		} else {
			// Stalemate
			mateScore = pe.getScoreForStalemate();
		}
		return mateScore;
	}
	
	public int getMateDistanceInPly(short score) {
		if (Math.abs(score) > Short.MAX_VALUE-200) {
			return (score < 0) ? Math.abs(Short.MIN_VALUE - score) : Short.MAX_VALUE - score;
		} else return 0;		
	}
}