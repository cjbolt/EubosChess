package eubos.search;

import eubos.position.PositionManager;
import eubos.position.MoveList;
import eubos.position.MoveListIterator;

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
	    
	    public long perft()  {
	        if (currPly < requestedDepthPly) {        
	            MoveList ml = new MoveList(pm);
	            MoveListIterator iter = ml.iterator();
	            while (iter.hasNext()) {
	            	int move = iter.nextInt();
					pm.performMove(move, false);
	                currPly+=1;
	                perft();
	                currPly-=1;
	                pm.unperformMove(false);
	            }
	        } else {
	        	nodeCount += 1;
	        }
	        return nodeCount;
	    }
	}
