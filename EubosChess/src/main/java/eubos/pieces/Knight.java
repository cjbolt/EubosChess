package eubos.pieces;

import java.util.ArrayList;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.*;

import eubos.board.Board;
import eubos.board.BoardManager;
import eubos.board.Direction;

public class Knight extends Piece {

	public Knight( Colour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}

	private void checkAddMove(LinkedList<GenericMove> moveList, Board theBoard, GenericPosition targetSquare) {
		if ( targetSquare != null ) {
			Piece targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if (theBoard.squareIsEmpty(targetSquare)) {
				moveList.add( new GenericMove( onSquare, targetSquare ));
			}
			else if (targetPiece != null && isOppositeColour(targetPiece)) {
				// Indicates a capture
				moveList.add( new GenericMove( onSquare, targetSquare ));
			}
			// Indicates blocked by own piece.
		}
	}
	
	private GenericPosition getSq( Direction dir ) {
		return Direction.getIndirectMoveSq(dir, onSquare);
	}
	
	@Override
	public LinkedList<GenericMove> generateMoves(BoardManager bm) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		Board theBoard = bm.getTheBoard();
		checkAddMove(moveList, theBoard, getSq(Direction.upRight));
		checkAddMove(moveList, theBoard, getSq(Direction.upLeft));
		checkAddMove(moveList, theBoard, getSq(Direction.rightUp));
		checkAddMove(moveList, theBoard, getSq(Direction.rightDown));
		checkAddMove(moveList, theBoard, getSq(Direction.downRight));
		checkAddMove(moveList, theBoard, getSq(Direction.downLeft));
		checkAddMove(moveList, theBoard, getSq(Direction.leftUp));
		checkAddMove(moveList, theBoard, getSq(Direction.leftDown));
		return moveList;		
	}

	@Override
	public boolean attacks(BoardManager bm, GenericPosition [] pos) {
		ArrayList<GenericPosition> targetSqs = new ArrayList<GenericPosition>();
		targetSqs.add(getSq(Direction.upRight));
		targetSqs.add(getSq(Direction.upLeft));
		targetSqs.add(getSq(Direction.rightUp));
		targetSqs.add(getSq(Direction.rightDown));
		targetSqs.add(getSq(Direction.downRight));
		targetSqs.add(getSq(Direction.downLeft));
		targetSqs.add(getSq(Direction.leftUp));
		targetSqs.add(getSq(Direction.leftDown));
		return (evaluateIfAttacks( pos, targetSqs ));
	}

}
