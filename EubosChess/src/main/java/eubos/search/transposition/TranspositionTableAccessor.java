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
	
	public ITransposition getTransposition() {
		ITransposition trans = null;
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
			trans = hashMap.getTransposition(pos.getHash());
		}
		return trans;
	}
	
	public ITransposition setTransposition(ITransposition trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove) {
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
			return setTransposition(trans, new_Depth, new_score, new_bound, new_bestMove, null);
		} 
		return trans;
	}
	
	public ITransposition setTransposition(ITransposition trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove, List<Integer> pv) {
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
			boolean is_created = false;
			if (trans == null) {
				// Needed, because we want to merge this transposition with that of other threads, not to lose their effort.
				// Read, modify, write, otherwise we blindly update the transposition table, potentially overwriting other thread's Transposition object.
				trans = hashMap.getTransposition(pos.getHash());
				if (trans == null) {
					trans = createTranpositionAddToTable(new_Depth, new_score, new_bound, new_bestMove, pv);
					is_created = true;
				}
			}
			if (!is_created) {
				boolean is_updated = trans.checkUpdate(new_Depth, new_score, new_bound, new_bestMove, pv);
				if (is_updated) {
					if (SearchDebugAgent.DEBUG_ENABLED) sda.printTransUpdate(trans, pos.getHash());
				}
			}
		}
		return trans;
	}
	
	private ITransposition createTranpositionAddToTable(byte new_Depth, short new_score, byte new_bound, int new_bestMove, List<Integer> pv) {
		ITransposition new_trans;
		if (SearchDebugAgent.DEBUG_ENABLED) sda.printCreateTrans(pos.getHash());
		if (USE_PRINCIPAL_VARIATION_TRANSPOSITIONS) {
			new_trans = new PrincipalVariationTransposition(pos.getHash(), new_Depth, new_score, new_bound, new_bestMove, pv);
		} else {
			new_trans= new Transposition(pos.getHash(), new_Depth, new_score, new_bound, new_bestMove, null);
		}
		hashMap.putTransposition(pos.getHash(), new_trans);
		if (SearchDebugAgent.DEBUG_ENABLED) sda.printTransUpdate(new_trans, pos.getHash());
		return new_trans;
	}
}
