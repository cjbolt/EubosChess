package eubos.score;

import static eubos.score.PositionEvaluator.DOUBLED_PAWN_HANDICAP;
import static eubos.score.PositionEvaluator.PASSED_PAWN_BOOST;
import static eubos.score.PositionEvaluator.ROOK_FILE_PASSED_PAWN_BOOST;
import static eubos.score.PositionEvaluator.ISOLATED_PAWN_HANDICAP;
import static eubos.score.PositionEvaluator.BACKWARD_PAWN_HANDICAP;
import static eubos.score.PositionEvaluator.NO_PAWNS_HANDICAP;
import static eubos.score.PositionEvaluator.HEAVY_PIECE_BEHIND_PASSED_PAWN;
import static eubos.score.PositionEvaluator.SAFE_MOBILE_PASSED_PAWN;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import eubos.board.Piece;
import eubos.position.Position;
import eubos.position.PositionManager;
import eubos.search.DrawChecker;

public class PawnEvaluatorTest {

	PositionEvaluator SUT;
	PositionManager pm;
	long [][][] attacks;
	long whitePawns;
	long blackPawns;
	
	@Before
	public void setUp() throws Exception {
	}

	protected void setUpPosition(String fen) {
		pm = new PositionManager(fen, new DrawChecker());
		SUT = (PositionEvaluator) pm.getPositionEvaluator();
		attacks = pm.getTheBoard().calculateCountedAttacksAndMobility(pm.getTheBoard().me);
		whitePawns = pm.getTheBoard().getWhitePawns();
		blackPawns = pm.getTheBoard().getBlackPawns();
	}

	@Test
	public void test_DiscourageDoubledPawns_w() {
		setUpPosition("8/pppppp2/8/8/8/1P2P3/1P1P2PP/8 b - - 0 1");
		int score = SUT.pawn_eval.getDoubledPawnsHandicap(whitePawns);
		assertEquals(-DOUBLED_PAWN_HANDICAP, score);
	}
	
	@Test
	public void test_DiscourageDoubledPawns_b() {
		setUpPosition("8/pp2p1p1/3p2p1/8/8/8/2PPPPPP/8 w - - 0 1 ");
		int score = SUT.pawn_eval.getDoubledPawnsHandicap(blackPawns);
		assertEquals(-DOUBLED_PAWN_HANDICAP, score);
	}
	
	@Test
	public void test_DiscourageTripledPawns_w() {
		setUpPosition("8/8/8/8/2P5/2P5/2P5/8 w - - 0 38 ");
		int score = SUT.pawn_eval.getDoubledPawnsHandicap(whitePawns);
		assertEquals(-2*DOUBLED_PAWN_HANDICAP, score);
	}

