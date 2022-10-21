package eubos.score;

import com.fluxchess.jcpi.models.IntFile;

import eubos.board.BitBoard;
import eubos.board.Board;
import eubos.board.Direction;
import eubos.board.IForEachPieceCallback;
import eubos.board.Piece;
import eubos.board.SquareAttackEvaluator;
import eubos.main.EubosEngineMain;
import eubos.position.IPositionAccessors;
import eubos.position.Position;

public class PawnEvaluator implements IForEachPieceCallback {
	
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
	public static final int CONNECTED_PASSED_PAWN_BOOST = 50;
	public static final int HEAVY_PIECE_BEHIND_PASSED_PAWN = 20;
	
	public static final boolean ENABLE_PAWN_HASH_TABLE = true;
	public static final boolean ENABLE_KPK_EVALUATION = true;
	public static final boolean ENABLE_CANDIDATE_PP_EVALUATION = true;
	public static final boolean ENABLE_PP_IMBALANCE_EVALUATION = true;
	
	public final int[] ppImbalanceTable = {0, 15, 65, 110, 220, 400, 700, 800, 900};
	
	// Static for lifetime of object
	IPositionAccessors pm;
	Board bd;
	private PawnEvalHashTable pawnHash;
	
	// Variables updated whilst considering each pawn of side, i.e. valid for side to move
	protected int queeningDistance; // initialised in setQueeningDistance()
	protected int weighting; 		// initialised in setQueeningDistance()
	public int piecewisePawnScoreAccumulator = 0; // initialised when starting evaluatePawnsForSide()
	
	// Scope is for each call to evaluatePawnStructure
	private boolean onMoveIsWhite;
	public long[][][] attacks;
	
	private class PawnHashStatistics {
		
		long nodeCount;
		long skippedCount;
		
		public PawnHashStatistics() {}
		
		public void report() {
			//EubosEngineMain.logger.info(String.format("PawnStats nodes=%d skipped=%d percentage=%.5f",
			//        nodeCount, skippedCount, ((double)skippedCount)*100.0d/((double)nodeCount)));
			System.out.println(String.format("PawnStats nodes=%d skipped=%d percentage=%.5f",
					nodeCount, skippedCount, ((double)skippedCount)*100.0d/((double)nodeCount)));
		}
	}
	
	public void reportPawnHashStatistics() {
		if (MEASURE_PAWN_HASH) {
			pawnStat.report();
		}
	}
	
	PawnHashStatistics pawnStat = null;
	
	private static final boolean MEASURE_PAWN_HASH = true;
	
	public PawnEvaluator(IPositionAccessors pm, PawnEvalHashTable pawnHash) {
		bd = pm.getTheBoard();
		this.pm = pm;
		this.pawnHash = pawnHash;
		this.onMoveIsWhite = pm.onMoveIsWhite();
		pawnStat = new PawnHashStatistics();
	}
	
	protected int getScaleFactorForGamePhase() {
		return 1 + ((bd.me.phase+640) / 4096) + ((bd.me.phase+320) / 4096);
	}
	
	protected void setQueeningDistance(int atPos, boolean pawnIsWhite) {
		int rank = Position.getRank(atPos);
		if (pawnIsWhite) {
			queeningDistance = 7-rank;
			weighting = rank;
		} else {
			queeningDistance = rank;
			weighting = 7-queeningDistance;
		}
	}
	
