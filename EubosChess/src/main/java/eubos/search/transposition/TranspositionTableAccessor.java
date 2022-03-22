package eubos.search.transposition;

import java.util.List;

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
	
	public long setTransposition(long trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove) {
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
			return setTransposition(trans, new_Depth, new_score, new_bound, new_bestMove, null);
		} 
		return trans;
	}
	
	public long setTransposition(long trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove, List<Integer> pv) {
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
			boolean is_created = false;
			if (trans == 0) {
				// Needed, because we want to merge this transposition with that of other threads, not to lose their effort.
				// Read, modify, write, otherwise we blindly update the transposition table, potentially overwriting other thread's Transposition object.
				trans = hashMap.getTransposition(pos.getHash());
				if (trans == 0L) {
					trans = createTranpositionAddToTable(new_Depth, new_score, new_bound, new_bestMove, pv);
					is_created = true;
				}
			}
			if (!is_created) {
				long old_trans = trans;
				trans = Transposition.checkUpdate(trans, new_Depth, new_score, new_bound, new_bestMove, pv);
				if (old_trans != trans) {
					// Need to update the position in the hash table when we update the transposition.
					hashMap.putTransposition(pos.getHash(), trans);
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printTransUpdate(trans, pos.getHash());
				}
			}
		}
		return trans;
	}
	
	private long createTranpositionAddToTable(byte new_Depth, short new_score, byte new_bound, int new_bestMove, List<Integer> pv) {
		if (SearchDebugAgent.DEBUG_ENABLED) sda.printCreateTrans(pos.getHash());
		long new_trans = Transposition.valueOf(new_Depth, new_score, new_bound, new_bestMove, null);
		hashMap.putTransposition(pos.getHash(), new_trans);
		if (SearchDebugAgent.DEBUG_ENABLED) sda.printTransUpdate(new_trans, pos.getHash());
		return new_trans;
	}
}
