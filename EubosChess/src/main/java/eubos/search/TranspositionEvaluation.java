package eubos.search;

public class TranspositionEvaluation {
	
	public TranspositionEvaluation() {
		status = TranspositionTableStatus.insufficientNoData;
		trans = null;
	}
	
	public enum TranspositionTableStatus {
		insufficientNoData,
		sufficientTerminalNodeAlpha,
		sufficientTerminalNodeBeta,
		sufficientRefutation,
		sufficientSeedMoveList		
	};
	
	public TranspositionTableStatus status;
	public Transposition trans;
}
