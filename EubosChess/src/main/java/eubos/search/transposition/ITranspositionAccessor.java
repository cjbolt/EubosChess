package eubos.search.transposition;

import java.util.List;

public interface ITranspositionAccessor {
	
	public static boolean USE_PRINCIPAL_VARIATION_TRANSPOSITIONS = false;
	
	TranspositionEvaluation getTransposition(int depthRequiredPly, int beta);
	ITransposition setTransposition(ITransposition trans, byte depth, short new_score, byte new_bound, int new_bestMove);
	ITransposition setTransposition(ITransposition trans, byte depth, short new_score, byte new_bound, int new_bestMove, List<Integer> pv);
}
