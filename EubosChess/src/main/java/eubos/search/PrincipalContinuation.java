package eubos.search;

import eubos.main.EubosEngineMain;
import eubos.position.Move;
import it.unimi.dsi.fastutil.ints.IntArrays;

public class PrincipalContinuation {

	private int [][] pc;
	private int [] length;
	private SearchDebugAgent sda;

	public PrincipalContinuation(int searchDepth, SearchDebugAgent sda) {
		// Create the pc list at each ply
		pc = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY][];
		for (int i = 0; i < EubosEngineMain.SEARCH_DEPTH_IN_PLY; i++) {
			pc[i] = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY-i];
		}
		length = new int[EubosEngineMain.SEARCH_DEPTH_IN_PLY];
		this.sda = sda;
	}
	
	public int getBestMove(byte currPly) {
		if (currPly < EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
			if (length[currPly] != 0)
				return pc[currPly][0];
		}
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
		if (currPly < EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
			int [] pv = IntArrays.trim(pc[currPly], length[currPly]);
			return pv;
		}
		return null;
	}
	
	void initialise(int currPly, int currMove) {
		if (currPly < EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
			length[currPly] = 1;
			pc[currPly][0] = currMove;
		}
	}
	
	// Bring down a pv from node further down the tree, with curr move added at the head
	void update(int currPly, int currMove) {
		if (currPly < EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
			length[currPly] = 1;
			pc[currPly][0] = currMove;
			int nextPly = currPly+1;
			if (nextPly < EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
				// Bring down, if possible
				for (int i=0; i<length[nextPly]; i++) {
					pc[currPly][i+1] = pc[nextPly][i];
					length[currPly] += 1;
				}
			}
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printPrincipalContinuation(this);
		}
	}
	
	// Update a principal continuation from a Transposition hit where we don't have onwards pv
	void set(int currPly, int currMove) {
		if (currPly < EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
			length[currPly] = 1;
			pc[currPly][0] = currMove;
			clearContinuationBeyondPly(currPly);
			if (SearchDebugAgent.DEBUG_ENABLED) sda.printPrincipalContinuation(this);
		}
	}
	
	// Clear all downstream pv's, from the current ply
	void clearContinuationBeyondPly(int currPly) {
		int nextPly = currPly+1;
		if (nextPly < EubosEngineMain.SEARCH_DEPTH_IN_PLY) {
			length[nextPly] = 0;
		}
	}
}
