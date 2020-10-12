package eubos.search.transposition;

import java.util.List;

public interface ITransposition {

	byte getType();

	void setType(byte scoreType);

	short getScore();

	void setScore(short score);

	byte getDepthSearchedInPly();

	void setDepthSearchedInPly(byte depthSearchedInPly);

	int getBestMove();

	void setBestMove(int bestMove);

	String report();

	void update(byte new_Depth, short new_score, byte new_bound, int new_bestMove,
			List<Integer> pv);
	
	short getAccessCount();
	
	void setAccessCount(short accessCount);

	List<Integer> getPv();
}