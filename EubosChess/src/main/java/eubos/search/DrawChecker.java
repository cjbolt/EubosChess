package eubos.search;

import java.util.concurrent.ConcurrentHashMap;

public class DrawChecker {
	private ConcurrentHashMap<Long,Byte> positionCount;
	
	public DrawChecker() {
		positionCount = new ConcurrentHashMap<Long,Byte>();
	}
	
	public void incrementPositionReachedCount(Long posHash) {
		Byte count = positionCount.get(posHash);
		if (count == null) {
			positionCount.put(posHash, (byte)1);
		} else {
			count++;
			positionCount.put(posHash, count);
		}
	}
	
	public Byte getPositionReachedCount(Long posHash) {
		return positionCount.get(posHash);
	}

	public boolean isPositionDraw(long positionHash) {
		boolean isDrawn = false;
		Byte reachedCount = getPositionReachedCount(positionHash);
		if (reachedCount != null && reachedCount >= 2) {
			isDrawn = true;
		}
		return isDrawn;
	}

	public void decrementPositionReachedCount(long posHash) {
		Byte count = positionCount.get(posHash);
		if (count == null) {
			assert false;
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
