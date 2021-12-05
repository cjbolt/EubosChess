package eubos.search.transposition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openjdk.jol.info.ClassLayout;

import eubos.main.EubosEngineMain;

public class FixedSizeTranspositionTable {
	
	private static final boolean DEBUG_LOGGING = false;
	
	public static final long ELEMENTS_DEFAULT_HASH_SIZE = (1L << 25);
	
	public static final long BYTES_TRANSPOSTION_ELEMENT;
	static {
		BYTES_TRANSPOSTION_ELEMENT = ClassLayout.parseClass(Transposition.class).instanceSize();
	}
	
	public static final long BYTES_HASHMAP_ENTRY;
	static {
		BYTES_HASHMAP_ENTRY = ClassLayout.parseClass(HashMap.Entry.class).instanceSize();
	}
	
	public static final long BYTES_HASHMAP_ZOBRIST_KEY = 8L;
	
	public static final long BYTES_PER_TRANSPOSITION =  BYTES_TRANSPOSTION_ELEMENT + BYTES_HASHMAP_ENTRY + BYTES_HASHMAP_ZOBRIST_KEY;
	
	public static final long BYTES_PER_MEGABYTE = (1024L * 1000L);
	
	public static final long MBYTES_DEFAULT_HASH_SIZE = (ELEMENTS_DEFAULT_HASH_SIZE*BYTES_PER_TRANSPOSITION)/BYTES_PER_MEGABYTE;
	
	private Map<Long, ITransposition> hashMap = null;
	private long hashMapSize = 0;
	private long maxHashMapSize = ELEMENTS_DEFAULT_HASH_SIZE;
	
	private TranspositionTableMonitorThread monitorThread;
	
	public long getHashMapSize() {
		return hashMapSize;
	}
	
	public long getHashMapMaxSize() {
		return maxHashMapSize;
	}
	
	public FixedSizeTranspositionTable() {
		this(MBYTES_DEFAULT_HASH_SIZE, 1);
	}
	
	public FixedSizeTranspositionTable(long hashSizeMBytes, int numThreads) {
		long hashSizeElements = (hashSizeMBytes * BYTES_PER_MEGABYTE) / BYTES_PER_TRANSPOSITION;
		long maxHeapSize = Runtime.getRuntime().maxMemory();
		if ((hashSizeMBytes * BYTES_PER_MEGABYTE) > ((maxHeapSize*4)/10)) {
			/* If the configured hash size is greater than 40% of the heap, then reduce the hash size
			 * as we are resource constrained and garbage collection will kill speed of the engine. */
			hashSizeElements = ((maxHeapSize*4)/10) / BYTES_PER_TRANSPOSITION;
		}
		
		if (DEBUG_LOGGING) {
			EubosEngineMain.logger.info(String.format(
					"BYTES_TRANSPOSTION_ELEMENT=%d, BYTES_PER_TRANSPOSITION=%d", 
					BYTES_TRANSPOSTION_ELEMENT, BYTES_PER_TRANSPOSITION));
			
			EubosEngineMain.logger.info(String.format(
					"Hash dimensions requestedSizeMBytes=%d maxHeapSizeMBytes=%d, maxSizeElements=%d, maxSizeMBytes=%d", 
					hashSizeMBytes, maxHeapSize/BYTES_PER_MEGABYTE, hashSizeElements,
					(hashSizeElements*BYTES_PER_TRANSPOSITION)/BYTES_PER_MEGABYTE));
		}

		hashMap = new ConcurrentHashMap<Long, ITransposition>((int)hashSizeElements, (float)0.75);
		hashMapSize = 0;
		maxHashMapSize = hashSizeElements;
		
		monitorThread = new TranspositionTableMonitorThread();
		monitorThread.start();
	}
	
	public void haltMonitor() {
		monitorThread.halt();
	}
	
	public void wakeMonitor() {
		monitorThread.wake();
	}
	
