package eubos.search;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.pieces.Piece;

public class SearchDebugAgent {

	private static String indent = "";
	private static final boolean isDebugOn = false;
	private static int lastPly = 0;

	SearchDebugAgent( int currPly ) {
		computeIndent(currPly);
	}
	
	private static void computeIndent(int currPly) {
		indent="";
		for (int i=0; i<currPly; i++) {
			indent += "\t";
		}
		lastPly = currPly;		
	}

	static void printPerformMove(int currPly, GenericMove currMove) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			System.out.println(indent+"performMove("+currMove.toString()+") at Ply="+currPly);
		}
	}

	static void printSearchPly(int currPly, Piece.Colour onMove) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			System.out.println(indent+"searchPly("+currPly+", "+onMove.toString()+")");
		}
	}

	static void printUndoMove(int currPly, GenericMove currMove) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			System.out.println(indent+"undoMove("+currMove.toString()+") at Ply="+currPly);
		}
	}

	static void printBackUpScore(int currPly, int positionScore) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			System.out.println(indent+"backedUpScore:"+positionScore+" at Ply="+currPly);
		}
	}

	static void printPrincipalContinuation(int currPly, PrincipalContinuation pc) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			System.out.println(indent+"principal continuation found: "+pc.toStringAfter(currPly));
		}
	}
	
	static void printMateFound( int currPly) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			System.out.println(indent+"possible Checkmate found at Ply="+currPly);
		}
	}
	
	static void printRefutationFound( int currPly) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			System.out.println(indent+"refutation found (cut-off search) at Ply="+currPly);
		}
	}
	
	static void printAlphaBetaCutOffLimit(int currPly, int score) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			System.out.println(indent+"alpha beta brought down score:"+score+" at Ply="+currPly);
		}
	}
}