	@Test
	public void test_DiscourageTripledPawns_b() {
		setUpPosition("8/8/8/8/2p5/2p5/2p5/8 w - - 0 38 ");
		int score = SUT.pawn_eval.getDoubledPawnsHandicap(blackPawns);
		assertEquals(-2*DOUBLED_PAWN_HANDICAP, score);
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnE6() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/3pp3/8/3p4/8/2P5/8 b - - 0 1 ");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.BLACK_PAWN, Position.e6);
		int expectedScore = ((7-5)*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN)) - BACKWARD_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/3pp3/8/8/8/2Pp4/8 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.BLACK_PAWN, Position.d2);
		int expectedScore = (7-1)*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN);
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_discourageBackwardsPawns_forBlack_OnD6() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/3pp3/8/8/8/2Pp4/8 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.BLACK_PAWN, Position.d6);
		assertEquals(-BACKWARD_PAWN_HANDICAP, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_discourageIsolatedPawns_forWhite_OnE2() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/3pp3/8/8/8/2Pp4/8 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.WHITE_PAWN, Position.e2);
		assertEquals(-ISOLATED_PAWN_HANDICAP, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_fullEval_TwoPassedPawnsForBlack() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/3pp3/8/8/8/2Pp4/8 b - - 0 1");
		int score = SUT.pawn_eval.evaluatePawnStructure(attacks);
		// black
		int expectedScore = 0;
		expectedScore += (7-1)*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN); // black d pawn about to queen
		expectedScore += ((7-5)*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) - BACKWARD_PAWN_HANDICAP); // black e pawn 6th rank
		expectedScore += -BACKWARD_PAWN_HANDICAP; // black d pawn 6th rank
		expectedScore += -DOUBLED_PAWN_HANDICAP; // black doubled pawns
		// white
		expectedScore += ISOLATED_PAWN_HANDICAP;
		if (PositionEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += 200;
		}
		assertEquals(expectedScore, score);
	}
	
	@Test
	public void test_encouragePassedPawns_ForWhite_ppOnD7() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("8/2pPp3/8/2P1P3/8/8/8/7n w - - 0 1 ");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.WHITE_PAWN, Position.d7);
		int expectedScore = 6*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN);
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_fullEval_encouragePassedPawns_ForWhite() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("8/2pPp3/8/2P1P3/8/8/8/7n w - - 0 1 ");
		int score = SUT.pawn_eval.evaluatePawnStructure(attacks);
		// white
		int expectedScore = 0;
		expectedScore += 6*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN);
		expectedScore += -BACKWARD_PAWN_HANDICAP;
		expectedScore += -BACKWARD_PAWN_HANDICAP;
		// white
		expectedScore -= -ISOLATED_PAWN_HANDICAP;
		expectedScore -= -ISOLATED_PAWN_HANDICAP;
		if (PositionEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += 15;
		}
		assertEquals(expectedScore, score);
	}

	@Test
	public void test_encouragePassedPawns_forBlack_ppOnH6_rookFile() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/3p3p/8/8/8/2Pp4/8 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.BLACK_PAWN, Position.h6);
		int expectedScore = (7-5)*3*(ROOK_FILE_PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_crazyPassedPawnScenario() {
		setUpPosition("3r2k1/1Nr2pp1/1n3n2/1P1P2q1/3P4/1pP2p1p/1K6/R3QB1R b - - 0 29 ");
		int score = SUT.pawn_eval.evaluatePawnStructure(attacks);
		assertEquals(-15, score);
	} 
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn() {
		setUpPosition("n7/8/8/8/8/5p2/6P1/8 w - - 0 1 ");
		int score = SUT.pawn_eval.evaluatePawnStructure(attacks);
		assertEquals(-96 /* two candidate passed pawns, blacks is more valuable, but white can take it on the next move! */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn1() {
		setUpPosition("n7/8/8/8/5p2/8/6P1/8 w - - 0 1 ");
		int score = SUT.pawn_eval.evaluatePawnStructure(attacks);
		assertEquals(0 /* no passed f pawn, fully passed */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn2() {
		setUpPosition("n7/8/8/8/6p1/8/6P1/8 w - - 0 1 ");
		int score = SUT.pawn_eval.evaluatePawnStructure(attacks);
		assertEquals(0 /* blocked g pawn not passed */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn2_w() {
		setUpPosition("n7/8/8/8/6p1/8/6P1/8 b - - 0 1 ");
		int score = SUT.pawn_eval.evaluatePawnStructure(attacks);
		assertEquals(0 /* blocked g pawn not passed */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_BothPassedPawns() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/8/8/6P1/8/6p1/8 b - - 0 1 ");
		int score = SUT.pawn_eval.evaluatePawnStructure(attacks);
		/* both pawns on the same file, passed, white at 3rd rank, black at 1st rank. */
		int expected_eval = (((7-3)*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN))-ISOLATED_PAWN_HANDICAP)-(1*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN)-ISOLATED_PAWN_HANDICAP);
		assertEquals(expected_eval, score);
	}
	 
	@Test
	public void test_encouragePassedPawns_CandidatePasserAtB5() {
		setUpPosition("8/p7/8/PP6/8/8/8/8 w - - 0 1");
		int score = SUT.pawn_eval.evaluatePawnStructure(attacks);
		assertEquals(105 /* b5 pawn will queen, not including material */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_CandidatePasserAtB5NotSupportedByDefender() {
		setUpPosition("8/p7/8/1P6/8/8/8/8 w - - 0 1");
		int score = SUT.pawn_eval.evaluatePawnStructure(attacks);
		assertEquals(0 /* b5 pawn will not queen, because b6 is not defended by our pawn, not including material */, score);
	}
	
	@Test
	public void test_won_KP_endgame_oppo_outside_square_of_pawn() {
		if (PositionEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/k7/6K1/8/4p3/8/8 w - - 0 1");
			int expectedScore = -817;
			if (PositionEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= 15;
			}
			assertEquals(expectedScore /* passed pawn on second rank, can't be caught */, SUT.getFullEvaluation());
		}
	}
	
	@Test
	public void test_drawn_KP_endgame_1() {
		if (PositionEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/k7/8/7K/4p3/8/8 w - - 0 1 ");
			System.out.println(SUT.getFullEvaluation());
			int expectedScore = -117;
			if (PositionEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= 15;
			}
			assertEquals(expectedScore /* passed pawn on second rank, can be caught */, SUT.getFullEvaluation());
		}
	}
	
	@Test
	public void test_drawn_KP_endgame() {
		if (PositionEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/k7/8/8/4p1K1/8/8 w - - 0 1");
			int expectedScore = -117;
			if (PositionEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= 15;
			}
			assertEquals(expectedScore /* passed pawn on second rank, can be caught */, SUT.getFullEvaluation());
		}
	}
	
	@Test
	public void test_won_KP_endgame_defended_by_own_king() {
		if (PositionEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/8/8/7K/4p3/5k2/8 w - - 0 1");
			int expectedScore = -817;
			if (PositionEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= 15;
			}
			assertEquals(expectedScore /* own king can block enemy king */, SUT.getFullEvaluation());
		}
	}
	
	@Test
	public void test_won_KP_endgame_one() {
		if (PositionEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/8/8/8/2k1p1K1/8/8 w - - 44 1");
			/* In this test, the white king cannot get to the queening square in time,
			   because it is blocked by the square that the pawn is attacking, at f2. */
			int expectedScore = -417;
			if (PositionEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= 15;
			}
			assertEquals(expectedScore /* probably unstoppable passer */, SUT.getFullEvaluation());
		}
	}
	
	@Test
	public void test_won_KP_endgame_two() {
		if (PositionEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/8/8/8/2k1p3/6K1/8 w - - 44 1");
			int expectedScore = -417;
			if (PositionEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= 15;
			}
			assertEquals(expectedScore /* probably unstoppable passer */, SUT.getFullEvaluation());
		}
	}
	
	@Test
	public void test_won_KP_endgame_three() {
		if (PositionEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/8/8/8/1k2p3/6K1/8 b - - 44 1");
			int expectedScore = 417;
			if (PositionEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore += 15;
			}
			assertEquals(expectedScore /* passed pawn */, SUT.getFullEvaluation());
		}
	}
	
	@Test
	public void test_drawn_KP_endgame_one() {
		if (PositionEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/8/8/8/2k1p3/8/5K2 w - - 44 1");
			int expectedScore = -117;
			if (PositionEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= 15;
			}
			assertEquals(expectedScore /* passed pawn */, SUT.getFullEvaluation());
		}
	}
	
	@Test
	public void test_drawn_KP_endgame_two() {
		if (PositionEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/8/8/8/1k2p3/6K1/8 w - - 44 1");
			int expectedScore = -117;
			if (PositionEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= 15;
			}
			assertEquals(expectedScore /* passed pawn */, SUT.getFullEvaluation());
		}
	}
	
	@Test
	public void test_won_KP_endgame_oppo_outside_square_of_pawn_white() {
		if (PositionEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/4P3/8/6k1/K7/8/8 b - - 0 1 ");
			int expectedScore = -817;
			if (PositionEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= 15;
			}
			assertEquals(expectedScore /* passed pawn on second rank, can't be caught */, SUT.getFullEvaluation());
		}
	}
	
	@Test
	public void test_drawn_KP_endgame_1_white() {
		if (PositionEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/4P3/7k/8/K7/8/8 b - - 0 1 ");
			System.out.println(SUT.getFullEvaluation());
			int expectedScore = -117;
			if (PositionEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= 15;
			}
			assertEquals(expectedScore /* passed pawn on second rank, can be caught */, SUT.getFullEvaluation());
		}
	}
	
	@Test
	public void test_drawn_KP_endgame_white() {
		if (PositionEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/4P1k1/8/8/K7/8/8 b - - 0 1 ");
			int expectedScore = -117;
			if (PositionEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= 15;
			}
			assertEquals(expectedScore /* passed pawn on second rank, can be caught */, SUT.getFullEvaluation());
		}
	}
	
	@Test
	public void test_won_KP_endgame_defended_by_own_king_white() {
		if (PositionEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/5K2/4P3/7k/8/8/8/8 b - - 0 1 ");
			int expectedScore = -817;
			if (PositionEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= 15;
			}
			assertEquals(expectedScore /* own king can block enemy king */, SUT.getFullEvaluation());
		}
	}
	
	@Test
	public void test_fullEval_WhenNoPawns() {
		setUpPosition("8/8/8/8/8/8/8/8 b - - 0 1");
		int score = SUT.pawn_eval.evaluatePawnStructure(attacks);
		assertEquals(0, score);
	}
	
	@Test
	public void test_fullEval_OneWhitePawn() {
		setUpPosition("n7/8/8/8/8/8/7P/8 b - - 0 1");
		int score = SUT.pawn_eval.evaluatePawnStructure(attacks);
		// white
		int expectedScore = 0;
		expectedScore += -(1*3*(ROOK_FILE_PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) + -ISOLATED_PAWN_HANDICAP); // white has an h file passed pawn
		expectedScore += -NO_PAWNS_HANDICAP; // black has no pawns
		assertEquals(expectedScore, score);
	}
	
	@Test
	public void test_fullEval_OneBlackPawn() {
		setUpPosition("n7/7p/8/8/8/8/8/8 b - - 0 1");
		int score = SUT.pawn_eval.evaluatePawnStructure(attacks);
		// white
		int expectedScore = 0;
		expectedScore += (1*3*(ROOK_FILE_PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) + -ISOLATED_PAWN_HANDICAP); // black has an h file passed pawn
		expectedScore += NO_PAWNS_HANDICAP; // white has no pawns
		assertEquals(expectedScore, score);
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2_rookSupport() {
		setUpPosition("3r4/8/8/8/8/8/3p4/8 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.BLACK_PAWN, Position.d2);
		int expectedScore = (7-1)*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) + HEAVY_PIECE_BEHIND_PASSED_PAWN - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2_rookSupportBlocked() {
		setUpPosition("3r4/8/8/3p4/8/8/3p4/8 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.BLACK_PAWN, Position.d2);
		int expectedScore = (7-1)*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2_rookSupportNotBehindPawn() {
		setUpPosition("8/8/8/3p4/8/8/r2p4/8 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.BLACK_PAWN, Position.d2);
		int expectedScore = (7-1)*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2_rookSupportInFront() {
		setUpPosition("8/8/8/3p4/8/8/3p4/3r4 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.BLACK_PAWN, Position.d2);
		int expectedScore = (7-1)*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookSupport() {
		setUpPosition("8/8/8/1P6/1R6/8/8/8 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.WHITE_PAWN, Position.b5);
		int expectedScore = 4*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) + HEAVY_PIECE_BEHIND_PASSED_PAWN - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookSupportBlocked() {
		setUpPosition("8/8/8/1P6/1K6/8/8/1R6 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.WHITE_PAWN, Position.b5);
		int expectedScore = 4*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookSupportNotBehindPawn() {
		setUpPosition("8/8/8/1P5R/8/8/8/8 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.WHITE_PAWN, Position.b5);
		int expectedScore = 4*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookSupportInFront() {
		setUpPosition("1R6/8/8/1P6/8/8/8/8 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.WHITE_PAWN, Position.b5);
		int expectedScore = 4*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookSupportComplex() {
		setUpPosition("1R6/8/8/1P6/1K6/8/8/1R6 b - - 0 1");
		// Rook in front defends passed pawn, K block rook behind from defending passed pawn
		// so pawn is not directly defended by rook behind pawn
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.WHITE_PAWN, Position.b5);
		// two rooks so phase scaling is 2...
		int expectedScore = 4*2*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	
	
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2_rookAttack() {
		setUpPosition("3R4/8/8/8/8/8/3p4/8 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.BLACK_PAWN, Position.d2);
		int expectedScore = (7-1)*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) - HEAVY_PIECE_BEHIND_PASSED_PAWN - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2_rookAttackBlocked() {
		setUpPosition("3R4/8/8/3p4/8/8/3p4/8 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.BLACK_PAWN, Position.d2);
		int expectedScore = (7-1)*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2_rookAttackNotBehindPawn() {
		setUpPosition("8/8/8/3p4/8/8/R2p4/8 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.BLACK_PAWN, Position.d2);
		int expectedScore = (7-1)*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2_rookAttackInFront() {
		setUpPosition("8/8/8/3p4/8/8/3p4/3R4 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.BLACK_PAWN, Position.d2);
		// Rook blocks the pawn
		int expectedScore = ((7-1)*3*PASSED_PAWN_BOOST) - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookAttack() {
		setUpPosition("8/8/8/1P6/1r6/8/8/8 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.WHITE_PAWN, Position.b5);
		int expectedScore = 4*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) - HEAVY_PIECE_BEHIND_PASSED_PAWN - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookAttackBlocked() {
		setUpPosition("8/8/8/1P6/1K6/8/8/1r6 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.WHITE_PAWN, Position.b5);
		int expectedScore = 4*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookAttackNotBehindPawn() {
		setUpPosition("8/8/8/1P5r/8/8/8/8 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.WHITE_PAWN, Position.b5);
		int expectedScore = 4*3*(PASSED_PAWN_BOOST+SAFE_MOBILE_PASSED_PAWN) - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookAttackInFront() {
		setUpPosition("1r6/8/8/1P6/8/8/8/8 b - - 0 1");
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.WHITE_PAWN, Position.b5);
		// Rook blocks the pawn
		int expectedScore = 4*3*PASSED_PAWN_BOOST - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookAttackComplex() {
		setUpPosition("1r6/8/8/1P6/1K6/8/8/1r6 b - - 0 1");
		// Rook in front defends passed pawn, K block rook behind from defending passed pawn
		// so pawn is not directly defended by rook behind pawn
		SUT.pawn_eval.initialise(attacks);
		SUT.pawn_eval.callback(Piece.WHITE_PAWN, Position.b5);
		// two rooks so phase scaling is 2...
		// Rook blocks the pawn
		int expectedScore = 4*2*PASSED_PAWN_BOOST - ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, SUT.pawn_eval.piecewisePawnScoreAccumulator);
	}
}
