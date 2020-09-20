package eubos.position;

import eubos.board.Board;
import eubos.board.Piece.Colour;

public interface IPositionAccessors {
	public Board getTheBoard();
	public Colour getOnMove();
	public boolean onMoveIsWhite();
	public boolean hasCastled(Colour colour);
	public boolean isKingInCheck();
	public int getMoveNumber();
	public boolean lastMoveWasCapture();
	public boolean lastMoveWasCheck();
	public int getCastlingFlags();
	public long getHash();
	public String getFen();
	public boolean isThreefoldRepetitionPossible();
}
