package eubos.score;

import eubos.board.Board;
import eubos.board.Piece;
import eubos.neural_net.NNUE;
import eubos.position.IPositionAccessors;
import eubos.position.PositionManager;

public class PositionEvaluator implements IEvaluate {
	
	private IPositionAccessors pm;
	private boolean goForMate;
	private boolean isDraw;
	private int score;
	private Board bd;
	
	private static final int FUTILITY_MARGIN_BY_PIECE[] = new int[8];
    static {
    	FUTILITY_MARGIN_BY_PIECE[Piece.QUEEN] = 175;
    	FUTILITY_MARGIN_BY_PIECE[Piece.ROOK] = 150;
    	FUTILITY_MARGIN_BY_PIECE[Piece.BISHOP] = 130;
    	FUTILITY_MARGIN_BY_PIECE[Piece.KNIGHT] = 175;
    	FUTILITY_MARGIN_BY_PIECE[Piece.KING] = 150;
    	FUTILITY_MARGIN_BY_PIECE[Piece.PAWN] = 125;
    }
	
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
	
	private int internalFullEval() {
		if (!isDraw)
			score = neural_net_eval();
		return score;
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
	
	int getCrudeEvaluation() {
		initialise();
		return internalFullEval();
	}
	
	public int getFullEvaluation() {
		initialise();
		return internalFullEval();
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
