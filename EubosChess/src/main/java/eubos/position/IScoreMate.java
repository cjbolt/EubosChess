package eubos.position;

import eubos.board.pieces.Piece.Colour;

public interface IScoreMate {
	int scoreMate(int currPly, boolean isWhite, Colour initialOnMove);
}
