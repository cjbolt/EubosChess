package eubos.score;

import com.fluxchess.jcpi.models.IntFile;

import eubos.board.Board;
import eubos.board.IForEachPieceCallback;
import eubos.board.Piece;
import eubos.position.IPositionAccessors;
import eubos.position.Position;

public class PositionEvaluator implements IEvaluate {

	IPositionAccessors pm;
	
	public static final int DOUBLED_PAWN_HANDICAP = 12;
	public static final int ISOLATED_PAWN_HANDICAP = 33;
	public static final int BACKWARD_PAWN_HANDICAP = 12;
	
	public static final int PASSED_PAWN_BOOST = 12;
	public static final int ROOK_FILE_PASSED_PAWN_BOOST = 8;
	public static final int CANDIDATE_PAWN = 8;
	public static final int ROOK_FILE_CANDIDATE_PAWN = 5;
	public static final int CONNECTED_PASSED_PAWN_BOOST = 75;
	
	public static final boolean ENABLE_PAWN_EVALUATION = true;
	public static final boolean ENABLE_KPK_EVALUATION = false;
	public static final boolean ENABLE_CANDIDATE_PP_EVALUATION = true;
	
	public static final boolean ENABLE_KING_SAFETY_EVALUATION = true;
	
	public static final boolean ENABLE_DYNAMIC_POSITIONAL_EVALUATION = true;

	private static final int BISHOP_PAIR_BOOST = 25;
	
	boolean isWhite;
	int midgameScore = 0;
	int endgameScore = 0;
	public boolean isDraw;
	public short score;
	public Board bd;
	PawnEvaluator pawn_eval;
	
	public boolean goForMate;
	public boolean goForMate() {
		return goForMate;
	}
	
	public PositionEvaluator(IPositionAccessors pm) {	
		this.pm = pm;
		bd = pm.getTheBoard();
		pawn_eval = new PawnEvaluator();
		// If either side can't win (e.g. bare King) then do a mate search.
		goForMate = ((Long.bitCount(bd.getBlackPieces()) == 1) || 
				     (Long.bitCount(bd.getWhitePieces()) == 1));
	}
	
	private void initialise() {
		boolean threeFold = pm.isThreefoldRepetitionPossible();
		boolean insufficient = bd.isInsufficientMaterial();
		
		isDraw = (threeFold || insufficient);
		isWhite = pm.onMoveIsWhite();
		score = 0;
		midgameScore = 0;
		endgameScore = 0;
	}
	
	private short taperEvaluation(int midgameScore, int endgameScore) {
		int phase = bd.me.getPhase();
		return (short)(((midgameScore * (4096 - phase)) + (endgameScore * phase)) / 4096);
	}
	
	public int getCrudeEvaluation() {
		initialise();
		if (!isDraw) {
			bd.me.dynamicPosition = 0;
			score += evaluateBishopPair();
			midgameScore = score + (isWhite ? bd.me.getMiddleGameDelta() + bd.me.getPosition() : -(bd.me.getMiddleGameDelta() + bd.me.getPosition()));
			endgameScore = score + (isWhite ? bd.me.getEndGameDelta() + bd.me.getEndgamePosition() : -(bd.me.getEndGameDelta() + bd.me.getEndgamePosition()));
			score = taperEvaluation(midgameScore, endgameScore);
		}
		return score;
	}
	
	public int getFullEvaluation() {
		initialise();
		if (!isDraw) {
			long [][] attacks = pm.getAttacks();
			// Score factors common to each phase, material, pawn structure and piece mobility
			bd.me.dynamicPosition = 0;
			score += evaluateBishopPair();
			if (PositionEvaluator.ENABLE_DYNAMIC_POSITIONAL_EVALUATION && !goForMate) {
				//bd.calculateDynamicMobility(bd.me);
				bd.me.dynamicPosition += Long.bitCount(attacks[0][2])*2;
				bd.me.dynamicPosition -= Long.bitCount(attacks[1][2])*2;
			}
			if (ENABLE_PAWN_EVALUATION) {
				score += pawn_eval.evaluatePawnStructure(attacks);
			}
			// Add phase specific static mobility (PSTs)
			midgameScore = score + (isWhite ? bd.me.getMiddleGameDelta() + bd.me.getPosition() : -(bd.me.getMiddleGameDelta() + bd.me.getPosition()));
			endgameScore = score + (isWhite ? bd.me.getEndGameDelta() + bd.me.getEndgamePosition() : -(bd.me.getEndGameDelta() + bd.me.getEndgamePosition()));
			// Add King Safety in middle game
			if (ENABLE_KING_SAFETY_EVALUATION && !goForMate) {
				midgameScore += evaluateKingSafety(attacks);
			}
			if (!goForMate) {
				score = taperEvaluation(midgameScore, endgameScore);
			} else {
				score += isWhite ? bd.me.getMiddleGameDelta() : -bd.me.getMiddleGameDelta();
			}
		}
		return score;
	}
	
	int evaluateKingSafety(long[][] attacks) {
		int kingSafetyScore = 0;
		kingSafetyScore = pm.getTheBoard().evaluateKingSafety(attacks, isWhite ? Piece.Colour.white : Piece.Colour.black);
		kingSafetyScore -= pm.getTheBoard().evaluateKingSafety(attacks, !isWhite ? Piece.Colour.white : Piece.Colour.black);
		return kingSafetyScore;
	}
	
	int evaluateBishopPair() {
		int score = 0;
		int onMoveBishopCount = isWhite ? bd.me.numberOfPieces[Piece.WHITE_BISHOP] : bd.me.numberOfPieces[Piece.BLACK_BISHOP];
		if (onMoveBishopCount >= 2) {
			score += BISHOP_PAIR_BOOST;
		}
		int opponentBishopCount = isWhite ? bd.me.numberOfPieces[Piece.BLACK_BISHOP] : bd.me.numberOfPieces[Piece.WHITE_BISHOP];
		if (opponentBishopCount >= 2) {
			score -= BISHOP_PAIR_BOOST;
		}
		return score;
	}
	
