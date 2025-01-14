package eubos.search.transposition;



public class DummyTranspositionTable implements ITranspositionAccessor {
	
	boolean isSinglethreaded = true;
	
	public DummyTranspositionTable() {
	}
	
	public synchronized long getTransposition(long hashCode) {
		return 0L;
	}
	
	public synchronized void putTransposition(long hashCode, long trans) {
	}
	
	public long setTransposition(long hash, long trans, byte new_Depth, short new_score, byte new_bound, short new_bestMove, int move_number, short static_eval) {
		return 0L;
	}
	
	public short getHashUtilisation() {
		return (short) 0;
	}
}
