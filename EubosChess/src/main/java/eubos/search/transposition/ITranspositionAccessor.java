package eubos.search.transposition;

import java.util.List;

import eubos.position.MoveList;
import eubos.search.Score.ScoreType;

public interface ITranspositionAccessor {
	TranspositionEvaluation getTransposition(byte currPly, int depthRequiredPly);
	Transposition setTransposition(byte currPly, Transposition trans, byte new_Depth, short new_score, ScoreType new_bound, MoveList new_ml, int new_bestMove, List<Integer> pv);
}
