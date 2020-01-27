package eubos.search.transposition;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.MoveList;
import eubos.position.Move;
import eubos.search.Score;
import eubos.search.Score.ScoreType;

public class Transposition {
	private byte depthSearchedInPly;
	private short score;
	private MoveList ml;
	private int bestMove;
	private ScoreType scoreType;

	public Transposition(byte depth, short score, ScoreType scoreType, MoveList ml, GenericMove bestMove) {
		// Only used by tests
		this(depth, score, scoreType, ml, Move.toMove(bestMove, Move.TYPE_NONE));
	}
	
	public Transposition(byte depth, short score, ScoreType scoreType, MoveList ml, int bestMove) {
		this.ml = ml;
		setDepthSearchedInPly(depth);
		setScore(score);
		setScoreType(scoreType);
		setBestMove(bestMove);
	}
	
	public Transposition(byte depth, Score score, MoveList ml, int bestMove) {
		this(depth, score.getScore(), score.getType(), ml, bestMove);
	}

	public MoveList getMoveList() {
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

	public int getBestMoveAsInt() {
		return bestMove;
	}
	
	public void setBestMove(int bestMove) {
		if (!Move.areEqual(this.bestMove, bestMove)) {
			this.bestMove = bestMove;
			if (bestMove != 0) {
				this.ml.reorderWithNewBestMove(bestMove);
			}
		}
	}
	
	public String report() {
		return "trans best: "+Move.toString(bestMove)+" dep:"+depthSearchedInPly+" sc:"+score+" type:"+scoreType +" ml: " + ml + " ref:" + Integer.toHexString(System.identityHashCode(ml));
	}
	
	public void update(Transposition updateFrom) {
		// order is important because setBestMove uses ml
		this.ml = updateFrom.ml;
	    this.setDepthSearchedInPly(updateFrom.getDepthSearchedInPly());
	    this.setScoreType(updateFrom.getScoreType());
	    this.setScore(updateFrom.getScore());
	    this.setBestMove(updateFrom.getBestMoveAsInt());
	}

	public void setMoveList(MoveList new_ml) {
		this.ml = new_ml;		
	}
}
