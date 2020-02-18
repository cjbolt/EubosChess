package eubos.search.generators;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.board.Piece;
import eubos.position.MoveList;
import eubos.position.PositionManager;
import eubos.search.NoLegalMoveException;
import eubos.search.SearchResult;

class RandomMoveGenerator implements IMoveGenerator {
	private MoveList ml;
	
	RandomMoveGenerator( PositionManager pm, Piece.Colour sideToMove) {
		ml = new MoveList(pm);
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
	public SearchResult findMove(byte searchDepth, List<Integer> lastPc) throws NoLegalMoveException, InvalidPieceException {
		GenericMove bestMove = ml.getRandomMove();
		if (bestMove == null) {
			throw new NoLegalMoveException();
		}
		return new SearchResult(bestMove, false);
	}
}
