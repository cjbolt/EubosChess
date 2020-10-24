package eubos.search.transposition;

import java.util.List;

public interface ITranspositionAccessor {
	
	public static boolean USE_PRINCIPAL_VARIATION_TRANSPOSITIONS = false;
	
	TranspositionEvaluation getTransposition(byte currPly, int depthRequiredPly);
	ITransposition setTransposition(ITransposition trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove);
	ITransposition setTransposition(ITransposition trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove, List<Integer> pv);
	public short getHashUtilisation();
}
