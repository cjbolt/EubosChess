package eubos.search.generators;

import java.util.List;


import eubos.search.NoLegalMoveException;
import eubos.search.SearchMetricsReporter;
import eubos.search.SearchResult;

public interface IMoveGenerator {
	public SearchResult findMove(byte searchDepth) throws NoLegalMoveException;
	public SearchResult findMove(byte searchDepth, List<Integer> lastPc, SearchMetricsReporter sr) throws NoLegalMoveException;
}
