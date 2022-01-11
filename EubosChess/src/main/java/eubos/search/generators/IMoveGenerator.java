package eubos.search.generators;

import eubos.search.SearchMetricsReporter;
import eubos.search.SearchResult;

public interface IMoveGenerator {
	public SearchResult findMove(byte searchDepth);
	public SearchResult findMove(byte searchDepth, SearchMetricsReporter sr);
}
