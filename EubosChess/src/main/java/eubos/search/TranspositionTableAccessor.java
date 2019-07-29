package eubos.search;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.pieces.Piece.Colour;
import eubos.position.IPositionAccessors;
import eubos.position.Transposition;
import eubos.position.Transposition.ScoreType;

public class TranspositionTableAccessor {
	
	private FixedSizeTranspositionTable hashMap;
	private IPositionAccessors pos;
	private ScoreTracker st;
	private PrincipalContinuation pc;
	private List<GenericMove> lastPc;
	
	public enum TranspositionTableStatus {
		none,
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
		this.pc = pc;
		this.lastPc = lastPc;
	}
	
	TranspositionTableStatus evaluateTranspositionData(int currPly, int searchDepthPly) {
		TranspositionTableStatus status = TranspositionTableStatus.sufficientSeedMoveList;
		Colour onMove = pos.getOnMove();
		
		Transposition trans = hashMap.getTransposition(pos.getHash().hashCode);
		if (trans == null)
			return status;
		
		int depth = trans.getDepthSearchedInPly();
		int score = trans.getScore();
		ScoreType bound = trans.getScoreType();
		GenericMove move = trans.getBestMove();
		
		int prevScore = st.getBackedUpScoreAtPly(currPly);
		
		// Condition for considering Transposition score sufficient to be used as terminal node
		if (depth >= (searchDepthPly-currPly)) {
			if (bound == ScoreType.exact) {
				if ((onMove==Colour.white) && (score > prevScore)) {
					status = TranspositionTableStatus.sufficientTerminalNode;
				} else if ((onMove==Colour.black) && (score < prevScore)) {
					status = TranspositionTableStatus.sufficientTerminalNode;
				}
			}
		    // Transposition score is a refutation of previous move
			else if (bound == ScoreType.upperBound || bound == ScoreType.lowerBound) {
				if (st.isAlphaBetaCutOff(currPly, prevScore, score )) {
					status = TranspositionTableStatus.sufficientRefutation;
		        }
			}
		// Transposition just sufficient to seed the MoveList for searching
		} else if (move != null) {
			// Seed the move list for the next search with previous best move.
			status = TranspositionTableStatus.sufficientSeedMoveList;
			try {
				lastPc.set(currPly, move);
			} catch (IndexOutOfBoundsException e) {
				for (int i=lastPc.size(); i < currPly; i++) {
					lastPc.add(i, move);
				}
			}
		}
		if (status == TranspositionTableStatus.sufficientTerminalNode) {
			st.setBackedUpScoreAtPly(currPly, score);
			pc.update(currPly, trans.getBestMove());
		}
		return status;
	}
	
	void storeTranspositionScore(int currPly, int searchDepthPly, int score, ScoreType bound) {
		boolean updateTransposition = false;
		int depthPositionSearched = (searchDepthPly - currPly);
		
		Transposition trans = hashMap.getTransposition(pos.getHash().hashCode);
		if (trans != null) {
			int currentDepth = trans.getDepthSearchedInPly();
			ScoreType currentBound = trans.getScoreType();
		
			if (currentDepth < depthPositionSearched) {
				updateTransposition = true;
			} else if ((currentDepth == depthPositionSearched) && 
				       ((currentBound == ScoreType.upperBound) || (currentBound == ScoreType.lowerBound)) &&
				        bound == ScoreType.exact) {
			    updateTransposition = true;
			} else {
				 // No other condition to store
			}
		} else {
			updateTransposition = true;
		}
		if (updateTransposition) {
			GenericMove move = (depthPositionSearched == 0) ? null : pc.getBestMove(currPly);
			trans = new Transposition(move, depthPositionSearched, score, bound);
			hashMap.putTransposition(pos.getHash().hashCode, trans);	
		}
	}
}
