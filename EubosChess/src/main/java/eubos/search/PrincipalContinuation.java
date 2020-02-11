package eubos.search;

import java.util.ArrayList;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.Move;

public class PrincipalContinuation {

	private List<List<Integer>> pc;

	public PrincipalContinuation(int searchDepth) {
		pc = new ArrayList<List<Integer>>(searchDepth);
		for (int i=0; i<searchDepth; i++) {
			pc.add(new ArrayList<Integer>());
		}
	}
	
	public GenericMove getBestMove() {
		List<Integer> plyList = pc.get(0);
		return (!plyList.isEmpty()) ? Move.toGenericMove(plyList.get(0)) : null;
	}
	
	public GenericMove getBestMove(int currPly) {
		if (currPly < pc.size()) {
			List<Integer> plyList = pc.get(currPly);
			return (!plyList.isEmpty()) ? Move.toGenericMove(plyList.get(0)) : null;
		}
		return null;
	}
	
	public int getBestMoveAsInt(byte currPly) {
		if (currPly < pc.size()) {
			List<Integer> plyList = pc.get(currPly);
			return (!plyList.isEmpty()) ? plyList.get(0) : Move.NULL_MOVE;
		}
		return Move.NULL_MOVE;
	}
	
	public String toString() {
		String output = "";
		for (int currPly = 0; currPly < pc.size(); currPly++) {
			output += String.format("Ply %d (%s) ,", currPly, toStringAfter(currPly));
		}
		return output;
	}

	String toStringAfter(int currPly) {
		String output = "";
		List<Integer> plyList = pc.get(currPly);
		if (!plyList.isEmpty()) {
			for (int currMove : plyList) {
				assert currMove != Move.NULL_MOVE;
				output+=(Move.toString(currMove)+" ");
			}
		}
		return output;
	}
	
	public List<GenericMove> toPvList() { 
		List<GenericMove> mv = new ArrayList<GenericMove>();
		for (int currMove : pc.get(0)) {
			mv.add(Move.toGenericMove(currMove));
		}
		return mv;
	}
	
	void update(int currPly, int currMove) {
		// Update Principal Continuation, bring down from the next ply
		List<Integer> plyToUpdatePc = pc.get(currPly);
		plyToUpdatePc.clear();
		plyToUpdatePc.add(currMove);
		if (currPly+1 < pc.size()) {
			// Bring down if possible
			plyToUpdatePc.addAll(pc.get(currPly+1));
		}
		SearchDebugAgent.printPrincipalContinuation(currPly, this);
	}
	
	public void update(int currPly, List<Integer> source_pc) {
		// Update principal continuation from Transposition hit
		List<Integer> plyToUpdatePc = pc.get(currPly);
		plyToUpdatePc.clear();
		plyToUpdatePc.addAll(source_pc);
		clearTreeBeyondPly(currPly);
		// question set up plies beyond this one?
	}
	
	void clearTreeBeyondPly(int currPly) {
		// Clear all principal continuations beyond the specified depth
		for (List<Integer> plyList : pc.subList(currPly+1, pc.size())) {
			plyList.clear();
		}
	}

	void truncateAfterPly(int currPly ) {
		/*
		// Truncate all variations after the specified ply depth
		int truncateFrom = currPly+1;
		clearTreeBeyondPly(currPly);
		for (List<Integer> plyList : pc) {
			if (truncateFrom < plyList.size())
				plyList.subList(truncateFrom, plyList.size()).clear();
		}
		*/
		// this is not sensible with lists
	}

	public void clearRowsBeyondPly(int clearAfter) {
		clearTreeBeyondPly(clearAfter);
		/*int firstPlyToClear = clearAfter+1;
		if (firstPlyToClear < pc.size()) {
			for (int ply=firstPlyToClear; ply < searchDepthPly; ply++) {
				pc.get(ply).clear();
			}
		}*/
	}
}
