package eubos.search.transposition;

import java.util.ArrayList;
import java.util.List;

import eubos.board.InvalidPieceException;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.score.IEvaluate;
import eubos.search.PrincipalContinuation;
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
		ret.status = TranspositionTableStatus.insufficientNoData;
		ret.trans = hashMap.getTransposition(pos.getHash());
		if (ret.trans == null)
			return ret;
		
		if (ret.trans.getDepthSearchedInPly() >= depthRequiredPly) {
			
			if (ret.trans.getScoreType() == ScoreType.exact) {
				ret.status = TranspositionTableStatus.sufficientTerminalNode;
				SearchDebugAgent.printHashIsTerminalNode(currPly, ret.trans.getBestMove(), ret.trans.getScore(),pos.getHash());
			} else {
				// must be either (bound == ScoreType.upperBound || bound == ScoreType.lowerBound)
				if (st.isAlphaBetaCutOff(currPly, new Score(ret.trans.getScore(), ret.trans.getScoreType()))) {
					SearchDebugAgent.printHashIsRefutation(currPly, ret.trans.getBestMove(),pos.getHash());
					ret.status = TranspositionTableStatus.sufficientRefutation;
		        } else {
		        	ret.status = TranspositionTableStatus.sufficientSeedMoveList;
		        }
			}
		} else {
			ret.status = TranspositionTableStatus.sufficientSeedMoveList;
		}
		
		if (ret.trans.getBestMove() == null) {
			// It is possible that we don't have a move to seed the list with, guard against that.
			if (ret.status == TranspositionTableStatus.sufficientSeedMoveList) {
				ret.status = TranspositionTableStatus.insufficientNoData;
			}
		} else if (ret.status == TranspositionTableStatus.sufficientTerminalNode || 
				   ret.status == TranspositionTableStatus.sufficientRefutation) {
			// Check hashed position causing a search cut off is still valid (i.e. not a draw)
			try {
				int move = ret.trans.getBestMoveAsInt();
				if (move != Move.NULL_MOVE) {
					pm.performMove(move);
					if (pe.isThreeFoldRepetition(pos.getHash())) {
						currPly+=1;
						SearchDebugAgent.printRepeatedPositionHash(currPly, pos.getHash());
						removeTransposition(pos.getHash());
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
	
	public Transposition setTransposition(SearchMetrics sm, byte currPly, Transposition trans, byte new_Depth, short new_score, ScoreType new_bound, MoveList new_ml, int new_bestMove) {
		if (trans == null) {
			trans = getTransCreateIfNew(currPly, new_Depth, new_score, new_bound, new_ml, new_bestMove);
			sm.setHashFull(getHashUtilisation());
		}
		trans = checkForUpdateTrans(currPly, trans, new_Depth, new_score, new_bound, new_ml, new_bestMove);
		return trans;
	}
	
	private void removeTransposition(long hashCode) {
		hashMap.remove(hashCode);
	}
	
	public void createPrincipalContinuation(PrincipalContinuation pc, byte searchDepthPly, IChangePosition pm) throws InvalidPieceException {
		byte plies = 0;
		int numMoves = 0;
		List<Integer> constructed_pc = new ArrayList<Integer>(searchDepthPly);
		for (plies = 0; plies < searchDepthPly; plies++) {
			/* Apply move and find best move from hash */
			int pcMove = pc.getBestMove(plies); // Check against principal continuation where it is available
		    TranspositionEvaluation eval = this.getTransposition(searchDepthPly-plies);
			if (eval.status != TranspositionTableStatus.insufficientNoData && eval.trans != null) {
				int currMove = eval.trans.getBestMoveAsInt();
				if (currMove != Move.NULL_MOVE) {
					// Note, if the depth searched is more (from prev searches), it can be different to the pc for this search
					if (pcMove != Move.NULL_MOVE && (eval.trans.getDepthSearchedInPly() <= (searchDepthPly-plies))) {
						assert Move.areEqual(currMove, pcMove) : 
							"Error: "+pcMove+" != "+currMove+" @ply="+plies;
					}
					hashMap.protectHash(pos.getHash());
					constructed_pc.add(currMove);
					pm.performMove(currMove);
					numMoves++;
				}
			}
		}
		for (plies = (byte)(numMoves-1); plies >= 0; plies--) {
			pm.unperformMove();
		}
		pc.update(0, constructed_pc);
	}
	
	private Transposition getTransCreateIfNew(int currPly, byte new_Depth, short new_score, ScoreType new_bound, MoveList new_ml, int new_bestMove) {
		SearchDebugAgent.printTransNull(currPly, pos.getHash());
		Transposition trans = hashMap.getTransposition(pos.getHash());
		if (trans == null) {
			Transposition new_trans = new Transposition(new_Depth, new_score, new_bound, new_ml, new_bestMove);
			SearchDebugAgent.printCreateTrans(currPly, pos.getHash());
			hashMap.putTransposition(pos.getHash(), new_trans);
			SearchDebugAgent.printTransUpdate(currPly, new_trans, pos.getHash());
			trans = new_trans;
		}
		return trans;
	}
	
	private Transposition checkForUpdateTrans(int currPly, Transposition current_trans, byte new_Depth, short new_score, ScoreType new_bound, MoveList new_ml, int new_bestMove) {
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
			
		    hashMap.putTransposition(pos.getHash(), current_trans);
		    SearchDebugAgent.printTransUpdate(currPly, current_trans, pos.getHash());
		}
		return current_trans;
	}
	
	private short getHashUtilisation() {
		return (short) (( ((long) hashMap.getHashMapSize())*(long)1000) / hashMap.getHashMapMaxSize());
	}
}
