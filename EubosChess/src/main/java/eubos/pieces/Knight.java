package eubos.pieces;

import java.util.LinkedList;

import com.fluxchess.jcpi.models.*;

import eubos.board.BoardManager;

public class Knight extends IndirectMovePiece {

	public Knight( Colour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}
	
	@Override
	public LinkedList<GenericMove> generateMoves(BoardManager bm) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		return moveList;		
	}

}
