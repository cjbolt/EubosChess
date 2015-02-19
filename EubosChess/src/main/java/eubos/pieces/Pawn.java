package eubos.pieces;

import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;

public class Pawn extends SinglesquareDirectMovePiece {

	public Pawn( PieceColour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}
	
	@Override
	public LinkedList<GenericMove> generateMoveList(Board theBoard) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		// Start with just moving one square
		if (isBlack()){
			moveList.add( new GenericMove( onSquare, GenericPosition.valueOf( onSquare.file, onSquare.rank.prev())));
		}
		return moveList;
	}

}
