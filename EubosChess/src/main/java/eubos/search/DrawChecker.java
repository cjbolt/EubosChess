package eubos.search;

import eubos.main.EubosEngineMain;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;

public class DrawChecker {
	
	public static final boolean ENABLE_THREEFOLD_POSITION_DRAW_CHECK = true;
	public static final int THREEFOLD_THRESHOLD = 2;
	
	private Long2ByteOpenHashMap positionCount;
	
	public DrawChecker() {
		positionCount = new Long2ByteOpenHashMap();
	}
	
	public DrawChecker(Long2ByteOpenHashMap clone) {
		positionCount = new Long2ByteOpenHashMap(clone);
	}
	
	public Long2ByteOpenHashMap getState() {
		return positionCount;
	}
	
	public void reset() {
		positionCount.clear();
	}
		
	public boolean incrementPositionReachedCount(long posHash) {
		boolean repetitionPossible = false;
		byte count = positionCount.get(posHash);
		if (count == 0) {
			positionCount.put(posHash, (byte) 1);
		} else {
			count++;
			positionCount.put(posHash, count);
			if (ENABLE_THREEFOLD_POSITION_DRAW_CHECK) {
				if (count >= THREEFOLD_THRESHOLD) {
					repetitionPossible = true;
				}
			}
		}
		return repetitionPossible;
	}
	
	public Byte getPositionReachedCount(long posHash) {
		return positionCount.get(posHash);
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
		byte count = positionCount.get(posHash);
		if (count == 0) {
			if (EubosEngineMain.ENABLE_ASSERTS)
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
