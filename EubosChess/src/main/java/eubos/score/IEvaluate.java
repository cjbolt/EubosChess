package eubos.score;

import eubos.search.SearchContext;

public interface IEvaluate {
	short evaluatePosition();
	boolean isQuiescent();
	boolean couldLeadToThreeFoldRepetiton(Long hashCode);
	SearchContext getSearchContext();
	boolean isInsufficientMaterial();
}