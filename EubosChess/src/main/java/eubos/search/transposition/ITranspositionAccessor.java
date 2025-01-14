package eubos.search.transposition;

public interface ITranspositionAccessor {
	
	long getTransposition(long hash);
	long setTransposition(long hash, long trans, byte depth, short new_score, byte new_bound, short new_bestMove, int new_age, short new_static_eval);
}
