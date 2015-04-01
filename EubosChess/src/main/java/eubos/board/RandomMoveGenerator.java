package eubos.board;

import java.util.LinkedList;
import java.util.Random;
import java.util.Iterator;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.pieces.Piece;

public class RandomMoveGenerator extends MoveGenerator implements IMoveGenerator {
	
	private Piece.Colour onMove;
	
	public RandomMoveGenerator( BoardManager bm, Piece.Colour sideToMove ) {
		super( bm );
		onMove = sideToMove;
	}

	// Find a random legal move for the colour "on move"
	public GenericMove findMove() throws NoLegalMoveException {
		GenericMove bestMove = null;
		LinkedList<GenericMove> entireMoveList = new LinkedList<GenericMove>();
		// For each piece of the "on Move" colour, add it's legal moves to the entire move list
		Iterator<Piece> iter_p = bm.getTheBoard().iterateColour(onMove);
		while ( iter_p.hasNext() ) {
			entireMoveList.addAll( iter_p.next().generateMoves( bm ));
		}
		addCastlingMoves(entireMoveList);
		// Scratch any moves resulting in the king being in check
		Iterator<GenericMove> iter_ml = entireMoveList.iterator();
		while ( iter_ml.hasNext() ) {
			GenericMove currMove = iter_ml.next();
			bm.performMove( currMove );
			if (inCheck()) {
				iter_ml.remove();
			}
			bm.undoPreviousMove();
		}
		if ( !entireMoveList.isEmpty()) {
			// For the time-being, return a random valid move, not the best move.
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(entireMoveList.size());
			bestMove = entireMoveList.get(indexToGet);			
		} else {
			throw new NoLegalMoveException();
		}
		return bestMove;
	}
}
