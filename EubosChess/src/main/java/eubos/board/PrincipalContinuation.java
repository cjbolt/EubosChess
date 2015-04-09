package eubos.board;

import com.fluxchess.jcpi.models.GenericMove;

public class PrincipalContinuation {

	private GenericMove pc[][];
	private int searchDepthPly;

	public PrincipalContinuation(int searchDepth) {
		pc = new GenericMove[searchDepth][searchDepth];
		searchDepthPly = searchDepth;
	}
	
	public GenericMove getBestMove() {
		return pc[0][0];
	}

	public String toStringAfter(int currPly) {
		String output = ""+pc[currPly][currPly];
		for ( int nextPly = currPly+1; nextPly < searchDepthPly; nextPly++) {
			output+=(", "+pc[currPly][nextPly]);
		}
		return output;
	}

	public void update(int currPly, GenericMove currMove) {
		// Update Principal Continuation
		pc[currPly][currPly]=currMove;
		for (int nextPly=currPly+1; nextPly < searchDepthPly; nextPly++) {
			pc[currPly][nextPly]=pc[currPly+1][nextPly];
		}
	}

	public void clearAfter(int currPly ) {
		// Clear the principal continuation after the indicated ply depth
		for (int nextPly=currPly+1; nextPly < searchDepthPly; nextPly++) {
			for (int i=0; i<searchDepthPly; i++)
				// All plies need to be cleared.
				pc[i][nextPly]=null;
		}
	}	
}
