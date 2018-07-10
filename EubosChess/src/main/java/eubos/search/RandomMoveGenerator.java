package eubos.search;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.board.pieces.Piece;
import eubos.position.IGenerateMoveList;

class RandomMoveGenerator implements IMoveGenerator {
	
	private IGenerateMoveList mlgen;
	
	RandomMoveGenerator( IGenerateMoveList mlgen, Piece.Colour sideToMove ) {
		this.mlgen = mlgen;
	}

	@Override
	public GenericMove findMove() throws NoLegalMoveException, InvalidPieceException {
		return this.findMove(0);
	}
	
	@Override
	public GenericMove findMove(int searchDepth) throws NoLegalMoveException, InvalidPieceException {
		return this.findMove(searchDepth, null);
	}
	
	// Find a random legal move for the colour "on move"
	public GenericMove findMove(int searchDepth, LinkedList<GenericMove> lastPc) throws NoLegalMoveException, InvalidPieceException {
		GenericMove bestMove = null;
		List<GenericMove> entireMoveList = mlgen.getMoveList();
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
