package eubos.score;

import eubos.board.BitBoard;
import eubos.board.Board;
import eubos.board.Piece;
import eubos.board.SquareAttackEvaluator;
import eubos.main.EubosEngineMain;
import eubos.neural_net.NNUE;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.PositionManager;

public class PositionEvaluator implements IEvaluate {
	
	public static final boolean ENABLE_THREAT_EVALUATION = true;

	/* The threshold for lazy evaluation was tuned by empirical evidence collected from
	running with the logging in TUNE_LAZY_EVAL for Eubos2.14 and post processing the logs.
	It will need to be re-tuned if the evaluation function is altered significantly. */
	public static int lazy_eval_threshold_cp = 275;
	private static final int BISHOP_PAIR_BOOST = 25;
	
	private IPositionAccessors pm;
	private boolean onMoveIsWhite;
	private boolean goForMate;
	private int midgameScore = 0;
	private int endgameScore = 0;
	private boolean isDraw;
	private int score;
	private Board bd;
	
	boolean passedPawnPresent;
	
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
		onMoveIsWhite = pm.onMoveIsWhite();
		isDraw = false;
		passedPawnPresent = bd.isPassedPawnPresent();
		score = 0;
		midgameScore = 0;
		endgameScore = 0;
	}
	
	private void initialise() {
		basicInit();
		isDraw = pm.isThreefoldRepetitionPossible();
		if (!isDraw) {
			isDraw = pm.isInsufficientMaterial();
		}
	}
	
	private short taperEvaluation(int midgameScore, int endgameScore) {
		int phase = 0;
		return (short)(((midgameScore * (4096 - phase)) + (endgameScore * phase)) / 4096);
	}
	
	private void doMaterialAndPst() {}
	
	private int evaluateThreatsForSide(long[][][] attacks, boolean onMoveIsWhite) {
		int threatScore = 0;
		long own = onMoveIsWhite ? bd.getWhitePieces() : bd.getBlackPieces();
		own &= ~(onMoveIsWhite ? bd.getWhiteKing() : bd.getBlackKing()); // Don't include King in this evaluation
		// if a piece is attacked and not defended, add a penalty
		long enemyAttacks = attacks[onMoveIsWhite ? 1 : 0][3][0];
		long ownAttacks = attacks[onMoveIsWhite ? 0 : 1][3][0];
		long attackedOnlyByEnemy = enemyAttacks & ~ownAttacks;
		long scratchBitBoard = own & attackedOnlyByEnemy;
		int bit_offset;
		while (scratchBitBoard != 0L && (bit_offset = BitBoard.convertToBitOffset(scratchBitBoard)) != BitBoard.INVALID) {		
			// Add a penalty based on the value of the piece not defended, 50% of piece value
			threatScore -= Math.abs((Piece.PIECE_TO_MATERIAL_LUT[0][bd.getPieceAtSquare(scratchBitBoard)] / 2));
			scratchBitBoard ^= (1L << bit_offset);
		}
		return threatScore;
	}
	
	private int evaluateBishopPair() {
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
	
	private int internalCrudeEval() {
		// Initialised in lazyEvaluation function
		if (!isDraw) {
			if (EubosEngineMain.ENABLE_NEURAL_NET_EVAL) {
				score = neural_net_eval();
			} else {
				// Add phase specific static mobility (PSTs)
				doMaterialAndPst();
				score = taperEvaluation(midgameScore, endgameScore);
				score += evaluateBishopPair();
			}
		}
		return score;
	}
	
	private int internalFullEval() {
		if (!isDraw)
			score = neural_net_eval();
		return score;
	}
	
	int neural_net_eval() {
		NNUE network = new NNUE((PositionManager) this.pm);
		return network.evaluate();
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
		return internalCrudeEval();
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
		if (EubosEngineMain.ENABLE_NEURAL_NET_EVAL) {
			return neural_net_eval();
		} else {
			return passedPawnPresent ? internalFullEval() : internalCrudeEval(); 
		}
	}
	
	public int evaluateThreats(long[][][] attacks, boolean onMoveIsWhite) {
		int threatScore = 0;
		if (ENABLE_THREAT_EVALUATION) {
			threatScore = evaluateThreatsForSide(attacks, onMoveIsWhite);
			threatScore -= evaluateThreatsForSide(attacks, !onMoveIsWhite);
		}
		return threatScore;
	}
	
	boolean isKingExposed() {
		int kingBitOffset = bd.getKingPosition(onMoveIsWhite);
		// Only meant to cater for quite extreme situations
		long kingZone = SquareAttackEvaluator.KingZone_Lut[onMoveIsWhite ? 0 : 1][kingBitOffset];
		kingZone =  onMoveIsWhite ? kingZone >>> 8 : kingZone << 8;
		long defenders = onMoveIsWhite ? bd.getWhitePieces() : bd.getBlackPieces();
		int defenderCount = Long.bitCount(kingZone&defenders);

		int attackQueenOffset = bd.getQueenPosition(!onMoveIsWhite);
		if (attackQueenOffset != BitBoard.INVALID) {
			int attackingQueenDistance = BitBoard.ManhattanDistance[attackQueenOffset][kingBitOffset];
			return (defenderCount < 3 || attackingQueenDistance < 3);
		} else {
			return defenderCount < 3;
		}
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
	
	public boolean goForMate() {
		return goForMate;
	}
	
	public PositionEvaluator(IPositionAccessors pm, PawnEvalHashTable pawnHash) {	
		this.pm = pm;
		bd = pm.getTheBoard();
		// If either side can't win (e.g. bare King) then do a mate search.
		goForMate = ((Long.bitCount(bd.getBlackPieces()) == 1) || 
				     (Long.bitCount(bd.getWhitePieces()) == 1));
		initialise();
	}
}
