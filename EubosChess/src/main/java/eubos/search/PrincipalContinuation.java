package eubos.search;

import java.util.ArrayList;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.Move;
import eubos.position.MoveList.MoveClassification;

public class PrincipalContinuation {

	private int pc[][];
	private int searchDepthPly;

	public PrincipalContinuation(int searchDepth) {
		pc = new int[searchDepth][searchDepth];
		searchDepthPly = searchDepth;
	}
	
	public GenericMove getBestMove() {
		return Move.toGenericMove(pc[0][0]);
	}
	
	public GenericMove getBestMove(int currPly) {
		return Move.toGenericMove(pc[currPly][currPly]);
	}

	String toStringAfter(int currPly) {
		int currMove = pc[currPly][currPly];
		String output = ""+Move.toString(currMove);
		for ( int nextPly = currPly+1; nextPly < searchDepthPly; nextPly++) {
			currMove = pc[currPly][nextPly];
			if (currMove == 0)
				break;
			output+=(", "+Move.toString(currMove));
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
			GenericMove currMove = Move.toGenericMove(pc[startPly][currPly]); 
			if (currMove != null) {
				mv.add(currMove);
			}
		}
		return mv;
	}

	void update(int currPly, int currMove) {
		// Update Principal Continuation, bring down from next ply
		pc[currPly][currPly]=currMove;
		for (int nextPly=currPly+1; nextPly < searchDepthPly; nextPly++) {
			pc[currPly][nextPly]=pc[currPly+1][nextPly];
		}
		SearchDebugAgent.printPrincipalContinuation(currPly,this);
	}
	
	public void update(int currPly, List<GenericMove> source_pc) {
		// Update principal continuation from Transposition hit
		int pc_len = source_pc.size();
		int index = 0;
		for (int column=currPly; column < searchDepthPly; column++, index++) {
			for (int i=0; i <= index; i++) {
				if (index < pc_len) {
					GenericMove currMove = source_pc.get(index);
					MoveClassification type = (currMove.promotion != null) ?  
							MoveClassification.PROMOTION : MoveClassification.REGULAR;
					pc[currPly+i][column]=Move.toMove(currMove, type);
				} else {
					/* Note: if the principal continuation ends in a mate, 
					 * it is valid that the continuation can be shorter than
					 * the depth searched.
				     */
					pc[currPly+i][column]=0;
				}
			}		
		}
	}
	
	void clearTreeBeyondPly(int currPly) {
		// Clear all principal continuation plies after the specified ply depth
		int index = 0;
		for (int column=currPly; column < searchDepthPly; column++, index++) {
			for (int i=0; i <= index; i++) {
			    pc[currPly+i][column] = 0;
			}		
		}
	}

	void truncateAfterPly(int currPly ) {
		// Truncate all variations after the specified ply depth
		for (int nextPly=currPly+1; nextPly < searchDepthPly; nextPly++) {
			for (int i=0; i<searchDepthPly; i++)
				// All plies need to be cleared.
				pc[i][nextPly] = 0;
		}
	}

	public void clearRowsBeyondPly(int clearAfter) {
		for (int row=clearAfter+1; row < searchDepthPly; row++) {
			for (int column=0; column<searchDepthPly; column++) {
				pc[row][column] = 0;
			}
		}
	}

}
