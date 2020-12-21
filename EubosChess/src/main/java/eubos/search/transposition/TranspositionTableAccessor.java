package eubos.search.transposition;

import java.util.List;

import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.search.Score;
import eubos.search.ScoreTracker;
import eubos.search.SearchDebugAgent;
import eubos.search.transposition.TranspositionEvaluation.*;

public class TranspositionTableAccessor implements ITranspositionAccessor {
	
	private FixedSizeTranspositionTable hashMap;
	private IPositionAccessors pos;
	private ScoreTracker st;
	
	public TranspositionTableAccessor(
			FixedSizeTranspositionTable transTable,
			IPositionAccessors pos,
			ScoreTracker st) {
		hashMap = transTable;
		this.pos = pos;
		this.st = st;
	}
	
	public TranspositionEvaluation getTransposition(byte currPly, int depthRequiredPly) {
		TranspositionEvaluation ret = new TranspositionEvaluation();
		ret.trans = hashMap.getTransposition(pos.getHash());
		if (ret.trans == null)
			return ret;
		
		synchronized(ret.trans) {
			if (ret.trans.getDepthSearchedInPly() >= depthRequiredPly) {
				
				if (ret.trans.getType() == Score.exact) {
					ret.status = TranspositionTableStatus.sufficientTerminalNode;
					SearchDebugAgent.printHashIsTerminalNode(ret.trans, pos.getHash());
				} else {
					// must be either (bound == Score.upperBound || bound == Score.lowerBound)
					if (st.isAlphaBetaCutOffForHash(currPly, ret.trans.getScore())) {
						SearchDebugAgent.printHashIsRefutation(pos.getHash(), ret.trans);
						ret.status = TranspositionTableStatus.sufficientRefutation;
			        } else {
			        	ret.status = TranspositionTableStatus.sufficientSeedMoveList;
			        }
				}
			} else {
				ret.status = TranspositionTableStatus.sufficientSeedMoveList;
			}
			
			if (ret.trans.getBestMove() == Move.NULL_MOVE) {
				// It is possible that we don't have a move to seed the list with, guard against that.
				if (ret.status == TranspositionTableStatus.sufficientSeedMoveList) {
					ret.status = TranspositionTableStatus.insufficientNoData;
				}
			}
		}
		return ret;
	}
	
	public ITransposition setTransposition(ITransposition trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove) {
		return setTransposition(trans, new_Depth, new_score, new_bound, new_bestMove, null);
	}
	
	public ITransposition setTransposition(ITransposition trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove, List<Integer> pv) {
		boolean is_created = false;
		if (trans == null) {
			synchronized(hashMap) {
				// Needed, because we want to merge this transposition with that of other threads, not to lose their effort.
				// Read, modify, write, otherwise we blindly update the transposition table, potentially overwriting other thread's Transposition object.
				// This is done in a lock to ensure that no-one adds whilst we are adding
				trans = hashMap.getTransposition(pos.getHash());
				if (trans == null) {
					trans = createTranpositionAddToTable(new_Depth, new_score, new_bound, new_bestMove, pv);
					is_created = true;
				}
			}
		}
		if (!is_created) {
			boolean is_updated = trans.checkUpdate(new_Depth, new_score, new_bound, new_bestMove, pv);
			if (is_updated) {
				SearchDebugAgent.printTransUpdate(trans, pos.getHash());
			}
		}
		return trans;
	}
	
	private ITransposition createTranpositionAddToTable(byte new_Depth, short new_score, byte new_bound, int new_bestMove, List<Integer> pv) {
		SearchDebugAgent.printTransNull(pos.getHash());
		ITransposition new_trans;
		if (USE_PRINCIPAL_VARIATION_TRANSPOSITIONS) {
			new_trans = new PrincipalVariationTransposition(new_Depth, new_score, new_bound, new_bestMove, pv);
		} else {
			new_trans= new Transposition(new_Depth, new_score, new_bound, new_bestMove, null);
		}
		SearchDebugAgent.printCreateTrans(pos.getHash());
		hashMap.putTransposition(pos.getHash(), new_trans);
		SearchDebugAgent.printTransUpdate(new_trans, pos.getHash());
		return new_trans;
	}
}
