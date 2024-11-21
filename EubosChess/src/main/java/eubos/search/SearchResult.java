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
	public int score;
	
	public SearchResult() {
		this(new int [] {Move.NULL_MOVE}, false, 0L, 0, false, 0);
	}
	
	public SearchResult(int bestMove, int score) {
		this(new int [] {bestMove}, false, 0L, 0, false, score);
	}
	
	public SearchResult(int[] pv, boolean foundMate, long rootTransposition, int depth, boolean trusted, int score) {
		this.pv = pv;
		this.foundMate = foundMate;
		this.rootTrans = rootTransposition;
		this.depth = depth;
		this.trusted = trusted;
		this.score = score;
	}

	public String report(Board theBoard) {
		StringBuilder sb = new StringBuilder();
		if (pv != null) {
			for (int move: pv) {
				if (move == Move.NULL_MOVE) break;
				sb.append(Move.toString(move));
				sb.append(' ');
			}
		}
		String output = String.format("result: pv=%s, score=%d mate=%s, depth=%d, trusted=%d rootTrans=(%s)", 
				sb.toString(),
				score,
				foundMate,
				depth,
				trusted ? 1 : 0,
				(rootTrans != 0L) ? Transposition.report(rootTrans, theBoard) : "0L");
		return output;
	}
}
