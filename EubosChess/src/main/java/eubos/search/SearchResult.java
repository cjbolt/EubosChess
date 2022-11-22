package eubos.search;

public class SearchResult {
	public int bestMove;
	public boolean foundMate;
	public long rootTrans;
	
	public SearchResult(int bestMove, boolean foundMate, long rootTransposition) {
		this.bestMove = bestMove;
		this.foundMate = foundMate;
		this.rootTrans = rootTransposition;
	}
}
