package eubos.search.transposition;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.Move;
import eubos.search.Score;

public class Transposition implements ITransposition {
	protected byte depthSearchedInPly;
	protected short score;
	protected byte type;
	protected int bestMove;
	protected short accessCount;
	protected int hashFragment;

	public Transposition(long hash, byte depth, short score, byte bound, GenericMove bestMove) {
		// Only used by tests
		this(hash, depth, score, bound, Move.toMove(bestMove, null, Move.TYPE_REGULAR_NONE), null);
	}
	
	public Transposition(long hash, byte depth, short score, byte bound, int bestMove, List<Integer> pv) {
		setDepthSearchedInPly(depth);
		setScore(score);
		setType(bound);
		setBestMove(bestMove);
		setAccessCount((short)0);
		setHashFragment(hash);
	}

	private void setHashFragment(long hash) {
		hashFragment = (int)(hash >> 32);
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
		if (depthSearchedInPly < new_Depth) {
			updateTransposition = true;	
		} else if (depthSearchedInPly == new_Depth) {
			if (type != Score.exact && new_score > getScore()) {
				updateTransposition = true;
			} else {
				// don't update, worse bound score than we currently have
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
	public synchronized boolean checkUpdateToExact(byte currDepthSearchedInPly) {
		boolean wasSetAsExact = false;
		if (getDepthSearchedInPly() == currDepthSearchedInPly && getType() != Score.exact) {
			// We need to be careful that the depth searched is appropriate, i.e. we don't set exact for wrong depth...
			setType(Score.exact);
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

	@Override
	public boolean checkHash(int hashCode) {
		return hashCode == hashFragment;
	}
}
