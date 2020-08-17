package eubos.search.transposition;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.MoveList;
import eubos.position.Move;
import eubos.search.Score;
import eubos.search.Score.ScoreType;

public class Transposition implements ITransposition {
	private byte depthSearchedInPly;
	private short score;
	private MoveList ml;
	private int bestMove;
	private ScoreType scoreType;

	public Transposition(byte depth, short score, ScoreType scoreType, MoveList ml, GenericMove bestMove) {
		// Only used by tests
		this(depth, score, scoreType, ml, Move.toMove(bestMove, null, Move.TYPE_NONE), null);
	}
	
	public Transposition(byte depth, short score, ScoreType scoreType, MoveList ml, int bestMove, List<Integer> pv) {
		setMoveList(ml);
		setDepthSearchedInPly(depth);
		setScore(score);
		setScoreType(scoreType);
		setBestMove(bestMove);
	}
	
	public Transposition(byte depth, Score score, MoveList ml, int bestMove, List<Integer> pv) {
		this(depth, score.getScore(), score.getType(), ml, bestMove, pv);
	}

	@Override
	public MoveList getMoveList() {
		return ml;
	}
	
	@Override
	public ScoreType getScoreType() {
		return scoreType;
	}

	@Override
	public void setScoreType(ScoreType scoreType) {
		this.scoreType = scoreType;
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
			if (bestMove != 0) {
				this.ml.reorderWithNewBestMove(bestMove);
			}
		}
	}
	
	void setMoveList(MoveList new_ml) {
		this.ml = new_ml;		
	}
	
	@Override
	public List<Integer> getPv() {
		return null;
	}

	@Override
	public void setPv(List<Integer> pv) {
	}
	
	@Override
	public String report() {
		String onward_pv = "";
		String output = String.format("trans best=%s, dep=%d, sc=%d, type=%s, pv=%s", 
				Move.toString(bestMove),
				depthSearchedInPly,
				score,
				scoreType,
				onward_pv);
		return output;
	}
	
	@Override
	public void update(
			byte new_Depth, 
			short new_score, 
			ScoreType new_bound, 
			MoveList new_ml, 
			int new_bestMove, 
			List<Integer> pv) {
		// order is important because setBestMove uses ml
		setMoveList(new_ml);
		setDepthSearchedInPly(new_Depth);
		setScoreType(new_bound);
		setScore(new_score);
		setBestMove(new_bestMove);
	}
}
