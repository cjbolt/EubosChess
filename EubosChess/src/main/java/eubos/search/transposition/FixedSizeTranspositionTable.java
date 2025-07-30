package eubos.search.transposition;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import eubos.main.EubosEngineMain;
import eubos.position.Move;

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
	private long [] hashes = null;
	private long tableSize = 0;
	long maxTableSize = 0;
	private long mask = 0;
	
	long numHits = 0;
	long numMisses = 0;
	long numOverwritten = 0;
	
	boolean isSinglethreaded = true;
	
	TranspositionTableCleaner currentCleaner = null;
	
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
		if (EubosEngineMain.ENABLE_TT_DIMENSIONED_TO_POWER_OF_TWO) {
			int highestBit = (int)Long.highestOneBit(hashSizeElements);
			mask = highestBit - 1;
			transposition_table = new long[highestBit];
			hashes = new long[highestBit];
			tableSize = 0;
			maxTableSize = highestBit;
		} else {
			int length = (int)hashSizeElements;
			transposition_table = new long[length];
			hashes = new long[length];
			tableSize = 0;
			maxTableSize = length;
		}
		
		if (numThreads > 1) {
			isSinglethreaded = false;
		}
	}
	
	public synchronized long getTransposition(long hashCode) {
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
			if (hashCode == 0L) return 0L; // To prune entries we have to give up on entry 0
			int index = (int) (EubosEngineMain.ENABLE_TT_DIMENSIONED_TO_POWER_OF_TWO ? 
					hashCode & mask : Long.remainderUnsigned(hashCode, maxTableSize));
			if (USE_ALWAYS_REPLACE) {
				if (hashes[index] == hashCode ) {
					if (EubosEngineMain.ENABLE_TT_DIAGNOSTIC_LOGGING) { numHits++; }
					return transposition_table[index];
				}
			} else {
				for (int i=index; (i < index+RANGE_TO_SEARCH) && (i < maxTableSize); i++) {
					if (hashes[i] == hashCode) {
						if (EubosEngineMain.ENABLE_TT_DIAGNOSTIC_LOGGING) { numHits++; }
						return transposition_table[i];
					}
				}
			}
			if (EubosEngineMain.ENABLE_TT_DIAGNOSTIC_LOGGING) { numMisses++; }
		}
		return 0L;
	}
	
	public synchronized void putTransposition(long hashCode, long trans) {
		putTransposition(hashCode, trans, 0);
	}
	
	public synchronized void putTransposition(long hashCode, long trans, int moveNumber) {
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
			if (hashCode == 0) return; // can't do anything with 0l
			int index = (int) (EubosEngineMain.ENABLE_TT_DIMENSIONED_TO_POWER_OF_TWO ? 
					hashCode & mask : Long.remainderUnsigned(hashCode, maxTableSize));
			if (EubosEngineMain.ENABLE_ASSERTS) assert (trans != 0L);
			if (USE_ALWAYS_REPLACE) {
				hashes[index] = hashCode;
				transposition_table[index] = trans;
				if (EubosEngineMain.ENABLE_TT_DIAGNOSTIC_LOGGING) { numOverwritten++; }
			} else {
				for (int i=index; (i < index+RANGE_TO_SEARCH) && (i < maxTableSize); i++) {
					// If exact hash match, overwrite entry in table
					if (hashes[i] == hashCode) {
						if (EubosEngineMain.ENABLE_ASSERTS) assert (transposition_table[i] != 0L);
						transposition_table[i] = trans;
						return;
					}
					// Try to find a free slot near the hash index
					else if (hashes[i] == 0L) {
						tableSize++;
						hashes[i] = hashCode;
						transposition_table[i] = trans;
						return;
					}
				}
				
				// failing that, overwrite based on adjusted age or depth
				boolean age_replacementFound = false;
				boolean depth_replacementFound = false;
				int age_ind = 0;
				int depth_ind = 0;
				int trans_depth = Transposition.getDepthSearchedInPly(trans);
				int oldest_adjusted_age = convertMoveNumberAndDepthToTransAge(moveNumber, trans_depth);
				for (int i=index; (i < index+RANGE_TO_SEARCH) && (i < maxTableSize); i++) {
					int index_age = Transposition.getAge(transposition_table[i]);
					int index_depth = Transposition.getDepthSearchedInPly(transposition_table[i]);
					int adjusted_age = index_age + index_depth/8;
					if (adjusted_age < oldest_adjusted_age) {
						oldest_adjusted_age = adjusted_age;
						age_ind = i;
						age_replacementFound = true;
					}
					if (index_depth < trans_depth) {
						trans_depth = index_depth;
						depth_replacementFound = true;
						depth_ind = i;
					}
				}
				if (age_replacementFound) {
					if (EubosEngineMain.ENABLE_TT_DIAGNOSTIC_LOGGING) { numOverwritten++; }
					hashes[age_ind] = hashCode;
					transposition_table[age_ind] = trans;
				} else if (depth_replacementFound) {
					if (EubosEngineMain.ENABLE_TT_DIAGNOSTIC_LOGGING) { numOverwritten++; }
					hashes[depth_ind] = hashCode;
					transposition_table[depth_ind] = trans;
				}
			}
		}
	}
	
	public long setTransposition(long hash, long trans, byte new_Depth, short new_score, byte new_bound, short new_bestMove, int move_number, short static_eval) {
		if (EubosEngineMain.ENABLE_ASSERTS) {		
			assert new_bestMove != Move.NULL_MOVE : "setTransposition best move is null";
		}
		int currentDepth = Transposition.getDepthSearchedInPly(trans);
		if (trans == 0L || currentDepth <= new_Depth) {
			trans = Transposition.valueOf(new_Depth, new_score, new_bound, new_bestMove, move_number >> 2, static_eval);
			if (EubosEngineMain.ENABLE_ASSERTS) {
				long old = Transposition.valueOf(new_Depth, new_score, new_bound, new_bestMove, move_number >> 2);
				old = Transposition.setStaticEval(old, static_eval);
				assert trans == old;
			}
			putTransposition(hash, trans, move_number);
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
	
	private int convertMoveNumberAndDepthToTransAge(int moveNumber, int depthInPly) {
		return (moveNumber - (depthInPly/2)) >> 2;
	}
	
	private synchronized void pruneTable(EubosEngineMain eubos, int moveNumber) {
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
			int start = getHashUtilisation();
			long numEntries = tableSize;
			for (int i=0; i < maxTableSize; i++) {
				if (hashes[i] == 0L || transposition_table[i] == 0L) continue;
				int currentDepth = Transposition.getDepthSearchedInPly(transposition_table[i]);
				int currentAge = Transposition.getAge(transposition_table[i]);
				int transAge = convertMoveNumberAndDepthToTransAge(moveNumber, currentDepth);
				if (currentAge < transAge) {
					transposition_table[i] = 0L;
					hashes[i] = 0L;
					tableSize--;
				}
			}
			eubos.sendInfoString(String.format("TranspositionTableCleaner move %d removed %d (utilisation start %d end %d)",
					             moveNumber, numEntries-tableSize, start, getHashUtilisation()));
		}
		currentCleaner = null;
	}
	
	public void reset() {
		for (int i=0; i < maxTableSize; i++) {
			hashes[i] = 0L;
			//transposition_table[i] = 0L;
		}
	}
	
	public void clearUp(EubosEngineMain eubos, int moveNumber) {
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
			if (getHashUtilisation() < 800) return;
			if (currentCleaner == null) {
				EubosEngineMain.logger.info("clearUp TranspositionTable creating");
				currentCleaner = new TranspositionTableCleaner(eubos, moveNumber);
				currentCleaner.start();
			}
		}
	}
	
	public class TranspositionTableCleaner extends Thread {
		int moveNumber;
		EubosEngineMain eubos;
		
		public TranspositionTableCleaner(EubosEngineMain eubos, int moveNumber) {
			this.moveNumber = moveNumber;
			this.eubos = eubos;
			this.setName(String.format("TranspositionTableCleaner move=%d", moveNumber));
			Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
		}
		
		public void run() {
			try {
				pruneTable(eubos, moveNumber);
			} catch (Exception e) {
				handleException(e);
			}
		}
		
		private void handleException(Exception e) {
			Writer buffer = new StringWriter();
			PrintWriter pw = new PrintWriter(buffer);
			e.printStackTrace(pw);
			String error = String.format("TranspositionTableCleaner crashed with: %s\n%s",
					e.getMessage(), buffer.toString());
			System.err.println(error);
			EubosEngineMain.logger.severe(error);
		}
	}
}
