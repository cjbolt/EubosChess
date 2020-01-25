package eubos.search.transposition;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;


public class FixedSizeTranspositionTable {
	
	public static final long ELEMENTS_DEFAULT_HASH_SIZE = (1L << 22);
	public static final long BYTES_TRANSPOSTION_ELEMENT = (8+4+30*4*2);
	public static final long BYTES_PER_MEGABYTE = (1024L*1000L);
	public static final long MBYTES_DEFAULT_HASH_SIZE = (ELEMENTS_DEFAULT_HASH_SIZE*BYTES_TRANSPOSTION_ELEMENT)/BYTES_PER_MEGABYTE;
	
	private ConcurrentHashMap<Long, Transposition> hashMap = null;
	private long hashMapSize = 0;
	private long maxHashMapSize = ELEMENTS_DEFAULT_HASH_SIZE;
	private ConcurrentHashMap<Long, Integer> accessCount = null;
	
	public long getHashMapSize() {
		return hashMapSize;
	}
	
	public long getHashMapMaxSize() {
		return maxHashMapSize;
	}
	
	public void remove(long hashCode) {
		hashMap.remove(hashCode);
	}
	
	public FixedSizeTranspositionTable() {
		this(MBYTES_DEFAULT_HASH_SIZE);
	}
	
	public FixedSizeTranspositionTable(long hashSizeMBytes) {
		/* Capping hash table - 
		 * size of hash code 8 bytes
		 * size of transposition 32 bytes
		 * generic move 24 bytes
		 * average move list */
		long hashSizeElements = (hashSizeMBytes * BYTES_PER_MEGABYTE) / BYTES_TRANSPOSTION_ELEMENT;
		hashMap = new ConcurrentHashMap<Long, Transposition>((int)hashSizeElements, (float)0.75);
		accessCount = new ConcurrentHashMap<Long, Integer>();
		hashMapSize = 0;
		maxHashMapSize = hashSizeElements;
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
		int twentyPercentIndex = (int) (maxHashMapSize/5);
		Integer bottomTwentyPercentAccessThreshold = list.get(twentyPercentIndex);
		LinkedList<Long> removed = new LinkedList<Long>();
		Iterator<Entry<Long, Integer>> it = accessCount.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, Integer> pair = (Map.Entry<Long, Integer>)it.next();
			if (pair.getValue() <= bottomTwentyPercentAccessThreshold) {
				removed.add(pair.getKey());
				hashMap.remove(pair.getKey());
				hashMapSize--;
			} else {
				// Normalise remaining counts after every cull operation
				pair.setValue(pair.getValue()-bottomTwentyPercentAccessThreshold);
			}
		}
		for (long removedHashCode : removed) {
			accessCount.remove(removedHashCode);
		}
		list.clear();
		removed.clear();
	}
	
	public void putTransposition(long hashCode, Transposition trans) {
		if (hashMapSize >= maxHashMapSize) {
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
