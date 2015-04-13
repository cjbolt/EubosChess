package eubos.pieces;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.Direction;

public abstract class PieceMultisquareDirectMove extends Piece {

	protected ArrayList<GenericPosition> getAllSqs(Direction dir, Board theBoard) {
		ArrayList<GenericPosition> targetSquares = new ArrayList<GenericPosition>();
		GenericPosition currTargetSq = onSquare;
		while ((currTargetSq = Direction.getDirectMoveSq(dir, currTargetSq)) != null) {
			targetSquares.add(currTargetSq);
			if (sqConstrainsAttack(theBoard, currTargetSq)) break;
		}
		return targetSquares;
	}
	
	private boolean checkAddMove(LinkedList<GenericMove> moveList, Board theBoard, GenericPosition targetSquare) {
		boolean continueAddingMoves = false;
		if ( targetSquare != null ) {
			Piece targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if (theBoard.squareIsEmpty(targetSquare)) {
				moveList.add( new GenericMove( onSquare, targetSquare ));
				continueAddingMoves = true;
			}
			else if (targetPiece != null && isOppositeColour(targetPiece)) {
				// Indicates a capture
				moveList.add( new GenericMove( onSquare, targetSquare ));
			}
			// Indicates blocked by own piece.
		}
		return continueAddingMoves;
	}
		
	private boolean sqConstrainsAttack(Board theBoard, GenericPosition targetSquare) {
		boolean constrains = false;
		if ( targetSquare != null ) {
			Piece targetPiece = theBoard.getPieceAtSquare(targetSquare);
			if (targetPiece != null) {
				constrains = true;
			}
		}
		return constrains;
	}	

	protected void addMoves(LinkedList<GenericMove> moveList, Board theBoard, ArrayList<GenericPosition> targetSqs) {
		boolean continueAddingMoves = true;
		Iterator<GenericPosition> it = targetSqs.iterator();
		while ( it.hasNext() && continueAddingMoves ) {
			continueAddingMoves = checkAddMove(moveList, theBoard, it.next());
		}
	}
}
