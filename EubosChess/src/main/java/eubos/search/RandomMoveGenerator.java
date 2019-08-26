package eubos.search;

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
	public SearchResult findMove() throws NoLegalMoveException, InvalidPieceException {
		return this.findMove((byte)0);
	}
	
	@Override
	public SearchResult findMove(byte searchDepth) throws NoLegalMoveException, InvalidPieceException {
		return this.findMove(searchDepth, null);
	}
	
	// Find a random legal move for the colour "on move"
	public SearchResult findMove(byte searchDepth, List<GenericMove> lastPc) throws NoLegalMoveException, InvalidPieceException {
		GenericMove bestMove = null;
		List<GenericMove> entireMoveList = mlgen.getMoveList();
		if ( !entireMoveList.isEmpty()) {
			Random randomIndex = new Random();
			Integer indexToGet = randomIndex.nextInt(entireMoveList.size());
			bestMove = entireMoveList.get(indexToGet);			
		} else {
			throw new NoLegalMoveException();
		}
		return new SearchResult(bestMove, false);
	}
}
