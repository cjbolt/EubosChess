package eubos.search.transposition;

import java.util.List;

import eubos.position.MoveList;

public interface ITranspositionAccessor {
	TranspositionEvaluation getTransposition(byte currPly, int depthRequiredPly);
	ITransposition setTransposition(byte currPly, ITransposition trans, byte new_Depth, short new_score, byte new_bound, MoveList new_ml, int new_bestMove, List<Integer> pv);
}
