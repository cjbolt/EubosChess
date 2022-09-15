package eubos.search.transposition;

import eubos.main.EubosEngineMain;

public class FixedSizeTranspositionTable {
	
	private static final boolean DEBUG_LOGGING = true;

	public static final long BYTES_HASHMAP_ZOBRIST_KEY = 8L;
	public static final long BYTES_TRANSPOSTION_ELEMENT = 8L;
	public static final long BYTES_PER_TRANSPOSITION =
			BYTES_TRANSPOSTION_ELEMENT + BYTES_HASHMAP_ZOBRIST_KEY;
	
	public static final long BYTES_PER_MEGABYTE = 1_000_000L;
	public static final long MBYTES_DEFAULT_HASH_SIZE = 256L;
	
	static final int RANGE_TO_SEARCH = 30; 
			
	private long [] transposition_table = null;
	private long [] hashes = null;
	private long tableSize = 0;
	long maxTableSize = 0;
	private long mask = 0;
	
	public FixedSizeTranspositionTable() {
		this(MBYTES_DEFAULT_HASH_SIZE, 1);
	}
	
	public FixedSizeTranspositionTable(long hashSizeMBytes, int numThreads) {
		long hashSizeElements = (hashSizeMBytes * BYTES_PER_MEGABYTE) / BYTES_PER_TRANSPOSITION;
		
		if (DEBUG_LOGGING) {
			EubosEngineMain.logger.info(String.format(
					"Hash dimensions requestedSizeMBytes=%d BYTES_PER_TRANSPOSITION=%d, maxSizeElements=%d", 
					hashSizeMBytes, BYTES_PER_TRANSPOSITION, hashSizeElements));
		}
		
		hashSizeElements &= Integer.MAX_VALUE;
		
		int highestBit = (int)Long.highestOneBit(hashSizeElements);
		mask = highestBit - 1;
		transposition_table = new long[highestBit];
		hashes = new long[highestBit];
		tableSize = 0;
		maxTableSize = highestBit;
	}
	
	public synchronized long getTransposition(long hashCode) {
		int index = (int)(hashCode & mask);
		for (int i=index; i >= 0 && i > (index-RANGE_TO_SEARCH); i--) {
			if (hashes[i] == hashCode) {
				return transposition_table[i];
			}
		}
		return 0L;
	}
	
	public synchronized void putTransposition(long hashCode, long trans) {
		int index = (int)(hashCode & mask);
		// Ideal case
		if (hashes[index] == hashCode) {
			transposition_table[index] = trans;
			return;
		}
		boolean found_slot = false;
		// Try to find a free slot near the hash index
		for (int i=index; i >= 0 && i > (index-RANGE_TO_SEARCH); i--) {
			if (transposition_table[i] == 0L) {
				tableSize++;
				transposition_table[i] = trans;
				hashes[i] = hashCode;
				found_slot = true;
				break;
			}
			if (hashes[i] == hashCode) {
				transposition_table[i] = trans;
				found_slot = true;
				break;
			}
		}
		// failing that, overwrite based on age
		if (!found_slot) {
			int oldest_age = Transposition.getAge(trans);
			int oldest_index = index;
			for (int i=index; i >= 0 && i > (index-RANGE_TO_SEARCH); i--) {
				int index_age = Transposition.getAge(transposition_table[i]);
				if (index_age < oldest_age) {
					oldest_age = index_age;
					oldest_index = i;
				}
			}
			transposition_table[oldest_index] = trans;
			hashes[oldest_index] = hashCode;
			found_slot = true;
		}
	}
	
	public synchronized short getHashUtilisation() {
		return (short) ((tableSize*1000L)/maxTableSize);
	}	
}
