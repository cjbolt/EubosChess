package eubos.search.transposition;

public interface ITranspositionAccessor {
	
	long getTransposition(long hash);
	long setTransposition(long hash, long trans, byte depth, char new_score, byte new_bound, char new_bestMove, char new_age, char new_static_eval);
}
