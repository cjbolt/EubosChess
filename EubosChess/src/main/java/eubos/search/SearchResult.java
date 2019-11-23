package eubos.search;

import com.fluxchess.jcpi.models.GenericMove;

public class SearchResult {
	public GenericMove bestMove;
	public boolean foundMate;
	
	public SearchResult(GenericMove bestMove, boolean foundMate) {
		this.bestMove = bestMove;
		this.foundMate = foundMate;
	}
}
