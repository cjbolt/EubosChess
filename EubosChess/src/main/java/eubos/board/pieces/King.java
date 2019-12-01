package eubos.board.pieces;

import java.util.LinkedList;
import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.Direction;

public class King extends PieceSinglesquareDirectMove {
	
	public static final short MATERIAL_VALUE = 4000;

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
	public List<GenericMove> generateMoves(Board theBoard) {
		List<GenericMove> moveList = new LinkedList<GenericMove>();
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

	private void checkAddMove(List<GenericMove> moveList, Board theBoard, GenericPosition targetSquare) {
		if ( targetSquare != null ) {
			PieceType targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if ( theBoard.squareIsEmpty(targetSquare) || 
					(targetPiece != PieceType.NONE && isOppositeColour(targetPiece))) {
				moveList.add( new GenericMove( onSquare, targetSquare ) );
			}
		}
	}
}
