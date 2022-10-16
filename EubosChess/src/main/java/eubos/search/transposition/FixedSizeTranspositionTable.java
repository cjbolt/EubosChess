package eubos.search.transposition;

import eubos.main.EubosEngineMain;

public class FixedSizeTranspositionTable implements ITranspositionAccessor {
	
	private static final boolean DEBUG_LOGGING = true;

	public static final long BYTES_HASHMAP_ZOBRIST_KEY = 8L;
	public static final long BYTES_TRANSPOSTION_ELEMENT = 5L;
	public static final long BYTES_PER_TRANSPOSITION =
			BYTES_TRANSPOSTION_ELEMENT + BYTES_HASHMAP_ZOBRIST_KEY;
	
	public static final long BYTES_PER_MEGABYTE = 1_024_000L;
	public static final long MBYTES_DEFAULT_HASH_SIZE = 256L;
	
	static final int RANGE_TO_SEARCH = 10;
	static final boolean USE_ALWAYS_REPLACE = (RANGE_TO_SEARCH <= 1);
			
	private long [] transposition_table = null;
	private int [] hashes = null;
	private byte [] hashes2 = null;
	private long tableSize = 0;
	long maxTableSize = 0;
	private long mask = 0;
	
	boolean isSinglethreaded = true;
	
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
		hashes = new int[highestBit];
		hashes2 = new byte[highestBit];
		tableSize = 0;
		maxTableSize = highestBit;
		
		if (numThreads > 1) {
			isSinglethreaded = false;
		}
	}
	
	public synchronized long getTransposition(long hashCode) {
		int index = (int)(hashCode & mask);
		int hash_ms_fragment = (int)(hashCode>>>32);
		byte hash_ls_fragment = (byte)(hashCode>>>24);
		if (USE_ALWAYS_REPLACE) {
			if (hashes[index] == hash_ms_fragment && hashes2[index] == hash_ls_fragment) {
				return transposition_table[index];
			}
		} else {
			for (int i=index; (i < index+RANGE_TO_SEARCH) && (i < maxTableSize); i++) {
				if (transposition_table[i] == 0L) break;
				if (hashes[i] == hash_ms_fragment && hashes2[i] == hash_ls_fragment) {
					return transposition_table[i];
				}
			}
		}
		return 0L;
	}
	
	public synchronized void putTransposition(long hashCode, long trans) {
		int index = (int)(hashCode & mask);
		int hash_ms_fragment = (int)(hashCode>>>32);
		byte hash_ls_fragment = (byte)(hashCode>>>24);
		if (USE_ALWAYS_REPLACE) {
			hashes[index] = hash_ms_fragment;
			hashes2[index] = hash_ls_fragment;
			transposition_table[index] = trans;
		} else {
			for (int i=index; (i < index+RANGE_TO_SEARCH) && (i < maxTableSize); i++) {
				// If exact hash match, overwrite entry in table
				if (hashes[i] == hash_ms_fragment && hashes2[i] == hash_ls_fragment) {
					if (EubosEngineMain.ENABLE_ASSERTS) assert (transposition_table[i] != 0L);
					transposition_table[i] = trans;
					return;
				}
				// Try to find a free slot near the hash index
				else if (transposition_table[i] == 0L) {
					tableSize++;
					hashes[i] = hash_ms_fragment;
					hashes2[i] = hash_ls_fragment;
					transposition_table[i] = trans;
					return;
				}
			}
			// failing that, overwrite based on age
			int oldest_age = Transposition.getAge(trans);
			int threshold_age = Math.max(0, oldest_age - 4);
			int oldest_index = index;
			for (int i=index; (i < index+RANGE_TO_SEARCH) && (i < maxTableSize); i++) {
				int index_age = Transposition.getAge(transposition_table[i]);
				if (index_age < oldest_age) {
					oldest_age = index_age;
					oldest_index = i;
				}
				if (oldest_age < threshold_age) {
					break;
				}
			}
			hashes[oldest_index] = hash_ms_fragment;
			hashes2[oldest_index] = hash_ls_fragment;
			transposition_table[oldest_index] = trans;
		}
	}
	
	public long setTransposition(long hash, long trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove, int new_age) {
		boolean is_created = false;
		boolean is_updated = false;
		
		// Quantise move count to 6 bits for age 
		new_age >>= 2;
		
		if (trans == 0L) {
			// Needed, because we want to merge this transposition with that of other threads, not to lose their effort.
			// Read, modify, write, otherwise we blindly update the transposition table, potentially overwriting other thread's Transposition object.
			if (!isSinglethreaded) {
				trans = getTransposition(hash);
			}
			if (trans == 0L) {
				trans = Transposition.valueOf(new_Depth, new_score, new_bound, new_bestMove, new_age);
				is_created = true;
			}
		}
		if (!is_created) {
			long old_trans = trans;
			trans = Transposition.checkUpdate(trans, new_Depth, new_score, new_bound, new_bestMove, new_age);
			is_updated = (old_trans != trans);
		}
		if (is_created || is_updated) {
			putTransposition(hash, trans);
		}
		return trans;
	}
	
	public synchronized short getHashUtilisation() {
		return (short) ((tableSize*1000L)/maxTableSize);
	}
}
