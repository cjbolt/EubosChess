package eubos.search.transposition;

public class TranspositionEvaluation {
	
	public TranspositionEvaluation() {
		status = Status.insufficientNoData;
		trans = null;
	}
	
	public enum Status {
		insufficientNoData,
		sufficientTerminalNode,
		sufficientTerminalNodeInExtendedSearch,
		sufficientRefutation,
		sufficientSeedMoveList		
	};
	
	public Status status;
	public ITransposition trans;
}
