package eubos.search.transposition;

import java.util.List;

import eubos.board.InvalidPieceException;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.score.IEvaluate;
import eubos.search.Score;
import eubos.search.ScoreTracker;
import eubos.search.SearchDebugAgent;
import eubos.search.SearchMetrics;
import eubos.search.transposition.TranspositionEvaluation.*;
import eubos.search.Score.ScoreType;

public class TranspositionTableAccessor implements ITranspositionAccessor {
	
	private FixedSizeTranspositionTable hashMap;
	private IPositionAccessors pos;
	private IChangePosition pm;
	private ScoreTracker st;
	private IEvaluate pe;
	
	public TranspositionTableAccessor(
			FixedSizeTranspositionTable transTable,
			IPositionAccessors pos,
			ScoreTracker st,
			IChangePosition pm,
			IEvaluate pe) {
		hashMap = transTable;
		this.pos = pos;
		this.pm = pm;
		this.st = st;
		this.pe = pe;
	}
	
	public TranspositionEvaluation getTransposition(byte currPly, int depthRequiredPly) {
		TranspositionEvaluation ret = new TranspositionEvaluation();
		ret.trans = hashMap.getTransposition(pos.getHash());
		if (ret.trans == null)
			return ret;
		
		if (ret.trans.getDepthSearchedInPly() >= depthRequiredPly) {
			
			if (ret.trans.getScoreType() == ScoreType.exact) {
				ret.status = TranspositionTableStatus.sufficientTerminalNode;
				SearchDebugAgent.printHashIsTerminalNode(currPly, ret.trans, pos.getHash());
			} else {
				// must be either (bound == ScoreType.upperBound || bound == ScoreType.lowerBound)
				if (st.isAlphaBetaCutOff(currPly, new Score(ret.trans.getScore(), ret.trans.getScoreType()))) {
					SearchDebugAgent.printHashIsRefutation(currPly, pos.getHash(), ret.trans);
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
		} else if (ret.status == TranspositionTableStatus.sufficientTerminalNode || 
				   ret.status == TranspositionTableStatus.sufficientRefutation) {
			// Check hashed position causing a search cut off is still valid (i.e. not a draw)
			try {
				int move = ret.trans.getBestMove();
				if (move != Move.NULL_MOVE) {
					pm.performMove(move);
					if (pe.isThreeFoldRepetition(pos.getHash())) {
						currPly+=1;
						SearchDebugAgent.printRepeatedPositionHash(currPly, pos.getHash());
						hashMap.remove(pos.getHash());
						ret.status = TranspositionTableStatus.sufficientSeedMoveList;
						currPly-=1;
					}
					pm.unperformMove();
				}
			} catch (InvalidPieceException e) {
				e.printStackTrace();
			}
		}

		return ret;
	}
	
	public Transposition setTransposition(SearchMetrics sm, byte currPly, Transposition trans, byte new_Depth, short new_score, ScoreType new_bound, MoveList new_ml, int new_bestMove, List<Integer> pv) {
		if (trans == null) {
			trans = getTransCreateIfNew(currPly, new_Depth, new_score, new_bound, new_ml, new_bestMove, pv);
			sm.setHashFull(getHashUtilisation());
		}
		trans = checkForUpdateTrans(currPly, trans, new_Depth, new_score, new_bound, new_ml, new_bestMove, pv);
		return trans;
	}
	
	private Transposition getTransCreateIfNew(int currPly, byte new_Depth, short new_score, ScoreType new_bound, MoveList new_ml, int new_bestMove, List<Integer> pv) {
		SearchDebugAgent.printTransNull(currPly, pos.getHash());
		Transposition trans = hashMap.getTransposition(pos.getHash());
		if (trans == null) {
			Transposition new_trans = new Transposition(new_Depth, new_score, new_bound, new_ml, new_bestMove, pv);
			SearchDebugAgent.printCreateTrans(currPly, pos.getHash());
			hashMap.putTransposition(pos.getHash(), new_trans);
			SearchDebugAgent.printTransUpdate(currPly, new_trans, pos.getHash());
			trans = new_trans;
		}
		return trans;
	}
	
	private Transposition checkForUpdateTrans(int currPly, Transposition current_trans, byte new_Depth, short new_score, ScoreType new_bound, MoveList new_ml, int new_bestMove, List<Integer> pv) {
		boolean updateTransposition = false;
		int currentDepth = current_trans.getDepthSearchedInPly();
		ScoreType currentBound = current_trans.getScoreType();
		
		SearchDebugAgent.printTransDepthCheck(currPly, currentDepth, new_Depth);
		
		if (currentDepth < new_Depth) {
			updateTransposition = true;
		} 
		if (currentDepth == new_Depth) {
			SearchDebugAgent.printTransBoundScoreCheck(
					currPly,
					currentBound,
					current_trans.getScore(),
					new_score);
			if (((currentBound == ScoreType.upperBound) || (currentBound == ScoreType.lowerBound)) &&
					new_bound == ScoreType.exact) {
			    updateTransposition = true;
			} else if ((currentBound == ScoreType.upperBound) &&
					   (new_score < current_trans.getScore())) {
				assert currentBound == new_bound;
				updateTransposition = true;
			} else if ((currentBound == ScoreType.lowerBound) &&
					   (new_score > current_trans.getScore())) {
				assert currentBound == new_bound;
				updateTransposition = true;
			}
		}
		if (updateTransposition) {
			// order is important because setBestMove uses ml
			current_trans.setMoveList(new_ml);
			current_trans.setDepthSearchedInPly(new_Depth);
			current_trans.setScoreType(new_bound);
			current_trans.setScore(new_score);
			current_trans.setBestMove(new_bestMove);
			current_trans.setPv(pv);
			
		    hashMap.putTransposition(pos.getHash(), current_trans);
		    SearchDebugAgent.printTransUpdate(currPly, current_trans, pos.getHash());
		}
		return current_trans;
	}
	
	private short getHashUtilisation() {
		return (short) (( ((long) hashMap.getHashMapSize())*(long)1000) / hashMap.getHashMapMaxSize());
	}
}
