package eubos.search;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

class SearchMetrics {
	private long nodesSearched;
	private long time;
	private List<GenericMove> pv;
	private boolean pvValid = false;
	private int cpScore;
	private int depth;
	private GenericMove currMove;
	private int currMoveNum;
	
	SearchMetrics(int searchDepth) {
		nodesSearched = 0;
		time = 0;
		cpScore = 0;
		pvValid = false;
		depth = searchDepth;
		currMoveNum = 0;
	}
	
	synchronized void incrementNodesSearched() { nodesSearched++; }
	synchronized long getNodesSearched() { return nodesSearched; }
	synchronized void incrementTime(int delta) { time += delta; }
	synchronized long getTime() { return time; }
	synchronized int getNodesPerSecond() {
		int nps = 0;
		if (time != 0) {
			nps = (int)(nodesSearched*1000/time);
		}
		return nps;
	}
	synchronized void setPrincipalVariation(List<GenericMove> pc) { 
		pvValid = true;
		pv = pc;
	}
	synchronized List<GenericMove> getPrincipalVariation() { return (pvValid ? pv : null);}
	synchronized int getCpScore() { return cpScore; }
	synchronized void setCpScore(int cpScore) { this.cpScore = cpScore; }
	synchronized int getDepth() { return depth; }
	synchronized void setCurrentMove(GenericMove mov) { currMove = mov;}
	synchronized GenericMove getCurrentMove() { return currMove;	}
	synchronized int getCurrentMoveNumber() { return currMoveNum; }
	synchronized void incrementCurrentMoveNumber() { currMoveNum+=1; }
}
