package eubos.pieces;

import java.util.ArrayList;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.BoardManager;
import eubos.board.Direction;

public class Queen extends MultisquareDirectMovePiece {
	
	public Queen( Colour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}
	
	@Override
	public LinkedList<GenericMove> generateMoves(BoardManager bm) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		Board theBoard = bm.getTheBoard();
		addMoves(moveList, theBoard, getAll(Direction.downLeft, theBoard));
		addMoves(moveList, theBoard, getAll(Direction.upLeft, theBoard));
		addMoves(moveList, theBoard, getAll(Direction.downRight, theBoard));
		addMoves(moveList, theBoard, getAll(Direction.upRight, theBoard));
		addMoves(moveList, theBoard, getAll(Direction.down, theBoard));
		addMoves(moveList, theBoard, getAll(Direction.up, theBoard));
		addMoves(moveList, theBoard, getAll(Direction.left, theBoard));
		addMoves(moveList, theBoard, getAll(Direction.right, theBoard));
		return moveList;	
	}

	@Override
	public boolean attacks(BoardManager bm, GenericPosition [] pos) {
		Board theBoard = bm.getTheBoard();
		ArrayList<GenericPosition> targetSqs = getAll(Direction.downLeft, theBoard);
		targetSqs.addAll(getAll(Direction.upLeft, theBoard));
		targetSqs.addAll(getAll(Direction.downRight, theBoard));
		targetSqs.addAll(getAll(Direction.upRight, theBoard));
		targetSqs.addAll(getAll(Direction.down, theBoard));
		targetSqs.addAll(getAll(Direction.up, theBoard));
		targetSqs.addAll(getAll(Direction.left, theBoard));
		targetSqs.addAll(getAll(Direction.right, theBoard));
		return (evaluateIfAttacks( pos, targetSqs ));
	}
}
