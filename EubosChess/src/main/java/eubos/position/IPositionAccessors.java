package eubos.position;

import eubos.board.Board;
import eubos.board.pieces.Piece.Colour;

public interface IPositionAccessors {
	public Board getTheBoard();
	public Colour getOnMove();
	public boolean hasCastled(Colour colour);
	public boolean isKingInCheck();
	public int getMoveNumber();
}
