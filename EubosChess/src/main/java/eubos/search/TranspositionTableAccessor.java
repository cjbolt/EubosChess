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
		
		int depth = ret.trans.getDepthSearchedInPly();
		int score = ret.trans.getScore();
		ScoreType bound = ret.trans.getScoreType();
		GenericMove move = ret.trans.getBestMove();
		
		if (depth >= depthRequiredPly) {
			
			if (bound == ScoreType.exact) {
				ret.status = TranspositionTableStatus.sufficientTerminalNode;
			} else {
				// must be (bound == ScoreType.upperBound || bound == ScoreType.lowerBound)
				int prevScore = st.getBackedUpScoreAtPly(currPly);
				if (st.isAlphaBetaCutOff(currPly, prevScore, score )) {
					// Transposition score is a refutation of previous move
					ret.status = TranspositionTableStatus.sufficientRefutation;
		        } else if (move != null) {
		        	// Not a refutation so seed move list.
		        	ret.status = TranspositionTableStatus.sufficientSeedMoveList;
		        }
			}
		} else if (move != null) {
			// Transposition just sufficient to seed the MoveList for searching
			ret.status = TranspositionTableStatus.sufficientSeedMoveList;
		}
		return ret;
	}
	
	void storeTranspositionScore(int depthPositionSearchedPly, GenericMove bestMove, int score, ScoreType bound) {
		boolean updateTransposition = false;
				
		Transposition trans = hashMap.getTransposition(pos.getHash().hashCode);
		if (trans != null) {
			int currentDepth = trans.getDepthSearchedInPly();
			ScoreType currentBound = trans.getScoreType();
		
			if (currentDepth < depthPositionSearchedPly) {
				updateTransposition = true;
			} else if ((currentDepth == depthPositionSearchedPly) && 
				       ((currentBound == ScoreType.upperBound) || (currentBound == ScoreType.lowerBound)) &&
				        bound == ScoreType.exact) {
			    updateTransposition = true;
			} else if ((currentDepth == depthPositionSearchedPly) && 
					   (currentBound == ScoreType.upperBound) &&
					   (score < trans.getScore())) {
				updateTransposition = true;
			} else if ((currentDepth == depthPositionSearchedPly) && 
					   (currentBound == ScoreType.lowerBound) &&
					   (score > trans.getScore())) {
				updateTransposition = true;
			} else {
				 // No other condition requires an update
			}
		} else {
			hashMap.putTransposition(pos.getHash().hashCode,
					new Transposition(bestMove, depthPositionSearchedPly, score, bound));
		}
		if (updateTransposition) {
			trans.setScoreType(bound);
            trans.setBestMove(bestMove);
            trans.setDepthSearchedInPly(depthPositionSearchedPly);
            trans.setScore(score);	
		}
	}
}
