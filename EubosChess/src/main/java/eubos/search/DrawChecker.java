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
		if (reachedCount != null && reachedCount >= 2) {
			// it will be a draw by 3-fold repetition if we have entered the position twice before
			isDrawn = true;
		}
		return isDrawn;
	}
}
