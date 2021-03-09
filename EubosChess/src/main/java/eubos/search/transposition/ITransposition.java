package eubos.search.transposition;

import java.util.List;

public interface ITransposition {

	byte getType();

	short getScore();

	byte getDepthSearchedInPly();

	int getBestMove();

	String report();

	boolean checkUpdate(byte new_Depth, short new_score, byte new_bound, int new_bestMove, List<Integer> pv);
	
	boolean checkUpdateToExact(byte depth);
	
	short getAccessCount();
	
	void setAccessCount(short accessCount);

	List<Integer> getPv();
	
	boolean checkHash(int hashCode);
}