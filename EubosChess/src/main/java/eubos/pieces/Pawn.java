package eubos.pieces;

import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;

import eubos.board.Board;

public class Pawn extends SinglesquareDirectMovePiece {

	public Pawn( PieceColour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}
	
	private boolean isBlackPawnNeverMoved() {
		return (!everMoved && onSquare.rank.equals( GenericRank.R7 ));
	}

	private GenericPosition genOneSqTarget() {
		GenericPosition toPos = GenericPosition.valueOf( onSquare.file, onSquare.rank.prev());
		return toPos;
	}	
	
	private GenericPosition genTwoSqTarget() {
		GenericPosition toPos = GenericPosition.valueOf( onSquare.file, onSquare.rank.prev().prev());
		return toPos;
	}
	
	private GenericPosition genLeftCaptureTarget() {
		GenericPosition toPos = null;
		if ( onSquare.file != GenericFile.Fh ) {
			toPos = GenericPosition.valueOf( onSquare.file.next(), onSquare.rank.prev());
		}
		return toPos;		
	}
	
	private GenericPosition genRightCaptureTarget() {
		GenericPosition toPos = null;
		if ( onSquare.file != GenericFile.Fa ) {
			toPos = GenericPosition.valueOf( onSquare.file.prev(), onSquare.rank.prev());
		}
		return toPos;		
	}
	
	@Override
	public LinkedList<GenericMove> generateMoveList(Board theBoard) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		if ( isBlack() ) {
			// Check for one and two square moves
			GenericPosition moveTo = genOneSqTarget();
			if ( theBoard.isSquareEmpty( moveTo )) {
				moveList.add( new GenericMove( onSquare, moveTo ) );
				if ( isBlackPawnNeverMoved() ) {
					moveTo = genTwoSqTarget();
					if ( theBoard.isSquareEmpty( moveTo )) {
						moveList.add( new GenericMove( onSquare, moveTo ) );
					}
				}	
			}
			// Check for capture moves
			GenericPosition captureAt = genLeftCaptureTarget();
			if ( captureAt != null && theBoard.isSquareWhitePiece( captureAt )) {
				moveList.add( new GenericMove( onSquare, captureAt ) );
			}
			captureAt = genRightCaptureTarget();
			if ( captureAt != null && theBoard.isSquareWhitePiece( captureAt )) {
				moveList.add( new GenericMove( onSquare, captureAt ) );
			}
			// TODO Check for en passant capture moves
			// TODO Check for promotion moves
		}
		return moveList;
	}

}
