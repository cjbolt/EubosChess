package eubos.search.transposition;

import eubos.board.Board;

public interface ITranspositionAccessor {
	
	long getTransposition(long hash, Board theBoard, boolean isInCheck, boolean onMoveIsWhite);
	long setTransposition(long hash, long trans, byte depth, short new_score, byte new_bound, int new_bestMove, int new_age, short new_static_eval);
}
