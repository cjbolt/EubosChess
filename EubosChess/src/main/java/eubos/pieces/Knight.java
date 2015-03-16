package eubos.pieces;

import java.util.ArrayList;
import java.util.LinkedList;

import com.fluxchess.jcpi.models.*;

import eubos.board.Board;
import eubos.board.BoardManager;

public class Knight extends IndirectMovePiece {

	public Knight( Colour Colour, GenericPosition at ) {
		colour = Colour;
		onSquare = at;
	}

	private GenericPosition getUpRight() {
		if ( onSquare.file != GenericFile.Fh && ((onSquare.rank != GenericRank.R8) && (onSquare.rank != GenericRank.R7))) {
			return GenericPosition.valueOf( onSquare.file.next(), onSquare.rank.next().next());	
		}
		return null;
	}
	
	private GenericPosition getUpLeft() {
		if ( onSquare.file != GenericFile.Fa && ((onSquare.rank != GenericRank.R8) && (onSquare.rank != GenericRank.R7))) {
			return GenericPosition.valueOf( onSquare.file.prev(), onSquare.rank.next().next());
		}
		return null;
	}
	
	private GenericPosition getDownRight() {
		if ( onSquare.file != GenericFile.Fh && ((onSquare.rank != GenericRank.R1) && (onSquare.rank != GenericRank.R2))) {
			return GenericPosition.valueOf( onSquare.file.next(), onSquare.rank.prev().prev());
		}
		return null;
	}
	
	private GenericPosition getDownLeft() {
		if ( onSquare.file != GenericFile.Fa && ((onSquare.rank != GenericRank.R1) && (onSquare.rank != GenericRank.R2))) {
			return GenericPosition.valueOf( onSquare.file.prev(), onSquare.rank.prev().prev());
		}
		return null;
	}

	private GenericPosition getRightUp() {
		if (((onSquare.file != GenericFile.Fg) && (onSquare.file != GenericFile.Fh)) && onSquare.rank != GenericRank.R8 ) {
			return GenericPosition.valueOf( onSquare.file.next().next(), onSquare.rank.next());
		}
		return null;
	}	
	
	private GenericPosition getRightDown() {
		if (((onSquare.file != GenericFile.Fg) && (onSquare.file != GenericFile.Fh)) && onSquare.rank != GenericRank.R1  ) {
			return GenericPosition.valueOf( onSquare.file.next().next(), onSquare.rank.prev());
		}
		return null;
	}	

	private GenericPosition getLeftUp() {
		if (((onSquare.file != GenericFile.Fa) && (onSquare.file != GenericFile.Fb)) && onSquare.rank != GenericRank.R8 )  {
			return GenericPosition.valueOf( onSquare.file.prev().prev(), onSquare.rank.next());
		}
		return null;
	}	
	
	private GenericPosition getLeftDown() {
		if (((onSquare.file != GenericFile.Fa) && (onSquare.file != GenericFile.Fb)) && onSquare.rank != GenericRank.R1 )  {
			return GenericPosition.valueOf( onSquare.file.prev().prev(), onSquare.rank.prev());
		}
		return null;
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
	
	@Override
	public LinkedList<GenericMove> generateMoves(BoardManager bm) {
		LinkedList<GenericMove> moveList = new LinkedList<GenericMove>();
		Board theBoard = bm.getTheBoard();
		checkAddMove(moveList, theBoard, getUpRight());
		checkAddMove(moveList, theBoard, getUpLeft());
		checkAddMove(moveList, theBoard, getRightUp());
		checkAddMove(moveList, theBoard, getRightDown());
		checkAddMove(moveList, theBoard, getDownRight());
		checkAddMove(moveList, theBoard, getDownLeft());
		checkAddMove(moveList, theBoard, getLeftUp());
		checkAddMove(moveList, theBoard, getLeftDown());
		return moveList;		
	}

	@Override
	public boolean attacks(GenericPosition [] pos) {
		ArrayList<GenericPosition> targetSqs = new ArrayList<GenericPosition>();
		targetSqs.add(getUpRight());
		targetSqs.add(getUpLeft());
		targetSqs.add(getRightUp());
		targetSqs.add(getRightDown());
		targetSqs.add(getDownRight());
		targetSqs.add(getDownLeft());
		targetSqs.add(getLeftUp());
		targetSqs.add(getLeftDown());
		return (evaluateIfAttacks( pos, targetSqs ));
	}

}
