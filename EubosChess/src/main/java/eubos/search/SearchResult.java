package eubos.search;

public class SearchResult {
	public int bestMove;
	public boolean foundMate;
	
	public SearchResult(int bestMove, boolean foundMate) {
		this.bestMove = bestMove;
		this.foundMate = foundMate;
	}
}
