package eubos.pieces;

import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;

import eubos.board.Board;

public class Pawn extends SinglesquareDirectMovePiece {

	public Pawn( PieceColour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}
	
	@Override
	public LinkedList<GenericMove> generateMoveList(Board theBoard) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		if (isBlack()) {
			// Start with just moving one square
			GenericPosition moveTo = GenericPosition.valueOf( onSquare.file, onSquare.rank.prev());
			if ( theBoard.isSquareEmpty( moveTo )) {
				moveList.add( new GenericMove( onSquare, moveTo ) );
				// try moving two squares on a first move
				if ( !everMoved && onSquare.rank.equals( GenericRank.R7 )) {
					moveTo = GenericPosition.valueOf( onSquare.file, onSquare.rank.prev().prev());
					if ( theBoard.isSquareEmpty( moveTo )) {
						moveList.add( new GenericMove( onSquare, moveTo ) );
					}
				}	
			}
		}
		return moveList;
	}

}
