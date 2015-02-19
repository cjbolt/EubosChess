package eubos.pieces;

import java.util.LinkedList;

import com.fluxchess.jcpi.models.*;

import eubos.board.Board;

public class Knight extends IndirectMovePiece {

	public Knight( PieceColour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}
	
	@Override
	public LinkedList<GenericMove> generateMoveList(Board theBoard) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		return moveList;		
	}

}
