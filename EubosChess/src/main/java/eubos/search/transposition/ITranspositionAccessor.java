package eubos.search.transposition;

public interface ITranspositionAccessor {
	TranspositionEvaluation getTransposition(byte currPly, int depthRequiredPly);
	ITransposition setTransposition(byte currPly, ITransposition trans, byte new_Depth, short new_score, byte new_bound, int new_bestMove);
}
