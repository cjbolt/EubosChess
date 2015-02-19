package eubos.pieces;

import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;

public class King extends SinglesquareDirectMovePiece {

	public King( PieceColour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}
	
	@Override
	public LinkedList<GenericMove> generateMoveList(Board theBoard) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		return moveList;
	}

}
