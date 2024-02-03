package eubos.search;

import eubos.position.Move;
import it.unimi.dsi.fastutil.ints.IntComparator;

public class History {

	private int[][] historyLut;
	
	public History() {
		historyLut = new int[64][64];
	}
	
	public final MoveHistoryComparator moveHistoryComparator = new MoveHistoryComparator();
	
    public class MoveHistoryComparator implements IntComparator {
    	@Override public int compare(int move1, int move2) {
        	int from1 = Move.getOriginPosition(move1);
        	int to1 = Move.getTargetPosition(move1);
        	int from2 = Move.getOriginPosition(move2);
        	int to2 = Move.getTargetPosition(move2);
        	if (historyLut[from1][to1] > historyLut[from2][to2]) {
        		return -1;
        	} else if (historyLut[from1][to1] == historyLut[from2][to2]) {
        		return 0;
        	} else {
        		return 1;
        	}
	    }
    }
    
    public void updateMove(int depth, int move) {
    	int from = Move.getOriginPosition(move);
    	int to = Move.getTargetPosition(move);
    	historyLut[from][to] += depth*depth;
    }
}
