package eubos.search;

import eubos.board.pieces.Piece.Colour;
import eubos.position.MaterialEvaluator;

public class PrincipalContinuationUpdateHelper
	{
		public static final int PLIES_PER_MOVE = 2;
		short positionScore;
		
		private SearchMetrics sm;
		private SearchMetricsReporter sr;
		PrincipalContinuation pc;
		Colour initialOnMove;
		
		PrincipalContinuationUpdateHelper(
				Colour initialOnMove,
				PrincipalContinuation pc,
				SearchMetrics sm,
				SearchMetricsReporter sr) {
			this.pc = pc;
			this.sm = sm;
			this.sr = sr;
			this.initialOnMove = initialOnMove;
		}

		void report(short score, byte partialDepth) {
			positionScore = score;
			sm.setPartialDepth(partialDepth);
			assignPrincipalVariationToSearchMetrics();
			assignCentipawnScoreToSearchMetrics();
			sr.reportPrincipalVariation();
		}	
		
		private void assignPrincipalVariationToSearchMetrics() {
			truncatePrincipalContinuation();
			sm.setPrincipalVariation(pc.toPvList());
		}	

		private void assignCentipawnScoreToSearchMetrics() {
			if (initialOnMove.equals(Colour.black))
				positionScore = (short) -positionScore; // Negated due to UCI spec (from engine pov)
			sm.setCpScore(positionScore);
		}
		
		private void truncatePrincipalContinuation() {
			if (isScoreIndicatesMate()) {
				// If the positionScore indicates a mate, truncate the pc accordingly
				int matePly = calculatePlyMateOccurredOn();
				pc.truncateAfterPly(matePly);
			}
		}
		
		private boolean isScoreIndicatesMate() {
			return Math.abs(positionScore) >= MaterialEvaluator.MATERIAL_VALUE_KING;
		}
		
		private int calculatePlyMateOccurredOn() {
			int mateMove = 0;
			int matePly = 0;
			if (isOwnMate()) {
				if (positionScore > 0) {
					mateMove = Short.MIN_VALUE + positionScore;
				} else {
					mateMove = Short.MAX_VALUE - positionScore;
				}
				//matePly = mateMove * PLIES_PER_MOVE;
				matePly = Math.abs(mateMove);
			} else {
				if (positionScore > 0) {
					mateMove = Short.MAX_VALUE - positionScore;
				} else {
					mateMove = Short.MIN_VALUE + positionScore;
				}
				matePly =((mateMove-1)* PLIES_PER_MOVE)+1;
			}
			return (matePly > 0) ? matePly : 0;
		}
		
		private boolean isOwnMate() {
			return ((initialOnMove==Colour.white && positionScore<0) ||
			        (initialOnMove==Colour.black && positionScore>0));
		}
}
