package eubos.search;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.pieces.Piece;

public class SearchDebugAgent {

	private String indent = "";
	private boolean isActive=false;
	private int lastPly = 0;

	public SearchDebugAgent( int currPly, boolean active ) {
		isActive = active;
		computeIndent(currPly);
	}
	
	private void computeIndent(int currPly) {
		for (int i=0; i<currPly; i++) {
			indent += "\t";
		}
		lastPly = currPly;		
	}

	public void printPerformMove(int currPly, GenericMove currMove) {
		if (isActive) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			System.out.println(indent+"performMove("+currMove.toString()+") at Ply="+currPly);
		}
	}

	void printSearchPly(int currPly, Piece.Colour onMove) {
		if (isActive) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			System.out.println(indent+"searchPly("+currPly+", "+onMove.toString()+")");
		}
	}

	void printUndoMove(int currPly, GenericMove currMove) {
		if (isActive) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			System.out.println(indent+"undoMove("+currMove.toString()+") at Ply="+currPly);
		}
	}

	void printBackUpScore(int currPly, int positionScore) {
		if (isActive) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			System.out.println(indent+"backedUpScore:"+positionScore+" at Ply="+currPly);
		}
	}

	void printPrincipalContinuation(int currPly, PrincipalContinuation pc) {
		if (isActive) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			System.out.println(indent+"principal continuation found: "+pc.toStringAfter(currPly));
		}
	}
	
	void printMateFound( int currPly) {
		if (isActive) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			System.out.println(indent+"possible Checkmate found at Ply="+currPly);
		}
	}
	
	void printRefutationFound( int currPly) {
		if (isActive) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			System.out.println(indent+"refutation found (cut-off search) at Ply="+currPly);
		}
	}
	
	void printAlphaBetaCutOffLimit(int currPly, int score) {
		if (isActive) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			System.out.println(indent+"alpha beta brought down score:"+score+" at Ply="+currPly);
		}
	}
}