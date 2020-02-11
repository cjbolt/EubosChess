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
	
	public int getBestMove(byte currPly) {
		if (currPly < pc.size()) {
			List<Integer> plyList = pc.get(currPly);
			if (!plyList.isEmpty())
				return plyList.get(0);
		}
		return Move.NULL_MOVE;
	}
	
	public String toString() {
		String output = "";
		for (int currPly = 0; currPly < pc.size(); currPly++) {
			output += String.format("Ply %d (%s) ,", currPly, toStringAt(currPly));
		}
		return output;
	}

	String toStringAt(int currPly) {
		String output = "";
		if (currPly < pc.size()) {
			List<Integer> plyList = pc.get(currPly);
			if (!plyList.isEmpty()) {
				for (int currMove : plyList) {
					assert currMove != Move.NULL_MOVE;
					output+=(Move.toString(currMove)+" ");
				}
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
		if (currPly < pc.size()) {
			List<Integer> plyToUpdatePc = pc.get(currPly);
			plyToUpdatePc.clear();
			plyToUpdatePc.add(currMove);
			int nextPly = currPly+1;
			if (nextPly < pc.size()) {
				// Bring down, if possible
				plyToUpdatePc.addAll(pc.get(nextPly));
			}
			SearchDebugAgent.printPrincipalContinuation(currPly, this);
		}
	}
	
	public void update(int currPly, List<Integer> source_pc) {
		if (currPly < pc.size()) {
			// Update principal continuation from Transposition hit
			List<Integer> plyToUpdatePc = pc.get(currPly);
			plyToUpdatePc.clear();
			plyToUpdatePc.addAll(source_pc);
			clearContinuationsBeyondPly(currPly);
			// question set up plies beyond this one?
		}
	}
	
	void clearContinuationsBeyondPly(int currPly) {
		int nextPly = currPly+1;
		if (nextPly < pc.size()) {
			for (List<Integer> plyList : pc.subList(nextPly, pc.size())) {
				plyList.clear();
			}
		}
	}
}
