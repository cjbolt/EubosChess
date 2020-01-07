package eubos.search;

import java.util.concurrent.ConcurrentHashMap;

public class DrawChecker {
	private ConcurrentHashMap<Long,Integer> positionCount;
	
	public DrawChecker() {
		positionCount = new ConcurrentHashMap<Long,Integer>();
	}
	
	public void incrementPositionReachedCount(Long posHash) {
		Integer count = positionCount.get(posHash);
		if (count == null) {
			positionCount.put(posHash, 1);
		} else {
			count+=1;
			positionCount.put(posHash, count);
		}
	}
	
	Integer getPositionReachedCount(Long posHash) {
		return positionCount.get(posHash);
	}

	public boolean isPositionDraw(long positionHash) {
		boolean isDrawn = false;
		Integer reachedCount = getPositionReachedCount(positionHash);
		if (reachedCount != null && reachedCount >= 3) {
			isDrawn = true;
		}
		return isDrawn;
	}

	public void decrementPositionReachedCount(long posHash) {
		Integer count = positionCount.get(posHash);
		if (count == null) {
			assert false;
		} else {
			count-=1;
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
