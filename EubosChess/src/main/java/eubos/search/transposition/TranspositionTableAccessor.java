package eubos.search.transposition;

import java.util.List;

import eubos.board.InvalidPieceException;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.score.IEvaluate;
import eubos.search.Score;
import eubos.search.ScoreTracker;
import eubos.search.SearchDebugAgent;
import eubos.search.SearchMetrics;
import eubos.search.transposition.TranspositionEvaluation.*;

public class TranspositionTableAccessor implements ITranspositionAccessor {
	
	private FixedSizeTranspositionTable hashMap;
	private IPositionAccessors pos;
	private IChangePosition pm;
	private ScoreTracker st;
	private IEvaluate pe;
	private SearchMetrics sm;
	
	public TranspositionTableAccessor(
			FixedSizeTranspositionTable transTable,
			IPositionAccessors pos,
			ScoreTracker st,
			IChangePosition pm,
			IEvaluate pe,
			SearchMetrics sm) {
		hashMap = transTable;
		this.pos = pos;
		this.pm = pm;
		this.st = st;
		this.pe = pe;
		this.sm = sm;
	}
	
	public TranspositionEvaluation getTransposition(byte currPly, int depthRequiredPly) {
		TranspositionEvaluation ret = new TranspositionEvaluation();
		ret.trans = hashMap.getTransposition(pos.getHash());
		if (ret.trans == null)
			return ret;
		
		if (ret.trans.getDepthSearchedInPly() >= depthRequiredPly) {
			
			if (ret.trans.getType() == Score.exact) {
				ret.status = TranspositionTableStatus.sufficientTerminalNode;
				SearchDebugAgent.printHashIsTerminalNode(currPly, ret.trans, pos.getHash());
			} else {
				// must be either (bound == Score.upperBound || bound == Score.lowerBound)
				if (st.isAlphaBetaCutOff(currPly, new Score(ret.trans.getScore(), ret.trans.getType()))) {
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
			// Check hashed position causing a search cut off is still valid (i.e. not a potential draw)
			if (isHashedPositionCouldLeadToDraw(currPly, ret.trans.getBestMove())) {
				// This will cause the position to be re-searched and re-scored in line with the current search context.
				ret.status = TranspositionTableStatus.sufficientSeedMoveList;
			}
		}
		return ret;
	}

	private boolean isHashedPositionCouldLeadToDraw(byte currPly, int move) {
		boolean retVal = false;
		try {
			if (move != Move.NULL_MOVE) {
				pm.performMove(move);
				// we have to apply the move the hashed score is for to detect whether this hash is encountered for a second time
				if (pe.couldLeadToThreeFoldRepetiton(pos.getHash())) {
					SearchDebugAgent.printRepeatedPositionHash((byte)(currPly+1), pos.getHash(), pos.getFen());
					retVal = true;
				}
				pm.unperformMove();
			}
		} catch (InvalidPieceException e) {
			e.printStackTrace();
		}
		return retVal;
	}
	
	public ITransposition setTransposition(byte currPly, ITransposition trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove) {
		if (trans == null) {
			trans = getTransCreateIfNew(currPly, new_Depth, new_score, new_bound, new_bestMove);
			if (EubosEngineMain.UCI_INFO_ENABLED)
				sm.setHashFull(getHashUtilisation());
		}
		trans = checkForUpdateTrans(currPly, trans, new_Depth, new_score, new_bound, new_bestMove, null);
		return trans;
	}
	
	private ITransposition getTransCreateIfNew(int currPly, byte new_Depth, short new_score, byte new_bound, int new_bestMove) {
		SearchDebugAgent.printTransNull(currPly, pos.getHash());
		ITransposition trans = hashMap.getTransposition(pos.getHash());
		if (trans == null) {
			ITransposition new_trans = new Transposition(new_Depth, new_score, new_bound, new_bestMove, null);
			SearchDebugAgent.printCreateTrans(currPly, pos.getHash());
			hashMap.putTransposition(pos.getHash(), new_trans);
			SearchDebugAgent.printTransUpdate(currPly, new_trans, pos.getHash());
			trans = new_trans;
		}
		return trans;
	}
	
	private ITransposition checkForUpdateTrans(int currPly, ITransposition current_trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove, List<Integer> pv) {
		boolean updateTransposition = false;
		int currentDepth = current_trans.getDepthSearchedInPly();
		byte currentBound = current_trans.getType();
		
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
			if (((currentBound == Score.upperBound) || (currentBound == Score.lowerBound)) &&
					new_bound == Score.exact) {
			    updateTransposition = true;
			} else if ((currentBound == Score.upperBound) &&
					   (new_score < current_trans.getScore())) {
				assert currentBound == new_bound;
				updateTransposition = true;
			} else if ((currentBound == Score.lowerBound) &&
					   (new_score > current_trans.getScore())) {
				assert currentBound == new_bound;
				updateTransposition = true;
			}
		}
		if (updateTransposition) {
			current_trans.update(new_Depth, new_score, new_bound, new_bestMove, pv );
		    hashMap.putTransposition(pos.getHash(), current_trans);
		    SearchDebugAgent.printTransUpdate(currPly, current_trans, pos.getHash());
		}
		return current_trans;
	}
	
	private short getHashUtilisation() {
		return (short) (( ((long) hashMap.getHashMapSize())*(long)1000) / hashMap.getHashMapMaxSize());
	}
}
