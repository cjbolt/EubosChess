package eubos.search;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.search.transposition.ITransposition;

public class SearchDebugAgent {

	private static String indent = "";
	public static final boolean DEBUG_ENABLED = false;
	private static int lastPly = 0;
	private static FileWriter fw;
	private static String filenameBase = "";
	
	public static void open(int moveNumber, boolean isWhite) {
		if (DEBUG_ENABLED) {
			try {
				fw = new FileWriter(new File(filenameBase+"_move"+moveNumber+"_"+(isWhite?"w":"b")+".txt"));
			} catch (IOException e) {
			}
		}
	}
	
	public static void close() {
		if (DEBUG_ENABLED) {
			try {
				if (fw != null)
					fw.close();
			} catch (IOException e) {
			}
		}
	}
	
	private static void printOutput(String output) {
		try {
			fw.write(output+'\n');
			fw.flush();
		} catch (IOException e) {
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
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"do("+Move.toString(currMove)+") @"+currPly);
		}
	}

	static void printUndoMove(int currPly, int currMove) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"undo("+Move.toString(currMove)+") @"+currPly);
		}
	}

	static void printBackUpScore(int currPly, int prevScore, int positionScore) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"backedUp was:"+prevScore+" now:"+positionScore+" @"+currPly);
		}
	}

	static void printPrincipalContinuation(int currPly, PrincipalContinuation pc) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"pc:"+pc.toStringAt(currPly));
		}
	}
	
	static void printMateFound( int currPly) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"possible mate @"+currPly);
		}
	}
	
	static void printRefutationFound( int currPly) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"ref @"+currPly);
		}
	}
	
	static void printAlphaBetaCutOffLimit(int currPly, int score) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
		}
	}

	public static void printHashIsTerminalNode(int currPly, ITransposition trans, long hash) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"hash "+hash+" term "+trans.report()+" @"+currPly);
		}
	}

	public static void printHashIsRefutation(int currPly, long hash, ITransposition trans) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"hash "+hash+" ref "+trans.report()+" @ Ply="+currPly);
		}
		
	}

	public static void printHashIsSeedMoveList(int currPly, int move, long hash) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"hash "+hash+" sufficient seed move list with best move:"+Move.toString(move)+" at Ply="+currPly);
		}
	}

	public static void printAlphaBetaComparison(int currPly, int prevPlyScore, int positionScore) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"ab cmp prev:"+prevPlyScore+" curr:"+positionScore+" @"+currPly);
		}
		
	}

	public static void printTransUpdate(int currPly, ITransposition trans, long hashCode) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+trans.report()+", hash: "+hashCode + " ref" + trans.toString());
		}		
	}

	public static void printTransNull(int currPly, long hashCode) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"trans is null, hash: "+hashCode);
		}		
	}
	
	public static void printCreateTrans(int currPly, long hashCode) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"trans create, hash: "+hashCode);
		}		
	}
	
	public static void printExactTrans(int currPly, long hashCode, ITransposition trans) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"trans now exact, hash: "+hashCode+" trans:"+trans.report());
		}		
	}

	public static void printStartPlyInfo(byte currPly, ScoreTracker st, IPositionAccessors pos, byte originalSearchDepthRequiredInPly) {
		if (DEBUG_ENABLED) {
			if (currPly == 0) {
				printOutput(String.format("\n\n\n NEW ITERATION %d\n\n\n", originalSearchDepthRequiredInPly));
			}
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"search @"+currPly+" prov="+st.getBackedUpScoreAtPly(currPly).getScore());
			printOutput(indent+"fen:"+pos.getFen());
		}
	}

	public static void setFileNameBaseString(String dateTime) {
		filenameBase = dateTime;
	}

	public static void printRepeatedPositionHash(byte currPly, long hash) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"3-fold rep @"+currPly+", hash: "+hash);
		}
	}

	public static void printTransDepthCheck(int currPly, int currentDepth, int newDepth) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"trans set @"+currPly+", depth curr: "+currentDepth+", depth new: "+newDepth);
		}		
	}

	public static void printTransBoundScoreCheck(int currPly, byte currentBound, short score, short score2) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"trans set @"+currPly+", bound:"+currentBound+", curr score: "+score+", new score: "+score2);
		}	
	}

	public static void inExtendedSearchAlternatives(int currPly, int currMove, short score) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"extSearch @"+currPly+", move:"+Move.toString(currMove)+", alt score: "+score);
		}
	}

	public static void printExtSearchNoMoves(byte currPly, Score theScore) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(indent+"extSearch NoMoves term @"+currPly+", score: "+theScore.getScore());
		}
	}
}