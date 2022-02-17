package eubos.search.transposition;

import java.util.List;

import eubos.board.Board;

public interface ITransposition {

	byte getType();

	short getScore();

	byte getDepthSearchedInPly();

	int getBestMove(Board theBoard);
	
	int getBestMove();

	String report();

	boolean checkUpdate(byte new_Depth, short new_score, byte new_bound, int new_bestMove, List<Integer> pv);

	List<Integer> getPv();
}