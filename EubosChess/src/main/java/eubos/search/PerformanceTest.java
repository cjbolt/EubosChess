package eubos.search;

import eubos.position.PositionManager;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.MoveListIterator;

public class PerformanceTest {

	    private PositionManager pm;
	    private MoveList ml;
	    
	    private long nodeCount = 0;
	    private int currPly = 0;
	    private int requestedDepthPly = 0;

		public PerformanceTest(PositionManager pm, int depth) {
	        this.pm = pm;
	        this.ml = new MoveList(pm, 0);
	        requestedDepthPly = depth;
	    }
		
	    public void setRequestedDepthPly(int requestedDepthPly) {
			this.requestedDepthPly = requestedDepthPly;
		}
	    
	    public long perft()  {
	        if (currPly < requestedDepthPly) {        
	            MoveListIterator iter = ml.createForPly(Move.NULL_MOVE, null, false, pm.isKingInCheck(), currPly);
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
