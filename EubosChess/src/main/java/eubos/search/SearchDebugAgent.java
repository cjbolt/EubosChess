package eubos.search;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.pieces.Piece;

public class SearchDebugAgent {

	private static String indent = "";
	private static boolean isDebugOn = true;
	private static int lastPly = 0;
	private static FileWriter fw;

	SearchDebugAgent( int currPly ) {
		try {
			fw = new FileWriter(new File("unit_test_log.txt"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 		computeIndent(currPly);
	}
	
	public static void open() {
		try {
			fw = new FileWriter(new File("unit_test_log.txt"));
		} catch (IOException e) {
			isDebugOn = false;
		}		
	}
	
	public static void close() {
		try {
			fw.close();
		} catch (IOException e) {
			isDebugOn = false;
		}
	}
	
	private static void printOutput(String output) {
		//System.out.println(output);
		try {
			fw.write(output+'\n');
		} catch (IOException e) {
			isDebugOn = false;
		}
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
			printOutput(indent+"performMove("+currMove.toString()+") at Ply="+currPly);
		}
	}

	static void printSearchPly(int currPly, int provScore, Piece.Colour onMove) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"searchPly("+currPly+", "+onMove.toString()+") provisionalScore="+provScore);
		}
	}

	static void printUndoMove(int currPly, GenericMove currMove) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"undoMove("+currMove.toString()+") at Ply="+currPly);
		}
	}

	static void printBackUpScore(int currPly, int prevScore, int positionScore) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"backedUpScore was:"+prevScore+" now:"+positionScore+" at Ply="+currPly);
		}
	}

	static void printPrincipalContinuation(int currPly, PrincipalContinuation pc) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"principal continuation found: "+pc.toStringAfter(currPly));
		}
	}
	
	static void printMateFound( int currPly) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"possible Checkmate found at Ply="+currPly);
		}
	}
	
	static void printRefutationFound( int currPly) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"refutation found (cut-off search) at Ply="+currPly);
		}
	}
	
	static void printAlphaBetaCutOffLimit(int currPly, int score) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"alpha beta brought down score:"+score+" at Ply="+currPly);
		}
	}

	public static void printHashIsTerminalNode(int currPly, GenericMove move, int score ) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"hash sufficient terminal node with best move:"+move.toString()+" score:"+ score +" at Ply="+currPly);
		}
	}

	public static void printHashIsRefutation(int currPly, GenericMove move) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"hash sufficient for refutation at Ply="+currPly+" move: "+move.toString());
		}
		
	}

	public static void printHashIsSeedMoveList(int currPly, GenericMove move) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"hash sufficient seed move list with best move:"+move.toString()+" at Ply="+currPly);
		}
	}

	public static void printAlphaBetaComparison(int currPly, int prevPlyScore, int positionScore) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"alphaBeta compare prev:"+prevPlyScore+" curr:"+positionScore+" at Ply="+currPly);
		}
		
	}
}