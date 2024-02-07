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
    	@Override public int compare(int move1, int move2) {
        	int piece1 = Move.getOriginPiece(move1);
        	int to1 = Move.getTargetPosition(move1);
        	int piece2 = Move.getOriginPiece(move2);
        	int to2 = Move.getTargetPosition(move2);
        	boolean gt = historyLut[piece1][to1] > historyLut[piece2][to2];
        	boolean eq = historyLut[piece1][to1] == historyLut[piece2][to2];
        	return gt ? -1 : (eq ? 0 : 1); // want list in descending order, hence opposite return values
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
