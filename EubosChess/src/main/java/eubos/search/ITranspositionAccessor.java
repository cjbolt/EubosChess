package eubos.search;

import eubos.board.InvalidPieceException;
import eubos.position.IChangePosition;

interface ITranspositionAccessor {
	TranspositionEvaluation getTransposition(byte currPly, int depthRequiredPly);
	TranspositionEvaluation getTransposition(int depthRequiredPly);
	Transposition setTransposition(SearchMetrics sm, byte currPly, Transposition trans, Transposition new_trans);
	
	void createPrincipalContinuation(PrincipalContinuation pc, byte searchDepthPly, IChangePosition pm) throws InvalidPieceException;
}
