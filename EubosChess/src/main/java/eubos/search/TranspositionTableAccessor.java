package eubos.search;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.IPositionAccessors;
import eubos.search.Transposition.ScoreType;

public class TranspositionTableAccessor {
	
	private FixedSizeTranspositionTable hashMap;
	private IPositionAccessors pos;
	private ScoreTracker st;
	
	public enum TranspositionTableStatus {
		insufficientNoData,
		sufficientTerminalNode,
		sufficientRefutation,
		sufficientSeedMoveList		
	};
	
	TranspositionTableAccessor(
			FixedSizeTranspositionTable transTable,
			IPositionAccessors pos,
			ScoreTracker st,
			List<GenericMove> lastPc) {
		hashMap = transTable;
		this.pos = pos;
		this.st = st;
	}
	
	public class TranspositionEval {
		public TranspositionTableStatus status;
		public Transposition trans;
	} 
	
	TranspositionEval evaluateTranspositionData(byte currPly, int depthRequiredPly) {
		TranspositionEval ret = new TranspositionEval();
		ret.status = TranspositionTableStatus.insufficientNoData;
		ret.trans = hashMap.getTransposition(pos.getHash());
		if (ret.trans == null)
			return ret;
		
		if (ret.trans.getDepthSearchedInPly() >= depthRequiredPly) {
			
			if (ret.trans.getScoreType() == ScoreType.exact) {
				ret.status = TranspositionTableStatus.sufficientTerminalNode;
				SearchDebugAgent.printHashIsTerminalNode(currPly, ret.trans.getBestMove(), ret.trans.getScore(),pos.getHash());
			} else { // must be (bound == ScoreType.upperBound || bound == ScoreType.lowerBound)
				short provisionalScoreAtThisPly = st.getProvisionalScoreAtPly(currPly);
				if (st.isAlphaBetaCutOff(currPly, provisionalScoreAtThisPly, ret.trans.getScore())) {
					SearchDebugAgent.printHashIsRefutation(currPly, ret.trans.getBestMove(),pos.getHash());
					ret.status = TranspositionTableStatus.sufficientRefutation;
		        } else {
		        	ret.status = TranspositionTableStatus.sufficientSeedMoveList;
		        }
			}
		} else {
			ret.status = TranspositionTableStatus.sufficientSeedMoveList;
		}
		
		// It is possible that we don't have a move to seed the list with, guard against that.
		if ((ret.status == TranspositionTableStatus.sufficientSeedMoveList) && 
			 ret.trans.getBestMove() == null) {
			ret.status = TranspositionTableStatus.insufficientNoData;
		}
		return ret;
	}
	
	public Transposition updateTranspositionTable(SearchMetrics sm, byte currPly, Transposition trans, Transposition new_trans) {
		if (trans == null) {
			trans = getTransCreateIfNew(currPly, new_trans);
			sm.setHashFull(getHashUtilisation());
		}
		trans = checkForUpdateTrans(currPly, new_trans, trans);
		return trans;
	}
	
	public Transposition getTransCreateIfNew(int currPly, Transposition new_trans) {
		SearchDebugAgent.printTransNull(currPly, pos.getHash());
		Transposition trans = hashMap.getTransposition(pos.getHash());
		if (trans == null) {
			SearchDebugAgent.printCreateTrans(currPly, pos.getHash());
			hashMap.putTransposition(pos.getHash(), new_trans);
			SearchDebugAgent.printTransUpdate(currPly, new_trans, pos.getHash());
			trans = new_trans;
		}
		return trans;
	}

	public Transposition checkForUpdateTrans(int currPly, Transposition new_trans, Transposition current_trans) {
		boolean updateTransposition = false;
		int currentDepth = current_trans.getDepthSearchedInPly();
		ScoreType currentBound = current_trans.getScoreType();

		if (currentDepth < new_trans.getDepthSearchedInPly()) {
			updateTransposition = true;
		} 
		if (currentDepth == new_trans.getDepthSearchedInPly()) {
			if (((currentBound == ScoreType.upperBound) || (currentBound == ScoreType.lowerBound)) &&
					new_trans.getScoreType() == ScoreType.exact) {
			    updateTransposition = true;
			} else if ((currentBound == ScoreType.upperBound) &&
					   (new_trans.getScore() < current_trans.getScore())) {
				updateTransposition = true;
			} else if ((currentBound == ScoreType.lowerBound) &&
					   (new_trans.getScore() > current_trans.getScore())) {
				updateTransposition = true;
			}
		}
		if (updateTransposition) {
			//current_trans.setPrincipalContinuation(new_trans.getPrincipalContinuation());
			current_trans.setScoreType(new_trans.getScoreType());
			current_trans.setScore(new_trans.getScore());
		    current_trans.setBestMove(new_trans.getBestMove());
		    current_trans.setDepthSearchedInPly(new_trans.getDepthSearchedInPly());
		    current_trans.setMoveList(new_trans.getMoveList());
		    //hashMap.putTransposition(pos.getHash(), current_trans);
		    SearchDebugAgent.printTransUpdate(currPly, current_trans, pos.getHash());
		}
		return current_trans;
	}

	public short getHashUtilisation() {
		return (short) (( ((long) hashMap.getHashMapSize())*(long)1000) / FixedSizeTranspositionTable.MAX_SIZE_OF_HASH_MAP);
	}
}
