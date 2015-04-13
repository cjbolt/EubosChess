package eubos.pieces;

import java.util.ArrayList;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.BoardManager;
import eubos.board.Board;
import eubos.board.Direction;

public class King extends PieceSinglesquareDirectMove {

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
		checkAddMove(moveList, theBoard, getOneSq(Direction.up));
		checkAddMove(moveList, theBoard, getOneSq(Direction.upRight));
		checkAddMove(moveList, theBoard, getOneSq(Direction.right));
		checkAddMove(moveList, theBoard, getOneSq(Direction.downRight));
		checkAddMove(moveList, theBoard, getOneSq(Direction.down));
		checkAddMove(moveList, theBoard, getOneSq(Direction.downLeft));
		checkAddMove(moveList, theBoard, getOneSq(Direction.left));
		checkAddMove(moveList, theBoard, getOneSq(Direction.upLeft));
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
	public boolean attacks(BoardManager bm, GenericPosition [] pos) {
		ArrayList<GenericPosition> targetSqs = new ArrayList<GenericPosition>();
		targetSqs.add(getOneSq(Direction.up));
		targetSqs.add(getOneSq(Direction.upRight));
		targetSqs.add(getOneSq(Direction.right));
		targetSqs.add(getOneSq(Direction.downRight));
		targetSqs.add(getOneSq(Direction.down));
		targetSqs.add(getOneSq(Direction.downLeft));
		targetSqs.add(getOneSq(Direction.left));
		targetSqs.add(getOneSq(Direction.upLeft));
		return (evaluateIfAttacks( pos, targetSqs ));
	}
}
