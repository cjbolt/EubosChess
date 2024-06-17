package eubos.position;

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
	public int getPawnHash();
	public String getFen();
	public boolean isThreefoldRepetitionPossible();
	public boolean isInsufficientMaterial();
	public IEvaluate getPositionEvaluator();
	public String unwindMoveStack();
	public CastlingManager getCastling();
}
