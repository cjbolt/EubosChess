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
			printOutput(String.format("%sdo(%s) @%d", indent, Move.toString(currMove), currPly));
		}
	}

	static void printUndoMove(int currPly, int currMove) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%sundo(%s) @%d", indent, Move.toString(currMove), currPly));
		}
	}

	static void printBackUpScore(int currPly, int prevScore, int positionScore) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%sbackedUp was:%d now:%d @%d", indent, prevScore, positionScore, currPly));
		}
	}

	static void printPrincipalContinuation(int currPly, PrincipalContinuation pc) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%spc:%s", indent, pc.toStringAt(currPly)));
		}
	}
	
	static void printMateFound( int currPly) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%spossible mate @%d", indent, +currPly));
		}
	}
	
	static void printRefutationFound( int currPly) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%sref @%d", indent, currPly));
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
			printOutput(String.format("%shash:%d term:%s @%d", indent, hash, trans.report(), currPly));
		}
	}

	public static void printHashIsRefutation(int currPly, long hash, ITransposition trans) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%shash:%d ref:%s @%d", indent, hash, trans.report(), currPly));
		}
		
	}

	public static void printHashIsSeedMoveList(int currPly, int move, long hash) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%shash:%d sufficient seed ml with best:%s @%d", indent, hash, Move.toString(move), currPly));
		}
	}

	public static void printAlphaBetaComparison(int currPly, int prevPlyScore, int positionScore) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%sab cmp prev:%d curr:%d @%d", indent, prevPlyScore, positionScore, currPly));
		}
		
	}

	public static void printTransUpdate(int currPly, ITransposition trans, long hashCode) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%s%s hash:%d object:%s", indent, trans.report(), hashCode, trans.toString()));
		}		
	}

	public static void printTransNull(int currPly, long hashCode) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%strans is null, hash:%d", indent, hashCode));
		}		
	}
	
	public static void printCreateTrans(int currPly, long hashCode) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%strans create, hash:%d", indent, hashCode));
		}		
	}
	
	public static void printExactTrans(int currPly, long hashCode, ITransposition trans) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%strans now exact, hash:%d trans:%s", indent, hashCode, trans.report()));
		}		
	}

	public static void printStartPlyInfo(byte currPly, ScoreTracker st, IPositionAccessors pos, byte originalSearchDepthRequiredInPly) {
		if (DEBUG_ENABLED) {
			if (currPly == 0) {
				printOutput(String.format("\n\n\n NEW ITERATION %d\n\n\n", originalSearchDepthRequiredInPly));
			}
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%ssearch @:%d prov:%d", indent, currPly, st.getBackedUpScoreAtPly(currPly).getScore()));
			printOutput(String.format("%sfen:%s", indent, pos.getFen()));
		}
	}

	public static void setFileNameBaseString(String dateTime) {
		filenameBase = dateTime;
	}

	public static void printRepeatedPositionHash(byte currPly, long hash, String fen) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%s3-fold rep @%d hash:%d fen:%s", indent, currPly, hash, fen));
		}
	}

	public static void printTransDepthCheck(int currPly, int currentDepth, int newDepth) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%strans set @%d depth curr:%d depth new:%d", indent, currPly, currentDepth, newDepth));
		}		
	}

	public static void printTransBoundScoreCheck(int currPly, byte currentBound, short score, short score2) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%strans set @%d bound:%d curr score:%d new score:%d", indent, currPly, currentBound, score, score2));
		}	
	}

	public static void inExtendedSearchAlternatives(int currPly, int currMove, short score) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%sextSearch @%d move:%s alt score:%d", indent, currPly, Move.toString(currMove), score));
		}
	}

	public static void printExtSearchNoMoves(byte currPly, Score theScore) {
		if (DEBUG_ENABLED) {
			if ( currPly != lastPly )
				computeIndent(currPly);
			printOutput(String.format("%sextSearch NoMoves term @%d score:%s", indent, currPly, theScore.getScore()));
		}
	}
}