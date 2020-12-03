package eubos.search;

import java.util.concurrent.ConcurrentHashMap;

import eubos.main.EubosEngineMain;

public class DrawChecker {
	
	public static final boolean ENABLE_THREEFOLD_POSITION_DRAW_CHECK = true;
	public static final int THREEFOLD_THRESHOLD = 2;
	
	private ConcurrentHashMap<Integer,Byte> positionCount;
	
	public DrawChecker() {
		positionCount = new ConcurrentHashMap<Integer,Byte>();
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
			if (ENABLE_THREEFOLD_POSITION_DRAW_CHECK) {
				if (count >= THREEFOLD_THRESHOLD) {
					repetitionPossible = true;
				}
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
		if (ENABLE_THREEFOLD_POSITION_DRAW_CHECK) {
			Byte reachedCount = getPositionReachedCount(positionHash);
			if (reachedCount != null && reachedCount >= THREEFOLD_THRESHOLD) {
				opponentCouldClaimDraw = true;
			}
		}
		return opponentCouldClaimDraw;
	}

	public void decrementPositionReachedCount(long posHash) {
		Integer truncatedHash = (int) (posHash >> 32);
		Byte count = positionCount.get(truncatedHash);
		if (count == null) {
			if (EubosEngineMain.ASSERTS_ENABLED)
				assert false;
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
