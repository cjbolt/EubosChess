package eubos.pieces;

import java.util.ArrayList;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.BoardManager;

public class Queen extends MultisquareDirectMovePiece {
	
	public Queen( Colour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}
	
	@Override
	public LinkedList<GenericMove> generateMoves(BoardManager bm) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		Board theBoard = bm.getTheBoard();
		addMoves(moveList, theBoard, getAllDownLeft(theBoard));
		addMoves(moveList, theBoard, getAllUpLeft(theBoard));
		addMoves(moveList, theBoard, getAllDownRight(theBoard));
		addMoves(moveList, theBoard, getAllUpRight(theBoard));
		addMoves(moveList, theBoard, getAllDown(theBoard));
		addMoves(moveList, theBoard, getAllUp(theBoard));
		addMoves(moveList, theBoard, getAllLeft(theBoard));
		addMoves(moveList, theBoard, getAllRight(theBoard));
		return moveList;	
	}

	@Override
	public boolean attacks(BoardManager bm, GenericPosition [] pos) {
		Board theBoard = bm.getTheBoard();
		ArrayList<GenericPosition> targetSqs = getAllDown(theBoard);
		targetSqs.addAll(getAllUp(theBoard));
		targetSqs.addAll(getAllLeft(theBoard));
		targetSqs.addAll(getAllRight(theBoard));
		targetSqs.addAll(getAllDownLeft(theBoard));
		targetSqs.addAll(getAllUpLeft(theBoard));
		targetSqs.addAll(getAllDownRight(theBoard));
		targetSqs.addAll(getAllUpRight(theBoard));
		return (evaluateIfAttacks( pos, targetSqs ));
	}
}
