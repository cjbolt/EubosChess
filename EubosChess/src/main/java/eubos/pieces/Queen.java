package eubos.pieces;

import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.BoardManager;

public class Queen extends MultisquareDirectMovePiece {
	
	public Queen( Colour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}
	
	@Override
	public LinkedList<GenericMove> generateMoveList(BoardManager bm) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		return moveList;	
	}

}
