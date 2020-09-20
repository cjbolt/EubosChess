package eubos.score;

import eubos.board.Piece.Colour;
import eubos.position.IPositionAccessors;
import eubos.search.SearchContext;

public class MateScoreGenerator implements IScoreMate {
	
	private IPositionAccessors pos;
	private Colour initialOnMove;
	private SearchContext sc;

	public static final int PLIES_PER_MOVE = 2;
	
	public MateScoreGenerator(IPositionAccessors pos, SearchContext sc) {
		this.pos = pos;
		initialOnMove = pos.getOnMove();
		this.sc = sc;
	}
	
	public short scoreMate(byte currPly) {
		// Handle mates (indicated by no legal moves)
		short mateScore = 0;
		if (pos.isKingInCheck()) {
			short mateMoveNum = (short)(((currPly-1)/PLIES_PER_MOVE)+1); // currPly-1 because mate was caused by the move from the previousPly
			// If white got mated, need to back up a large negative score (good for black)
			if (Colour.isWhite(initialOnMove)) {
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
			mateScore = sc.getScoreForStalemate();
		}
		return mateScore;
	}	
}