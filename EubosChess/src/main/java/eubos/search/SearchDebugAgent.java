package eubos.search;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.IPositionAccessors;
import eubos.position.Transposition;
import eubos.position.Transposition.ScoreType;

public class SearchDebugAgent {

	private static String indent = "";
	private static boolean isDebugOn = false;
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
			fw.flush();
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
			printOutput(indent+"do("+currMove.toString()+") @"+currPly);
		}
	}

	static void printUndoMove(int currPly, GenericMove currMove) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"undo("+currMove.toString()+") @"+currPly);
		}
	}

	static void printBackUpScore(int currPly, int prevScore, int positionScore) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"backedUp was:"+prevScore+" now:"+positionScore+" @"+currPly);
		}
	}

	static void printPrincipalContinuation(int currPly, PrincipalContinuation pc) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"pc:"+pc.toStringAfter(currPly));
		}
	}
	
	static void printMateFound( int currPly) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"possible mate @"+currPly);
		}
	}
	
	static void printRefutationFound( int currPly) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"ref @"+currPly);
		}
	}
	
	static void printAlphaBetaCutOffLimit(int currPly, int score) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			//printOutput(indent+"alpha beta brought down score:"+score+" at Ply="+currPly);
		}
	}

	public static void printHashIsTerminalNode(int currPly, GenericMove move, int score, long hash) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"hash "+hash+" term best:"+move.toString()+" score:"+ score +" @"+currPly);
		}
	}

	public static void printHashIsRefutation(int currPly, GenericMove move, long hash) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"hash "+hash+" ref @ Ply="+currPly+" move: "+move.toString());
		}
		
	}

	public static void printHashIsSeedMoveList(int currPly, GenericMove move, long hash) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"hash "+hash+" sufficient seed move list with best move:"+move.toString()+" at Ply="+currPly);
		}
	}

	public static void printAlphaBetaComparison(int currPly, int prevPlyScore, int positionScore) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"ab cmp prev:"+prevPlyScore+" curr:"+positionScore+" @"+currPly);
		}
		
	}

	public static void printTransUpdate(int currPly, GenericMove bestMove, int depthPositionSearchedPly, int score,
			ScoreType bound, long hash) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"trans hash: "+hash+" mv:"+bestMove.toString()+" dep:"+depthPositionSearchedPly+" sc:"+score+" type:"+bound);
		}		
	}

	public static void printTransUpdate(int currPly, Transposition trans, long hashCode) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+trans.report()+", hash: "+hashCode + " ref" + trans.toString());
		}		
	}

	public static void printTransNull(int currPly, long hashCode) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"trans is null, hash: "+hashCode);
		}		
	}
	
	public static void printCreateTrans(int currPly, long hashCode) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"trans create, hash: "+hashCode);
		}		
	}

	public static void printStartPlyInfo(byte currPly, byte depthRequiredPly, short provisionalScoreAtPly,
			IPositionAccessors pos) {
		if (isDebugOn) {
			if (currPly == 0) {
				printOutput("\n\n\n NEW ITERATION to Depth "+depthRequiredPly+"\n\n\n");
			}
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"search @"+currPly+" prov="+provisionalScoreAtPly);
			printOutput(indent+"fen:"+pos.getFen());
		}
	}
}