	private void incrementAccessCount(ITransposition trans) {
		if (trans != null) {
			trans.incrementAccessCount();
		}
	}
	
	public ITransposition getTransposition(long hashCode) {
		ITransposition retrievedTrans = hashMap.get(hashCode);
		if (retrievedTrans != null) {
			incrementAccessCount(retrievedTrans);
			return retrievedTrans;
		} else {
			return null;
		}
	}
	
	public void putTransposition(long hashCode, ITransposition trans) {
		if (hashMapSize < maxHashMapSize) {
			if (hashMap.put(hashCode, trans) == null) {
				// Only increment size if hash wasn't already contained, otherwise overwrites
				hashMapSize++;
			}
			incrementAccessCount(trans);
		}
		else
		{
			wakeMonitor();
			Thread.yield();
		}
	}
	
	public short getHashUtilisation() {
		return (short) (( ((long) hashMapSize)*(long)1000) / maxHashMapSize);
	}
	
	class TranspositionTableMonitorThread extends Thread {
		
		private boolean isActive = true;
		static final private int pollRateMillisecs = 30000;
		
		int mean_depth = 0;
		short twentyPercentValue = 0;
		
		public TranspositionTableMonitorThread() {
			this.setName("TranspositionTableMonitorThread");
		}
		
		public void run() {
			while (isActive) {
				if (hashMapSize >= maxHashMapSize*0.75) {
					// Remove the least used 20% of hashes by access count
					removeLeastUsed();
				}
				try {
					Thread.sleep(pollRateMillisecs);
				} catch (InterruptedException e) {
				}
			}
		}
		
		public void halt() {
			isActive = false;
			this.interrupt();
		}
		
		public void wake() {
			this.interrupt();
		}
		
		private void getBottomTwentyPercentAccessThreshold() {
			ArrayList<Short> theAccessCounts = new ArrayList<Short>(hashMap.size());
			mean_depth = 0;
			long depth_accumulator = 0L;
			for (ITransposition trans : hashMap.values()) {
				theAccessCounts.add(trans.getAccessCount());
				depth_accumulator += trans.getDepthSearchedInPly();
			}
			if (!theAccessCounts.isEmpty()) {
				Collections.sort(theAccessCounts);
				int twentyPercentIndex = (int) (maxHashMapSize/5);
				twentyPercentValue = theAccessCounts.get(twentyPercentIndex);
				int maxValue = (theAccessCounts.size() > 0) ? theAccessCounts.get(theAccessCounts.size()-1): 0;
				mean_depth = (int)(depth_accumulator / theAccessCounts.size());
				EubosEngineMain.logger.info(String.format("Trans access counts max=%d, min=%d, 20pc=%d mean_depth=%d acc=%d",
						maxValue, theAccessCounts.get(0), twentyPercentValue, mean_depth, depth_accumulator));
			} else {
				twentyPercentValue = 0;
			}
		}
		
		private void removeLeastUsed() {
			EubosEngineMain.logger.info("Starting to free least used Hash Table entries by access count and depth");
			getBottomTwentyPercentAccessThreshold();
			Short bottomTwentyPercentAccessThreshold = twentyPercentValue;
			int depth_threshold = Math.max(mean_depth, 1);
			if (bottomTwentyPercentAccessThreshold != 0) {
				Iterator<Long> it = hashMap.keySet().iterator();
				while (it.hasNext()){
					ITransposition trans = hashMap.get(it.next());
					short count = trans.getAccessCount();
					int depth = trans.getDepthSearchedInPly();
					if (count <= bottomTwentyPercentAccessThreshold && depth <= depth_threshold) {
						it.remove();
						hashMapSize--;
					} else {
						// Normalise remaining counts after every cull operation
						trans.setAccessCount((short)(count-bottomTwentyPercentAccessThreshold));
					}
				}
			}
			EubosEngineMain.logger.info("Completed freeing least used Hash Table entries by access count and depth");
		}
	}
	
}
