package eubos.score;

import com.fluxchess.jcpi.models.IntFile;

import eubos.board.Board;
import eubos.board.IForEachPieceCallback;
import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.position.IPositionAccessors;
import eubos.position.Position;

public class PositionEvaluator implements IEvaluate, IForEachPieceCallback {

	IPositionAccessors pm;
	
	public static final int DOUBLED_PAWN_HANDICAP = 12;
	public static final int ISOLATED_PAWN_HANDICAP = 33;
	public static final int BACKWARD_PAWN_HANDICAP = 12;
	
	public static final int PASSED_PAWN_BOOST = 12;
	public static final int ROOK_FILE_PASSED_PAWN_BOOST = 8;
	
	public static final int CONNECTED_PASSED_PAWN_BOOST = 75;
	
	public static final boolean ENABLE_PAWN_EVALUATION = true;
	public static final boolean ENABLE_KING_SAFETY_EVALUATION = true;
	public static final boolean ENABLE_DYNAMIC_POSITIONAL_EVALUATION = true;
	
	public boolean isDraw;
	public short score;
	public boolean goForMate;
	public Board bd;
	
	public PositionEvaluator(IPositionAccessors pm) {	
		this.pm = pm;
		bd = pm.getTheBoard();
		// If either side can't win (e.g. bare King) then do a mate search.
		goForMate = ((Long.bitCount(bd.getBlackPieces()) == 1) || 
				     (Long.bitCount(bd.getWhitePieces()) == 1));
	}
	
	private void initialise() {
		boolean threeFold = pm.isThreefoldRepetitionPossible();
		boolean insufficient = bd.isInsufficientMaterial();
		
		isDraw = (threeFold || insufficient);
		score = 0;
	}
	
	private short taperEvaluation(int midgameScore, int endgameScore) {
		int phase = bd.me.getPhase();
		return (short)(((midgameScore * (4096 - phase)) + (endgameScore * phase)) / 4096);
	}
	
	public int getCrudeEvaluation() {
		int midgameScore = 0;
		int endgameScore = 0;
		initialise();
		if (!isDraw) {
			bd.me.dynamicPosition = 0;
			score += pm.onMoveIsWhite() ? bd.me.getDelta() : -bd.me.getDelta();
			midgameScore = score + (pm.onMoveIsWhite() ? bd.me.getPosition() : -bd.me.getPosition());
			endgameScore = score + (pm.onMoveIsWhite() ? bd.me.getEndgamePosition() : -bd.me.getEndgamePosition());
			score = taperEvaluation(midgameScore, endgameScore);
		}
		return score;
	}
	
	public int getFullEvaluation() {
		int midgameScore = 0;
		int endgameScore = 0;
		initialise();
		if (!isDraw) {
			// Score factors common to each phase, material, pawn structure and piece mobility
			bd.me.dynamicPosition = 0;
			score += pm.onMoveIsWhite() ? bd.me.getDelta() : -bd.me.getDelta();
			if (PositionEvaluator.ENABLE_DYNAMIC_POSITIONAL_EVALUATION && !goForMate) {
				bd.calculateDynamicMobility(bd.me);
			}
			if (ENABLE_PAWN_EVALUATION) {
				score += evaluatePawnStructure();
			}
			// Add phase specific static mobility (PSTs)
			midgameScore = score + (pm.onMoveIsWhite() ? bd.me.getPosition() : -bd.me.getPosition());
			endgameScore = score + (pm.onMoveIsWhite() ? bd.me.getEndgamePosition() : -bd.me.getEndgamePosition());
			// Add King Safety in middle game
			if (ENABLE_KING_SAFETY_EVALUATION && !goForMate) {
				midgameScore += evaluateKingSafety();
			}
			if (!goForMate) {
				score = taperEvaluation(midgameScore, endgameScore);
			}
		}
		return score;
	}
	
	int evaluatePawnStructure() {
		int pawnEvaluationScore = 0;
		long pawnsToTest = pm.onMoveIsWhite() ? bd.getWhitePawns() : bd.getBlackPawns();
		if (pawnsToTest != 0x0) {
			pawnEvaluationScore = evaluatePawnsForColour(pm.getOnMove());
		}
		pawnsToTest = (!pm.onMoveIsWhite()) ? bd.getWhitePawns() : bd.getBlackPawns();
		if (pawnsToTest != 0x0) {
			pawnEvaluationScore -= evaluatePawnsForColour(Colour.getOpposite(pm.getOnMove()));
		}
		return pawnEvaluationScore;
	}
	
	int evaluateKingSafety() {
		int kingSafetyScore = 0;
		kingSafetyScore = pm.getTheBoard().evaluateKingSafety(pm.getOnMove());
		kingSafetyScore -= pm.getTheBoard().evaluateKingSafety(Piece.Colour.getOpposite(pm.getOnMove()));
		return kingSafetyScore;
	}
	
	Colour onMoveIs;
	int individualPawnEval = 0;
	
	@Override
	public void callback(int piece, int atPos) {
		if (bd.isPassedPawn(atPos, onMoveIs)) {
			int weighting = 1;
			if (Piece.isBlack(piece)) {
				weighting = 7-Position.getRank(atPos);
			} else {
				weighting = Position.getRank(atPos);
			}
			// scale weighting for game phase as well as promotion proximity, up to 3x
			int scale = 1 + ((bd.me.phase+640) / 4096) + ((bd.me.phase+320) / 4096);
			weighting *= scale;
			if (Position.getFile(atPos) == IntFile.Fa || Position.getFile(atPos) == IntFile.Fh) {
				individualPawnEval += weighting*ROOK_FILE_PASSED_PAWN_BOOST;
			} else {
				individualPawnEval += weighting*PASSED_PAWN_BOOST;
			}
		}
		if (bd.isIsolatedPawn(atPos, onMoveIs)) {
			individualPawnEval -= ISOLATED_PAWN_HANDICAP;
		} else if (bd.isBackwardsPawn(atPos, onMoveIs)) {
			individualPawnEval -= BACKWARD_PAWN_HANDICAP;
		}
	}
	
	private int evaluatePawnsForColour(Colour onMove) {
		this.onMoveIs = onMove;
		this.individualPawnEval = 0;
		int pawnHandicap = -bd.countDoubledPawnsForSide(onMove)*DOUBLED_PAWN_HANDICAP;
		bd.forEachPawnOfSide(this, Colour.isBlack(onMove));
		return pawnHandicap + individualPawnEval;
	}
}
