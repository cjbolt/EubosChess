package eubos.search;

import eubos.board.Board;
import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.position.IPositionAccessors;
import eubos.score.PiecewiseEvaluation;
import eubos.score.ReferenceScore;

public class SearchContext {
	PiecewiseEvaluation initial;
	IPositionAccessors pos;
	Piece.Colour initialOnMove;
	Piece.Colour opponent;
	SearchGoal goal;
	
	static final short SIMPLIFY_THRESHOLD = 100;
	static final short DRAW_THRESHOLD = -150;
	
	static final short SIMPLIFICATION_BONUS = 75;
	static final short ACHIEVES_DRAW_BONUS = Board.MATERIAL_VALUE_KING/2;
	
	static final boolean ALWAYS_TRY_FOR_WIN = false;
	
	private enum SearchGoal {
		try_for_mate,
		try_for_win,
		simplify,
		try_for_draw
	};
	
	public SearchContext(IPositionAccessors pos, PiecewiseEvaluation initialMaterial, ReferenceScore refScore) {
		this.pos = pos;
		// Make a copy of the initial Material Evaluation and store it here
		initial = new PiecewiseEvaluation(initialMaterial.getWhite(), initialMaterial.getBlack(), initialMaterial.getPosition());
		initialOnMove = pos.getOnMove();
		opponent = Colour.getOpposite(initialOnMove);
		setGoal(refScore);
	}

	private void setGoal(ReferenceScore refScore) {
		goal = SearchGoal.try_for_win;
		if (!ALWAYS_TRY_FOR_WIN) {
			if (pos.getTheBoard().isInsufficientMaterial(Colour.getOpposite(initialOnMove))) {
				// When opponent can't win
				goal = SearchGoal.try_for_mate;
				// consider cleaning hash table when we first get this goal?
				// otherwise we can get a problem where prior hash scores exist which can have e.g. try_for_draw scores in them, causing errors!
				// see this position - 8/1P6/2K2k2/8/4n3/8/8/8 w - - - 75
				
			} else if (pos.getTheBoard().isInsufficientMaterial(initialOnMove) ||
					   (refScore != null && refScore.getReference().score < DRAW_THRESHOLD)) {
				// When we can't win
				goal = SearchGoal.try_for_draw;
				
			} else if (refScore != null && 
					   (!pos.getTheBoard().isEndgame && refScore.getReference().score > SIMPLIFY_THRESHOLD) ||
					   (pos.getTheBoard().isEndgame && refScore.getReference().score < 0)) {
				// When simplification possible, but don't oversimplify in the endgame, if winning. Can simplify endgame if losing.
				goal = SearchGoal.simplify;
			} else {
				// Default; try_for_win
			}
		}
	}
	
	public boolean isTryForDraw() {
		return goal == SearchGoal.try_for_draw; 
	}
	
	public boolean isTryForMate() {
		return goal == SearchGoal.try_for_mate; 
	}
		
	public class SearchContextEvaluation {
		public boolean isDraw;
		public short score;
		
		public SearchContextEvaluation() {
			isDraw = false;
			score = 0;
		}
	}
	
	public SearchContextEvaluation computeSearchGoalBonus(PiecewiseEvaluation current) {
		
		SearchContextEvaluation eval = new SearchContextEvaluation();
		boolean threeFold = pos.isThreefoldRepetitionPossible();
		boolean insufficient = pos.getTheBoard().isInsufficientMaterial();
		
		eval.isDraw = (threeFold || insufficient);
		if (eval.isDraw) {
			// If we drew, score according to our goal
			if (isTryForDraw() && insufficient) {
				eval.score = (pos.getOnMove() == initialOnMove) ? ACHIEVES_DRAW_BONUS : -ACHIEVES_DRAW_BONUS;
			} else {
				eval.score = 0;
			}
		} else {
			// We just moved and it isn't a draw, score according to our goal
			switch(goal) {
			case simplify: 
		    	if ((pos.getOnMove() == initialOnMove) && isPositionSimplified(current)) {
			    	eval.score = SIMPLIFICATION_BONUS;
		    	}
		    	// Deliberate drop through
			case try_for_draw:
			case try_for_win:
			//case try_for_mate:
				// Add on positional weightings
				eval.score += Colour.isBlack(pos.getOnMove()) ? -current.getPosition() : current.getPosition();
				break;
			case try_for_mate:
			default:
				// Don't evaluate for positional factors, which prevents aimless faffing about board
				break;
			}
		}
		return eval;
	}
	
	private boolean isPositionSimplified(PiecewiseEvaluation current) {
		boolean isSimplification = false;
		if (Colour.isWhite(initialOnMove)) {
			if ((initial.getDelta() <= current.getDelta()) && initial.getWhite() > current.getWhite()) {
				isSimplification = true;
			}
		} else {
			if ((initial.getDelta() >= current.getDelta()) && initial.getBlack() > current.getBlack()) {
				isSimplification = true;
			}			
		}
		return isSimplification;
	}
	
	public short getScoreForStalemate() {
		short mateScore = 0; // ((pos.getOnMove() == initialOnMove) && isTryForDraw()) ? 0 : 0;
		return mateScore;	
	}

	public String getGoal() {
		return goal.toString();
	}
}
