package eubos.search;

import java.util.HashMap;

public class DrawChecker {
	private HashMap<Integer,Byte> positionCount;
	
	public DrawChecker() {
		positionCount = new HashMap<Integer,Byte>();
	}
	
	public void reset() {
		positionCount.clear();
	}
	
	public boolean incrementPositionReachedCount(Long posHash) {
		boolean repetitionPossible = false;
		Integer truncatedHash = (int) (posHash >> 32);
		Byte count = positionCount.get(truncatedHash);
		if (count == null) {
			positionCount.put(truncatedHash, (byte)1);
		} else {
			count++;
			positionCount.put(truncatedHash, count);
			if (count >= 2) {
				repetitionPossible = true;
			}
		}
		return repetitionPossible;
	}
	
	public Byte getPositionReachedCount(Long posHash) {
		Integer truncatedHash = (int) (posHash >> 32);
		return positionCount.get(truncatedHash);
	}

	public boolean isPositionOpponentCouldClaimDraw(long positionHash) {
		boolean opponentCouldClaimDraw = false;
		Byte reachedCount = getPositionReachedCount(positionHash);
		if (reachedCount != null && reachedCount >= 2) {
			opponentCouldClaimDraw = true;
		}
		return opponentCouldClaimDraw;
	}

	public void decrementPositionReachedCount(long posHash) {
		Integer truncatedHash = (int) (posHash >> 32);
		Byte count = positionCount.get(truncatedHash);
		if (count == null) {
			// Now we clear the drawchecker in some circumstances this isn't a failure
			//assert false;
		} else {
			count--;
			if (count == 0) {
				positionCount.remove(truncatedHash);
			} else {
				positionCount.put(truncatedHash, count);
			}
		}
	}
	
	public Integer getNumEntries() {
		return positionCount.size();
	}
	
	public String toString() {
		return positionCount.toString();
	}
}
