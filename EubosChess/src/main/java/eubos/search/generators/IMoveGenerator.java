package eubos.search.generators;

import java.util.List;

import eubos.board.InvalidPieceException;
import eubos.search.NoLegalMoveException;
import eubos.search.SearchResult;

public interface IMoveGenerator {
	public SearchResult findMove() throws NoLegalMoveException, InvalidPieceException;
	public SearchResult findMove(byte searchDepth) throws NoLegalMoveException, InvalidPieceException;
	public SearchResult findMove(byte searchDepth, List<Integer> lastPc) throws NoLegalMoveException, InvalidPieceException;
}
