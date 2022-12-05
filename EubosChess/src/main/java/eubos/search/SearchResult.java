package eubos.search;

import eubos.position.Move;
import eubos.search.transposition.Transposition;

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

	public String report() {
		StringBuilder sb = new StringBuilder();
		for (int move: pv) {
			if (move == Move.NULL_MOVE) break;
			sb.append(Move.toString(move));
			sb.append(' ');
		}
		String output = String.format("result: pv=%s, mate=%s, depth=%d, rootTrans=%s", 
				sb.toString(),
				foundMate,
				depth,
				(rootTrans != 0L) ? Transposition.report(rootTrans) : "0L");
		return output;
	}
}
