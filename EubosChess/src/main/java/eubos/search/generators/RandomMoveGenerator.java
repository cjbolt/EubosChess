package eubos.search.generators;

import eubos.board.Piece;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.PositionManager;

import eubos.search.SearchMetricsReporter;
import eubos.search.SearchResult;

class RandomMoveGenerator implements IMoveGenerator {
	private MoveList ml;
	
	RandomMoveGenerator( PositionManager pm, Piece.Colour sideToMove)  {
		ml = new MoveList(pm, 0);
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0);	
	}

	public SearchResult findMove()  {
		return this.findMove((byte)0);
	}
	
	@Override
	// Find a random legal move for the colour "on move"
	public SearchResult findMove(byte searchDepth)  {
		return new SearchResult();
	}

	@Override
	public SearchResult findMove(byte searchDepth, SearchMetricsReporter sr) {
		return this.findMove(searchDepth);
	}
}
