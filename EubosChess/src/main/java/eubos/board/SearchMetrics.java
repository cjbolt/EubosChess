package eubos.board;

//import java.util.LinkedList;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

public class SearchMetrics {
	private long nodesSearched;
	private long time;
	private List<GenericMove> pv;
	private boolean pvValid = false;
	private int cpScore;
	private int depth;
	
	public SearchMetrics(int searchDepth) {
		nodesSearched = 0;
		time = 0;
		cpScore = 0;
		pvValid = false;
		depth = searchDepth;
	}
	
	public synchronized void incrementNodesSearched() { nodesSearched++; }
	public synchronized long getNodesSearched() { return nodesSearched; }
	public synchronized void incrementTime(int delta) { time += delta; }
	public synchronized long getTime() { return time; }
	public synchronized int getNodesPerSecond() { return (int)(nodesSearched*1000/time); }
	public synchronized void setPrincipalVariation(List<GenericMove> pc) { 
		pvValid = true;
		pv = pc;
	}
	public synchronized List<GenericMove> getPrincipalVariation() { return (pvValid ? pv : null);}
	public synchronized int getCpScore() { return cpScore; }
	public synchronized void setCpScore(int cpScore) { this.cpScore = cpScore; }
	public synchronized int getDepth() { return depth; }
}
