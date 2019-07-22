package eubos.search;

import java.util.HashMap;

import eubos.position.Transposition;

public class FixedSizeTranspositionTable {
	
	private HashMap<Long, Transposition> hashMap = null;
	
	public FixedSizeTranspositionTable() {
		hashMap = new HashMap<Long, Transposition>();
	}
}
