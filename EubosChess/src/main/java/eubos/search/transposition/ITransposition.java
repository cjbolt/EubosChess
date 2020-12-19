package eubos.search.transposition;

import java.util.List;

public interface ITransposition {

	byte getType();

	short getScore();

	byte getDepthSearchedInPly();

	int getBestMove();

	String report();

	void update(byte new_Depth, short new_score, byte new_bound, int new_bestMove, List<Integer> pv);
	
	void updateToExact(short new_score, int new_bestMove);
	
	short getAccessCount();
	
	void setAccessCount(short accessCount);

	List<Integer> getPv();
}