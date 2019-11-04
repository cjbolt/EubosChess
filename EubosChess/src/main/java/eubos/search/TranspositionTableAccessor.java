package eubos.search;

import java.util.ArrayList;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.search.Transposition.ScoreType;
import eubos.search.TranspositionEvaluation.TranspositionTableStatus;

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
		ret.status = TranspositionTableStatus.insufficientNoData;
		ret.trans = hashMap.getTransposition(pos.getHash());
		if (ret.trans == null)
			return ret;
		
		if (ret.trans.getDepthSearchedInPly() >= depthRequiredPly) {
			
			if (ret.trans.getScoreType() == ScoreType.exact) {
				ret.status = TranspositionTableStatus.sufficientTerminalNode;
				SearchDebugAgent.printHashIsTerminalNode(currPly, ret.trans.getBestMove(), ret.trans.getScore(),pos.getHash());
			} else if (ret.trans.getPreviousExactDepth() >= depthRequiredPly) {
				ret.status = TranspositionTableStatus.sufficientTerminalNodeBeta;
				SearchDebugAgent.printHashIsTerminalNode(currPly, ret.trans.getBestMove(), ret.trans.getPreviousExactScore(),pos.getHash());
			} else { // must be (bound == ScoreType.upperBound || bound == ScoreType.lowerBound)
				if (st.isAlphaBetaCutOff(currPly, ret.trans.getScore())) {
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
	
	public TranspositionEvaluation getTransposition(int depthRequiredPly) {
		TranspositionEvaluation ret = new TranspositionEvaluation();
		ret.status = TranspositionTableStatus.insufficientNoData;
		ret.trans = hashMap.getTransposition(pos.getHash());
		if (ret.trans != null) {
			if ((ret.trans.getDepthSearchedInPly() >=  depthRequiredPly) || ret.trans.getBestMove() != null) {
				ret.status = TranspositionTableStatus.sufficientSeedMoveList;
			}
		}
		return ret;
	}
	
	public Transposition setTransposition(SearchMetrics sm, byte currPly, Transposition trans, Transposition new_trans) {
		if (trans == null) {
			trans = getTransCreateIfNew(currPly, new_trans);
			sm.setHashFull(getHashUtilisation());
		}
		trans = checkForUpdateTrans(currPly, new_trans, trans);
		return trans;
	}
	
	public void createPrincipalContinuation(PrincipalContinuation pc, byte searchDepthPly, IChangePosition pm) throws InvalidPieceException {
		byte plies = 0;
		int numMoves = 0;
		List<GenericMove> constructed_pc = new ArrayList<GenericMove>(searchDepthPly);
		for (plies = 0; plies < searchDepthPly; plies++) {
			/* Apply move and find best move from hash */
			GenericMove pcMove = pc.getBestMove(plies); // Check against principal continuation where it is available
		    TranspositionEvaluation eval = this.getTransposition(searchDepthPly-plies);
			if (eval.status != TranspositionTableStatus.insufficientNoData && eval.trans != null) {
				GenericMove currMove = eval.trans.getBestMove();
				if (pcMove != null) assert currMove == pcMove : "Error: "+pcMove+" != "+currMove+" @ply="+plies;
				constructed_pc.add(currMove);
				pm.performMove(currMove);
				numMoves++;
			}
		}
		for (plies = (byte)(numMoves-1); plies >= 0; plies--) {
			pm.unperformMove();
		}
		pc.update(0, constructed_pc);
	}
	
	private Transposition getTransCreateIfNew(int currPly, Transposition new_trans) {
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
	
	private Transposition checkForUpdateTrans(int currPly, Transposition new_trans, Transposition current_trans) {
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
				assert currentBound == new_trans.getScoreType();
				updateTransposition = true;
			} else if ((currentBound == ScoreType.lowerBound) &&
					   (new_trans.getScore() > current_trans.getScore())) {
				assert currentBound == new_trans.getScoreType();
				updateTransposition = true;
			}
		}
		if (updateTransposition) {
			current_trans.update(new_trans);
		    hashMap.putTransposition(pos.getHash(), current_trans);
		    SearchDebugAgent.printTransUpdate(currPly, current_trans, pos.getHash());
		}
		return current_trans;
	}
	
	private short getHashUtilisation() {
		return (short) (( ((long) hashMap.getHashMapSize())*(long)1000) / FixedSizeTranspositionTable.MAX_SIZE_OF_HASH_MAP);
	}
}
