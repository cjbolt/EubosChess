package eubos.board.pieces;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.position.Direction;

public abstract class PieceSinglesquareDirectMove extends Piece {

	protected GenericPosition getOneSq( Direction dir ) {
		return Direction.getDirectMoveSq(dir, onSquare);
	}
}
