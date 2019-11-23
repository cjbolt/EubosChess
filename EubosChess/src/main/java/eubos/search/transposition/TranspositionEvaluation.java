package eubos.search.transposition;

public class TranspositionEvaluation {
	
	public TranspositionEvaluation() {
		status = TranspositionTableStatus.insufficientNoData;
		trans = null;
	}
	
	public enum TranspositionTableStatus {
		insufficientNoData,
		sufficientTerminalNode,
		sufficientTerminalNodeInExtendedSearch,
		sufficientRefutation,
		sufficientSeedMoveList		
	};
	
	public TranspositionTableStatus status;
	public Transposition trans;
}
