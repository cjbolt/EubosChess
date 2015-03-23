package eubos.pieces;

import java.util.ArrayList;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.BoardManager;
import eubos.board.Direction;

public class Rook extends MultisquareDirectMovePiece {

	public Rook( Colour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}
	
	@Override
	public LinkedList<GenericMove> generateMoves(BoardManager bm) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		Board theBoard = bm.getTheBoard();
		addMoves(moveList, theBoard, getAll(Direction.down, theBoard));
		addMoves(moveList, theBoard, getAll(Direction.up, theBoard));
		addMoves(moveList, theBoard, getAll(Direction.left, theBoard));
		addMoves(moveList, theBoard, getAll(Direction.right, theBoard));
		return moveList;	
	}

	@Override
	public boolean attacks(BoardManager bm, GenericPosition [] pos) {
		Board theBoard = bm.getTheBoard();
		ArrayList<GenericPosition> targetSqs = getAll(Direction.down, theBoard);
		targetSqs.addAll(getAll(Direction.up, theBoard));
		targetSqs.addAll(getAll(Direction.left, theBoard));
		targetSqs.addAll(getAll(Direction.right, theBoard));
		return (evaluateIfAttacks( pos, targetSqs ));
	}
}
