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
	public static final long MOVELIST_NORMAL_WORST_SIZE = 40L;
	public static final long MOVELIST_EXTENDED_AVERAGE_SIZE = 5L;
	public static final long MOVELIST_AVERAGE_SIZE = (MOVELIST_NORMAL_WORST_SIZE+MOVELIST_EXTENDED_AVERAGE_SIZE);
	public static final long BYTES_MOVELIST_AVERAGE = (MOVELIST_AVERAGE_SIZE*Integer.BYTES+2*Byte.BYTES);
	public static final long BYTES_TRANSPOSTION_ELEMENT = (Long.BYTES/*Zobrist*/+Short.BYTES/*score*/+Byte.BYTES/*depth*/+Integer.BYTES/*best*/+Integer.BYTES/*bound*/+4/*MoveList reference size?*/);
	public static final long BYTES_ACCESS_COUNT = (Integer.BYTES+Long.BYTES);
	public static final long BYTES_DRAW_CHECKER = (Long.BYTES+Byte.BYTES);
	
	public static final long BYTES_PER_TRANSPOSITION = (BYTES_TRANSPOSTION_ELEMENT + BYTES_MOVELIST_AVERAGE + BYTES_ACCESS_COUNT+BYTES_DRAW_CHECKER);
	public static final long BYTES_PER_MEGABYTE = (1024L*1000L);
	
	public static final long MBYTES_DEFAULT_HASH_SIZE = (ELEMENTS_DEFAULT_HASH_SIZE*BYTES_PER_TRANSPOSITION)/BYTES_PER_MEGABYTE;
	
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
		long hashSizeElements = (hashSizeMBytes * BYTES_PER_MEGABYTE) / BYTES_PER_TRANSPOSITION;
		hashSizeElements = (hashSizeElements*4)/10;
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
