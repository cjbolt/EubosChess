package eubos.search;

import java.util.ArrayList;
import java.util.List;

import eubos.position.Move;

public class PrincipalContinuation {

	private List<List<Integer>> pc;
	private SearchDebugAgent sda;

	public PrincipalContinuation(int searchDepth, SearchDebugAgent sda) {
		pc = new ArrayList<List<Integer>>(searchDepth);
		for (int i=0; i<searchDepth; i++) {
			pc.add(new ArrayList<Integer>(searchDepth));
		}
		this.sda = sda;
	}
	
	public int getBestMove(byte currPly) {
		if (currPly < pc.size()) {
			List<Integer> plyList = pc.get(currPly);
			if (!plyList.isEmpty())
				return plyList.get(0);
		}
		return Move.NULL_MOVE;
	}
	
	public String toString() {
		StringBuilder output = new StringBuilder();
		for (int currPly = 0; currPly < pc.size(); currPly++) {
			output.append(String.format("Ply %d (%s) ,", currPly, toStringAt(currPly)));
		}
		return output.toString();
	}

	public String toStringAt(int currPly) {
		StringBuilder output = new StringBuilder();
		if (currPly < pc.size()) {
			List<Integer> plyList = pc.get(currPly);
			if (!plyList.isEmpty()) {
				for (int currMove : plyList) {
					//assert currMove != Move.NULL_MOVE;
					output.append((Move.toString(currMove)));
					output.append(' ');
				}
			}
		}
		return output.toString();
	}
	
	public static String pvToString(List<Integer> pv) {
		StringBuilder output = new StringBuilder();
		if (pv != null && !pv.isEmpty()) {
			for (int currMove : pv) {
				//assert currMove != Move.NULL_MOVE;
				output.append((Move.toString(currMove)));
				output.append(' ');
			}
		}
		return output.toString();
	}
	
	public List<Integer> toPvList(int currPly) { 
		List<Integer> mv = new ArrayList<Integer>(pc.size());
		if (currPly < pc.size()) {
			for (int currMove : pc.get(currPly)) {
				mv.add(currMove);
			}
		}
		return mv;
	}
	
	void initialise(int currPly, int currMove) {
		if (currPly < pc.size()) {
			List<Integer> plyToUpdatePc = pc.get(currPly);
			plyToUpdatePc.clear();
			plyToUpdatePc.add(currMove);
		}
	}
	
	void update(int currPly, int currMove) {
		if (currPly < pc.size()) {
			List<Integer> plyToUpdatePc = pc.get(currPly);
			plyToUpdatePc.clear();
			plyToUpdatePc.add(currMove);
			int nextPly = currPly+1;
			if (nextPly < pc.size()) {
				// Bring down, if possible
				plyToUpdatePc.addAll(pc.get(nextPly));
			}
			sda.printPrincipalContinuation(this);
		}
	}
	
	void set(int currPly, int currMove) {
		if (currPly < pc.size()) {
			List<Integer> plyToUpdatePc = pc.get(currPly);
			plyToUpdatePc.clear();
			plyToUpdatePc.add(currMove);
			clearContinuationBeyondPly(currPly);
			sda.printPrincipalContinuation(this);
		}
	}
	
	public void update(int currPly, List<Integer> onwards_pv) {
		if (currPly < pc.size()) {
			// Update a principal continuation from a Transposition hit
			List<Integer> plyToUpdatePc = pc.get(currPly);
			plyToUpdatePc.clear();
			if (onwards_pv != null) {
				plyToUpdatePc.addAll(onwards_pv);
			}
			clearContinuationBeyondPly(currPly);
		}
	}
	
	void clearContinuationBeyondPly(int currPly) {
		int nextPly = currPly+1;
		if (nextPly < pc.size()) {
			pc.get(nextPly).clear();
		}
	}
}
