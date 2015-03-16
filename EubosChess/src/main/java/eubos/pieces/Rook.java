package eubos.pieces;

import java.util.ArrayList;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.BoardManager;

public class Rook extends MultisquareDirectMovePiece {

	public Rook( Colour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}
	
	@Override
	public LinkedList<GenericMove> generateMoves(BoardManager bm) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		Board theBoard = bm.getTheBoard();
		addMoves(moveList, theBoard, getAllDown());
		addMoves(moveList, theBoard, getAllUp());
		addMoves(moveList, theBoard, getAllLeft());
		addMoves(moveList, theBoard, getAllRight());
		return moveList;	
	}

	@Override
	public boolean attacks(GenericPosition [] pos) {
		ArrayList<GenericPosition> targetSqs = getAllDown();
		targetSqs.addAll(getAllUp());
		targetSqs.addAll(getAllLeft());
		targetSqs.addAll(getAllRight());
		return (evaluateIfAttacks( pos, targetSqs ));
	}
}
