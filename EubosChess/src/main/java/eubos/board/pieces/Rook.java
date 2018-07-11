package eubos.board.pieces;

import java.util.LinkedList;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.Direction;

public class Rook extends PieceMultisquareDirectMove {
	
	public static final int MATERIAL_VALUE = 500;

	public Rook( Colour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}
	
	@Override
	public List<GenericMove> generateMoves(Board theBoard) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		addMoves(moveList, theBoard, getAllSqs(Direction.down, theBoard));
		addMoves(moveList, theBoard, getAllSqs(Direction.up, theBoard));
		addMoves(moveList, theBoard, getAllSqs(Direction.left, theBoard));
		addMoves(moveList, theBoard, getAllSqs(Direction.right, theBoard));
		return moveList;	
	}
}
