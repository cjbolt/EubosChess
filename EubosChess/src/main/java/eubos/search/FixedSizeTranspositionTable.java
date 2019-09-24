package eubos.search;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;


public class FixedSizeTranspositionTable {
	
	private HashMap<Long, Transposition> hashMap = null;
	private long hashMapSize = 0;
	private HashMap<Long, Integer> accessCount = null;
	
	public long getHashMapSize() {
		return hashMapSize;
	}

	public static final long MAX_SIZE_OF_HASH_MAP = (1L << 20); 
	
	public FixedSizeTranspositionTable() {
		hashMap = new HashMap<Long, Transposition>((int)MAX_SIZE_OF_HASH_MAP, (float)0.75);
		accessCount = new HashMap<Long, Integer>();
		hashMapSize = 0;
	}
	
	public boolean containsHash(long hashCode) {
		return hashMap.containsKey(hashCode);
	}
	
	private void incrementAccessCount(long hashCode) {
		if (accessCount.containsKey(hashCode)) {
			Integer count = accessCount.get(hashCode);
			accessCount.put(hashCode, count+1);
		} else {
			accessCount.put(hashCode,1);
		}
	}
	
	public Transposition getTransposition(long hashCode) {
		Transposition retrievedTrans = hashMap.get(hashCode);
		if (retrievedTrans != null) {
			incrementAccessCount(hashCode);
		}
		return retrievedTrans;
	}
	
	private void removeLeastUsed() {
		Collection<Integer> theAccessCounts = accessCount.values();
		LinkedList<Integer> list = new LinkedList<Integer>(theAccessCounts);
		Collections.sort(list);
		int twentyPercentIndex = (int) (MAX_SIZE_OF_HASH_MAP/5);
		Integer bottomTwentyPercentAccessThreshold = list.get(twentyPercentIndex);
		LinkedList<Long> removed = new LinkedList<Long>();
		Iterator<Entry<Long, Integer>> it = accessCount.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, Integer> pair = (Map.Entry<Long, Integer>)it.next();
			if (pair.getValue() <= bottomTwentyPercentAccessThreshold) {
				removed.add(pair.getKey());
				hashMap.remove(pair.getKey());
				hashMapSize--;
			}
		}
		for (long removedHashCode : removed) {
			accessCount.remove(removedHashCode);
		}
		list.clear();
		removed.clear();
	}
	
	public void putTransposition(long hashCode, Transposition trans) {
		if (hashMapSize >= MAX_SIZE_OF_HASH_MAP) {
			// Remove the oldest 20% of hashes to make way for this one
			removeLeastUsed();
		}
		if (hashMap.put(hashCode, trans) == null) {
			// Only increment size if hash wasn't already contained, otherwise overwrites
			hashMapSize++;
		}
		incrementAccessCount(hashCode);
	}
}
