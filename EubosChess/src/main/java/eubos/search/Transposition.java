package eubos.search;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.MoveList;

public class Transposition {
	private byte depthSearchedInPly;
	private short score;
	private MoveList ml;
	private GenericMove bestMove;

	public enum ScoreType { 
		exact, upperBound, lowerBound;
	};
	private ScoreType scoreType;	
	
	public Transposition(byte depth, short score, ScoreType scoreType, MoveList ml, GenericMove bestMove) {
		setDepthSearchedInPly(depth);
		setScore(score);
		setScoreType(scoreType);
		setBestMove(bestMove);
		setMoveList(ml);
	}

	public MoveList getMoveList() {
	    adjustMoveListForBestMove();
		return ml;
	}
	
	public void setMoveList(MoveList ml) {
		this.ml = ml;
		adjustMoveListForBestMove();
	}

	protected void adjustMoveListForBestMove() {
		if (ml != null) {
			ml.adjustForBestMove(bestMove);
		}
	}
	
	public ScoreType getScoreType() {
		return scoreType;
	}

	public void setScoreType(ScoreType scoreType) {
		this.scoreType = scoreType;
	}

	public short getScore() {
		return score;
	}

	public void setScore(short score) {
		this.score = score;
	}

	public byte getDepthSearchedInPly() {
		return depthSearchedInPly;
	}

	public void setDepthSearchedInPly(byte depthSearchedInPly) {
		this.depthSearchedInPly = depthSearchedInPly;
	}

	public GenericMove getBestMove() {
		return bestMove;
	}

	public void setBestMove(GenericMove bestMove) {
		this.bestMove = bestMove;
	}
	
	public String report() {
		return "trans best:"+bestMove+" dep:"+depthSearchedInPly+" sc:"+score+" type:"+scoreType;
	}
}
