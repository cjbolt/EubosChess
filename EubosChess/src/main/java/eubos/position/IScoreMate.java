package eubos.position;

import eubos.board.pieces.Piece.Colour;

public interface IScoreMate {
	short scoreMate(byte currPly, boolean isWhite, Colour initialOnMove);
}
