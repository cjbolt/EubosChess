package eubos.search;

import java.util.ArrayList;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

class PrincipalContinuation {

	private GenericMove pc[][];
	private int searchDepthPly;

	PrincipalContinuation(int searchDepth) {
		pc = new GenericMove[searchDepth][searchDepth];
		searchDepthPly = searchDepth;
	}
	
	GenericMove getBestMove() {
		return pc[0][0];
	}
	
	GenericMove getBestMove(int currPly) {
		return pc[currPly][currPly];
	}

	String toStringAfter(int currPly) {
		GenericMove currMove = pc[currPly][currPly];
		String output = ""+currMove;
		for ( int nextPly = currPly+1; nextPly < searchDepthPly; nextPly++) {
			currMove = pc[currPly][nextPly];
			if (currMove == null)
				break;
			output+=(", "+currMove);
		}
		return output;
	}
	
	List<GenericMove> toPvList() {
		return toPvList(0);
	}
	
	List<GenericMove> toPvList(int startPly) {
		List<GenericMove> mv;
		mv = new ArrayList<GenericMove>();
		for (int currPly=startPly; currPly < searchDepthPly; currPly++) {
			GenericMove currMove = pc[startPly][currPly]; 
			if (currMove != null) {
				mv.add(currMove);
			}
		}
		return mv;
	}

	void update(int currPly, GenericMove currMove) {
		// Update Principal Continuation, bring down from next ply
		pc[currPly][currPly]=currMove;
		for (int nextPly=currPly+1; nextPly < searchDepthPly; nextPly++) {
			pc[currPly][nextPly]=pc[currPly+1][nextPly];
		}
		SearchDebugAgent.printPrincipalContinuation(currPly,this);
	}
	
	void update(int currPly, List<GenericMove> source_pc) {
		// Update principal continuation from Transposition hit
		int index = 0;
		for (int column=currPly; column < searchDepthPly; column++, index++) {
			for (int i=0; i <= index; i++) {
				try {
					pc[column][currPly+i]=source_pc.get(index);
				} catch (IndexOutOfBoundsException e) {
					pc[column][currPly+i]=null;
				}
			}		
		}
	}

	void clearAfter(int currPly ) {
		// Clear the principal continuation after the indicated ply depth
		for (int nextPly=currPly+1; nextPly < searchDepthPly; nextPly++) {
			for (int i=0; i<searchDepthPly; i++)
				// All plies need to be cleared.
				pc[i][nextPly]=null;
		}
	}

}
