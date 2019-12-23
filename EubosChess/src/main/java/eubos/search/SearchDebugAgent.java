package eubos.search;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.IPositionAccessors;
import eubos.search.transposition.Transposition;
import eubos.search.Score.ScoreType;

public class SearchDebugAgent {

	private static String indent = "";
	public static boolean isDebugOn = false;
	private static int lastPly = 0;
	private static FileWriter fw;
	private static String filenameBase = "";
	
	public static void open(int moveNumber) {
		if (isDebugOn) {
			try {
				fw = new FileWriter(new File(filenameBase+"_move"+moveNumber+".txt"));
			} catch (IOException e) {
				isDebugOn = false;
			}
		}
	}
	
	public static void close() {
		try {
			if (fw != null)
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
			printOutput(indent+"hash "+hash+" term best:"+((move!=null)?move.toString():"")+" score:"+ score +" @"+currPly);
		}
	}

	public static void printHashIsRefutation(int currPly, GenericMove move, long hash) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"hash "+hash+" ref @ Ply="+currPly+" move: "+((move!=null)?move.toString():""));
		}
		
	}

	public static void printHashIsSeedMoveList(int currPly, GenericMove move, long hash) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"hash "+hash+" sufficient seed move list with best move:"+((move!=null)?move.toString():"")+" at Ply="+currPly);
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
			printOutput(indent+"trans hash: "+hash+" mv:"+((bestMove!=null)?bestMove.toString():"")+" dep:"+depthPositionSearchedPly+" sc:"+score+" type:"+bound);
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
	
	public static void printExactTrans(int currPly, long hashCode) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"trans now exact, hash: "+hashCode);
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

	public static void setFileNameBaseString(String dateTime) {
		filenameBase = dateTime;
	}
}