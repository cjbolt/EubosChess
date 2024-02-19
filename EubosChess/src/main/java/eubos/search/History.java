package eubos.search;

import eubos.board.BitBoard;
import eubos.board.Piece;
import eubos.position.Move;
import it.unimi.dsi.fastutil.ints.IntComparator;

public class History {

	private int[][] historyLut;
	
	private static final int MAX_HISTORY_SCORE = (Integer.MAX_VALUE - 1000);
	
	public History() {
		historyLut = new int[Piece.PIECE_LENGTH][BitBoard.INVALID];
	}
	
	public final MoveHistoryComparator moveHistoryComparator = new MoveHistoryComparator();
	
    public class MoveHistoryComparator implements IntComparator {
//    	private int cacheScore1;
//    	private int cacheMove1;
//    	private int cacheScore2;
//    	private int cacheMove2;
		int score1, score2;
		int piece1, to1, piece2, to2;
    	@Override public int compare(int move1, int move2) {
//    		if (move1 == cacheMove1) {
//    			score1 = cacheScore1;
//    		} else if (move1 == cacheMove2) {
//    			score1 = cacheScore2;
//    		} else {
            	piece1 = Move.getOriginPiece(move1);
            	to1 = Move.getTargetPosition(move1);
            	score1 = historyLut[piece1][to1];
//            	cacheScore1 = score1;
//            	cacheMove1 = move1;
//    		}
//    		if (move2 == cacheMove1) {
//    			score2 = cacheScore1;
//    		} else if (move2 == cacheMove2) {
//    			score2 = cacheScore2;
//    		} else {
            	piece2 = Move.getOriginPiece(move2);
            	to2 = Move.getTargetPosition(move2);
    			score2 = historyLut[piece2][to2];
//    			cacheScore2 = score2;
//    			cacheMove1 = move2;
//    		}
    		// want list in descending order, hence opposite return values
        	return (score1 > score2) ? -1 : (score1 == score2 ? 0 : 1);
	    }
    }
    
    public void updateMove(int depth, int move) {
    	// Don't want to increment the history score for captures or promos
    	// For example this affects the score for pawn push moves, which can't be distinguished
    	// from adjacent captures, as only the piece type and target square are known.
    	if (Move.isNotCaptureOrPromotion(move)) {
	    	int piece = Move.getOriginPiece(move);
	    	int to = Move.getTargetPosition(move);
	    	int curr_score = historyLut[piece][to];
	    	historyLut[piece][to] += curr_score < MAX_HISTORY_SCORE ? depth*depth : 0;
    	}
    }
}
