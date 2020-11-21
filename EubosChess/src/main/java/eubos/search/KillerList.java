package eubos.search;

import eubos.position.Move;

public class KillerList {
	
	public static final boolean ENABLE_KILLER_MOVES = false;
	
	private int [][] killerList = null;
	private int [] replaceIndex = null;
	
	public KillerList(int maxDepth) {
		if (ENABLE_KILLER_MOVES) {
			// Initialise the killer list
			killerList = new int[maxDepth][];
			for (int i=0; i<maxDepth; i++) {
				killerList[i] = new int [] { Move.NULL_MOVE, Move.NULL_MOVE };
			}
			replaceIndex = new int[maxDepth];
		}
	}
	
	public void addMove(int ply, int move) {
		if (ENABLE_KILLER_MOVES) {
			if (ply < killerList.length) {
				int indexToUpdate = replaceIndex[ply];
				int existingMove = killerList[ply][indexToUpdate];
				if (Move.areEqualForBestKiller(move, existingMove)) {
					// Don't replace if the move to replace is already this killer
					return;
				}
				
				int otherIndex = (indexToUpdate == 0) ? 1 : 0;
				int otherMove = killerList[ply][otherIndex];
				if (Move.areEqualForBestKiller(move, otherMove)) {
					// Don't allow both moves to be the same, if they are, don't add the new move
				} else {
					// update the move and change the index to update next time
					killerList[ply][indexToUpdate] = move;
					replaceIndex[ply] = (indexToUpdate == 0) ? 1 : 0;
				}
			}
		}
	}
	
	public int getMove(int ply) {
		if (ENABLE_KILLER_MOVES) {
			if (ply < killerList.length) {
				return killerList[ply][0];
			}
		}
		return Move.NULL_MOVE;
	}
	
	public int[] getMoves(int ply) {
		if (ENABLE_KILLER_MOVES) {
			if (ply < killerList.length) {
				return killerList[ply];
			}
		}
		return new int [] { Move.NULL_MOVE, Move.NULL_MOVE };
	}
	
	public void shuffleList(int ply) {
		if (ENABLE_KILLER_MOVES) {
			int [][] temp = new int[killerList.length][];
			for (int i=0; i<(killerList.length-ply); i++) {
				temp[i] = killerList[i+ply];
			}
			killerList = temp;
		}
	}
}
