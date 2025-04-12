package eubos.evaluation;

import eubos.board.Board;

import eubos.board.Piece;
import eubos.position.Move;
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
		return NNUE.evaluate((PositionManager) this.pm);
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
	
	private static final int FUTILITY_MARGIN_BY_PIECE[] = new int[8];
    static {
    	FUTILITY_MARGIN_BY_PIECE[Piece.QUEEN] = 175;
    	FUTILITY_MARGIN_BY_PIECE[Piece.ROOK] = 150;
    	FUTILITY_MARGIN_BY_PIECE[Piece.BISHOP] = 130;
    	FUTILITY_MARGIN_BY_PIECE[Piece.KNIGHT] = 175;
    	FUTILITY_MARGIN_BY_PIECE[Piece.KING] = 150;
    	FUTILITY_MARGIN_BY_PIECE[Piece.PAWN] = 125;
    }
	
	public int estimateMovePositionalContribution(int move) {
		int originPiece = Move.getOriginPiece(move);
		int originNoColour = originPiece & Piece.PIECE_NO_COLOUR_MASK;
		int futility = FUTILITY_MARGIN_BY_PIECE[originNoColour];
		
		if (originNoColour == Piece.PAWN) {
			int pawnIsAt = Move.getOriginPosition(move);
			long pawnMask = 1L << pawnIsAt;
			long pp = pm.getTheBoard().getPassedPawns();
			if ((pp & pawnMask) != 0L) {
				/* If the moving pawn is already passed, inflate futility. */
				futility += 100;
			} else {
				int pawnWillBeAt = Move.getTargetPosition(move);
				if (bd.isPassedPawn(pawnWillBeAt, pawnMask)) {
					/* If the moving pawn is becoming passed, inflate futility. */
					futility += 125;
				}
			}
			
		} 
		return futility;
	}
	
	public PositionEvaluator(IPositionAccessors pm) {	
		this.pm = pm;
		bd = pm.getTheBoard();
		// If either side can't win (e.g. bare King) then do a mate search.
		goForMate = ((Long.bitCount(bd.getBlackPieces()) == 1) || 
				     (Long.bitCount(bd.getWhitePieces()) == 1)) ||
				Math.abs(getStaticEvaluation()) > 2500;
		initialise();
	}
}
