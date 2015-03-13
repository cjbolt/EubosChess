package eubos.pieces;

import java.util.ArrayList;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.BoardManager;

public class Bishop extends MultisquareDirectMovePiece {

	public Bishop( Colour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}
	
	@Override
	public LinkedList<GenericMove> generateMoves(BoardManager bm) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		Board theBoard = bm.getTheBoard();
		addMoves(moveList, theBoard, getAllDownLeft());
		addMoves(moveList, theBoard, getAllUpLeft());
		addMoves(moveList, theBoard, getAllDownRight());
		addMoves(moveList, theBoard, getAllUpRight());
		return moveList;
	}

	@Override
	public boolean attacks(GenericPosition pos) {
		ArrayList<GenericPosition> targetSqs = getAllDownLeft();
		targetSqs.addAll(getAllUpLeft());
		targetSqs.addAll(getAllDownRight());
		targetSqs.addAll(getAllUpRight());
		if (targetSqs.contains(pos))
			return true;
		else
			return false;
	}
}
