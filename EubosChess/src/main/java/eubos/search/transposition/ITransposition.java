package eubos.search.transposition;

import java.util.List;

import eubos.position.MoveList;
import eubos.search.Score.ScoreType;

public interface ITransposition {

	MoveList getMoveList();

	ScoreType getScoreType();

	void setScoreType(ScoreType scoreType);

	short getScore();

	void setScore(short score);

	byte getDepthSearchedInPly();

	void setDepthSearchedInPly(byte depthSearchedInPly);

	int getBestMove();

	void setBestMove(int bestMove);

	List<Integer> getPv();

	void setPv(List<Integer> pv);

	String report();

	void update(byte new_Depth, short new_score, ScoreType new_bound, MoveList new_ml, int new_bestMove,
			List<Integer> pv);

}