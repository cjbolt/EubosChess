package eubos.position;

import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.Board;
import eubos.board.Piece.Colour;
import eubos.score.IEvaluate;

public interface IPositionAccessors {
	public Board getTheBoard();
	public Colour getOnMove();
	public boolean onMoveIsWhite();
	public boolean isKingInCheck();
	public int getMoveNumber();
	public int getPlyNumber();
	public long getHash();
	public String getFen();
	public boolean isThreefoldRepetitionPossible();
	public IEvaluate getPositionEvaluator();
	public boolean isQuiescent();
	public boolean moveLeadsToThreefold(int move);
	public String unwindMoveStack();
	public int enemyAdvancedPassedPawn();
	public boolean promotablePawnPresent();
	public int getNativeMove(String bestMoveSAN) throws IllegalNotationException;
}
