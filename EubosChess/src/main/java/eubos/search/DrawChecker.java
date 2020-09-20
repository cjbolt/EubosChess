package eubos.search;

import java.util.concurrent.ConcurrentHashMap;

public class DrawChecker {
	private ConcurrentHashMap<Long,Byte> positionCount;
	
	public DrawChecker() {
		positionCount = new ConcurrentHashMap<Long,Byte>();
	}
	
	public void reset() {
		positionCount.clear();
	}
	
	public boolean incrementPositionReachedCount(Long posHash) {
		boolean repetitionPossible = false;
		Byte count = positionCount.get(posHash);
		if (count == null) {
			positionCount.put(posHash, (byte)1);
		} else {
			count++;
			positionCount.put(posHash, count);
			if (count >= 2) {
				repetitionPossible = true;
			}
		}
		return repetitionPossible;
	}
	
	public Byte getPositionReachedCount(Long posHash) {
		return positionCount.get(posHash);
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
		Byte count = positionCount.get(posHash);
		if (count == null) {
			// Now we clear the drawchecker in some circumstances this isn't a failure
			//assert false;
		} else {
			count--;
			if (count == 0) {
				positionCount.remove(posHash);
			} else {
				positionCount.put(posHash, count);
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
