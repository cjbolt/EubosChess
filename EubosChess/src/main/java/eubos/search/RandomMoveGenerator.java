package eubos.search;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.board.pieces.Piece;
import eubos.position.IGenerateMoveList;
import eubos.position.MoveList;

class RandomMoveGenerator implements IMoveGenerator {
	
	private IGenerateMoveList pm;
	
	RandomMoveGenerator( IGenerateMoveList pm, Piece.Colour sideToMove ) {
		this.pm = pm;
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
		MoveList entireMoveList = pm.getMoveList();
		bestMove = entireMoveList.getRandomMove();
		if (bestMove == null) {
			throw new NoLegalMoveException();
		}
		return new SearchResult(bestMove, false);
	}
}
