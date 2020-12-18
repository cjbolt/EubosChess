package eubos.search.transposition;

import java.util.List;

import eubos.main.EubosEngineMain;
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
		return ret;
	}
	
	public ITransposition setTransposition(ITransposition trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove, List<Integer> pv) {
		if (trans == null) {
			trans = getTransCreateIfNew(new_Depth, new_score, new_bound, new_bestMove, pv);
		}
		trans = checkForUpdateTrans(trans, new_Depth, new_score, new_bound, new_bestMove, pv);
		return trans;
	}
	
	public ITransposition setTransposition(ITransposition trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove) {
		return setTransposition(trans, new_Depth, new_score, new_bound, new_bestMove, null);
	}
	
	private ITransposition getTransCreateIfNew(byte new_Depth, short new_score, byte new_bound, int new_bestMove, List<Integer> pv) {
		SearchDebugAgent.printTransNull(pos.getHash());
		ITransposition trans = hashMap.getTransposition(pos.getHash());
		if (trans == null) {
			ITransposition new_trans;
			if (USE_PRINCIPAL_VARIATION_TRANSPOSITIONS) {
				new_trans = new PrincipalVariationTransposition(new_Depth, new_score, new_bound, new_bestMove, pv);
			} else {
				new_trans= new Transposition(new_Depth, new_score, new_bound, new_bestMove, null);
			}
			SearchDebugAgent.printCreateTrans(pos.getHash());
			hashMap.putTransposition(pos.getHash(), new_trans);
			SearchDebugAgent.printTransUpdate(new_trans, pos.getHash());
			trans = new_trans;
		}
		return trans;
	}
	
	private ITransposition checkForUpdateTrans(ITransposition current_trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove, List<Integer> pv) {
		boolean updateTransposition = false;
		int currentDepth = current_trans.getDepthSearchedInPly();
		
		SearchDebugAgent.printTransDepthCheck(currentDepth, new_Depth);
		
		if (currentDepth < new_Depth) {
			updateTransposition = true;
		} else if (currentDepth == new_Depth) {
			byte currentBound = current_trans.getType();
			SearchDebugAgent.printTransBoundScoreCheck(currentBound, current_trans.getScore(), Score.getScore(new_score));
			if (((currentBound == Score.upperBound) || (currentBound == Score.lowerBound)) &&
					new_bound == Score.exact) {
			    updateTransposition = true;
			} else if ((currentBound == Score.upperBound) &&
					   (Score.getScore(new_score) < current_trans.getScore())) {
				if (EubosEngineMain.ASSERTS_ENABLED)
					assert currentBound == new_bound;
				updateTransposition = true;
			} else if ((currentBound == Score.lowerBound) &&
					   (Score.getScore(new_score) > current_trans.getScore())) {
				if (EubosEngineMain.ASSERTS_ENABLED)
					assert currentBound == new_bound;
				updateTransposition = true;
			}
		}
		if (updateTransposition) {
			current_trans.update(new_Depth, new_score, new_bound, new_bestMove, pv );
		    hashMap.putTransposition(pos.getHash(), current_trans);
		    SearchDebugAgent.printTransUpdate(current_trans, pos.getHash());
		}
		return current_trans;
	}
}
