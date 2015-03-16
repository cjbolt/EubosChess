package eubos.pieces;

import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.BoardManager;
import eubos.board.Board;

public class King extends SinglesquareDirectMovePiece {

	public King( Colour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}
	
	public boolean isOnInitialSquare() {
		if (colour == Colour.white) {
			if ( onSquare == GenericPosition.e1 )
				return true;
		} else {
			if ( onSquare == GenericPosition.e8 )
				return true;			
		}
		return false;
	}
	
	@Override
	public LinkedList<GenericMove> generateMoves(BoardManager bm) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		Board theBoard = bm.getTheBoard();
		checkAddMove(moveList, theBoard, getUp());
		checkAddMove(moveList, theBoard, getUpRight());
		checkAddMove(moveList, theBoard, getRight());
		checkAddMove(moveList, theBoard, getDownRight());
		checkAddMove(moveList, theBoard, getDown());
		checkAddMove(moveList, theBoard, getDownLeft());
		checkAddMove(moveList, theBoard, getLeft());
		checkAddMove(moveList, theBoard, getUpLeft());
		return moveList;
	}

	private void checkAddMove(LinkedList<GenericMove> moveList, Board theBoard, GenericPosition targetSquare) {
		if ( targetSquare != null ) {
			Piece targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if ( theBoard.squareIsEmpty(targetSquare) || 
					(targetPiece != null && isOppositeColour(targetPiece))) {
				moveList.add( new GenericMove( onSquare, targetSquare ) );
			}
		}
	}

	@Override
	public boolean attacks(GenericPosition pos) {
		// TODO Auto-generated method stub
		return false;
	}
}
