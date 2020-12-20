package eubos.search;

import java.util.Iterator;

import eubos.board.InvalidPieceException;
import eubos.position.PositionManager;
import eubos.position.MoveList;
import eubos.position.Position;

public class PerformanceTest {

	    private PositionManager pm;
	    private long nodeCount = 0;
	    private int currPly = 0;
	    private int requestedDepthPly = 0;

		public PerformanceTest(PositionManager pm, int depth) {
	        this.pm = pm;
	        requestedDepthPly = depth;
	    }
		
	    public void setRequestedDepthPly(int requestedDepthPly) {
			this.requestedDepthPly = requestedDepthPly;
		}
	    
	    public long perft() throws InvalidPieceException {
	        if (currPly < requestedDepthPly) {        
	            MoveList ml = new MoveList(pm);
	            Iterator<Integer> iter = ml.getStandardIterator(false, Position.NOPOSITION);
	            while (iter.hasNext()) {
	            	int move = iter.next();
	                try {
						pm.performMove(move, false);
		                currPly+=1;
		                perft();
		                currPly-=1;
		                pm.unperformMove(false);
					} catch (InvalidPieceException e) {
						e.printStackTrace();
					}
	            }
	        } else {
	        	nodeCount += 1;
	        }
	        return nodeCount;
	    }
	}
