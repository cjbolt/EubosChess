package eubos.score;

import com.fluxchess.jcpi.models.IntFile;

import eubos.board.BitBoard;
import eubos.board.Board;
import eubos.board.IForEachPieceCallback;
import eubos.board.Piece;
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
	public static final boolean ENABLE_PP_IMBALANCE_EVALUATION = false;
	
	private boolean onMoveIsWhite;
	public int piecewisePawnScoreAccumulator = 0;
	public long[][][] attacks;
	protected int queeningDistance;
	protected int weighting;
	protected boolean pawnIsBlack;
	protected int[] ppCount = {0,0};
	protected int ppFileMask = 0;
	protected int ppRankMask = 0;
	IPositionAccessors pm;
	Board bd;
	private PawnEvalHashTable pawnHash;
	
	public final int[] ppImbalanceTable = {0, 15, 200, 400, 700, 900, 900, 900, 900};
	
	public PawnEvaluator(IPositionAccessors pm, PawnEvalHashTable pawnHash) {
		bd = pm.getTheBoard();
		this.pm = pm;
		this.pawnHash = pawnHash;
		this.onMoveIsWhite = pm.onMoveIsWhite();
	}
	
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
	
	@SuppressWarnings("unused")
	@Override
	public void callback(int piece, int atPos) {
		boolean pawnIsWhite = Piece.isWhite(piece);
		long[][] enemy_attacks = attacks[pawnIsWhite ? 1:0];
		long[][] own_attacks = attacks[pawnIsWhite ? 0:1];
		
		if (bd.isPassedPawn(atPos, pawnIsWhite)) {
			boolean isOwnPawn = (onMoveIsWhite && pawnIsWhite) || (!onMoveIsWhite && !pawnIsWhite);
			ppCount[isOwnPawn ? 0:1] += 1;
			ppFileMask |= (1 << Position.getFile(atPos));
			ppRankMask |= (1 << Position.getRank(atPos));
		} else if (ENABLE_CANDIDATE_PP_EVALUATION) {
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
	
	public int getNumAdjacentPassedPawns(int fileMask) {
		if (fileMask == 0) return 0;
		int left = ((fileMask & 0xAA) >> 1) + (fileMask & 0x55);
		int right = (fileMask & 0x54) + ((fileMask & 0x2A) << 1);
		right &= 0xA8;
		right >>= 1;
		left &= 0xAA;
		return Long.bitCount(left | right);
	}
	
	public int evaluateConnectedPassedPawns()
	{
		int score = 0;
		int numAdjacentPassedPawns = getNumAdjacentPassedPawns(ppFileMask);
		if (numAdjacentPassedPawns > 0) {
			// Simplification, if many passed pawns it can fail 
			int adjacentRanks = getNumAdjacentPassedPawns(ppRankMask);
			if (adjacentRanks > 0 || Long.bitCount(ppRankMask) == 1) {
				score = numAdjacentPassedPawns * CONNECTED_PASSED_PAWN_BOOST;
			} else {
				score = CONNECTED_PASSED_PAWN_BOOST/2;
			}
		}
		return score;
	}
	
	public int evaluatePawnsForSide(long pawns, boolean isBlack) {
		int pawnEvaluationScore = 0;
		ppFileMask = ppRankMask = 0;
		if (pawns != 0x0) {
			piecewisePawnScoreAccumulator = 0;
			int pawnHandicap = getDoubledPawnsHandicap(pawns);
			bd.forEachPawnOfSide(this, isBlack);
			pawnEvaluationScore = pawnHandicap + piecewisePawnScoreAccumulator;
			pawnEvaluationScore += evaluateConnectedPassedPawns();
		} else {
			pawnEvaluationScore -= NO_PAWNS_HANDICAP;
		}
		return pawnEvaluationScore;
	}
	
	void initialise(long[][][] attacks) {
		onMoveIsWhite = pm.onMoveIsWhite();
		ppCount[0] = ppCount[1] = 0;
		this.attacks = attacks;
	}
	
	protected int evaluateKpkEndgame(int atPos, boolean isOwnPawn, long[][] ownAttacks) {
		// Special case, it is a KPK endgame
		int score = 0;
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
			score = 700;
		} else {
			if (bd.isFrontspanControlledInKpk(atPos, !pawnIsBlack, ownAttacks[3])) {
				// Rationale is whole frontspan can be blocked off from opposite King by own King
				score = 700;
			} else {
				// increase score also if we think the pawn can be defended by own king
				int ownKingPos = bd.getKingPosition(!pawnIsBlack);
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
	
	int computePassedPawnContribution(boolean isForWhite) {
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
				score = evaluateKpkEndgame(pawn_position, (pawnIsWhite == isForWhite), own_attacks);
			} else {
				score = evaluatePassedPawn(pawn_position, pawnIsWhite, own_attacks, enemy_attacks);
			}
			if (pawnIsWhite == isForWhite) {
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
		
		initialise(attacks);
	
		short hashEval = 0;
		int passedPawnScoreAtPosition = 0;
		if (ENABLE_PAWN_HASH_TABLE) {
			hashEval = pawnHash.get(pm.getPawnHash(), getScaleFactorForGamePhase(), white, black, onMoveIsWhite);
			if (hashEval != Short.MAX_VALUE) {
				// Recompute value of passed pawns in this position
				passedPawnScoreAtPosition = computePassedPawnContribution(onMoveIsWhite);
				return hashEval + passedPawnScoreAtPosition;
			}
		}
		
		// If no valid hash, recompute from scratch...
		long ownPawns = onMoveIsWhite ? white : black;
		long enemyPawns = onMoveIsWhite ? black : white;
		int pawnEvaluationScore = evaluatePawnsForSide(ownPawns, !onMoveIsWhite);
		pawnEvaluationScore -= evaluatePawnsForSide(enemyPawns, onMoveIsWhite);
		
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
		
		if (ENABLE_PAWN_HASH_TABLE) {
			pawnHash.put(pm.getPawnHash(), getScaleFactorForGamePhase(), pawnEvaluationScore, white, black, onMoveIsWhite);
		}
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			if (hashEval != Short.MAX_VALUE)
				assert pawnEvaluationScore == hashEval : 
					String.format("pawn score before passed pawn positions: %d != %d %s", pawnEvaluationScore, hashEval, pm.unwindMoveStack());
		}
		
		// Compute passed pawn positional contribution after storing the basic eval to the hash table
		piecewisePawnScoreAccumulator = 0;
		pawnEvaluationScore += computePassedPawnContribution(onMoveIsWhite);
		
		if (EubosEngineMain.ENABLE_ASSERTS) {
			if (hashEval != Short.MAX_VALUE)
				assert pawnEvaluationScore == hashEval+passedPawnScoreAtPosition :
					String.format("pawn score after pp position: %d != %d", pawnEvaluationScore, hashEval+passedPawnScoreAtPosition);
		}
		
		return pawnEvaluationScore;
	}
}
