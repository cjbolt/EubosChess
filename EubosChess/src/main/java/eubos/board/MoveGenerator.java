package eubos.board;

import java.util.LinkedList;
import java.util.Random;
import java.util.Iterator;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.pieces.Piece;

public class MoveGenerator implements IMoveGenerator {
	
	private BoardManager bm;
	private Piece.Colour sideToMove;
	
	public MoveGenerator( BoardManager bm, Piece.Colour sideToMove ) {
		this.bm = bm;
		this.sideToMove = sideToMove;
	}

	// TODO: for now find a random legal move for the side indicated
	public GenericMove findBestMove() throws NoLegalMoveException {
		// Generate the entire move list
		GenericMove bestMove = null;
		LinkedList<GenericMove> entireMoveList = new LinkedList<GenericMove>();
		// For each piece of the side to move on the board...
		Iterator<Piece> iter = bm.getTheBoard().iterateColour(sideToMove);
		while ( iter.hasNext() ) {
			// ...append the piece's legal moves to the entire move list
			entireMoveList.addAll( iter.next().generateMoves( bm ));
		}
		if ( !entireMoveList.isEmpty()) {
			// once the move list has been generated, remove any moves that would place
			// the king in check from consideration.
			for ( GenericMove currMove : entireMoveList) {
				bm.performMove( currMove );
				if (inCheck()) {
					// it represents an illegal move, reject it.
					entireMoveList.remove( currMove );
				}
				bm.undoPreviousMove();
			}
			// For the time-being, return a valid move at random
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(entireMoveList.size());
			bestMove = entireMoveList.get(indexToGet);			
		} else {
			throw new NoLegalMoveException();
		}
		return bestMove;
	}
	
	private boolean inCheck() {
		// loop through all the opposite colour pieces and see if any of them are currently attacking the king.
		boolean inCheck = false;
		return inCheck;
	}
}
