package eubos.search.transposition;

import eubos.main.EubosEngineMain;
import eubos.position.IPositionAccessors;
import eubos.search.SearchDebugAgent;

public class TranspositionTableAccessor implements ITranspositionAccessor {
	
	private FixedSizeTranspositionTable hashMap;
	private IPositionAccessors pos;
	private SearchDebugAgent sda;
	
	public TranspositionTableAccessor(
			FixedSizeTranspositionTable transTable,
			IPositionAccessors pos,
			SearchDebugAgent sda) {
		hashMap = transTable;
		this.pos = pos;
		this.sda = sda;
	}
	
	public long getTransposition() {
		long trans = 0L;
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
			trans = hashMap.getTransposition(pos.getHash());
		}
		return trans;
	}
	
	public long setTransposition(long trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove, int new_age) {
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
			long hash = pos.getHash();
			boolean is_created = false;
			boolean is_updated = false;
			
			// Quantise move count to 6 bits for age 
			new_age >>= 2;
			
			if (trans == 0L) {
				// Needed, because we want to merge this transposition with that of other threads, not to lose their effort.
				// Read, modify, write, otherwise we blindly update the transposition table, potentially overwriting other thread's Transposition object.
				trans = hashMap.getTransposition(hash);
				if (trans == 0L) {
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printCreateTrans(hash);
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
				hashMap.putTransposition(hash, trans);
				if (SearchDebugAgent.DEBUG_ENABLED) sda.printTransUpdate(trans, hash);
			}
		}
		return trans;
	}
}
