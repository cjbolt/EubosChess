package eubos.search.transposition;

public interface ITranspositionAccessor {
	
	long getTransposition();
	long setTransposition(long trans, byte depth, short new_score, byte new_bound, int new_bestMove, int new_age);
}
