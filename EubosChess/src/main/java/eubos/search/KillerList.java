package eubos.search;

import eubos.position.Move;

public class KillerList {
	
	private int [] killerList = null;
	
	public KillerList(int maxDepth) {
		killerList = new int[maxDepth];
		for (int i=0; i<maxDepth; i++) {
			killerList[i] = Move.NULL_MOVE;
		}
	}
	
	public void addMove(int ply, int move) {
		if (ply < killerList.length) {
			killerList[ply] = move;
		}
	}
	
	public int getMove(int ply) {
		if (ply < killerList.length) {
			return killerList[ply];
		} else {
			return Move.NULL_MOVE;
		}
	}
	
	public void shuffleList(int ply) {
		int [] temp = new int[killerList.length];
		for (int i=0; i<(killerList.length-ply); i++) {
			temp[i] = killerList[i+ply];
		}
		killerList = temp;
	}
}
