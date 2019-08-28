package eubos.position;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

public class Transposition {
	private GenericMove bestMove;
	private byte depthSearchedInPly;
	private short score;
	private List<GenericMove> ml;
	private List<GenericMove> pc;

	public enum ScoreType { 
		exact, upperBound, lowerBound;
	};
	private ScoreType scoreType;	
	
	public Transposition(GenericMove best, byte depth, short score, ScoreType scoreType, List<GenericMove> ml, List<GenericMove> pc) {
		setBestMove(best);
		this.setDepthSearchedInPly(depth);
		this.setScore(score);
		this.setScoreType(scoreType);
		this.ml = ml;
		this.pc = pc;
		this.ml = adjustMoveListForBestMove();
	}

	public List<GenericMove> getPrincipalContinuation() {
		return pc;
	}

	public void setPrincipalContinuation(List<GenericMove> pc) {
		this.pc = pc;
	}

	public List<GenericMove> getMoveList() {
		if (bestMove != null) {
			return adjustMoveListForBestMove();
		}
		return ml;
	}

	protected List<GenericMove> adjustMoveListForBestMove() {
		if (ml != null) {
			List<GenericMove> ordered_ml = new LinkedList<GenericMove>();
			ordered_ml.addAll(ml);
			ordered_ml.remove(bestMove);
			ordered_ml.add(0, bestMove);
			return new ArrayList<GenericMove>(ordered_ml);
		} else {
			return null;
		}
	}

	public void setMoveList(List<GenericMove> ml) {
		this.ml = ml;
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
		return "trans mv:"+bestMove.toString()+" dep:"+depthSearchedInPly+" sc:"+score+" type:"+scoreType;
	}
}
