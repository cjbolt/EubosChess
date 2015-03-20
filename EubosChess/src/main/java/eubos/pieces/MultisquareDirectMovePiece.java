package eubos.pieces;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericFile;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericRank;

import eubos.board.Board;

public abstract class MultisquareDirectMovePiece extends Piece {

	protected ArrayList<GenericPosition> getAllDownLeft() {
		ArrayList<GenericPosition> targetSquares = new ArrayList<GenericPosition>();
		GenericPosition currTargetSq = onSquare;
		while ( currTargetSq.file != GenericFile.Fa && currTargetSq.rank != GenericRank.R1 ) {
			currTargetSq = GenericPosition.valueOf( currTargetSq.file.prev(), currTargetSq.rank.prev());
			targetSquares.add(currTargetSq);
		}
		return targetSquares;
	}

	protected ArrayList<GenericPosition> getAllUpRight() {
		ArrayList<GenericPosition> targetSquares = new ArrayList<GenericPosition>();
		GenericPosition currTargetSq = onSquare;
		while ( currTargetSq.file != GenericFile.Fh && currTargetSq.rank != GenericRank.R8 ) {
			currTargetSq = GenericPosition.valueOf( currTargetSq.file.next(), currTargetSq.rank.next());	
			targetSquares.add(currTargetSq);
		}
		return targetSquares;
	}
	
	protected ArrayList<GenericPosition> getAllUpLeft() {
		ArrayList<GenericPosition> targetSquares = new ArrayList<GenericPosition>();
		GenericPosition currTargetSq = onSquare;
		while ( currTargetSq.file != GenericFile.Fa && currTargetSq.rank != GenericRank.R8 ) {
			currTargetSq = GenericPosition.valueOf( currTargetSq.file.prev(), currTargetSq.rank.next());
			targetSquares.add(currTargetSq);
		}
		return targetSquares;
	}
	
	protected ArrayList<GenericPosition> getAllDownRight() {
		ArrayList<GenericPosition> targetSquares = new ArrayList<GenericPosition>();
		GenericPosition currTargetSq = onSquare;
		while ( currTargetSq.file != GenericFile.Fh && currTargetSq.rank != GenericRank.R1 ) {
			currTargetSq = GenericPosition.valueOf( currTargetSq.file.next(), currTargetSq.rank.prev());
			targetSquares.add(currTargetSq);
		}
		return targetSquares;
	}

	protected ArrayList<GenericPosition> getAllDown() {
		ArrayList<GenericPosition> targetSquares = new ArrayList<GenericPosition>();
		GenericPosition currTargetSq = onSquare;
		while ( currTargetSq.rank != GenericRank.R1 ) {
			currTargetSq = GenericPosition.valueOf( currTargetSq.file, currTargetSq.rank.prev());
			targetSquares.add(currTargetSq);
		}
		return targetSquares;
	}	
	
	protected ArrayList<GenericPosition> getAllUp() {
		ArrayList<GenericPosition> targetSquares = new ArrayList<GenericPosition>();
		GenericPosition currTargetSq = onSquare;
		while ( currTargetSq.rank != GenericRank.R8 ) {
			currTargetSq = GenericPosition.valueOf( currTargetSq.file, currTargetSq.rank.next());
			targetSquares.add(currTargetSq);
		}
		return targetSquares;
	}	

	protected ArrayList<GenericPosition> getAllLeft() {
		ArrayList<GenericPosition> targetSquares = new ArrayList<GenericPosition>();
		GenericPosition currTargetSq = onSquare;
		while ( currTargetSq.file != GenericFile.Fa ) {
			currTargetSq = GenericPosition.valueOf( currTargetSq.file.prev(), currTargetSq.rank);
			targetSquares.add(currTargetSq);
		}
		return targetSquares;
	}	
	
	protected ArrayList<GenericPosition> getAllRight() {
		ArrayList<GenericPosition> targetSquares = new ArrayList<GenericPosition>();
		GenericPosition currTargetSq = onSquare;
		while ( currTargetSq.file != GenericFile.Fh ) {
			currTargetSq = GenericPosition.valueOf( currTargetSq.file.next(), currTargetSq.rank);
			targetSquares.add(currTargetSq);
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

	protected void addMoves(LinkedList<GenericMove> moveList, Board theBoard, ArrayList<GenericPosition> targetSqs) {
		boolean continueAddingMoves = true;
		Iterator<GenericPosition> it = targetSqs.iterator();
		while ( it.hasNext() && continueAddingMoves ) {
			continueAddingMoves = checkAddMove(moveList, theBoard, it.next());
		}
	}
}
