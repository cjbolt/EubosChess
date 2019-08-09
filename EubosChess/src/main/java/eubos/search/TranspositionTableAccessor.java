package eubos.search;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.IPositionAccessors;
import eubos.position.Transposition;
import eubos.position.Transposition.ScoreType;

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
			PrincipalContinuation pc,
			List<GenericMove> lastPc) {
		hashMap = transTable;
		this.pos = pos;
		this.st = st;
	}
	
	public class TranspositionEval {
		public TranspositionTableStatus status;
		public Transposition trans;
	} 
	
	TranspositionEval evaluateTranspositionData(int currPly, int depthRequiredPly) {
		TranspositionEval ret = new TranspositionEval();
		ret.status = TranspositionTableStatus.insufficientNoData;
		ret.trans = hashMap.getTransposition(pos.getHash().hashCode);
		if (ret.trans == null)
			return ret;
		
		if (ret.trans.getDepthSearchedInPly() >= depthRequiredPly) {
			
			if (ret.trans.getScoreType() == ScoreType.exact) {
				ret.status = TranspositionTableStatus.sufficientTerminalNode;
				SearchDebugAgent.printHashIsTerminalNode(currPly, ret.trans.getBestMove(), ret.trans.getScore());
			} else { // must be (bound == ScoreType.upperBound || bound == ScoreType.lowerBound)
				int provisionalScoreAtThisPly = st.getProvisionalScoreAtPly(currPly);
				if (st.isAlphaBetaCutOff(currPly, provisionalScoreAtThisPly, ret.trans.getScore())) {
					SearchDebugAgent.printHashIsRefutation(currPly, ret.trans.getBestMove());
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
	
	void storeTranspositionScore(int currPly, byte depthPositionSearchedPly, GenericMove bestMove, int score, ScoreType bound, List<GenericMove> ml, Transposition trans) {
		if (trans == null) {
			trans = hashMap.getTransposition(pos.getHash().hashCode);
		}
		if (trans == null) {
			trans = new Transposition(bestMove, depthPositionSearchedPly, score, bound, ml);
			hashMap.putTransposition(pos.getHash().hashCode, trans);
		} else {
			boolean updateTransposition = false;
			int currentDepth = trans.getDepthSearchedInPly();
			ScoreType currentBound = trans.getScoreType();
		
			if (currentDepth < depthPositionSearchedPly) {
				updateTransposition = true;
			} 
			if (currentDepth == depthPositionSearchedPly) {
				if (((currentBound == ScoreType.upperBound) || (currentBound == ScoreType.lowerBound)) &&
					  bound == ScoreType.exact) {
				    updateTransposition = true;
				} else if ((currentBound == ScoreType.upperBound) &&
						   (score < trans.getScore())) {
					updateTransposition = true;
				} else if ((currentBound == ScoreType.lowerBound) &&
						   (score > trans.getScore())) {
					updateTransposition = true;
				}
			}
			if (updateTransposition) {
				trans.setScoreType(bound);
	            trans.setBestMove(bestMove);
	            trans.setDepthSearchedInPly(depthPositionSearchedPly);
	            trans.setScore(score);
	            trans.setMoveList(ml);
	            SearchDebugAgent.printTransUpdate(currPly, bestMove, depthPositionSearchedPly, score, bound);
			}
		}
	}
}
