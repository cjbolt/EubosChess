package eubos.search;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;

public class DrawChecker {
	
	public static final boolean ENABLE_THREEFOLD_POSITION_DRAW_CHECK = true;
	public static final int THREEFOLD_THRESHOLD = 2;
	
	private Long2ByteMap positionCount;
	
	public DrawChecker() {
		positionCount = Long2ByteMaps.synchronize(new Long2ByteOpenHashMap());
	}
	
	public DrawChecker(Long2ByteMap clone) {
		positionCount = Long2ByteMaps.synchronize(new Long2ByteOpenHashMap(clone));
	}
	
	public Long2ByteMap getState() {
		return positionCount;
	}
	
	public void reset() {
		positionCount.clear();
	}
		
	public boolean incrementPositionReachedCount(long posHash) {
		boolean repetitionPossible = false;
		byte count = positionCount.get(posHash);
		count++;
		positionCount.put(posHash, count);
		if (ENABLE_THREEFOLD_POSITION_DRAW_CHECK) {
			if (count >= THREEFOLD_THRESHOLD) {
				repetitionPossible = true;
			}
		}
		return repetitionPossible;
	}
	
	public byte getPositionReachedCount(long posHash) {
		return positionCount.get(posHash);
	}

	public boolean isPositionOpponentCouldClaimDraw(long positionHash) {
		boolean opponentCouldClaimDraw = false;
		if (ENABLE_THREEFOLD_POSITION_DRAW_CHECK) {
			byte reachedCount = getPositionReachedCount(positionHash);
			if (reachedCount >= THREEFOLD_THRESHOLD) {
				opponentCouldClaimDraw = true;
			}
		}
		return opponentCouldClaimDraw;
	}

	public void decrementPositionReachedCount(long posHash) {
		byte count = positionCount.get(posHash);
		count--;
		if (count <= 0) {
			positionCount.remove(posHash);
		} else {
			positionCount.put(posHash, count);
		}
	}
	
	public Integer getNumEntries() {
		return positionCount.size();
	}
	
	public String toString() {
		return positionCount.toString();
	}
}
