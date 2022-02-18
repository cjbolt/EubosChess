package eubos.search;

import it.unimi.dsi.fastutil.longs.LongArrays;

public class DrawChecker {
	
	public long[] reachedPositions;
	public int checkFromPly;
	
	public DrawChecker() {
		reachedPositions = new long[300];
	}
	
	public DrawChecker(DrawChecker clone) {
		reachedPositions = LongArrays.copy(clone.reachedPositions);
		checkFromPly = clone.checkFromPly;
	}
	
	public void reset(int plyNumber) {
		checkFromPly = plyNumber;
	}
		
	public boolean setPositionReached(long posHash, int gamePly) {
		boolean repetitionPossible = false;
		if (isPositionReachedBefore(posHash, gamePly)) {
			repetitionPossible = true;
		} else {
			reachedPositions[gamePly] = posHash;
		}
		return repetitionPossible;
	}
	
	private boolean isPositionReachedBefore(long posHash, int currentPly) {
		// Only search previous positions at odd/even ply depth, don't need to check if not same on move. 
		int offset = (checkFromPly & 0x1) != (currentPly & 0x1) ? 1 : 0;
		for (int i=checkFromPly+offset; i < currentPly; i+=2 ) {
			if (reachedPositions[i] == posHash)
				return true;
		}
		return false;
	}
}
