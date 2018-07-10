package eubos.position;

import eubos.board.pieces.Piece.Colour;

public class MateScoreGenerator {
	
	private int searchDepth;
	private IPositionAccessors pos;

	public static final int PLIES_PER_MOVE = 2;
	
	public MateScoreGenerator(IPositionAccessors pos, int searchDepth) {
		this.pos = pos;
		this.searchDepth = searchDepth;	
	}
	
	public int scoreMate(int currPly, boolean isWhite, Colour initialOnMove) {
		// Handle mates (indicated by no legal moves)
		int mateScore = 0;
		if (pos.isKingInCheck()) {
			mateScore = generateScoreForCheckmate(currPly);
			// If white got mated, need to back up a large negative score (good for black)
			if (isWhite)
				mateScore=-mateScore;
			//debug.printMateFound(currPly);
		} else {
			mateScore = getScoreForStalemate();
			// TODO: introduce a more sophisticated system for handling stalemate scoring.
			if (initialOnMove==Colour.black)
				mateScore=-mateScore;
		}
		return mateScore;
	}	
	
	private int getScoreForStalemate() {
		// Avoid stalemates by giving them a large penalty score.
		return -MaterialEvaluator.KING_VALUE;
	}	
	
	public int generateScoreForCheckmate(int currPly) {
		// Favour earlier mates (i.e. Mate-in-one over mate-in-three) by giving them a larger score.
		int totalMovesSearched = searchDepth/PLIES_PER_MOVE;
		int mateMoveNum = (currPly-1)/PLIES_PER_MOVE; // currPly-1 because mate was caused by the move from the previousPly
		int multiplier = totalMovesSearched-mateMoveNum;
		return multiplier*MaterialEvaluator.KING_VALUE;
	}	
}