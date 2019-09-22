package eubos.search;

interface ITranspositionAccessor {
	TranspositionEvaluation getTransposition(byte currPly, int depthRequiredPly);
	TranspositionEvaluation getTransposition(int depthRequiredPly);
	Transposition setTransposition(SearchMetrics sm, byte currPly, Transposition trans, Transposition new_trans);
}
