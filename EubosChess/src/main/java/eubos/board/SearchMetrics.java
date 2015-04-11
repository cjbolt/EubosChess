package eubos.board;

public class SearchMetrics {
	private long nodesSearched;
	private long time;
	
	public SearchMetrics() {
		nodesSearched = 0;
		time = 0;
	}
	
	public synchronized void incrementNodesSearched() { nodesSearched++; }
	public synchronized long getNodesSearched() { return nodesSearched; }
	public synchronized void incrementTime(int delta) { time += delta; }
	public synchronized int getNodesPerSecond() { return (int)(nodesSearched*1000/time); }
}
