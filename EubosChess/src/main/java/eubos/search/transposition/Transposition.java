package eubos.search.transposition;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.MoveList;
import eubos.position.Move;
import eubos.position.MoveList.MoveClassification;
import eubos.search.Score;
import eubos.search.Score.ScoreType;

public class Transposition {
	private byte depthSearchedInPly;
	private short score;
	private MoveList ml;
	private int bestMove;
	private ScoreType scoreType;

	public Transposition(byte depth, short score, ScoreType scoreType, MoveList ml, GenericMove bestMove) {
		this(depth, score, scoreType, ml, Move.toMove(bestMove, MoveClassification.REGULAR));
	}
	
	public Transposition(byte depth, short score, ScoreType scoreType, MoveList ml, int bestMove) {
		this.ml = ml;
		setDepthSearchedInPly(depth);
		setScore(score);
		setScoreType(scoreType);
		setBestMoveFromInt(bestMove);
	}
	
	public Transposition(byte depth, Score score, MoveList ml, int bestMove) {
		this(depth, score.getScore(), score.getType(), ml, bestMove);
	}

	public MoveList getMoveList() {
		if (ml != null) {
			ml.reorderWithNewBestMove(bestMove);
		}
		return ml;
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
		if (bestMove == 0)
			return null;
		else 
			return Move.toGenericMove(bestMove);
	}

	public void setBestMove(GenericMove bestMove) {
		if (bestMove != null) {
			this.bestMove = Move.toMove(bestMove, ml.getMoveTypeFromNormalList(bestMove));
		} else {
			this.bestMove = 0;
		}
	}
	
	public int getBestMoveAsInt() {
		return bestMove;
	}
	
	public void setBestMoveFromInt(int bestMove) {
		this.bestMove = bestMove;
	}
	
	public String report() {
		return "trans best:"+bestMove+" dep:"+depthSearchedInPly+" sc:"+score+" type:"+scoreType;
	}
	
	public void update(Transposition updateFrom) {
		this.setBestMoveFromInt(updateFrom.getBestMoveAsInt());
	    this.setDepthSearchedInPly(updateFrom.getDepthSearchedInPly());
	    this.setScoreType(updateFrom.getScoreType());
	    this.setScore(updateFrom.getScore());
	    this.ml = updateFrom.ml;
	}
}
