package eubos.search;

import java.util.List;
import java.util.Random;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.PositionManager;
import eubos.board.InvalidPieceException;
import eubos.board.pieces.Piece;

class RandomMoveGenerator implements IMoveGenerator {
	
	private PositionManager pm;
	
	RandomMoveGenerator( PositionManager pm, Piece.Colour sideToMove ) {
		this.pm = pm;
	}

	// Find a random legal move for the colour "on move"
	public GenericMove findMove() throws NoLegalMoveException, InvalidPieceException {
		GenericMove bestMove = null;
		List<GenericMove> entireMoveList = pm.getMoveList();
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
