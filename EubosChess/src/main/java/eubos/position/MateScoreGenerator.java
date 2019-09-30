package eubos.position;

import eubos.board.pieces.Piece.Colour;
import eubos.board.pieces.King;

public class MateScoreGenerator implements IScoreMate {
	
	private IPositionAccessors pos;

	public static final int PLIES_PER_MOVE = 2;
	
	public MateScoreGenerator(IPositionAccessors pos) {
		this.pos = pos;
	}
	
	public short scoreMate(byte currPly, Colour initialOnMove) {
		// Handle mates (indicated by no legal moves)
		short mateScore = 0;
		if (pos.isKingInCheck()) {
			short mateMoveNum = (short)(((currPly-1)/PLIES_PER_MOVE)+1); // currPly-1 because mate was caused by the move from the previousPly
			// If white got mated, need to back up a large negative score (good for black)
			if (initialOnMove == Colour.white) {
				if ((currPly%2) == 0) {
					mateScore = (short) (Short.MIN_VALUE + mateMoveNum);
				} else {
					mateScore = (short) (Short.MAX_VALUE - mateMoveNum);
				}
			} else {
				if ((currPly%2) == 0) {
					mateScore = (short) (Short.MAX_VALUE - mateMoveNum);
				} else {
					mateScore = (short) (Short.MIN_VALUE + mateMoveNum);
				}
			}
		} else {
			mateScore = getScoreForStalemate();
			// TODO: introduce a more sophisticated system for handling stalemate scoring.
			if (initialOnMove==Colour.black)
				mateScore=(short) -mateScore;
		}
		return mateScore;
	}	
	
	private short getScoreForStalemate() {
		// Avoid stalemates by giving them a large penalty score.
		return -King.MATERIAL_VALUE;
	}	
}