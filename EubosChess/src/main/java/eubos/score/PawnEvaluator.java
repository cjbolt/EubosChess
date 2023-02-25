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
	
	private static final boolean MEASURE_PAWN_HASH = false;
	private static final boolean ENABLE_MAJOR_SUPPORT_EVAL_FOR_PASSED_PAWN = true;
	
	public static final int DOUBLED_PAWN_HANDICAP = 12;
	public static final int ISOLATED_PAWN_HANDICAP = 33;
	public static final int BACKWARD_PAWN_HANDICAP = 12;
	public static final int[] NO_PAWNS_HANDICAP_LUT = {0, 50, 75, 100, 200, 300, 500, 500, 500};
	public static final int[] PASSED_PAWN_IMBALANCE_LUT = {0, 15, 65, 110, 160, 300, 450, 700, 900};
	
	public static final int PASSED_PAWN_BOOST = 10;
	public static final int ROOK_FILE_PASSED_PAWN_BOOST = 8;
	public static final int CANDIDATE_PAWN = 5;
	public static final int ROOK_FILE_CANDIDATE_PAWN = 3;
	public static final int SAFE_MOBILE_PASSED_PAWN = 17;
	public static final int MOBILE_PASSED_PAWN = 5;
	public static final int CONNECTED_PASSED_PAWN_BOOST = 30;
	public static final int HEAVY_PIECE_BEHIND_PASSED_PAWN = 50;
	
	public static final boolean ENABLE_PAWN_HASH_TABLE = true;
	public static final boolean ENABLE_KPK_EVALUATION = true;
	public static final boolean ENABLE_CANDIDATE_PP_EVALUATION = true;
	public static final boolean ENABLE_PP_IMBALANCE_EVALUATION = true;	
	
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
			if (EubosEngineMain.ENABLE_LOGGING)
				EubosEngineMain.logger.info(String.format("PawnStats nodes=%d skipped=%d percentage=%.5f",
				        nodeCount, skippedCount, ((double)skippedCount)*100.0d/((double)nodeCount)));
		}
	}
	
	public void reportPawnHashStatistics() {
		if (MEASURE_PAWN_HASH) {
			pawnStat.report();
		}
	}
	
	PawnHashStatistics pawnStat = null;
	
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
	
	protected void setQueeningDistance(int bitOffset, boolean pawnIsWhite) {
		int rank = BitBoard.getRank(bitOffset);
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
	public void callback(int piece, int bitOffset) {
		/* All of the evaluation performed in this callback doesn't depend on the wider position or attacks mask,
		 * it can only be based on the pawn bitboard. This is because the result of this evaluation is stored in
		 * the pawn eval hash table and as such has to be valid for any position with the same pawn structure. */
		boolean pawnIsWhite = Piece.isWhite(piece);
		
		long passers = bd.getPassedPawns();
		long mask = 1L << bitOffset;
		if ((passers & mask) != 0L) {
			// check for directly supported and adjacent passed pawns
			long directConnectedPawns = pawnIsWhite ? SquareAttackEvaluator.WhitePawnAttacksFromPosition_Lut[bitOffset] : 
				                                SquareAttackEvaluator.BlackPawnAttacksFromPosition_Lut[bitOffset];
			directConnectedPawns |= AdjacentPawnFromPosition_Lut[bitOffset];
			long ownPassers = passers & (pawnIsWhite ? bd.getWhitePawns() : bd.getBlackPawns());
			piecewisePawnScoreAccumulator += Long.bitCount(ownPassers & directConnectedPawns) * CONNECTED_PASSED_PAWN_BOOST;
				
			// check for otherwise connected passed pawns, which could be supported
			piecewisePawnScoreAccumulator += Long.bitCount(ownPassers & BitBoard.PasserSupport_Lut[pawnIsWhite ? 0 : 1][bitOffset]) * CONNECTED_PASSED_PAWN_BOOST/2;
		} else if (ENABLE_CANDIDATE_PP_EVALUATION) {
			long[][] enemy_attacks = attacks[pawnIsWhite ? 1:0];
			long[][] own_attacks = attacks[pawnIsWhite ? 0:1];
			
			if (bd.isCandidatePassedPawn(bitOffset, pawnIsWhite, own_attacks[0], enemy_attacks[0])) {
				setQueeningDistance(bitOffset, pawnIsWhite);
				weighting *= getScaleFactorForGamePhase();
				if (BitBoard.getFile(bitOffset) == IntFile.Fa || BitBoard.getFile(bitOffset) == IntFile.Fh) {
					piecewisePawnScoreAccumulator += weighting*ROOK_FILE_CANDIDATE_PAWN;
				} else {
					piecewisePawnScoreAccumulator += weighting*CANDIDATE_PAWN;
				}
			}
		}
		if (bd.isIsolatedPawn(bitOffset, pawnIsWhite)) {
			piecewisePawnScoreAccumulator -= ISOLATED_PAWN_HANDICAP;
		} else if (bd.isBackwardsPawn(bitOffset, pawnIsWhite)) {
			piecewisePawnScoreAccumulator -= BACKWARD_PAWN_HANDICAP;
		}
	}
	
	@Override
	public boolean condition_callback(int piece, int atPos) {
		return bd.isPassedPawn(BitBoard.positionToBit_Lut[atPos], BitBoard.positionToMask_Lut[atPos]);
	}
	
	public int getDoubledPawnsHandicap(long pawns) {
		return -bd.countDoubledPawns(pawns)*DOUBLED_PAWN_HANDICAP;
	}
		
	int evaluatePawnsForSide(long pawns, boolean isBlack) {
		int pawnEvaluationScore = 0;
		piecewisePawnScoreAccumulator = 0;
		if (pawns != 0x0) {
			int pawnHandicap = getDoubledPawnsHandicap(pawns);
			bd.forEachPawnOfSide(this, isBlack);
			pawnEvaluationScore = pawnHandicap + piecewisePawnScoreAccumulator;
		} else {
			int num_enemy_pawns = bd.me.numberOfPieces[isBlack ? Piece.WHITE_PAWN : Piece.BLACK_PAWN];
			pawnEvaluationScore -= NO_PAWNS_HANDICAP_LUT[num_enemy_pawns];
		}
		return pawnEvaluationScore;
	}
	
	protected int evaluateKpkEndgame(int bitOffset, boolean pawnIsWhite, boolean isOwnPawn, long[][] ownAttacks) {
		// Special case, it is a KPK endgame
		int score = 0;
		int file = BitBoard.getFile(bitOffset);
		int queeningSquare = pawnIsWhite ? file+56 : file;
		int oppoKingPos = bd.getKingPosition(!pawnIsWhite);
		int oppoDistance = BitBoard.ManhattanDistance[queeningSquare][oppoKingPos];
		if (!isOwnPawn) {
			// if king is on move, assume it can get towards the square of the pawn
			oppoDistance -= 1;
		}
		if (oppoDistance > queeningDistance) {
			// can't be caught by opposite king, as outside square of pawn
			score = 700;
		} else {
			if (bd.isFrontspanControlledInKpk(bitOffset, pawnIsWhite, ownAttacks[3])) {
				// Rationale is whole frontspan can be blocked off from opposite King by own King
				score = 700;
			} else {
				// increase score also if we think the pawn can be defended by own king
				int ownKingPos = bd.getKingPosition(pawnIsWhite);
				int ownDistance = BitBoard.ManhattanDistance[queeningSquare][ownKingPos];
				if (ownDistance-1 <= oppoDistance) {
					score = 300;
				}
			}
		}
		return score;
	}
	
	//public final int[] KING_DIST_LUT = {0, 3, 2, 1, 0, -1, -2, -3, -4};
	//public final int[] KING_DIST_LUT = {0, 0, 0, 0, 0, 0, 0, 0, 0};
	protected int evaluatePassedPawn(int bitOffset, boolean pawnIsWhite, long[][] own_attacks, long [][] enemy_attacks) {
		int score = 0;
		boolean heavySupport = false;
		weighting *= getScaleFactorForGamePhase();
		int value = (BitBoard.getFile(bitOffset) == IntFile.Fa || BitBoard.getFile(bitOffset) == IntFile.Fh) ?
				ROOK_FILE_PASSED_PAWN_BOOST : PASSED_PAWN_BOOST;
		
		if (!bd.isPawnBlockaded(bitOffset, pawnIsWhite)) {
			if (ENABLE_MAJOR_SUPPORT_EVAL_FOR_PASSED_PAWN) {
				int heavySupportIndication = bd.checkForHeavyPieceBehindPassedPawn(bitOffset, pawnIsWhite);
				if (heavySupportIndication > 0) {
					score += HEAVY_PIECE_BEHIND_PASSED_PAWN;
					heavySupport = true;
				} else if (heavySupportIndication < 0) {
					score -= HEAVY_PIECE_BEHIND_PASSED_PAWN;
				} else {
					// neither attacked or defended along the rear span
				}
			}
			if (bd.isPawnFrontspanSafe(bitOffset, pawnIsWhite, own_attacks[3], enemy_attacks[3], heavySupport)) {
				value += SAFE_MOBILE_PASSED_PAWN;
			} else if (bd.canPawnAdvance(bitOffset, pawnIsWhite, own_attacks[3], enemy_attacks[3])) {
				value += MOBILE_PASSED_PAWN;
			}
		}
// Experimental King proximity to passed pawn evalution
//		int ownKingPos = bd.getKingPosition(pawnIsWhite);
//		if (ownKingPos != BitBoard.INVALID) {
//			int ownDistance = BitBoard.ManhattanDistance[bitOffset][ownKingPos];
//			value += KING_DIST_LUT[ownDistance];
//		}
		score += weighting*value;
		return score;
	}
	
	int computePassedPawnContribution() {
		int scoreForPassedPawns = 0;
		long white = bd.getWhitePawns();
		long scratchBitBoard = bd.getPassedPawns();
		
		while ( scratchBitBoard != 0x0L ) {
			int score = 0;
			
			int bit_offset = BitBoard.convertToBitOffset(scratchBitBoard);
			long bitMask = 1L << bit_offset;
			
			boolean pawnIsWhite = (white & bitMask) == bitMask;
			long[][] enemy_attacks = attacks[pawnIsWhite ? 1:0];
			long[][] own_attacks = attacks[pawnIsWhite ? 0:1];
			
			setQueeningDistance(bit_offset, pawnIsWhite);
			if (ENABLE_KPK_EVALUATION && bd.me.phase == 4096) {
				score = evaluateKpkEndgame(bit_offset, pawnIsWhite, (pawnIsWhite == onMoveIsWhite), own_attacks);
			} else {
				score = evaluatePassedPawn(bit_offset, pawnIsWhite, own_attacks, enemy_attacks);
			}
			if (pawnIsWhite == onMoveIsWhite) {
				scoreForPassedPawns += score;
			} else {
				scoreForPassedPawns -= score;
			}
			
			// clear the lssb
			scratchBitBoard ^= bitMask;
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
			int ppImbalanceFactor = PASSED_PAWN_IMBALANCE_LUT[Math.abs(lookupIndex)];
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
	public static final long[] AdjacentPawnFromPosition_Lut = new long[64];
	static {
		int bitOffset = 0;
		for (int square : Position.values) {
			AdjacentPawnFromPosition_Lut[bitOffset++] = createAdjacentPawnsFromSq(square);
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