	public class PawnEvaluator implements IForEachPieceCallback{
		
		public int piecewisePawnScoreAccumulator = 0;
		public long[][] attacks;
		protected int queeningDistance;
		protected int weighting;
		protected boolean pawnIsBlack;
		
		protected void setQueeningDistance(int atPos, boolean pawnIsWhite) {
			pawnIsBlack = !pawnIsWhite;
			int rank = Position.getRank(atPos);
			if (pawnIsBlack) {
				queeningDistance = rank;
				weighting = 7-queeningDistance;
			} else {
				queeningDistance = 7-rank;
				weighting = rank;
			}
		}
		
		@SuppressWarnings("unused")
		@Override
		public void callback(int piece, int atPos) {
			boolean pawnIsWhite = Piece.isWhite(piece);
			long[] enemy_attacks = attacks[pawnIsWhite ? 1:0];
			long[] own_attacks = attacks[pawnIsWhite ? 0:1];
			
			if (bd.isPassedPawn(atPos, pawnIsWhite)) {
				setQueeningDistance(atPos, pawnIsWhite);
				if (ENABLE_KPK_EVALUATION && bd.me.phase == 4096) {
					// Special case, it is a KPK endgame
					int file = Position.getFile(atPos);
					int queeningSquare = pawnIsBlack ? Position.valueOf(file, 0) : Position.valueOf(file, 7);
					int oppoKingPos = bd.getKingPosition(pawnIsBlack);
					int oppoDistance = Position.distance(queeningSquare, oppoKingPos);
					if ((isWhite && pawnIsBlack) || (!isWhite && !pawnIsBlack)) {
						// if king is on move, assume it can get towards the square of the pawn
						oppoDistance -= 1;
					}
					if (oppoDistance > queeningDistance) {
						// can't be caught by opposite king
						piecewisePawnScoreAccumulator += 700;
					} else {
						// Add code to increase score also if the pawn can be defended by own king
						int ownKingPos = bd.getKingPosition(!pawnIsBlack);
						int ownDistance = Position.distance(queeningSquare, ownKingPos);
						if (ownDistance-1 <= oppoDistance) {
							// Rationale is queen square can be blocked off from opposite King by own King
							piecewisePawnScoreAccumulator += 700;
						}
					}
				} else {
					// scale weighting for game phase as well as promotion proximity, up to 3x
					int scale = 1 + ((bd.me.phase+640) / 4096) + ((bd.me.phase+320) / 4096);
					weighting *= scale;
					boolean pawnIsBlocked = bd.isPawnFrontspanBlocked(atPos, pawnIsWhite, own_attacks[3], enemy_attacks[3]);
					int value = (Position.getFile(atPos) == IntFile.Fa || Position.getFile(atPos) == IntFile.Fh) ?
							ROOK_FILE_PASSED_PAWN_BOOST : PASSED_PAWN_BOOST;
					int score = weighting*value;
					if (pawnIsBlocked) {
						score /= 2;
					}
					piecewisePawnScoreAccumulator += score;
				}
			} else if (ENABLE_CANDIDATE_PP_EVALUATION) {
				if (bd.isCandidatePassedPawn(atPos, pawnIsWhite, own_attacks[0], enemy_attacks[0])) {
					setQueeningDistance(atPos, pawnIsWhite);
					// scale weighting for game phase as well as promotion proximity, up to 3x
					int scale = 1 + ((bd.me.phase+640) / 4096) + ((bd.me.phase+320) / 4096);
					weighting *= scale;
					if (Position.getFile(atPos) == IntFile.Fa || Position.getFile(atPos) == IntFile.Fh) {
						piecewisePawnScoreAccumulator += weighting*ROOK_FILE_CANDIDATE_PAWN;
					} else {
						piecewisePawnScoreAccumulator += weighting*CANDIDATE_PAWN;
					}
				}
			}
			if (bd.isIsolatedPawn(atPos, pawnIsWhite)) {
				piecewisePawnScoreAccumulator -= ISOLATED_PAWN_HANDICAP;
			} else if (bd.isBackwardsPawn(atPos, pawnIsWhite)) {
				piecewisePawnScoreAccumulator -= BACKWARD_PAWN_HANDICAP;
			}
		}
		
		int evaluatePawnStructure(long[][] attacks) {
			int pawnEvaluationScore = 0;
			isWhite = pm.onMoveIsWhite();
			long pawnsToTest = isWhite ? bd.getWhitePawns() : bd.getBlackPawns();
			long enemyPawns = isWhite ? bd.getBlackPawns() : bd.getWhitePawns();
			pawn_eval.attacks = attacks;
			if (pawnsToTest != 0x0) {
				piecewisePawnScoreAccumulator = 0;
				int pawnHandicap = -bd.countDoubledPawnsForSide(isWhite)*DOUBLED_PAWN_HANDICAP;
				bd.forEachPawnOfSide(this, !isWhite);
				pawnEvaluationScore = pawnHandicap + piecewisePawnScoreAccumulator;
			}
			if (enemyPawns != 0x0) {
				piecewisePawnScoreAccumulator = 0;
				int pawnHandicap = -bd.countDoubledPawnsForSide(!isWhite)*DOUBLED_PAWN_HANDICAP;
				bd.forEachPawnOfSide(this, isWhite);
				pawnEvaluationScore -= (pawnHandicap + piecewisePawnScoreAccumulator);
			}
			return pawnEvaluationScore;
		}
	}
}
