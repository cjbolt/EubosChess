package eubos.search;

import java.util.HashMap;

public class DrawChecker {
	private HashMap<Long,Integer> positionCount;
	
	public DrawChecker() {
		positionCount = new HashMap<Long,Integer>();
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
	
	public Integer getPositionReachedCount(Long posHash) {
		return positionCount.get(posHash);
	}

	public boolean isPositionDraw(long positionHash) {
		boolean isDrawn = false;
		Integer reachedCount = getPositionReachedCount(positionHash);
		if (reachedCount != null && reachedCount >= 3) {
			// is a draw by 3-fold repetition
			isDrawn = true;
		}
		return isDrawn;
	}
}
