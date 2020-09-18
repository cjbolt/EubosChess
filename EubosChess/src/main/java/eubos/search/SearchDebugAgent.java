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
	private static FileWriter fw;
	private static String filenameBase = "";
	private static int currPly = 0;
	
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
	
	private static void computeIndent() {
		indent="";
		for (int i=0; i<currPly; i++) {
			indent += "\t";
		}
	}
	
	public static void nextPly() {
		if (DEBUG_ENABLED) {
			currPly++;
			computeIndent();
		}
	}
	
	public static void prevPly() {
		if (DEBUG_ENABLED) {
			currPly--;
			computeIndent();
		}
	}

	static void printPerformMove(int currMove) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%sdo(%s) @%d", indent, Move.toString(currMove), currPly));
		}
	}

	static void printUndoMove(int currMove) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%sundo(%s) @%d", indent, Move.toString(currMove), currPly));
		}
	}

	static void printBackUpScore(int currPly, int prevScore, int positionScore) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%sbackedUp was:%d now:%d @%d", indent, prevScore, positionScore, currPly));
		}
	}

	static void printPrincipalContinuation(PrincipalContinuation pc) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%spc:%s", indent, pc.toStringAt(currPly)));
		}
	}
	
	static void printMateFound() {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%spossible mate @%d", indent, +currPly));
		}
	}
	
	static void printRefutationFound() {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%sref @%d", indent, currPly));
		}
	}
	
	static void printAlphaBetaCutOffLimit(int score) {
		if (DEBUG_ENABLED) {
		}
	}

	public static void printHashIsTerminalNode(ITransposition trans, long hash) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%shash:%d term:%s @%d", indent, hash, trans.report(), currPly));
		}
	}

	public static void printHashIsRefutation(long hash, ITransposition trans) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%shash:%d ref:%s @%d", indent, hash, trans.report(), currPly));
		}
		
	}

	public static void printHashIsSeedMoveList(int move, long hash) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%shash:%d sufficient seed ml with best:%s @%d", indent, hash, Move.toString(move), currPly));
		}
	}

	public static void printAlphaBetaComparison(int prevPlyScore, int positionScore) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%sab cmp prev:%d curr:%d @%d", indent, prevPlyScore, positionScore, currPly));
		}
		
	}

	public static void printTransUpdate(ITransposition trans, long hashCode) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%s%s hash:%d object:%s", indent, trans.report(), hashCode, trans.toString()));
		}		
	}

	public static void printTransNull(long hashCode) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%strans is null, hash:%d", indent, hashCode));
		}		
	}
	
	public static void printCreateTrans(long hashCode) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%strans create, hash:%d", indent, hashCode));
		}		
	}
	
	public static void printExactTrans(long hashCode, ITransposition trans) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%strans now exact, hash:%d trans:%s", indent, hashCode, trans.report()));
		}		
	}

	public static void printStartPlyInfo(ScoreTracker st, IPositionAccessors pos, byte originalSearchDepthRequiredInPly) {
		if (DEBUG_ENABLED) {
			if (currPly == 0) {
				printOutput(String.format("\n\n\n NEW ITERATION %d\n\n\n", originalSearchDepthRequiredInPly));
			}
			printOutput(String.format("%ssearch @:%d prov:%d", indent, currPly, st.getBackedUpScoreAtPly((byte)currPly).getScore()));
			printOutput(String.format("%sfen:%s", indent, pos.getFen()));
		}
	}

	public static void setFileNameBaseString(String dateTime) {
		filenameBase = dateTime;
	}

	public static void printRepeatedPositionHash(long hash, String fen) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%s3-fold in hash hit rep @%d hash:%d fen:%s", indent, currPly, hash, fen));
		}
	}

	public static void printTransDepthCheck(int currentDepth, int newDepth) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%strans set @%d depth curr:%d depth new:%d", indent, currPly, currentDepth, newDepth));
		}		
	}

	public static void printTransBoundScoreCheck(byte currentBound, short score, short score2) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%strans set @%d bound:%d curr score:%d new score:%d", indent, currPly, currentBound, score, score2));
		}	
	}

	public static void inExtendedSearchAlternatives(int currMove, short score) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%sextSearch @%d move:%s alt score:%d", indent, currPly, Move.toString(currMove), score));
		}
	}

	public static void printExtSearchNoMoves(Score theScore) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%sextSearch NoMoves term @%d score:%s", indent, currPly, theScore.getScore()));
		}
	}

	public static void printRepeatedPositionSearch(long hash, String fen) {
		if (DEBUG_ENABLED) {
			printOutput(String.format("%s3-fold in search rep @%d hash:%d fen:%s", indent, currPly, hash, fen));
		}
	}
}