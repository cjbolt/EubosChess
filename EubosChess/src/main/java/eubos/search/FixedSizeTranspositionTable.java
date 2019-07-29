package eubos.search;

import java.util.HashMap;

import eubos.position.Transposition;

public class FixedSizeTranspositionTable {
	
	private HashMap<Long, Transposition> hashMap = null;
	private long hashMapSize = 0;
	
	public long getHashMapSize() {
		return hashMapSize;
	}

	private static final long MAX_SIZE_OF_HASH_MAP = (1L << 23); 
	
	public FixedSizeTranspositionTable() {
		hashMap = new HashMap<Long, Transposition>((int)MAX_SIZE_OF_HASH_MAP, (float)0.75);
		hashMapSize = 0;
	}
	
	public boolean containsHash(long hashCode) {
		return hashMap.containsKey(hashCode);
	}
	
	public Transposition getTransposition(long hashCode) {
		return hashMap.get(hashCode);
	}
	
	public void putTransposition(long hashCode, Transposition trans) {
		if (hashMapSize >= MAX_SIZE_OF_HASH_MAP)
			return;
		if (hashMap.put(hashCode, trans) == null) {
			hashMapSize++;
		};
	}
}
