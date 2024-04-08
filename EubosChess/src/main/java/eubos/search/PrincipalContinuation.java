package eubos.search;

import java.util.Arrays;

import eubos.main.EubosEngineMain;
import eubos.position.Move;

public class PrincipalContinuation {

	private int [][] pc;
	public int [] length;
	private SearchDebugAgent sda;

	public PrincipalContinuation(int searchDepth, SearchDebugAgent sda) {
		// Create the pc list at each ply
		int max_length = EubosEngineMain.SEARCH_DEPTH_IN_PLY + 1;
		pc = new int[max_length][];
		for (int i = 0; i < max_length; i++) {
			pc[i] = new int[max_length-i];
		}
		length = new int[max_length];
		this.sda = sda;
	}
	
	public int getBestMove(byte currPly) {
		if (currPly < length[0])
			return pc[0][currPly];
		return Move.NULL_MOVE;
	}
	
	public int getBestMoveAtPly(byte currPly) {
		if (length[currPly] >= 1)
			return pc[currPly][0];
		return Move.NULL_MOVE;
	}
	
	public String toString() {
		StringBuilder output = new StringBuilder();
		for (int currPly = 0; currPly < length[0]; currPly++) {
			output.append(String.format("Ply %d (%s) ,", currPly, toStringAt(currPly)));
		}
		return output.toString();
	}

	public String toStringAt(int currPly) {
		StringBuilder output = new StringBuilder();
		if (currPly < EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
			if (length[currPly] != 0) {
				for (int i=0; i<length[currPly]; i++) {
					int currMove = pc[currPly][i];
					if (EubosEngineMain.ENABLE_ASSERTS) {
						assert currMove != Move.NULL_MOVE;
					}
					output.append(Move.toString(currMove));
					output.append(' ');
				}
			}
		}
		return output.toString();
	}
	
	public int [] toPvList(int currPly) { 
		if (length[currPly] != 0) {
			return Arrays.copyOfRange(pc[currPly], 0, length[0]);
		}
		return new int[] {Move.NULL_MOVE};
	}
	
	void initialise(int currPly, int currMove) {
		length[currPly] = 1;
		pc[currPly][0] = currMove;
	}
	
	void initialise(int currPly) {
		length[currPly] = 0;
	}
	
	// Bring down a pv from node further down the tree, with curr move added at the head
	void update(int currPly, int currMove) {
		initialise(currPly, currMove);
		int nextPly = currPly+1;
		for (int i=0; i<length[nextPly]; i++) {
			pc[currPly][i+1] = pc[nextPly][i];
			length[currPly] += 1;
		}
		if (SearchDebugAgent.DEBUG_ENABLED) sda.printPrincipalContinuation(this);
	}
	
	// Update a principal continuation from a Transposition hit where we don't have onwards pv
	void set(int currPly, int currMove) {
		initialise(currPly, currMove);
		clearContinuationBeyondPly(currPly);
		if (SearchDebugAgent.DEBUG_ENABLED) sda.printPrincipalContinuation(this);
	}
	
	// Clear all downstream pv's, from the current ply
	void clearContinuationBeyondPly(int currPly) {
		length[currPly+1] = 0;
	}
	
	void clearPvOnAspFail() {
		Arrays.fill(pc[0],0);
	}
	
	//-------------------------------------------------------------------------
	// Test API
	public void setArray(int [] array) {
		pc[0] = array;
		length[0] = array.length;
	}
}
