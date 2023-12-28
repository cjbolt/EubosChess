package eubos.search.transposition;

import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.search.Score;

public class FixedSizeTranspositionTable implements ITranspositionAccessor {

	public static final long BYTES_HASHMAP_ZOBRIST_KEY = 8L;
	public static final long BYTES_TRANSPOSTION_ELEMENT = 8L;
	public static final long BYTES_PER_TRANSPOSITION =
			BYTES_TRANSPOSTION_ELEMENT + BYTES_HASHMAP_ZOBRIST_KEY;
	
	public static final long BYTES_PER_MEGABYTE = 1_024_000L;
	public static final long MBYTES_DEFAULT_HASH_SIZE = 256L;
	
	static final int RANGE_TO_SEARCH = 10;
	static final boolean USE_ALWAYS_REPLACE = (RANGE_TO_SEARCH <= 1);
			
	private long [] transposition_table = null;
	private int [] hashes = null;
	private long tableSize = 0;
	long maxTableSize = 0;
	private long mask = 0;
	
	long numHits = 0;
	long numMisses = 0;
	long numOverwritten = 0;
	
	boolean isSinglethreaded = true;
	
	public FixedSizeTranspositionTable() {
		this(MBYTES_DEFAULT_HASH_SIZE, 1);
	}
	
	public FixedSizeTranspositionTable(long hashSizeMBytes, int numThreads) {
		long hashSizeElements = (hashSizeMBytes * BYTES_PER_MEGABYTE) / BYTES_PER_TRANSPOSITION;
		
		if (EubosEngineMain.ENABLE_TT_DIAGNOSTIC_LOGGING) {
			if (EubosEngineMain.ENABLE_LOGGING) {
				EubosEngineMain.logger.fine(String.format(
						"Hash dimensions requestedSizeMBytes=%d BYTES_PER_TRANSPOSITION=%d, maxSizeElements=%d", 
						hashSizeMBytes, BYTES_PER_TRANSPOSITION, hashSizeElements));
			}
		}
		
		hashSizeElements &= Integer.MAX_VALUE;
		
		int highestBit = (int)Long.highestOneBit(hashSizeElements);
		mask = highestBit - 1;		
		transposition_table = new long[highestBit];
		hashes = new int[highestBit];
		tableSize = 0;
		maxTableSize = highestBit;
		
		if (numThreads > 1) {
			isSinglethreaded = false;
		}
	}
	
	public synchronized long getTransposition(long hashCode) {
		int index = (int)(hashCode & mask);
		if (USE_ALWAYS_REPLACE) {
			if (hashes[index] == (int)(hashCode >>> 32)) {
				if (EubosEngineMain.ENABLE_TT_DIAGNOSTIC_LOGGING) { numHits++; }
				return transposition_table[index];
			}
		} else {
			for (int i=index; (i < index+RANGE_TO_SEARCH) && (i < maxTableSize); i++) {
				if (transposition_table[i] == 0L) break;
				if (hashes[i] == (int)(hashCode >>> 32)) {
					if (EubosEngineMain.ENABLE_TT_DIAGNOSTIC_LOGGING) { numHits++; }
					return transposition_table[i];
				}
			}
		}
		if (EubosEngineMain.ENABLE_TT_DIAGNOSTIC_LOGGING) { numMisses++; }
		return 0L;
	}
	
	public synchronized void putTransposition(long hashCode, long trans) {
		int index = (int)(hashCode & mask);
		if (EubosEngineMain.ENABLE_ASSERTS) assert (trans != 0L);
		if (USE_ALWAYS_REPLACE) {
			hashes[index] = (int)(hashCode >>> 32);
			transposition_table[index] = trans;
			if (EubosEngineMain.ENABLE_TT_DIAGNOSTIC_LOGGING) { numOverwritten++; }
		} else {
			for (int i=index; (i < index+RANGE_TO_SEARCH) && (i < maxTableSize); i++) {
				// If exact hash match, overwrite entry in table
				if (hashes[i] == (int)(hashCode >>> 32)) {
					if (EubosEngineMain.ENABLE_ASSERTS) assert (transposition_table[i] != 0L);
					transposition_table[i] = trans;
					return;
				}
				// Try to find a free slot near the hash index
				else if (transposition_table[i] == 0L) {
					tableSize++;
					hashes[i] = (int)(hashCode >>> 32);
					transposition_table[i] = trans;
					return;
				}
			}
			// failing that, overwrite based on age
			int oldest_age = Transposition.getAge(trans);
			int threshold_age = Math.max(0, oldest_age - 6);
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
			if (EubosEngineMain.ENABLE_TT_DIAGNOSTIC_LOGGING) { numOverwritten++; }
			hashes[oldest_index] = (int)(hashCode >>> 32);
			transposition_table[oldest_index] = trans;
		}
	}
	
	public long setTransposition(long hash, long trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove, int move_number, short static_eval) {
		boolean is_created = false;
		boolean is_updated = false;
		
		// Quantise move count to 6 bits for age 
		int new_age = move_number >> 2;
				
		if (EubosEngineMain.ENABLE_ASSERTS) {		
			assert new_bestMove != Move.NULL_MOVE : "setTransposition best move is null";
		}
		
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
			int currentDepth = Transposition.getDepthSearchedInPly(trans);
			if (currentDepth < new_Depth) {
				is_updated = true;	
			} else if (currentDepth == new_Depth) {
				// Don't insist on a higher score than transposition to update because of aspiration
				// windows and multi-threaded search.
				if (Transposition.getType(trans) != Score.exact) {
					is_updated = true;
				} else {
					// don't update, already have an exact score
				}
			} else {
				// don't update, depth is less than we currently have
			}
			if (is_updated) {
				trans = Transposition.valueOf(new_Depth, new_score, new_bound, new_bestMove, new_age);
				if (static_eval != Short.MAX_VALUE) {
					trans = Transposition.setStaticEval(trans, static_eval);
				}
			}
		}
		if (is_created || is_updated) {
			putTransposition(hash, trans);
		}
		return trans;
	}
	
	public short getHashUtilisation() {
		return (short) ((tableSize*1000L)/maxTableSize);
	}
	
	public synchronized String getDiagnostics() {
		if (EubosEngineMain.ENABLE_TT_DIAGNOSTIC_LOGGING) {
			String output = String.format("hits=%d misses=%d overwrites=%d", numHits, numMisses, numOverwritten);
			resetDiagnostics();
			return output;
		} else {
			return "";
		}
	}
	
	public synchronized void resetDiagnostics() {
		numOverwritten = numHits = numMisses = 0;
	}
}
