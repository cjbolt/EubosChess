package eubos.search;

import java.util.ArrayList;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

public class PrincipalContinuation {

	private GenericMove pc[][];
	private int searchDepthPly;

	public PrincipalContinuation(int searchDepth) {
		pc = new GenericMove[searchDepth][searchDepth];
		searchDepthPly = searchDepth;
	}
	
	public GenericMove getBestMove() {
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
	
	public List<GenericMove> toPvList() {
		return toPvList(0);
	}
	
	public List<GenericMove> toPvList(int startPly) {
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
		int pc_len = source_pc.size();
		int index = 0;
		for (int column=currPly; column < searchDepthPly; column++, index++) {
			for (int i=0; i <= index; i++) {
				if (index < pc_len) {
					pc[currPly+i][column]=source_pc.get(index);
				} else {
					/* Note: if the principal continuation ends in a mate, 
					 * it is valid that the continuation can be shorter than
					 * the depth searched.
				     */
					pc[currPly+i][column]=null;
				}
			}		
		}
	}
	
	void clearTreeBeyondPly(int currPly) {
		// Clear all principal continuation plies after the specified ply depth
		int index = 0;
		for (int column=currPly; column < searchDepthPly; column++, index++) {
			for (int i=0; i <= index; i++) {
			    pc[currPly+i][column] = null;
			}		
		}
	}

	void truncateAfterPly(int currPly ) {
		// Truncate all variations after the specified ply depth
		for (int nextPly=currPly+1; nextPly < searchDepthPly; nextPly++) {
			for (int i=0; i<searchDepthPly; i++)
				// All plies need to be cleared.
				pc[i][nextPly]=null;
		}
	}

	public void clearRowsBeyondPly(int clearAfter) {
		for (int row=clearAfter+1; row < searchDepthPly; row++) {
			for (int column=0; column<searchDepthPly; column++) {
				pc[row][column]=null;
			}
		}
	}

}
