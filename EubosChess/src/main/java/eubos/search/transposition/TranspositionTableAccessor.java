package eubos.search.transposition;

import java.util.List;

import eubos.board.InvalidPieceException;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.score.IEvaluate;
import eubos.search.Score;
import eubos.search.ScoreTracker;
import eubos.search.SearchDebugAgent;
import eubos.search.transposition.TranspositionEvaluation.*;

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
			
			if (ret.trans.getType() == Score.exact) {
				ret.status = TranspositionTableStatus.sufficientTerminalNode;
				SearchDebugAgent.printHashIsTerminalNode(ret.trans, pos.getHash());
			} else {
				// must be either (bound == Score.upperBound || bound == Score.lowerBound)
				if (st.isAlphaBetaCutOff(currPly, new Score(ret.trans.getScore(), ret.trans.getType()))) {
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
		} else if (ret.status == TranspositionTableStatus.sufficientTerminalNode || 
	               ret.status == TranspositionTableStatus.sufficientRefutation) {
			// Check hashed position causing a search cut off is still valid (i.e. not a potential draw)
			if (isHashedPositionCouldLeadToDraw(ret.trans.getBestMove())) {
				// This will cause the position to be re-searched and re-scored in line with the current search context.
				ret.status = TranspositionTableStatus.insufficientNoData;
				ret.trans = null;
			}
		}
		return ret;
	}

	private boolean isHashedPositionCouldLeadToDraw(int move) {
		boolean retVal = false;
		try {
			if (move != Move.NULL_MOVE) {
				pm.performMove(move);
				SearchDebugAgent.nextPly();
				// we have to apply the move the hashed score is for to detect whether this hash is encountered for a second time
				if (pe.couldLeadToThreeFoldRepetiton(pos.getHash())) {
					SearchDebugAgent.printRepeatedPositionHash(pos.getHash(), pos.getFen());
					retVal = true;
				}
				pm.unperformMove();
				SearchDebugAgent.prevPly();
				// remove the hash to not hit it again...
				hashMap.remove(pos.getHash());
			}
		} catch (InvalidPieceException e) {
			e.printStackTrace();
		}
		return retVal;
	}
	
	public ITransposition setTransposition(ITransposition trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove) {
		if (trans == null) {
			trans = getTransCreateIfNew(new_Depth, new_score, new_bound, new_bestMove);
		}
		trans = checkForUpdateTrans(trans, new_Depth, new_score, new_bound, new_bestMove, null);
		return trans;
	}
	
	private ITransposition getTransCreateIfNew(byte new_Depth, short new_score, byte new_bound, int new_bestMove) {
		SearchDebugAgent.printTransNull(pos.getHash());
		ITransposition trans = hashMap.getTransposition(pos.getHash());
		if (trans == null) {
			ITransposition new_trans = new Transposition(new_Depth, new_score, new_bound, new_bestMove, null);
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
		byte currentBound = current_trans.getType();
		
		SearchDebugAgent.printTransDepthCheck(currentDepth, new_Depth);
		
		if (currentDepth < new_Depth) {
			updateTransposition = true;
		} 
		if (currentDepth == new_Depth) {
			SearchDebugAgent.printTransBoundScoreCheck(currentBound, current_trans.getScore(), new_score);
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
		    SearchDebugAgent.printTransUpdate(current_trans, pos.getHash());
		}
		return current_trans;
	}
	
	public short getHashUtilisation() {
		return (short) (( ((long) hashMap.getHashMapSize())*(long)1000) / hashMap.getHashMapMaxSize());
	}
}