	@SuppressWarnings("unused")
	@Override
	public void callback(int piece, int atPos) {
		boolean pawnIsWhite = Piece.isWhite(piece);
		
		long passers = bd.getPassedPawns();
		long mask = BitBoard.positionToMask_Lut[atPos];
		if ((passers & mask) != 0L) {
			// check for directly supported and adjacent passed pawns
			long directConnectedPawns = pawnIsWhite ? SquareAttackEvaluator.WhitePawnAttacksFromPosition_Lut[atPos] : 
				                                SquareAttackEvaluator.BlackPawnAttacksFromPosition_Lut[atPos];
			directConnectedPawns |= AdjacentPawnFromPosition_Lut[atPos];
			long ownPassers = passers & (pawnIsWhite ? bd.getWhitePawns() : bd.getBlackPawns());
			piecewisePawnScoreAccumulator += (Long.bitCount(ownPassers & directConnectedPawns) * CONNECTED_PASSED_PAWN_BOOST);
				
			// check for otherwise connected passed pawns, which could be supported
			piecewisePawnScoreAccumulator += (Long.bitCount(ownPassers & BitBoard.PasserSupport_Lut[pawnIsWhite ? 0 : 1][atPos]) * CONNECTED_PASSED_PAWN_BOOST/2);
		} else if (ENABLE_CANDIDATE_PP_EVALUATION) {
			long[][] enemy_attacks = attacks[pawnIsWhite ? 1:0];
			long[][] own_attacks = attacks[pawnIsWhite ? 0:1];
			
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
	
	@Override
	public boolean condition_callback(int piece, int atPos) {
		return bd.isPassedPawn(atPos, Piece.isWhite(piece));
	}
	
	public int getDoubledPawnsHandicap(long pawns) {
		return -bd.countDoubledPawns(pawns)*DOUBLED_PAWN_HANDICAP;
	}
		
	public int evaluatePawnsForSide(long pawns, boolean isBlack) {
		int pawnEvaluationScore = 0;
		piecewisePawnScoreAccumulator = 0;
		if (pawns != 0x0) {
			int pawnHandicap = getDoubledPawnsHandicap(pawns);
			bd.forEachPawnOfSide(this, isBlack);
			pawnEvaluationScore = pawnHandicap + piecewisePawnScoreAccumulator;
		} else {
			pawnEvaluationScore -= NO_PAWNS_HANDICAP;
		}
		return pawnEvaluationScore;
	}
	
	protected int evaluateKpkEndgame(int atPos, boolean pawnIsWhite, boolean isOwnPawn, long[][] ownAttacks) {
		// Special case, it is a KPK endgame
		int score = 0;
		int file = Position.getFile(atPos);
		int queeningSquare = pawnIsWhite ? Position.valueOf(file, 7) : Position.valueOf(file, 0);
		int oppoKingPos = bd.getKingPosition(!pawnIsWhite);
		int oppoDistance = Position.distance(queeningSquare, oppoKingPos);
		if (!isOwnPawn) {
			// if king is on move, assume it can get towards the square of the pawn
			oppoDistance -= 1;
		}
		if (oppoDistance > queeningDistance) {
			// can't be caught by opposite king, as outside square of pawn
			score = 700;
		} else {
			if (bd.isFrontspanControlledInKpk(atPos, pawnIsWhite, ownAttacks[3])) {
				// Rationale is whole frontspan can be blocked off from opposite King by own King
				score = 700;
			} else {
				// increase score also if we think the pawn can be defended by own king
				int ownKingPos = bd.getKingPosition(pawnIsWhite);
				int ownDistance = Position.distance(queeningSquare, ownKingPos);
				if (ownDistance-1 <= oppoDistance) {
					score = 300;
				}
			}
		}
		return score;
	}
	
	protected int evaluatePassedPawn(int atPos, boolean pawnIsWhite, long[][] own_attacks, long [][] enemy_attacks) {
		int score = 0;
		weighting *= getScaleFactorForGamePhase();
		int value = (Position.getFile(atPos) == IntFile.Fa || Position.getFile(atPos) == IntFile.Fh) ?
				ROOK_FILE_PASSED_PAWN_BOOST : PASSED_PAWN_BOOST;
		
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
		return score;
	}
	
	int computePassedPawnContribution() {
		int scoreForPassedPawns = 0;
		long white = bd.getWhitePawns();
		long scratchBitBoard = bd.getPassedPawns();
		
		while ( scratchBitBoard != 0x0L ) {
			int score = 0;
			
			int bit_offset = Long.numberOfTrailingZeros(scratchBitBoard);
			long bitMask = 1L << bit_offset;
			int pawn_position = BitBoard.bitToPosition_Lut[bit_offset];
			
			boolean pawnIsWhite = (white & bitMask) == bitMask;
			long[][] enemy_attacks = attacks[pawnIsWhite ? 1:0];
			long[][] own_attacks = attacks[pawnIsWhite ? 0:1];
			
			setQueeningDistance(pawn_position, pawnIsWhite);
			if (ENABLE_KPK_EVALUATION && bd.me.phase == 4096) {
				score = evaluateKpkEndgame(pawn_position, pawnIsWhite, (pawnIsWhite == onMoveIsWhite), own_attacks);
			} else {
				score = evaluatePassedPawn(pawn_position, pawnIsWhite, own_attacks, enemy_attacks);
			}
			if (pawnIsWhite == onMoveIsWhite) {
				scoreForPassedPawns += score;
			} else {
				scoreForPassedPawns -= score;
			}
			
			// clear the lssb
			scratchBitBoard &= scratchBitBoard-1;
		}

		return scoreForPassedPawns;
	}
	
	@SuppressWarnings("unused")
	int evaluatePawnStructure(long[][][] attacks) {
		long white = bd.getWhitePawns();
		long black = bd.getBlackPawns();
		if (white == 0L && black == 0L)
			return 0;
		
		onMoveIsWhite = pm.onMoveIsWhite();
		this.attacks = attacks;
		pawnStat.nodeCount++;
		
		short hashEval = 0;
		int passedPawnScoreAtPosition = 0;
		if (ENABLE_PAWN_HASH_TABLE) {
			hashEval = pawnHash.get(pm.getPawnHash(), getScaleFactorForGamePhase(), white, black, onMoveIsWhite);
			if (hashEval != Short.MAX_VALUE) {
				pawnStat.skippedCount++;
				// Recompute value of passed pawns in this position
				passedPawnScoreAtPosition = computePassedPawnContribution();
				if (!EubosEngineMain.ENABLE_ASSERTS) {
					return hashEval + passedPawnScoreAtPosition;
				}
			}
		}
		
		// If no valid hash, recompute from scratch...
		long ownPawns = onMoveIsWhite ? white : black;
		long enemyPawns = onMoveIsWhite ? black : white;
		int pawnEvaluationScore = evaluatePawnsForSide(ownPawns, !onMoveIsWhite);
		pawnEvaluationScore -= evaluatePawnsForSide(enemyPawns, onMoveIsWhite);
		
		// Add a modification according to the imbalance of passed pawns in the position
		if (ENABLE_PP_IMBALANCE_EVALUATION /*&& bd.me.phase > 2048*/) {
			long passers = bd.getPassedPawns();
			int ownPasserCount = Long.bitCount(passers & ownPawns);
			int enemyPasserCount = Long.bitCount(passers & enemyPawns);
			int lookupIndex = ownPasserCount - enemyPasserCount;
			int ppImbalanceFactor = ppImbalanceTable[Math.abs(lookupIndex)];
			if (lookupIndex < 0) {
				// If negative, on move has fewer passed pawns, so subtract from score
				ppImbalanceFactor = -ppImbalanceFactor;
			}
			pawnEvaluationScore += ppImbalanceFactor;
		}
		
		if (ENABLE_PAWN_HASH_TABLE) {
			pawnHash.put(pm.getPawnHash(), getScaleFactorForGamePhase(), pawnEvaluationScore, white, black, onMoveIsWhite);
		}
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			if (hashEval != Short.MAX_VALUE)
				assert pawnEvaluationScore == hashEval : 
					String.format("pawn score before passed pawn positions: %d != %d %s", pawnEvaluationScore, hashEval, pm.unwindMoveStack());
		}
		
		// Compute passed pawn positional contribution after storing the non-positional basic evaluation to the hash table
		pawnEvaluationScore += computePassedPawnContribution();
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			if (hashEval != Short.MAX_VALUE)
				assert pawnEvaluationScore == hashEval+passedPawnScoreAtPosition :
					String.format("pawn score after pp position: %d != %d", pawnEvaluationScore, hashEval+passedPawnScoreAtPosition);
		}
		
		return pawnEvaluationScore;
	}
	
	/* 1-dimensional array:
	 * 1st index is a position integer, this is the origin square
	 * indexes a bit mask of the squares that the origin square can attack by a Black Pawn capture */
	public static final long[] AdjacentPawnFromPosition_Lut = new long[128];
	static {
		for (int square : Position.values) {
			AdjacentPawnFromPosition_Lut[square] = createAdjacentPawnsFromSq(square);
		}
	}
	static long createAdjacentPawnsFromSq(int atPos) {
		long mask = 0;
		if (Position.getRank(atPos) != 7 && Position.getRank(atPos) != 0) {
			int sq = Direction.getDirectMoveSq(Direction.right, atPos);
			if (sq != Position.NOPOSITION) {
				mask |= BitBoard.positionToMask_Lut[sq];
			}
			sq = Direction.getDirectMoveSq(Direction.left, atPos);
			if (sq != Position.NOPOSITION) {
				mask |= BitBoard.positionToMask_Lut[sq];
			}
		}
		return mask;
	}
}
