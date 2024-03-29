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
	        	MoveListIterator iter = ml.initialiseAtPly(Move.NULL_MOVE, null, pm.isKingInCheck(), false, currPly);
	        	int move;
	        	while ((move = iter.nextInt()) != Move.NULL_MOVE) {
					if (pm.performMove(move)) {
		                currPly+=1;
		                perft();
		                currPly-=1;
		                pm.unperformMove();
					}
	        	}
	        } else {
	        	nodeCount += 1;
	        }
	        return nodeCount;
	    }
	}
