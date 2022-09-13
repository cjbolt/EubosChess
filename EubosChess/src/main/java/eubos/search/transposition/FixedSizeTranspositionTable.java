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
	
	private static final int RANGE_TO_SEARCH = 10; 
			
	private long [] transposition_table = null;
	private long [] hashes = null;
	private long tableSize = 0;
	private long maxTableSize = 0;
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
		
		assert hashSizeElements < Integer.MAX_VALUE;
		
		int highestBit = (int)Long.highestOneBit(hashSizeElements);
		mask = highestBit - 1;
		
		transposition_table = new long[highestBit];
		hashes = new long[highestBit];
		tableSize = 0;
		maxTableSize = highestBit;
		
//		transposition_table = new long[(int)hashSizeElements];
//		hashes = new long[(int)hashSizeElements];
//		tableSize = 0;
//		maxTableSize = hashSizeElements;
	}
	
	public synchronized long getTransposition(long hashCode) {
		int index = (int)(hashCode & mask);
		//int index = (int)(hashCode % maxTableSize);
		if (maxTableSize - index > RANGE_TO_SEARCH) {
			for (int i=0; i < RANGE_TO_SEARCH; i++) {
				if (hashes[index+i] == hashCode) {
					return transposition_table[index+i];
				}
			}
		}
		return 0L;
	}
	
	public synchronized void putTransposition(long hashCode, long trans) {
		int index = (int)(hashCode & mask);
		//int index = (int)(hashCode % maxTableSize);
		boolean found_slot = false;
    	// Try to find a free slot near the hash index
		if (maxTableSize - index > RANGE_TO_SEARCH) {				
			for (int i=0; i < RANGE_TO_SEARCH; i++) {
				if (transposition_table[index+i] == 0L) {
					tableSize++;
					
					transposition_table[index+i] = trans;
					hashes[index+i] = hashCode;
					
					found_slot = true;
					break;
				}
			}
		}
		// failing that, overwrite based on age
		if (!found_slot) {
			int new_age = Transposition.getAge(trans);
			if (maxTableSize - index > RANGE_TO_SEARCH) {
				for (int i=0; i < RANGE_TO_SEARCH; i++) {
					int index_age = Transposition.getAge(transposition_table[index+i]);
					if ((index_age+(16 >> 2)) < new_age) {
						
						transposition_table[index+i] = trans;
						hashes[index+i] = hashCode;
						
						found_slot = true;
						break;
					}
				}
			}
		}
		// failing that, overwrite
		if (!found_slot) {
			transposition_table[index] = trans;
			hashes[index] = hashCode;
		}
	}
	
	public synchronized short getHashUtilisation() {
		return (short) ((tableSize*1000L)/maxTableSize);
	}	
}
