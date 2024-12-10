package eubos.search;

import eubos.main.EubosEngineMain;
import it.unimi.dsi.fastutil.longs.LongArrays;

public class DrawChecker {
	
	public long[] reachedPositions;
	public int checkFromPly;
	
	public DrawChecker() {
		reachedPositions = new long[EubosEngineMain.MAXIMUM_PLIES_IN_GAME];
	}
	
	public DrawChecker(DrawChecker clone) {
		reachedPositions = LongArrays.copy(clone.reachedPositions);
		checkFromPly = clone.checkFromPly;
	}
	
	public void reset(int plyNumber) {
		checkFromPly = plyNumber;
	}
	
	public void clearAfter(int plyNumber) {
		//for (int i=plyNumber; i<reachedPositions.length; i++) {
		//	reachedPositions[i]=0;
		//}
		if (plyNumber < reachedPositions.length)
			reachedPositions[plyNumber]=0;
	}
		
	public boolean setPositionReached(long posHash, int gamePly) {
		// Check for array overflow before reading/writing array
		if (gamePly > reachedPositions.length - 1) {
			reachedPositions = LongArrays.grow(reachedPositions, reachedPositions.length+50);
		}
		reachedPositions[gamePly] = posHash;
		return isPositionReachedBefore(posHash, gamePly);
	}
	
	private boolean isPositionReachedBefore(long posHash, int currentPly) {
//		// Only search previous positions at odd/even ply depth, don't need to check if not same on move. 
//		int offset = (checkFromPly & 0x1) != (currentPly & 0x1) ? 1 : 0;
//		for (int i=checkFromPly+offset; i < currentPly; i+=2 ) {
//			if (reachedPositions[i] == posHash)
//				return true;
//		}
		// Check all positions because null moves mean that sometimes the same side can be on move for successive moves
		for (int i=checkFromPly; i < currentPly; i+=1 ) {
			if (reachedPositions[i] == posHash)
				return true;
		}
		return false;
	}
	
	public String report(int currPly) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("Drawchecker reachedPositions[%d:%d] are {\n",checkFromPly, currPly));
		long array[] = LongArrays.copy(reachedPositions, checkFromPly, Math.max(0, currPly-checkFromPly));
		for (long hash: array) {
			sb.append('\t');
			sb.append(hash);
			sb.append(",\n");
		}
		sb.append("}\n");
		return sb.toString();
	}
}
