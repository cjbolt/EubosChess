package eubos.search.transposition;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.Move;
import eubos.search.Score;

public class Transposition implements ITransposition {
	private byte depthSearchedInPly;
	private short score;
	private int bestMove;
	private byte scoreType;
	private short accessCount;

	public Transposition(byte depth, short score, byte scoreType, GenericMove bestMove) {
		// Only used by tests
		this(depth, score, scoreType, Move.toMove(bestMove, null, Move.TYPE_REGULAR_NONE), null);
	}
	
	public Transposition(byte depth, short score, byte scoreType, int bestMove, List<Integer> pv) {
		setDepthSearchedInPly(depth);
		setScore(score);
		setType(scoreType);
		setBestMove(bestMove);
		setAccessCount((short)0);
	}
	
	public Transposition(byte depth, Score score, int bestMove, List<Integer> pv) {
		this(depth, score.getScore(), score.getType(), bestMove, pv);
	}

	@Override
	public byte getType() {
		return scoreType;
	}

	@Override
	public void setType(byte type) {
		this.scoreType = type;
	}

	@Override
	public short getScore() {
		return score;
	}

	@Override
	public void setScore(short score) {
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
				score,
				scoreType);
		return output;
	}
	
	@Override
	public void update(
			byte new_Depth, 
			short new_score, 
			byte new_bound, 
			int new_bestMove, 
			List<Integer> pv) {
		// TODO consider incrementing access count?
		setDepthSearchedInPly(new_Depth);
		setType(new_bound);
		setScore(new_score);
		setBestMove(new_bestMove);
	}
	
	public short getAccessCount() {
		return accessCount;
	}
	
	public void setAccessCount(short accessCount) {
		this.accessCount = accessCount;
	}
}
