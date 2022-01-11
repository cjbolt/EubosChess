package eubos.search;

import eubos.main.EubosEngineMain;
import eubos.position.Move;

public class KillerList {
	
	public static final boolean ENABLE_KILLER_MOVES = true;
	private static final int NUM_KILLERS_AT_PLY = 3;
	
	private int [][] killerList = null;
	private int [] replaceIndex = null;
	
	public KillerList() {
		if (ENABLE_KILLER_MOVES) {
			int maxDepth = EubosEngineMain.SEARCH_DEPTH_IN_PLY;
			// Initialise the killer list
			killerList = new int[maxDepth][];
			for (int i=0; i<maxDepth; i++) {
				killerList[i] = new int [NUM_KILLERS_AT_PLY];
			}
			replaceIndex = new int[maxDepth];
		}
	}
	
	public static boolean isMoveOnListAtPly(int [] list, int move) {
		if (KillerList.ENABLE_KILLER_MOVES && list != null) {
			for (int listMove : list) {
				if (Move.areEqualForBestKiller(listMove, move)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public void addMove(int ply, int move) {
		if (ENABLE_KILLER_MOVES) {
			if (Move.isNotCaptureOrPromotion(move) && ply < killerList.length) {
				if (!isMoveOnListAtPly(killerList[ply], move)) {
					// update the move and change the index to update next time
					int indexToUpdate = replaceIndex[ply];
					killerList[ply][indexToUpdate] = move;
					indexToUpdate++;
					if (indexToUpdate >= NUM_KILLERS_AT_PLY) {
						indexToUpdate = 0;
					}
					replaceIndex[ply] = indexToUpdate;
				}
			}
		}
	}
	
	public int[] getMoves(int ply) {
		if (ENABLE_KILLER_MOVES) {
			if (ply < killerList.length) {
				return killerList[ply];
			}
		}
		return new int [] { Move.NULL_MOVE, Move.NULL_MOVE,Move.NULL_MOVE };
	}
}
