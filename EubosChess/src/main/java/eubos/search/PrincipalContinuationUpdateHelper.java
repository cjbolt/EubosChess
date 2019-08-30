package eubos.search;

import eubos.board.pieces.King;
import eubos.board.pieces.Piece.Colour;

public class PrincipalContinuationUpdateHelper
	{
		public static final int PLIES_PER_MOVE = 2;
		short positionScore;
		
		private SearchMetrics sm;
		private SearchMetricsReporter sr;
		PrincipalContinuation pc;
		Colour initialOnMove;
		byte searchDepthPly;
		
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

		void report(short score, byte depth) {
			positionScore = score;
			this.searchDepthPly = depth;
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
			return Math.abs(positionScore) >= King.MATERIAL_VALUE;
		}
		
		private int calculatePlyMateOccurredOn() {
			int matePly = Math.abs(positionScore)/King.MATERIAL_VALUE;
			matePly *= PLIES_PER_MOVE;
			matePly = searchDepthPly - matePly;
			if (isOwnMate()) {
				if ((searchDepthPly&1) != 0x1)
					matePly += 1;
			} else {
				if ((searchDepthPly&1) == 0x1)
					matePly -= 1;	
			}
			return (matePly > 0) ? matePly : 0;
		}
		
		private boolean isOwnMate() {
			return ((initialOnMove==Colour.white && positionScore<0) ||
			        (initialOnMove==Colour.black && positionScore>0));
		}
}
