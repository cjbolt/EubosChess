package eubos.search.transposition;

import eubos.position.MoveList;
import eubos.search.PrincipalContinuation;
import eubos.search.Score.ScoreType;
import eubos.search.SearchMetrics;

public interface ITranspositionAccessor {
	TranspositionEvaluation getTransposition(byte currPly, int depthRequiredPly);
	TranspositionEvaluation getTransposition(int depthRequiredPly);
	Transposition setTransposition(SearchMetrics sm, byte currPly, Transposition trans, byte new_Depth, short new_score, ScoreType new_bound, MoveList new_ml, int new_bestMove, PrincipalContinuation pc);
}
