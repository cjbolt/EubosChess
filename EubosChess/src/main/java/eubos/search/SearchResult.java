package eubos.search;

import eubos.board.Board;
import eubos.position.Move;
import eubos.search.transposition.Transposition;

public class SearchResult {
	public int[] pv;
	public boolean foundMate;
	public long rootTrans;
	public int depth;
	public boolean trusted;
	
	public SearchResult() {
		this(new int [] {Move.NULL_MOVE}, false, 0L, 0, true);
	}
	
	public SearchResult(int bestMove) {
		this(new int [] {bestMove}, false, 0L, 0, true);
	}
	
	public SearchResult(int[] pv, boolean foundMate, long rootTransposition, int depth, boolean trusted) {
		this.pv = pv;
		this.foundMate = foundMate;
		this.rootTrans = rootTransposition;
		this.depth = depth;
		this.trusted = trusted;
	}

	public String report(Board theBoard) {
		StringBuilder sb = new StringBuilder();
		for (int move: pv) {
			if (move == Move.NULL_MOVE) break;
			sb.append(Move.toString(move));
			sb.append(' ');
		}
		String output = String.format("result: pv=%s, mate=%s, depth=%d, trusted=%d rootTrans=(%s)", 
				sb.toString(),
				foundMate,
				depth,
				trusted ? 1 : 0,
				(rootTrans != 0L) ? Transposition.report(rootTrans, theBoard) : "0L");
		return output;
	}
}
