package eubos.score;

import eubos.board.Board;
import eubos.neural_net.NNUE;
import eubos.position.IPositionAccessors;
import eubos.position.PositionManager;

public class PositionEvaluator implements IEvaluate {
	
	private IPositionAccessors pm;
	private boolean goForMate;
	private boolean isDraw;
	private int score;
	private Board bd;
	
	private void basicInit() {
		isDraw = false;
		score = 0;
	}
	
	private void initialise() {
		basicInit();
		isDraw = pm.isThreefoldRepetitionPossible();
		if (!isDraw) {
			isDraw = pm.isInsufficientMaterial();
		}
	}
	
	int neural_net_eval() {
		NNUE network = new NNUE((PositionManager) this.pm);
		return network.evaluate();
	}
	
	public int lazyEvaluation(int alpha, int beta) {
		basicInit();
		if (!isDraw) {
			score = neural_net_eval();
			if (score >= beta) {
				return beta;
			}
		}
		return score;
	}
	
	public int getFullEvaluation() {
		initialise();
		if (!isDraw)
			score = neural_net_eval();
		return score;
	}
	
	public int getStaticEvaluation() {
		// No point checking for draws, because we terminate search as soon as a likely draw is detected
		// and return draw score, so we can't get here if the position is a likely draw, the check would
		// be redundant
		basicInit();
		return neural_net_eval();
	}
	
	public boolean goForMate() {
		return goForMate;
	}
	
	public PositionEvaluator(IPositionAccessors pm) {	
		this.pm = pm;
		bd = pm.getTheBoard();
		// If either side can't win (e.g. bare King) then do a mate search.
		goForMate = ((Long.bitCount(bd.getBlackPieces()) == 1) || 
				     (Long.bitCount(bd.getWhitePieces()) == 1));
		initialise();
	}
}
