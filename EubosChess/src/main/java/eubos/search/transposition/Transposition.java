package eubos.search.transposition;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.Move;
import eubos.search.Score;

public class Transposition implements ITransposition {
	protected short score;
	protected int bestMove;
	protected short hashFragment;
	protected short bitfield;

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
		hashFragment = (short)(hash >>> 48);
	}
	
	@Override
	public byte getDepthSearchedInPly() {
		return (byte)((bitfield >>> 0) & 0x3F);
	}

	protected void setDepthSearchedInPly(byte depthSearchedInPly) {
		short limitedDepth = (short)Math.min(0x3F, depthSearchedInPly);
		bitfield &= ~(0x3F << 0);
		bitfield |= (short)((limitedDepth & 0x3F) << 0);
	}
	
	@Override
	public byte getType() {
		return (byte)((bitfield >>> 6) & 0x3);
	}

	protected void setType(byte type) {
		bitfield &= ~(0x3 << 6);
		bitfield |= (short)((type & 0x3) << 6);
	}
	
	public short getAccessCount() {
		return (short)((bitfield >>> 8) & 0xFF);
	}
	
	public void setAccessCount(short accessCount) {
		short limitedAccessCount = (short)Math.min(0xFF, accessCount);
		bitfield &= ~(0xFF << 8);
		bitfield |= (short)((limitedAccessCount & 0xFF) << 8);
	}

	@Override
	public short getScore() {
		return score;
	}

	protected void setScore(short new_score) {
		this.score = new_score;
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
				getDepthSearchedInPly(),
				Score.toString(score),
				getType());
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
		if (getDepthSearchedInPly() < new_Depth) {
			updateTransposition = true;	
		} else if (getDepthSearchedInPly() == new_Depth) {
			if (getType() != Score.exact && new_score > getScore()) {
				updateTransposition = true;
			} else {
				// don't update, worse bound score than we currently have
			}
		} else {
			// don't update, depth is less than what we have
		}
		if (updateTransposition) {
			setDepthSearchedInPly(new_Depth);
			score = new_score;
			setType(new_bound);
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
	
	public List<Integer> getPv() {
		return null;
	}

	@Override
	public boolean checkHash(int hashCode) {
		short checker = (short)(hashCode >>> 16);
		return (checker == hashFragment);
	}
}
