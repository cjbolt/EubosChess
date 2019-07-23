package eubos.position;

import com.fluxchess.jcpi.models.GenericMove;

public class Transposition {
	private GenericMove bestMove;
	private int depthSearchedInPly;
	private int score;
	public enum ScoreType { 
		exact, upperBound, lowerBound;
	};
	private ScoreType scoreType;	
	
	public Transposition(GenericMove best, int depth, int score, ScoreType scoreType) {
		setBestMove(best);
		this.setDepthSearchedInPly(depth);
		this.setScore(score);
		this.setScoreType(scoreType);
	}

	public ScoreType getScoreType() {
		return scoreType;
	}

	public void setScoreType(ScoreType scoreType) {
		this.scoreType = scoreType;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public int getDepthSearchedInPly() {
		return depthSearchedInPly;
	}

	public void setDepthSearchedInPly(int depthSearchedInPly) {
		this.depthSearchedInPly = depthSearchedInPly;
	}

	public GenericMove getBestMove() {
		return bestMove;
	}

	public void setBestMove(GenericMove bestMove) {
		this.bestMove = bestMove;
	}
}
