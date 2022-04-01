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
	        	boolean firstMove = true;
	        	do {
		            MoveListIterator iter = ml.stagedMoveGen(firstMove, Move.NULL_MOVE, null, pm.isKingInCheck(), currPly);
		            if (!iter.hasNext()) break;
		            do {
		            	int move = iter.nextInt();
						pm.performMove(move, false);
		                currPly+=1;
		                perft();
		                currPly-=1;
		                pm.unperformMove(false);
		                firstMove = false;
		            } while (iter.hasNext());
	        	} while(true);
	        } else {
	        	nodeCount += 1;
	        }
	        return nodeCount;
	    }
	}
