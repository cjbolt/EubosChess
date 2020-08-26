package eubos.search.transposition;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.openjdk.jol.info.ClassLayout;

import eubos.main.EubosEngineMain;
import eubos.position.MoveList;


public class FixedSizeTranspositionTable {
	
	public static final long ELEMENTS_DEFAULT_HASH_SIZE = (1L << 25);
	
	public static final long MOVELIST_NORMAL_WORST_SIZE = 40L;
	public static final long MOVELIST_NORMAL_AVERAGE_SIZE = 26L;
	public static final long MOVELIST_EXTENDED_AVERAGE_SIZE = 5L;
	
	public static final long MOVELIST_AVERAGE_SIZE = (
			MOVELIST_NORMAL_WORST_SIZE +
			//MOVELIST_NORMAL_AVERAGE_SIZE +
			MOVELIST_EXTENDED_AVERAGE_SIZE);
	
	//	public static final long BYTES_MOVELIST_AVERAGE = (
	//			MOVELIST_AVERAGE_SIZE*Integer.BYTES +
	//			2*Byte.BYTES);
	
	public static final long BYTES_MOVELIST_AVERAGE;
	static {
		BYTES_MOVELIST_AVERAGE = ClassLayout.parseClass(MoveList.class).instanceSize() +
				MOVELIST_AVERAGE_SIZE*Integer.BYTES;
	}
	
	//	public static final long BYTES_TRANSPOSTION_ELEMENT = (
	//			Long.BYTES /* Zobrist */ +
	//			Short.BYTES /* score */ +
	//			Byte.BYTES /* depth */ +
	//			Integer.BYTES /* best move */ +
	//			Byte.BYTES /* bound score */ +
	//			4 /* MoveList reference size */ +
	//			Short.BYTES /* access count */ +
	//			12 /* instance header, found by JOL */ +
	//			6 /* padding, found by JOL */ );
	
	public static final long BYTES_TRANSPOSTION_ELEMENT;
	static {
		BYTES_TRANSPOSTION_ELEMENT = ClassLayout.parseClass(Transposition.class).instanceSize();
	}
	
	public static final long BYTES_PER_TRANSPOSITION = (
			BYTES_TRANSPOSTION_ELEMENT /*+ 
			BYTES_MOVELIST_AVERAGE*/);
	
	public static final long BYTES_PER_MEGABYTE = (1024L * 1000L);
	
	public static final long MBYTES_DEFAULT_HASH_SIZE = (ELEMENTS_DEFAULT_HASH_SIZE*BYTES_PER_TRANSPOSITION)/BYTES_PER_MEGABYTE;
	
	private ConcurrentHashMap<Long, ITransposition> hashMap = null;
	private long hashMapSize = 0;
	private long maxHashMapSize = ELEMENTS_DEFAULT_HASH_SIZE;
	
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
		long maxHeapSize = Runtime.getRuntime().maxMemory();
		if ((hashSizeMBytes * BYTES_PER_MEGABYTE) > ((maxHeapSize*4)/10)) {
			/* If the configured hash size is greater than 40% of the heap, then reduce the hash size
			 * as we are resource constrained and garbage collection will kill speed of the engine. */
			hashSizeElements = ((maxHeapSize*4)/10) / BYTES_PER_TRANSPOSITION;
		}
		
		EubosEngineMain.logger.info(String.format(
				"BYTES_TRANSPOSTION_ELEMENT=%d BYTES_MOVELIST_AVERAGE=%d, BYTES_PER_TRANSPOSITION=%d", 
				BYTES_TRANSPOSTION_ELEMENT, BYTES_MOVELIST_AVERAGE,	BYTES_PER_TRANSPOSITION));
		
		EubosEngineMain.logger.info(String.format(
				"Hash dimensions requestedSizeMBytes=%d maxHeapSizeMBytes=%d, maxSizeElements=%d, maxSizeMBytes=%d", 
				hashSizeMBytes, maxHeapSize/BYTES_PER_MEGABYTE, hashSizeElements,
				(hashSizeElements*BYTES_PER_TRANSPOSITION)/BYTES_PER_MEGABYTE));
		
		hashMap = new ConcurrentHashMap<Long, ITransposition>((int)hashSizeElements, (float)0.75);
		hashMapSize = 0;
		maxHashMapSize = hashSizeElements;
	}
	
	public boolean containsHash(long hashCode) {
		return hashMap.containsKey(hashCode);
	}
	
	private void incrementAccessCount(long hashCode) {
		if (hashMap.containsKey(hashCode)) {
			ITransposition trans = hashMap.get(hashCode);
			short count = trans.getAccessCount();
			if (count != Short.MAX_VALUE) {
				trans.setAccessCount((short)(count+1));
			}
		}
	}
	
	public ITransposition getTransposition(long hashCode) {
		ITransposition retrievedTrans = hashMap.get(hashCode);
		if (retrievedTrans != null) {
			incrementAccessCount(hashCode);
		}
		return retrievedTrans;
	}
	
	private Short getBottomTwentyPercentAccessThreshold() {
		LinkedList<Short> theAccessCounts = new LinkedList<Short>(); 
		for (ITransposition trans : hashMap.values()) {
			theAccessCounts.add(trans.getAccessCount());
		}
		Collections.sort(theAccessCounts);
		int twentyPercentIndex = (int) (maxHashMapSize/5);
		return theAccessCounts.get(twentyPercentIndex);
	}
	
	private void removeLeastUsed() {
		Short bottomTwentyPercentAccessThreshold = getBottomTwentyPercentAccessThreshold();
		Iterator<Entry<Long, ITransposition>> it = hashMap.entrySet().iterator();
		ConcurrentHashMap<Long, ITransposition> hashMapCopy = new ConcurrentHashMap<Long, ITransposition>(hashMap);
		while (it.hasNext()){
			Map.Entry<Long, ITransposition> pair = (Map.Entry<Long, ITransposition>)it.next();
			short count = pair.getValue().getAccessCount();
			if (count <= bottomTwentyPercentAccessThreshold) {
				hashMapCopy.remove(pair.getKey());
				hashMapSize--;
			} else {
				// Normalise remaining counts after every cull operation
				pair.getValue().setAccessCount((short)(count-bottomTwentyPercentAccessThreshold));
			}
		}
		hashMap = hashMapCopy;
	}
	
	public void putTransposition(long hashCode, ITransposition trans) {
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

	public void protectHash(long hashCode) {
		if (hashMap.containsKey(hashCode)) {
			ITransposition trans = hashMap.get(hashCode);
			trans.setAccessCount(Short.MAX_VALUE);
		}
	}
}
