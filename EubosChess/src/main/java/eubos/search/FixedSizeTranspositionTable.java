package eubos.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class FixedSizeTranspositionTable {
	
	private HashMap<Long, Transposition> hashMap = null;
	private long hashMapSize = 0;
	private List<Long> ageList = null;
	
	public long getHashMapSize() {
		return hashMapSize;
	}

	public static final long MAX_SIZE_OF_HASH_MAP = (1L << 22); 
	
	public FixedSizeTranspositionTable() {
		hashMap = new HashMap<Long, Transposition>((int)MAX_SIZE_OF_HASH_MAP, (float)0.75);
		//ageList = new LinkedList<Long>();
		ageList = new ArrayList<Long>((int)MAX_SIZE_OF_HASH_MAP);
		hashMapSize = 0;
	}
	
	public boolean containsHash(long hashCode) {
		return hashMap.containsKey(hashCode);
	}
	
	private void moveToFrontOfAgeList(long hashCode) {
		int index = ageList.indexOf(hashCode);
		if (index != -1) {
			ageList.remove(index);
		}
		//ageList.addFirst(hashCode);
		ageList.add(0, hashCode);
	}
	
	public Transposition getTransposition(long hashCode) {
		Transposition retrievedTrans = hashMap.get(hashCode);
		if (retrievedTrans != null) {
			moveToFrontOfAgeList(hashCode);
		}
		return retrievedTrans;
	}
	
	public void putTransposition(long hashCode, Transposition trans) {
		if (hashMapSize >= MAX_SIZE_OF_HASH_MAP) {
			// Remove the oldest hash to make way for this one
			//Long hashToEject = ((LinkedList<Long>)ageList).removeLast();
			Long hashToEject = ageList.remove((int)MAX_SIZE_OF_HASH_MAP-1);
			hashMap.remove(hashToEject);
			hashMapSize--;
		}
		if (hashMap.put(hashCode, trans) == null) {
			// Only increment size if hash wasn't already contained, otherwise overwrites
			hashMapSize++;
		}
		moveToFrontOfAgeList(hashCode);
	}
}
