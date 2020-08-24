package eubos.search.transposition;

import java.util.List;

import eubos.position.MoveList;

public interface ITransposition {

	MoveList getMoveList();

	byte getType();

	void setType(byte scoreType);

	short getScore();

	void setScore(short score);

	byte getDepthSearchedInPly();

	void setDepthSearchedInPly(byte depthSearchedInPly);

	int getBestMove();

	void setBestMove(int bestMove);

	List<Integer> getPv();

	void setPv(List<Integer> pv);

	String report();

	void update(byte new_Depth, short new_score, byte new_bound, MoveList new_ml, int new_bestMove,
			List<Integer> pv);
	
	short getAccessCount();
	
	void setAccessCount(short accessCount);
}