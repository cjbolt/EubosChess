package eubos.pieces;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
	
	public boolean attacked(BoardManager bm) {
		boolean attacked = false;
		Board bd = bm.getTheBoard();
		// do/while loop is to allow the function to return attacked=true at earliest possibility
		do {
			// Check for pawn attacks
			attacked = attackedByPawn(bd, getOneSq(Direction.upRight));
			if (attacked) break;
			attacked = attackedByPawn(bd, getOneSq(Direction.upLeft));
			if (attacked) break;
			attacked = attackedByPawn(bd, getOneSq(Direction.downRight));
			if (attacked) break;
			attacked = attackedByPawn(bd, getOneSq(Direction.downLeft));
			if (attacked) break;
			// Check for king presence (to avoid moving into check by the enemy king)
			attacked = checkForKingPresence(bd);
			if (attacked) break;
			// Check for knight attacks
			attacked = checkForKnightAttacks(bd);
			if (attacked) break;
			// check for diagonal attacks
			attacked = checkForAttackerOnDiagonal(bd, getAllSqs(Direction.downLeft, bd));
			if (attacked) break;
			attacked = checkForAttackerOnDiagonal(bd, getAllSqs(Direction.upLeft, bd));
			if (attacked) break;
			attacked = checkForAttackerOnDiagonal(bd, getAllSqs(Direction.downRight, bd));
			if (attacked) break;
			attacked = checkForAttackerOnDiagonal(bd, getAllSqs(Direction.upRight, bd));
			if (attacked) break;
			// check for rank or file attacks
			attacked = checkForAttackerOnRankFile(bd, getAllSqs(Direction.down, bd));
			if (attacked) break;
			attacked = checkForAttackerOnRankFile(bd, getAllSqs(Direction.up, bd));
			if (attacked) break;
			attacked = checkForAttackerOnRankFile(bd, getAllSqs(Direction.left, bd));
			if (attacked) break;
			attacked = checkForAttackerOnRankFile(bd, getAllSqs(Direction.right, bd));
		} while (false);
		return attacked;
	}

	private boolean checkForKnightAttacks(Board bd) {
		boolean attacked = false;
		GenericPosition atPos;
		Piece currPiece;
		for (Direction dir: Direction.values()) {
			atPos = Direction.getIndirectMoveSq(dir, onSquare);
			if (atPos != null) {
				currPiece = bd.getPieceAtSquare(atPos);
				if ( currPiece != null && currPiece instanceof Knight && isOppositeColour(currPiece)) {
					attacked = true;
					break;
				}
			}
		}
		return attacked;
	}
	
	private boolean checkForKingPresence(Board bd) {
		boolean attacked = false;
		GenericPosition atPos;
		Piece currPiece;
		for (Direction dir: Direction.values()) {
			atPos = Direction.getDirectMoveSq(dir, onSquare);
			if (atPos != null) {
				currPiece = bd.getPieceAtSquare(atPos);
				if ( currPiece != null && currPiece instanceof King && isOppositeColour(currPiece)) {
					attacked = true;
					break;
				}
			}
		}
		return attacked;
	}	

	private List<GenericPosition> getAllSqs(Direction dir, Board theBoard) {
		ArrayList<GenericPosition> targetSquares = new ArrayList<GenericPosition>();
		GenericPosition currTargetSq = onSquare;
		while ((currTargetSq = Direction.getDirectMoveSq(dir, currTargetSq)) != null) {
			targetSquares.add(currTargetSq);
		}
		return targetSquares;
	}
	
	private boolean checkForAttackerOnDiagonal(Board theBoard, List<GenericPosition> targetSqs) {
		boolean attacked = false;
		for (GenericPosition attackerSq: targetSqs) {
			Piece currPiece = theBoard.getPieceAtSquare(attackerSq);
			if (currPiece != null ) {
				if (((currPiece instanceof Bishop) || (currPiece instanceof Queen)) && isOppositeColour(currPiece)) {
					// Indicates attacked
					attacked = true;
				} // else blocked by own piece or non-attacking enemy
				break;
			}
		}
		return attacked;
	}
	
	private boolean checkForAttackerOnRankFile(Board theBoard, List<GenericPosition> targetSqs) {
		boolean attacked = false;
		for (GenericPosition attackerSq: targetSqs) {
			Piece currPiece = theBoard.getPieceAtSquare(attackerSq);
			if (currPiece != null ) {
				if (((currPiece instanceof Rook) || (currPiece instanceof Queen)) && isOppositeColour(currPiece)) {
					// Indicates attacked
					attacked = true;
				} // else blocked by own piece or non-attacking enemy
				break;
			}
		}
		return attacked;
	}	
	
	private boolean attackedByPawn(Board bd, GenericPosition atPos) {
		Piece currPiece;
		boolean attacked = false;
		if (atPos != null) {
			currPiece = bd.getPieceAtSquare(atPos);
			if ( currPiece != null && currPiece instanceof Pawn && isOppositeColour(currPiece)) {
				attacked = true;
			}
		}
		return attacked;
	}
}
