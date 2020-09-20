package eubos.search;

import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.position.IPositionAccessors;
import eubos.score.MaterialEvaluation;
import eubos.score.MaterialEvaluator;

public class SearchContext {
	MaterialEvaluation initial;
	IPositionAccessors pos;
	Piece.Colour initialOnMove;
	SearchGoal goal;
	boolean isEndgame;
	
	static final short SIMPLIFY_THRESHOLD = 100;
	static final short DRAW_THRESHOLD = -150;
	
	static final short SIMPLIFICATION_BONUS = 75;
	static final short AVOID_DRAW_HANDICAP = -150;
	static final short ACHIEVES_DRAW_BONUS = MaterialEvaluator.MATERIAL_VALUE_KING/2;
	
	static final int ENDGAME_MATERIAL_THRESHOLD = 
			MaterialEvaluator.MATERIAL_VALUE_KING + 
			MaterialEvaluator.MATERIAL_VALUE_ROOK + 
			MaterialEvaluator.MATERIAL_VALUE_KNIGHT + 
			(4 * MaterialEvaluator.MATERIAL_VALUE_PAWN);
	
	static final int ENDGAME_MATERIAL_THRESHOLD_WITHOUT_QUEENS =
			MaterialEvaluator.MATERIAL_VALUE_KING + 
			MaterialEvaluator.MATERIAL_VALUE_ROOK + 
			MaterialEvaluator.MATERIAL_VALUE_KNIGHT +
			MaterialEvaluator.MATERIAL_VALUE_BISHOP +
			(4 * MaterialEvaluator.MATERIAL_VALUE_PAWN);
	
	static final boolean ALWAYS_TRY_FOR_WIN = false;
	
	private enum SearchGoal {
		try_for_win,
		simplify,
		try_for_draw
	};
	
	public SearchContext(IPositionAccessors pos, MaterialEvaluation initialMaterial) {
		this.pos = pos;
		initial = initialMaterial;
		initialOnMove = pos.getOnMove();
		boolean queensOffBoard = (pos.getTheBoard().getWhiteQueens() == 0) && (pos.getTheBoard().getBlackQueens() ==0);
		int opponentMaterial = Piece.Colour.isWhite(initialOnMove) ? initialMaterial.getBlack() : initialMaterial.getWhite();
		boolean queensOffMaterialThresholdReached = opponentMaterial <= ENDGAME_MATERIAL_THRESHOLD_WITHOUT_QUEENS;
		boolean materialQuantityThreshholdReached = initialMaterial.getWhite() <= ENDGAME_MATERIAL_THRESHOLD && initialMaterial.getBlack() <= ENDGAME_MATERIAL_THRESHOLD;
		if ((queensOffBoard && queensOffMaterialThresholdReached) || materialQuantityThreshholdReached) {
			isEndgame = true;
		}
		setGoal();
	}

	private void setGoal() {
		if (ALWAYS_TRY_FOR_WIN) {
			goal = SearchGoal.try_for_win;
		} else if (pos.getTheBoard().isInsufficientMaterial(initialOnMove)) {
			goal = SearchGoal.try_for_draw;
		} else if ((Colour.isWhite(initialOnMove) && initial.getDelta() > SIMPLIFY_THRESHOLD) ||
			(Colour.isBlack(initialOnMove) && initial.getDelta() < -SIMPLIFY_THRESHOLD )) {
			goal = SearchGoal.simplify;
		} else if ((Colour.isWhite(initialOnMove) && initial.getDelta() < DRAW_THRESHOLD) ||
				(Colour.isBlack(initialOnMove) && initial.getDelta() > -DRAW_THRESHOLD )) {
			goal = SearchGoal.try_for_draw;
		} else {
			goal = SearchGoal.try_for_win;
		}
		EubosEngineMain.logger.info(String.format("SearchContext is %s", goal));
	}
	
	public boolean isTryForDraw() {
		return goal == SearchGoal.try_for_draw; 
	}
		
	public class SearchContextEvaluation {
		public boolean isDraw;
		public short score;
		
		public SearchContextEvaluation() {
			isDraw = false;
			score = 0;
		}
	}
	
	public SearchContextEvaluation computeSearchGoalBonus(MaterialEvaluation current) {
		Piece.Colour opponent = Colour.getOpposite(initialOnMove);
		SearchContextEvaluation eval = new SearchContextEvaluation();
		
		boolean threeFold = pos.isThreefoldRepetitionPossible();
		boolean insufficient = pos.getTheBoard().isInsufficientMaterial();
		if (threeFold || insufficient) {
			eval.isDraw  = true;
		}
		
		// If we just moved, score as according to our goal
		if (pos.getOnMove().equals(opponent)) {
			switch(goal) {
			case simplify: 
			    if (isPositionSimplified(current)) {
			    	eval.score = SIMPLIFICATION_BONUS;
				} 
				break;
			case try_for_draw:
				if (insufficient) {
					eval.score = ACHIEVES_DRAW_BONUS;
				}
				break;
			case try_for_win:
			default:
				break;
			}
			eval.score = adjustScoreIfBlack(eval.score);
		}
		return eval;
	}
	
	private boolean isPositionSimplified(MaterialEvaluation current) {
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

	public boolean isEndgame() {
		// Could make this update for when we enter endgame as a consequence of moves applied during the search.
		return isEndgame;
	}
	
	public short getScoreForStalemate() {
		short mateScore = isTryForDraw() ? ACHIEVES_DRAW_BONUS : -ACHIEVES_DRAW_BONUS;
		return adjustScoreIfBlack(mateScore);	
	}
	
	private short adjustScoreIfBlack(short score) {
		if (Colour.isBlack(initialOnMove)) {
			score = (short) -score;
		}
		return score;
	}
}
