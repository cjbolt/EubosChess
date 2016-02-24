package eubos.search;

import java.util.List;
import java.util.Random;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.BoardManager;
import eubos.board.InvalidPieceException;
import eubos.board.pieces.Piece;

class RandomMoveGenerator implements IMoveGenerator {
	
	private BoardManager bm;
	
	RandomMoveGenerator( BoardManager bm, Piece.Colour sideToMove ) {
		this.bm = bm;
	}

	// Find a random legal move for the colour "on move"
	public GenericMove findMove() throws NoLegalMoveException, InvalidPieceException {
		GenericMove bestMove = null;
		List<GenericMove> entireMoveList = bm.getMoveList();
		if ( !entireMoveList.isEmpty()) {
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(entireMoveList.size());
			bestMove = entireMoveList.get(indexToGet);			
		} else {
			throw new NoLegalMoveException();
		}
		return bestMove;
	}
}
