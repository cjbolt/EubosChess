package eubos.search;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

public class Transposition {
	private byte depthSearchedInPly;
	private short score;
	private List<GenericMove> ml;
	private GenericMove bestMove;

	public enum ScoreType { 
		exact, upperBound, lowerBound;
	};
	private ScoreType scoreType;	
	
	public Transposition(byte depth, short score, ScoreType scoreType, List<GenericMove> ml, GenericMove bestMove /*List<GenericMove> pc*/) {
		this.setDepthSearchedInPly(depth);
		this.setScore(score);
		this.setScoreType(scoreType);
		this.ml = ml;
		this.bestMove = bestMove;
		this.ml = adjustMoveListForBestMove();
	}

	public List<GenericMove> getMoveList() {
		if (ml != null)
			return adjustMoveListForBestMove();
		else return null;
	}

	protected List<GenericMove> adjustMoveListForBestMove() {
		GenericMove best = bestMove; // pc.get(0);
		if (ml != null) {
			List<GenericMove> ordered_ml = new LinkedList<GenericMove>();
			ordered_ml.addAll(ml);
			ordered_ml.remove(best);
			ordered_ml.add(0, best);
			return new ArrayList<GenericMove>(ordered_ml);
		} else {
			return null;
		}
	}

	public void setMoveList(List<GenericMove> ml) {
		this.ml = ml;
		this.ml = adjustMoveListForBestMove();
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
