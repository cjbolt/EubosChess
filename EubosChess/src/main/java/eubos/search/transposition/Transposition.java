package eubos.search.transposition;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.search.Score;
import eubos.search.transposition.TranspositionEvaluation.TranspositionTableStatus;

public class Transposition implements ITransposition {
	protected byte depthSearchedInPly;
	protected short score;
	protected byte type;
	protected int bestMove;
	protected short accessCount;

	public Transposition(byte depth, short score, byte bound, GenericMove bestMove) {
		// Only used by tests
		this(depth, score, bound, Move.toMove(bestMove, null, Move.TYPE_REGULAR_NONE), null);
	}
	
	public Transposition(byte depth, short score, byte bound, int bestMove, List<Integer> pv) {
		setDepthSearchedInPly(depth);
		setScore(score);
		setType(bound);
		setBestMove(bestMove);
		setAccessCount((short)0);
	}

	@Override
	public byte getType() {
		return type;
	}

	protected void setType(byte type) {
		this.type = type;
	}

	@Override
	public short getScore() {
		return score;
	}

	protected void setScore(short new_score) {
		this.score = new_score;
	}
	
	@Override
	public byte getDepthSearchedInPly() {
		return depthSearchedInPly;
	}

	protected void setDepthSearchedInPly(byte depthSearchedInPly) {
		this.depthSearchedInPly = depthSearchedInPly;
	}

	@Override
	public int getBestMove() {
		return bestMove;
	}
	
	protected void setBestMove(int bestMove) {
		if (!Move.areEqual(this.bestMove, bestMove)) {
			this.bestMove = bestMove;
		}
	}
	
	@Override
	public String report() {
		String output = String.format("trans best=%s, dep=%d, sc=%s, type=%s", 
				Move.toString(bestMove),
				depthSearchedInPly,
				Score.toString(score),
				type);
		return output;
	}
	
	@Override
	public synchronized boolean checkUpdate(
			byte new_Depth, 
			short new_score,
			byte new_bound,
			int new_bestMove, 
			List<Integer> pv) {
		
		boolean updateTransposition = false;
		//mg.sda.printTransDepthCheck(depthSearchedInPly, new_Depth);
		
		if (depthSearchedInPly < new_Depth) {
			updateTransposition = true;
			
		} else if (depthSearchedInPly == new_Depth) {
			//mg.sda.printTransBoundScoreCheck(type, score, new_score);
			if (type == Score.bound && new_bound == Score.exact) {
			    updateTransposition = true;
			} else if (type == Score.bound && new_score > getScore()) {
				if (EubosEngineMain.ENABLE_ASSERTS)
					assert type == new_bound;
				updateTransposition = true;
			} else {
				// don't update, worse score
			}
		} else {
			// don't update, depth is less than what we have
		}
		
		if (updateTransposition) {
			depthSearchedInPly = new_Depth;
			score = new_score;
			type = new_bound;
			setBestMove(new_bestMove);
		}
		
		return updateTransposition;
	}
	
	@Override
	public synchronized boolean checkUpdateToExact(
			byte currDepthSearchedInPly,
			short new_score,
			int new_bestMove) {
		boolean wasSetAsExact = false;
		if (getDepthSearchedInPly() < currDepthSearchedInPly || (getDepthSearchedInPly() == currDepthSearchedInPly && type != Score.exact)) {
			// however we need to be careful that the depth is appropriate, we don't set exact for wrong depth...
			setScore(new_score);
			setType(Score.exact);
			setBestMove(new_bestMove);
			wasSetAsExact = true;
		}
		return wasSetAsExact;
	}
	
	public short getAccessCount() {
		return accessCount;
	}
	
	public void setAccessCount(short accessCount) {
		this.accessCount = accessCount;
	}
	
	public List<Integer> getPv() {
		return null;
	}
	
	public synchronized TranspositionTableStatus evaluateSuitability(int depthRequiredPly, int beta) {
		TranspositionTableStatus eval = TranspositionTableStatus.insufficientNoData;
		if (getDepthSearchedInPly() >= depthRequiredPly) {
			
			if (getType() == Score.exact) {
				eval = TranspositionTableStatus.sufficientTerminalNode;
				
			} else { // must be either (bound == Score.upperBound || bound == Score.lowerBound)
				if (getScore() >= beta && !Score.isProvisional(beta)) {
					eval = TranspositionTableStatus.sufficientRefutation;
		        } else {
		        	eval = TranspositionTableStatus.sufficientSeedMoveList;
		        }
			}
		} else {
			eval = TranspositionTableStatus.sufficientSeedMoveList;
		}
		
		if (eval == TranspositionTableStatus.sufficientSeedMoveList) {
			// It is possible that we don't have a move to seed the list with, guard against that.
			if (getBestMove() == Move.NULL_MOVE) {
				eval = TranspositionTableStatus.insufficientNoData;
			}
		}
		return eval;
	}
}
