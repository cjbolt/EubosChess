package eubos.search;

import eubos.position.Move;

public class SearchResult {
	public int[] pv;
	public boolean foundMate;
	public long rootTrans;
	public int depth;
	
	public SearchResult() {
		this(new int [] {Move.NULL_MOVE}, false, 0L, 0);
	}
	
	public SearchResult(int bestMove) {
		this(new int [] {bestMove}, false, 0L, 0);
	}
	
	public SearchResult(int[] pv, boolean foundMate, long rootTransposition, int depth) {
		this.pv = pv;
		this.foundMate = foundMate;
		this.rootTrans = rootTransposition;
		this.depth = depth;
	}
}
