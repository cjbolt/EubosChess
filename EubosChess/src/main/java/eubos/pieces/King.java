package eubos.pieces;

import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.Board;

public class King extends SinglesquareDirectMovePiece {

	public King( PieceColour Colour ) {
		colour = Colour;
	}
	
	@Override
	public LinkedList<GenericMove> generateMoveList(Board theBoard) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		return moveList;
	}

}
