package eubos.search;

import eubos.board.BitBoard;
import eubos.board.Piece;
import eubos.position.Move;
import it.unimi.dsi.fastutil.ints.IntComparator;

public class History {

	private int[][] historyLut;
	
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
    	int piece = Move.getOriginPiece(move);
    	int to = Move.getTargetPosition(move);
    	historyLut[piece][to] += depth*depth;
    }
}
