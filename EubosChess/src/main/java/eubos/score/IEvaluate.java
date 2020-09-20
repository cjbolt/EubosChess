package eubos.score;

import eubos.search.SearchContext;

public interface IEvaluate {
	short evaluatePosition();
	boolean isQuiescent();
	SearchContext getSearchContext();
	boolean isInsufficientMaterial();
}