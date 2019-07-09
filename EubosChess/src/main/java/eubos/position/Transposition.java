package eubos.position;

import com.fluxchess.jcpi.models.GenericMove;

public class Transposition {
	private GenericMove bestMove;
	private int depth;
	private int score;
	public enum ScoreType { 
		exact, upperBound, lowerBound;
	};
	private ScoreType type;	
}
