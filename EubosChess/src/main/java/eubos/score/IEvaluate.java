package eubos.score;

public interface IEvaluate {
	short evaluatePosition();
	boolean isQuiescent();
	boolean isThreeFoldRepetition(Long hashCode);
}