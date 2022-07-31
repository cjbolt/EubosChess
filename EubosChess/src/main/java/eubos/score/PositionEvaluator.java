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
	public static final int NO_PAWNS_HANDICAP = 50;
	
	public static final int PASSED_PAWN_BOOST = 15;
	public static final int ROOK_FILE_PASSED_PAWN_BOOST = 10;
	public static final int CANDIDATE_PAWN = 8;
	public static final int ROOK_FILE_CANDIDATE_PAWN = 5;
	public static final int SAFE_MOBILE_PASSED_PAWN = 10;
	public static final int MOBILE_PASSED_PAWN = 5;
	public static final int CONNECTED_PASSED_PAWN_BOOST = 75;
	public static final int HEAVY_PIECE_BEHIND_PASSED_PAWN = 20;
	
	public static final boolean ENABLE_PAWN_EVALUATION = true;
	public static final boolean ENABLE_KPK_EVALUATION = true;
	public static final boolean ENABLE_CANDIDATE_PP_EVALUATION = true;
	public static final boolean ENABLE_PP_IMBALANCE_EVALUATION = false;
	
	public static final boolean ENABLE_KING_SAFETY_EVALUATION = true;
	public static final boolean ENABLE_TWEAKED_KING_FLIGHT_SQUARES = false;
	
	public static final boolean ENABLE_DYNAMIC_POSITIONAL_EVALUATION = true;

	private static final int BISHOP_PAIR_BOOST = 25;
	
	boolean onMoveIsWhite;
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
		initialise();
	}
	
	private void initialise() {
		boolean threeFold = pm.isThreefoldRepetitionPossible();
		boolean insufficient = bd.isInsufficientMaterial();
		
		isDraw = (threeFold || insufficient);
		onMoveIsWhite = pm.onMoveIsWhite();
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
			midgameScore = score + (onMoveIsWhite ? bd.me.getMiddleGameDelta() + bd.me.getPosition() : -(bd.me.getMiddleGameDelta() + bd.me.getPosition()));
			endgameScore = score + (onMoveIsWhite ? bd.me.getEndGameDelta() + bd.me.getEndgamePosition() : -(bd.me.getEndGameDelta() + bd.me.getEndgamePosition()));
			score = taperEvaluation(midgameScore, endgameScore);
		}
		return score;
	}
	
	public int getFullEvaluation() {
		initialise();
		if (!isDraw) {
			// Score factors common to each phase, material, pawn structure and piece mobility
			bd.me.dynamicPosition = 0;
			
			// Only generate full attack mask if passed pawn present and past opening stage
			//boolean isPassedPawnPresent = bd.me.phase > 600 && bd.isPassedPawnPresent();
			long [][][] attacks = bd.calculateAttacksAndMobility(bd.me, false);
			
			score += evaluateBishopPair();
			
			if (ENABLE_PAWN_EVALUATION) {
				score += pawn_eval.evaluatePawnStructure(attacks);
			}
			// Add phase specific static mobility (PSTs)
			midgameScore = score + (onMoveIsWhite ? bd.me.getMiddleGameDelta() + bd.me.getPosition() : -(bd.me.getMiddleGameDelta() + bd.me.getPosition()));
			endgameScore = score + (onMoveIsWhite ? bd.me.getEndGameDelta() + bd.me.getEndgamePosition() : -(bd.me.getEndGameDelta() + bd.me.getEndgamePosition()));
			// Add King Safety in middle game
			if (ENABLE_KING_SAFETY_EVALUATION && !goForMate) {
				midgameScore += evaluateKingSafety(attacks);
			}
			if (!goForMate) {
				score = taperEvaluation(midgameScore, endgameScore);
			} else {
				score += onMoveIsWhite ? bd.me.getMiddleGameDelta() : -bd.me.getMiddleGameDelta();
			}
		}
		return score;
	}
	
	int evaluateKingSafety(long[][][] attacks) {
		int kingSafetyScore = 0;
		kingSafetyScore = pm.getTheBoard().evaluateKingSafety(attacks, onMoveIsWhite);
		kingSafetyScore -= pm.getTheBoard().evaluateKingSafety(attacks, !onMoveIsWhite);
		return kingSafetyScore;
	}
	
	int evaluateBishopPair() {
		int score = 0;
		int onMoveBishopCount = onMoveIsWhite ? bd.me.numberOfPieces[Piece.WHITE_BISHOP] : bd.me.numberOfPieces[Piece.BLACK_BISHOP];
		if (onMoveBishopCount >= 2) {
			score += BISHOP_PAIR_BOOST;
		}
		int opponentBishopCount = onMoveIsWhite ? bd.me.numberOfPieces[Piece.BLACK_BISHOP] : bd.me.numberOfPieces[Piece.WHITE_BISHOP];
		if (opponentBishopCount >= 2) {
			score -= BISHOP_PAIR_BOOST;
		}
		return score;
	}
	
	public class PawnEvaluator implements IForEachPieceCallback{
		
		public int piecewisePawnScoreAccumulator = 0;
		public long[][][] attacks;
		protected int queeningDistance;
		protected int weighting;
		protected boolean pawnIsBlack;
		protected int[] ppCount = {0,0};
		
		public final int[] ppImbalanceTable = {0, 15, 200, 400, 700, 900, 900, 900, 900};
		
		protected int getScaleFactorForGamePhase() {
			return 1 + ((bd.me.phase+640) / 4096) + ((bd.me.phase+320) / 4096);
		}
		
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
		
		protected void evaluateKpkEndgame(int atPos, boolean isOwnPawn, long[][] ownAttacks) {
			// Special case, it is a KPK endgame
			int file = Position.getFile(atPos);
			int queeningSquare = pawnIsBlack ? Position.valueOf(file, 0) : Position.valueOf(file, 7);
			int oppoKingPos = bd.getKingPosition(pawnIsBlack);
			int oppoDistance = Position.distance(queeningSquare, oppoKingPos);
			if (!isOwnPawn) {
				// if king is on move, assume it can get towards the square of the pawn
				oppoDistance -= 1;
			}
			if (oppoDistance > queeningDistance) {
				// can't be caught by opposite king, as outside square of pawn
				piecewisePawnScoreAccumulator += 700;
			} else {
				if (bd.isFrontspanControlledInKpk(atPos, !pawnIsBlack, ownAttacks[3])) {
					// Rationale is whole frontspan can be blocked off from opposite King by own King
					piecewisePawnScoreAccumulator += 700;
				} else {
					// increase score also if we think the pawn can be defended by own king
					int ownKingPos = bd.getKingPosition(!pawnIsBlack);
					int ownDistance = Position.distance(queeningSquare, ownKingPos);
					if (ownDistance-1 <= oppoDistance) {
						piecewisePawnScoreAccumulator += 300;
					}
				}
			}
		}
		
		protected void evaluatePassedPawn(int atPos, boolean pawnIsWhite, long[][] own_attacks, long [][] enemy_attacks) {
			weighting *= getScaleFactorForGamePhase();
			int value = (Position.getFile(atPos) == IntFile.Fa || Position.getFile(atPos) == IntFile.Fh) ?
					ROOK_FILE_PASSED_PAWN_BOOST : PASSED_PAWN_BOOST;
			
			int score = 0;
			if (!bd.isPawnBlockaded(atPos, pawnIsWhite)) {
				int heavySupportIndication = bd.checkForHeavyPieceBehindPassedPawn(atPos, pawnIsWhite);
				if (heavySupportIndication > 0) {
					score += HEAVY_PIECE_BEHIND_PASSED_PAWN;
				} else if (heavySupportIndication < 0) {
					score -= HEAVY_PIECE_BEHIND_PASSED_PAWN;
				} else {
					// neither attacked or defended along the rear span
				}
				if (bd.isPawnFrontspanSafe(atPos, pawnIsWhite, own_attacks[3], enemy_attacks[3], heavySupportIndication > 0)) {
					value += SAFE_MOBILE_PASSED_PAWN;
				} else if (bd.canPawnAdvance(atPos, pawnIsWhite, own_attacks[3], enemy_attacks[3])) {
					value += MOBILE_PASSED_PAWN;
				}
			}
			score += weighting*value;
			piecewisePawnScoreAccumulator += score;
		}
		
		@SuppressWarnings("unused")
		@Override
		public void callback(int piece, int atPos) {
			boolean pawnIsWhite = Piece.isWhite(piece);
			long[][] enemy_attacks = attacks[pawnIsWhite ? 1:0];
			long[][] own_attacks = attacks[pawnIsWhite ? 0:1];
			
			if (bd.isPassedPawn(atPos, pawnIsWhite)) {
				boolean isOwnPawn = (onMoveIsWhite && pawnIsWhite) || (!onMoveIsWhite && !pawnIsWhite);
				ppCount[isOwnPawn ? 0:1] += 1;
				setQueeningDistance(atPos, pawnIsWhite);
				if (ENABLE_KPK_EVALUATION && bd.me.phase == 4096) {
					evaluateKpkEndgame(atPos, isOwnPawn, own_attacks);
				} else {
					evaluatePassedPawn(atPos, pawnIsWhite, own_attacks, enemy_attacks);
				}
			} else if (ENABLE_CANDIDATE_PP_EVALUATION) {
				// TODO make it resolve the number of attacks...
				if (bd.isCandidatePassedPawn(atPos, pawnIsWhite, own_attacks[0], enemy_attacks[0])) {
					setQueeningDistance(atPos, pawnIsWhite);
					weighting *= getScaleFactorForGamePhase();
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
		
		public int getDoubledPawnsHandicap(long pawns) {
			return -bd.countDoubledPawns(pawns)*DOUBLED_PAWN_HANDICAP;
		}
		
		void initialise(long[][][] attacks) {
			ppCount[0] = ppCount[1] = 0;
			pawn_eval.attacks = attacks;
		}
		
		@SuppressWarnings("unused")
		int evaluatePawnStructure(long[][][] attacks) {
			initialise(attacks);
			int pawnEvaluationScore = 0;
			long ownPawns = onMoveIsWhite ? bd.getWhitePawns() : bd.getBlackPawns();
			long enemyPawns = onMoveIsWhite ? bd.getBlackPawns() : bd.getWhitePawns();
			if (ownPawns != 0x0) {
				piecewisePawnScoreAccumulator = 0;
				int pawnHandicap = getDoubledPawnsHandicap(ownPawns);
				bd.forEachPawnOfSide(this, !onMoveIsWhite);
				pawnEvaluationScore = pawnHandicap + piecewisePawnScoreAccumulator;
			} else {
				pawnEvaluationScore -= NO_PAWNS_HANDICAP;
			}
			if (enemyPawns != 0x0) {
				piecewisePawnScoreAccumulator = 0;
				int pawnHandicap = getDoubledPawnsHandicap(enemyPawns);
				bd.forEachPawnOfSide(this, onMoveIsWhite);
				pawnEvaluationScore -= (pawnHandicap + piecewisePawnScoreAccumulator);
			} else {
				pawnEvaluationScore += NO_PAWNS_HANDICAP;
			}
			// Add a modification according to the imbalance of passed pawns in the position
			if (ENABLE_PP_IMBALANCE_EVALUATION && bd.me.phase > 2048) {
				int lookupIndex = ppCount[0] - ppCount[1];
				int ppImbalanceFactor = ppImbalanceTable[Math.abs(lookupIndex)];
				if (lookupIndex < 0) {
					// If negative, on move has fewer passed pawns, so subtract from score
					ppImbalanceFactor = -ppImbalanceFactor;
				}
				pawnEvaluationScore += ppImbalanceFactor;
			}
			return pawnEvaluationScore;
		}
	}
}
