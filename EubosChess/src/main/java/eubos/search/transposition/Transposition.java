package eubos.search.transposition;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.Move;
import eubos.search.Score;

public class Transposition implements ITransposition {
	private byte depthSearchedInPly;
	private short score;
	private byte type;
	private int bestMove;
	private short accessCount;

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

	private void setType(byte type) {
		this.type = type;
	}

	@Override
	public short getScore() {
		return score;
	}

	private void setScore(short new_score) {
		this.score = new_score;
	}
	
	@Override
	public byte getDepthSearchedInPly() {
		return depthSearchedInPly;
	}

	private void setDepthSearchedInPly(byte depthSearchedInPly) {
		this.depthSearchedInPly = depthSearchedInPly;
	}

	@Override
	public int getBestMove() {
		return bestMove;
	}
	
	private void setBestMove(int bestMove) {
		if (!Move.areEqual(this.bestMove, bestMove)) {
			this.bestMove = bestMove;
		}
	}
	
	@Override
	public String report() {
		String output = String.format("trans best=%s, dep=%d, sc=%d, type=%s", 
				Move.toString(bestMove),
				depthSearchedInPly,
				score,
				type);
		return output;
	}
	
	@Override
	public synchronized void update(
			byte new_Depth, 
			short new_score,
			byte new_bound,
			int new_bestMove, 
			List<Integer> pv) {
		setDepthSearchedInPly(new_Depth);
		setScore(new_score);
		setType(new_bound);
		setBestMove(new_bestMove);
	}
	
	@Override
	public synchronized void updateToExact(
			short new_score,
			int new_bestMove) {
		setScore(new_score);
		setType(Score.exact);
		setBestMove(new_bestMove);
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
}
