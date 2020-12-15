package eubos.search.transposition;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.Move;
import eubos.search.Score;

public class Transposition implements ITransposition {
	private byte depthSearchedInPly;
	private int score;
	private int bestMove;
	private short accessCount;

	public Transposition(byte depth, int score, GenericMove bestMove) {
		// Only used by tests
		this(depth, score, Move.toMove(bestMove, null, Move.TYPE_REGULAR_NONE), null);
	}
	
	public Transposition(byte depth, int score, int bestMove, List<Integer> pv) {
		setDepthSearchedInPly(depth);
		this.score = score;
		setBestMove(bestMove);
		setAccessCount((short)0);
	}

	@Override
	public byte getType() {
		return (byte)Score.getType(score);
	}

	@Override
	public void setType(byte type) {
		this.score = Score.setType(this.score, type);
	}

	@Override
	public short getScore() {
		return Score.getScore(score);
	}

	@Override
	public void setScore(short score) {
		this.score = Score.valueOf(score, Score.getType(this.score));
	}
	
	@Override
	public void setScore(int score) {
		this.score = score;
	}

	@Override
	public byte getDepthSearchedInPly() {
		return depthSearchedInPly;
	}

	@Override
	public void setDepthSearchedInPly(byte depthSearchedInPly) {
		this.depthSearchedInPly = depthSearchedInPly;
	}

	@Override
	public int getBestMove() {
		return bestMove;
	}
	
	@Override
	public void setBestMove(int bestMove) {
		if (!Move.areEqual(this.bestMove, bestMove)) {
			this.bestMove = bestMove;
		}
	}
	
	@Override
	public String report() {
		String output = String.format("trans best=%s, dep=%d, sc=%d, type=%s", 
				Move.toString(bestMove),
				depthSearchedInPly,
				Score.getScore(score),
				Score.getType(score));
		return output;
	}
	
	@Override
	public void update(
			byte new_Depth, 
			int new_score, 
			int new_bestMove, 
			List<Integer> pv) {
		// TODO consider incrementing access count?
		setDepthSearchedInPly(new_Depth);
		setScore(new_score);
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
