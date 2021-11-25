package eubos.search.transposition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.openjdk.jol.info.ClassLayout;

import eubos.main.EubosEngineMain;
import eubos.position.MoveList;

public class FixedSizeTranspositionTable {
	
	public static final boolean DEBUG_LOGGING = false;
	
	public static final long ELEMENTS_DEFAULT_HASH_SIZE = (1L << 25);
	
	public static final long MOVELIST_NORMAL_WORST_SIZE = 40L;
	public static final long MOVELIST_NORMAL_AVERAGE_SIZE = 26L;
	public static final long MOVELIST_EXTENDED_AVERAGE_SIZE = 5L;
	
	public static final long MOVELIST_AVERAGE_SIZE = (
			MOVELIST_NORMAL_WORST_SIZE +
			MOVELIST_EXTENDED_AVERAGE_SIZE);
	
	public static final long BYTES_MOVELIST_AVERAGE;
	static {
		BYTES_MOVELIST_AVERAGE = ClassLayout.parseClass(MoveList.class).instanceSize() +
				MOVELIST_AVERAGE_SIZE*Integer.BYTES;
	}
	
	public static final long BYTES_TRANSPOSTION_ELEMENT;
	static {
		BYTES_TRANSPOSTION_ELEMENT = ClassLayout.parseClass(Transposition.class).instanceSize();
	}
	
	public static final long BYTES_HASHMAP_ENTRY;
	static {
		BYTES_HASHMAP_ENTRY = ClassLayout.parseClass(HashMap.Entry.class).instanceSize();
	}
	
	public static final long BYTES_PER_TRANSPOSITION =  BYTES_TRANSPOSTION_ELEMENT + BYTES_HASHMAP_ENTRY;
	
	public static final long BYTES_PER_MEGABYTE = (1024L * 1000L);
	
	public static final long MBYTES_DEFAULT_HASH_SIZE = (ELEMENTS_DEFAULT_HASH_SIZE*BYTES_PER_TRANSPOSITION)/BYTES_PER_MEGABYTE;
	
	private Map<Integer, ITransposition> hashMap = null;
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
					"BYTES_TRANSPOSTION_ELEMENT=%d BYTES_MOVELIST_AVERAGE=%d, BYTES_PER_TRANSPOSITION=%d", 
					BYTES_TRANSPOSTION_ELEMENT, BYTES_MOVELIST_AVERAGE,	BYTES_PER_TRANSPOSITION));
			
			EubosEngineMain.logger.info(String.format(
					"Hash dimensions requestedSizeMBytes=%d maxHeapSizeMBytes=%d, maxSizeElements=%d, maxSizeMBytes=%d", 
					hashSizeMBytes, maxHeapSize/BYTES_PER_MEGABYTE, hashSizeElements,
					(hashSizeElements*BYTES_PER_TRANSPOSITION)/BYTES_PER_MEGABYTE));
		}
		if (numThreads == 1) {
			// Now we have the monitor thread!
			hashMap = new ConcurrentHashMap<Integer, ITransposition>((int)hashSizeElements, (float)0.75);
		} else {
			hashMap = new ConcurrentHashMap<Integer, ITransposition>((int)hashSizeElements, (float)0.75);
		}
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
	
	private int getLeastSignificantBits(long hashCode) {
		return (int)(hashCode & 0xFFFFFFFF);
	}
	
	private int getMostSignificantBits(long hashCode) {
		return (int)(hashCode >> 32);
	}
	
	private boolean isMatchingHashCode(ITransposition trans, long hashCode) {
		return trans.checkHash(getMostSignificantBits(hashCode));
	}
	
	private void incrementAccessCount(ITransposition trans) {
		if (trans != null) {
			short count = trans.getAccessCount();
			if (count != Short.MAX_VALUE) {
				trans.setAccessCount((short)(count+1));
			}
		}
	}
	
	public ITransposition getTransposition(long hashCode) {
		ITransposition retrievedTrans = hashMap.get(getLeastSignificantBits(hashCode));
		if (retrievedTrans != null && isMatchingHashCode(retrievedTrans, hashCode)) {
			incrementAccessCount(retrievedTrans);
			return retrievedTrans;
		} else {
			return null;
		}
	}
	
	public void putTransposition(long hashCode, ITransposition trans) {
		if (hashMapSize < maxHashMapSize*0.98) {
			if (hashMap.put(getLeastSignificantBits(hashCode), trans) == null) {
				// Only increment size if hash wasn't already contained, otherwise overwrites
				hashMapSize++;
			}
			incrementAccessCount(trans);
		}
		else
		{
			EubosEngineMain.logger.info("Hash Map too full!");
			wakeMonitor();
		}
	}
	
	public short getHashUtilisation() {
		return (short) (( ((long) hashMapSize)*(long)1000) / maxHashMapSize);
	}
	
	class TranspositionTableMonitorThread extends Thread {
		
		private boolean isActive = true;
		static final private int pollRateMillisecs = 30000;
		
		public TranspositionTableMonitorThread() {
			this.setName("TranspositionTableMonitorThread");
		}
		
		public void run() {
			while (isActive) {
				if (hashMapSize >= maxHashMapSize*0.8) {
					// Remove the oldest 20% of hashes to make way for this one
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
		
		private Short getBottomTwentyPercentAccessThreshold() {
			ArrayList<Short> theAccessCounts = new ArrayList<Short>(hashMap.size()); 
			for (ITransposition trans : hashMap.values()) {
				theAccessCounts.add(trans.getAccessCount());
			}
			if (!theAccessCounts.isEmpty()) {
				Collections.sort(theAccessCounts);
				int twentyPercentIndex = (int) (maxHashMapSize/5);
				return theAccessCounts.get(twentyPercentIndex);
			} else {
				return 0;
			}
		}
		
		private void removeLeastUsed() {
			EubosEngineMain.logger.info("Starting to free bottom 20% of Hash Table");
			Short bottomTwentyPercentAccessThreshold = getBottomTwentyPercentAccessThreshold();
			if (bottomTwentyPercentAccessThreshold != 0) {
				Iterator<Integer> it = hashMap.keySet().iterator();
				while (it.hasNext()){
					ITransposition trans = hashMap.get(it.next());
					short count = trans.getAccessCount();
					if (count <= bottomTwentyPercentAccessThreshold) {
						it.remove();
						hashMapSize--;
					} else {
						// Normalise remaining counts after every cull operation
						trans.setAccessCount((short)(count-bottomTwentyPercentAccessThreshold));
					}
				}
			}
			EubosEngineMain.logger.info("Completed freeing bottom 20% of Hash Table");
		}
	}
	
}
