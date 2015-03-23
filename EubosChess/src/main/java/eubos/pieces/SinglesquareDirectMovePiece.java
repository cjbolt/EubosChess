package eubos.pieces;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Direction;

public abstract class SinglesquareDirectMovePiece extends Piece {

	protected GenericPosition getOneSq( Direction dir ) {
		return Direction.getDirectMoveSq(dir, onSquare);
	}
}
