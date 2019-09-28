package eubos.position;

import eubos.board.pieces.Piece.Colour;
import eubos.board.pieces.King;

public class MateScoreGenerator implements IScoreMate {
	
	private int searchDepth;
	private IPositionAccessors pos;

	public static final int PLIES_PER_MOVE = 2;
	
	public MateScoreGenerator(IPositionAccessors pos, int searchDepth) {
		this.pos = pos;
		this.searchDepth = searchDepth;	
	}
	
	public short scoreMate(byte currPly, boolean isWhite, Colour initialOnMove) {
		// Handle mates (indicated by no legal moves)
		short mateScore = 0;
		if (pos.isKingInCheck()) {
			mateScore = generateScoreForCheckmate(currPly);
			// If white got mated, need to back up a large negative score (good for black)
			if (isWhite)
				mateScore=(short) -mateScore;
			//debug.printMateFound(currPly);
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
	
	short generateScoreForCheckmate(byte currPly) {
		// Favour earlier mates (i.e. Mate-in-one over mate-in-three) by giving them a larger score.
		int totalMovesSearched = searchDepth/PLIES_PER_MOVE;
		int mateMoveNum = (currPly-1)/PLIES_PER_MOVE; // currPly-1 because mate was caused by the move from the previousPly
		int multiplier = totalMovesSearched-mateMoveNum;
		int score = multiplier*King.MATERIAL_VALUE;
		if (score >= Short.MAX_VALUE) {
			score = Short.MAX_VALUE-1;
		} else if (score <= Short.MIN_VALUE) {
			score = Short.MIN_VALUE+1;
		}
		return (short)(score);
	}	
}