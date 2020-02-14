package eubos.search;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.search.transposition.Transposition;
import eubos.search.Score.ScoreType;

public class SearchDebugAgent {

	private static String indent = "";
	public static boolean isDebugOn = true;
	private static int lastPly = 0;
	private static FileWriter fw;
	private static String filenameBase = "";
	
	public static void open(int moveNumber, boolean isWhite) {
		if (isDebugOn) {
			try {
				fw = new FileWriter(new File(filenameBase+"_move"+moveNumber+"_"+(isWhite?"w":"b")+".txt"));
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

	static void printPerformMove(int currPly, int currMove) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"do("+Move.toString(currMove)+") @"+currPly);
		}
	}

	static void printUndoMove(int currPly, int currMove) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"undo("+Move.toString(currMove)+") @"+currPly);
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
			printOutput(indent+"pc:"+pc.toStringAt(currPly));
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

	public static void printHashIsTerminalNode(int currPly, Transposition trans, long hash) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"hash "+hash+" term "+trans.report()+" @"+currPly);
		}
	}

	public static void printHashIsRefutation(int currPly, int move, long hash) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"hash "+hash+" ref @ Ply="+currPly+" move: "+Move.toString(move));
		}
		
	}

	public static void printHashIsSeedMoveList(int currPly, int move, long hash) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"hash "+hash+" sufficient seed move list with best move:"+Move.toString(move)+" at Ply="+currPly);
		}
	}

	public static void printAlphaBetaComparison(int currPly, int prevPlyScore, int positionScore) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"ab cmp prev:"+prevPlyScore+" curr:"+positionScore+" @"+currPly);
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

	public static void printRepeatedPositionHash(byte currPly, long hash) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"3-fold rep @"+currPly+", hash: "+hash);
		}
	}

	public static void printTransDepthCheck(int currPly, int currentDepth, int newDepth) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"trans set @"+currPly+", depth curr: "+currentDepth+", depth new: "+newDepth);
		}		
	}

	public static void printTransBoundScoreCheck(int currPly, ScoreType currentBound, short score, short score2) {
		if (isDebugOn) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"trans set @"+currPly+", bound:"+currentBound+", curr score: "+score+", new score: "+score2);
		}	
	}
}