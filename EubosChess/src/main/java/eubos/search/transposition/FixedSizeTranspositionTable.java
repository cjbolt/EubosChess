package eubos.search.transposition;

import eubos.main.EubosEngineMain;

import org.openjdk.jol.info.ClassLayout;

import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;

public class FixedSizeTranspositionTable {
	
	private static final boolean DEBUG_LOGGING = false;

	public static final long BYTES_HASHMAP_ENTRY;
	static {
		BYTES_HASHMAP_ENTRY = ClassLayout.parseClass(Long2LongMap.Entry.class).instanceSize();
	}
	
	public static final long BYTES_HASHMAP_ZOBRIST_KEY = 8L;
	public static final long BYTES_TRANSPOSTION_ELEMENT = 8L;
	public static final long BYTES_PER_TRANSPOSITION =
			BYTES_TRANSPOSTION_ELEMENT + BYTES_HASHMAP_ENTRY + BYTES_HASHMAP_ZOBRIST_KEY;
	
	public static final long BYTES_PER_MEGABYTE = (1024L * 1000L);
	
	public static final long MBYTES_DEFAULT_HASH_SIZE = 256L;
			
	private Long2LongLinkedOpenHashMap hashMap = null;
	private long hashMapSize = 0;
	private long maxHashMapSize = 0;
	
	public FixedSizeTranspositionTable() {
		this(MBYTES_DEFAULT_HASH_SIZE, 1);
	}
	
	public FixedSizeTranspositionTable(long hashSizeMBytes, int numThreads) {
		long hashSizeElements = (hashSizeMBytes * BYTES_PER_MEGABYTE) / BYTES_PER_TRANSPOSITION;
		
		if (DEBUG_LOGGING) {
			EubosEngineMain.logger.info(String.format("BYTES_PER_TRANSPOSITION=%d", BYTES_PER_TRANSPOSITION));
			EubosEngineMain.logger.info(String.format(
					"Hash dimensions requestedSizeMBytes=%d BYTES_PER_TRANSPOSITION=%d, maxSizeElements=%d", 
					hashSizeMBytes, BYTES_PER_TRANSPOSITION, hashSizeElements));
		}

		hashMap = new Long2LongLinkedOpenHashMap((int)hashSizeElements);
		hashMapSize = 0;
		maxHashMapSize = hashSizeElements;
	}
	
	public synchronized long getTransposition(long hashCode) {
		return hashMap.get(hashCode);
	}
	
	public synchronized void putTransposition(long hashCode, long trans) {
		if (hashMapSize >= maxHashMapSize) {
			hashMap.removeFirstLong();
			hashMapSize--;
		}
		if (hashMap.putAndMoveToLast(hashCode, trans) == hashMap.defaultReturnValue()) {
			// Only increment size if hash wasn't already contained, otherwise overwrites
			hashMapSize++;
		}
	}
	
	public synchronized short getHashUtilisation() {
		return (short) ((hashMapSize*1000L)/maxHashMapSize);
	}	
}
