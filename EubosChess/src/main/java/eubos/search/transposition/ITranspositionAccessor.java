package eubos.search.transposition;

import eubos.board.InvalidPieceException;
import eubos.position.IChangePosition;
import eubos.search.PrincipalContinuation;
import eubos.search.SearchMetrics;

public interface ITranspositionAccessor {
	TranspositionEvaluation getTransposition(byte currPly, int depthRequiredPly);
	TranspositionEvaluation getTransposition(int depthRequiredPly);
	Transposition setTransposition(SearchMetrics sm, byte currPly, Transposition trans, Transposition new_trans);
	void removeTransposition(long hashCode);
	void createPrincipalContinuation(PrincipalContinuation pc, byte searchDepthPly, IChangePosition pm) throws InvalidPieceException;
}
