package eubos.score;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import eubos.position.PositionManager;
import eubos.search.DrawChecker;

public class PawnEvaluatorTest {

	PawnEvaluator SUT;
	PositionManager pm;
	long [][][] attacks;
	long whitePawns;
	long blackPawns;
	
	@Before
	public void setUp() throws Exception {
	}

	protected void setUpPosition(String fen) {
		pm = new PositionManager(fen, new DrawChecker(), new PawnEvalHashTable());
		SUT = new PawnEvaluator(pm, new PawnEvalHashTable());
		attacks = pm.getTheBoard().mae.calculateCountedAttacksAndMobility(pm.getTheBoard().me);
		whitePawns = pm.getTheBoard().getWhitePawns();
		blackPawns = pm.getTheBoard().getBlackPawns();
	}

	@Test
	public void test_DiscourageDoubledPawns_w() {
		setUpPosition("8/pppppp2/8/8/8/1P2P3/1P1P2PP/8 b - - 0 1");
		int score = SUT.getDoubledPawnsHandicap(whitePawns);
		assertEquals(-PawnEvaluator.DOUBLED_PAWN_HANDICAP, score);
	}
	
	@Test
	public void test_DiscourageDoubledPawns_b() {
		setUpPosition("8/pp2p1p1/3p2p1/8/8/8/2PPPPPP/8 w - - 0 1 ");
		int score = SUT.getDoubledPawnsHandicap(blackPawns);
		assertEquals(-PawnEvaluator.DOUBLED_PAWN_HANDICAP, score);
	}
	
	@Test
	public void test_DiscourageTripledPawns_w() {
		setUpPosition("8/8/8/8/2P5/2P5/2P5/8 w - - 0 38 ");
		int score = SUT.getDoubledPawnsHandicap(whitePawns);
		assertEquals(-2*PawnEvaluator.DOUBLED_PAWN_HANDICAP, score);
	}

	@Test
	public void test_DiscourageTripledPawns_b() {
		setUpPosition("8/8/8/8/2p5/2p5/2p5/8 w - - 0 38 ");
		int score = SUT.getDoubledPawnsHandicap(blackPawns);
		assertEquals(-2*PawnEvaluator.DOUBLED_PAWN_HANDICAP, score);
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnE6() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/3pp3/8/3p4/8/2P5/8 b - - 0 1 ");
		// black pawns
		int expectedScore = -PawnEvaluator.BACKWARD_PAWN_HANDICAP;
		expectedScore += ((7-5)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN)) - PawnEvaluator.BACKWARD_PAWN_HANDICAP;
		expectedScore -= PawnEvaluator.DOUBLED_PAWN_HANDICAP;
		// White pawn
		expectedScore -= (3*3*(0)) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
		}
		assertEquals(expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/3pp3/8/8/8/2Pp4/8 b - - 0 1");
		// black pawns
		int expectedScore = (7-1)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN);
		expectedScore += -PawnEvaluator.BACKWARD_PAWN_HANDICAP;
		expectedScore += ((7-5)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN)) - PawnEvaluator.BACKWARD_PAWN_HANDICAP;
		expectedScore -= PawnEvaluator.DOUBLED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.CONNECTED_PASSED_PAWN_BOOST/2;
		// White pawn
		expectedScore -= -PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[2];
		}
		assertEquals(expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_fullEval_TwoPassedPawnsForBlack() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/3pp3/8/8/8/2Pp4/8 b - - 0 1");
		int score = SUT.evaluatePawnStructure(attacks);
		// black
		int expectedScore = 0;
		expectedScore += (7-1)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN); // black d pawn about to queen
		expectedScore += ((7-5)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.BACKWARD_PAWN_HANDICAP); // black e pawn 6th rank
		expectedScore += -PawnEvaluator.BACKWARD_PAWN_HANDICAP; // black d pawn 6th rank
		expectedScore += -PawnEvaluator.DOUBLED_PAWN_HANDICAP; // black doubled pawns
		expectedScore += PawnEvaluator.CONNECTED_PASSED_PAWN_BOOST/2;
		// white
		expectedScore += PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[2];
		}
		assertEquals(expectedScore, score);
	}
	
	@Test
	public void test_fullEval_encouragePassedPawns_ForWhite() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("8/2pPp3/8/2P1P3/8/8/8/7n w - - 0 1 ");
		int score = SUT.evaluatePawnStructure(attacks);
		// white
		int expectedScore = 0;
		expectedScore += 6*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN);
		expectedScore += -PawnEvaluator.BACKWARD_PAWN_HANDICAP;
		expectedScore += -PawnEvaluator.BACKWARD_PAWN_HANDICAP;
		// white
		expectedScore -= -PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore -= -PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
		}
		assertEquals(expectedScore, score);
	}

	@Test
	public void test_encouragePassedPawns_forBlack_ppOnH6_rookFile() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/3p3p/8/8/8/2Pp4/8 b - - 0 1");
		// black
		int expectedScore = (7-5)*3*(PawnEvaluator.ROOK_FILE_PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += (7-1)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.ISOLATED_PAWN_HANDICAP; // black d pawn about to queen
		expectedScore += -PawnEvaluator.ISOLATED_PAWN_HANDICAP; // black d pawn 6th rank
		expectedScore += -PawnEvaluator.DOUBLED_PAWN_HANDICAP; // black doubled pawns
		// white
		expectedScore += PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[2];
		}
		assertEquals(expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_crazyPassedPawnScenario() {
		setUpPosition("3r2k1/1Nr2pp1/1n3n2/1P1P2q1/3P4/1pP2p1p/1K6/R3QB1R b - - 0 29 ");
		int score = SUT.evaluatePawnStructure(attacks);
		assertEquals(2, score);
	} 
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn() {
		setUpPosition("n7/8/8/8/8/5p2/6P1/8 w - - 0 1 ");
		int score = SUT.evaluatePawnStructure(attacks);
		assertEquals(-60 /* two candidate passed pawns, blacks is more valuable, but white can take it on the next move! */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn1() {
		setUpPosition("n7/8/8/8/5p2/8/6P1/8 w - - 0 1 ");
		int score = SUT.evaluatePawnStructure(attacks);
		assertEquals(0 /* no passed f pawn, fully passed */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn2() {
		setUpPosition("n7/8/8/8/6p1/8/6P1/8 w - - 0 1 ");
		int score = SUT.evaluatePawnStructure(attacks);
		assertEquals(0 /* blocked g pawn not passed */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn2_w() {
		setUpPosition("n7/8/8/8/6p1/8/6P1/8 b - - 0 1 ");
		int score = SUT.evaluatePawnStructure(attacks);
		assertEquals(0 /* blocked g pawn not passed */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_BothPassedPawns() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/8/8/6P1/8/6p1/8 b - - 0 1 ");
		int score = SUT.evaluatePawnStructure(attacks);
		/* both pawns on the same file, passed, white at 3rd rank, black at 1st rank. */
		int expected_eval = (((7-3)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN))-PawnEvaluator.ISOLATED_PAWN_HANDICAP)-(1*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN)-PawnEvaluator.ISOLATED_PAWN_HANDICAP);
		assertEquals(expected_eval, score);
	}
	 
	@Test
	public void test_encouragePassedPawns_CandidatePasserAtB5() {
		setUpPosition("8/p7/8/PP6/8/8/8/8 w - - 0 1");
		int score = SUT.evaluatePawnStructure(attacks);
		assertEquals(69 /* b5 pawn will queen, not including material */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_CandidatePasserAtB5NotSupportedByDefender() {
		setUpPosition("8/p7/8/1P6/8/8/8/8 w - - 0 1");
		int score = SUT.evaluatePawnStructure(attacks);
		assertEquals(0 /* b5 pawn will not queen, because b6 is not defended by our pawn, not including material */, score);
	}
	
	@Test
	public void test_won_KP_endgame_oppo_outside_square_of_pawn() {
		if (PawnEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/k7/6K1/8/4p3/8/8 w - - 0 1");
			int expectedScore = -717;
			if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
			}
			assertEquals(expectedScore /* passed pawn on second rank, can't be caught */, SUT.evaluatePawnStructure(attacks));
		}
	}
	
	@Test
	public void test_drawn_KP_endgame_1() {
		if (PawnEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/k7/8/7K/4p3/8/8 w - - 0 1 ");
			System.out.println(SUT.evaluatePawnStructure(attacks));
			int expectedScore = -17;
			if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
			}
			assertEquals(expectedScore /* passed pawn on second rank, can be caught */, SUT.evaluatePawnStructure(attacks));
		}
	}
	
	@Test
	public void test_drawn_KP_endgame() {
		if (PawnEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/k7/8/8/4p1K1/8/8 w - - 0 1");
			int expectedScore = -17;
			if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
			}
			assertEquals(expectedScore /* passed pawn on second rank, can be caught */, SUT.evaluatePawnStructure(attacks));
		}
	}
	
	@Test
	public void test_won_KP_endgame_defended_by_own_king() {
		if (PawnEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/8/8/7K/4p3/5k2/8 w - - 0 1");
			int expectedScore = -717;
			if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
			}
			assertEquals(expectedScore /* own king can block enemy king */, SUT.evaluatePawnStructure(attacks));
		}
	}
	
	@Test
	public void test_won_KP_endgame_one() {
		if (PawnEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/8/8/8/2k1p1K1/8/8 w - - 44 1");
			/* In this test, the white king cannot get to the queening square in time,
			   because it is blocked by the square that the pawn is attacking, at f2. */
			int expectedScore = -317;
			if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
			}
			assertEquals(expectedScore /* probably unstoppable passer */, SUT.evaluatePawnStructure(attacks));
		}
	}
	
	@Test
	public void test_won_KP_endgame_two() {
		if (PawnEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/8/8/8/2k1p3/6K1/8 w - - 44 1");
			int expectedScore = -317;
			if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
			}
			assertEquals(expectedScore /* probably unstoppable passer */, SUT.evaluatePawnStructure(attacks));
		}
	}
	
	@Test
	public void test_won_KP_endgame_three() {
		if (PawnEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/8/8/8/1k2p3/6K1/8 b - - 44 1");
			int expectedScore = 317;
			if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
			}
			assertEquals(expectedScore /* passed pawn */, SUT.evaluatePawnStructure(attacks));
		}
	}
	
	@Test
	public void test_drawn_KP_endgame_one() {
		if (PawnEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/8/8/8/2k1p3/8/5K2 w - - 44 1");
			int expectedScore = -17;
			if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
			}
			assertEquals(expectedScore /* passed pawn */, SUT.evaluatePawnStructure(attacks));
		}
	}
	
	@Test
	public void test_drawn_KP_endgame_two() {
		if (PawnEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/8/8/8/1k2p3/6K1/8 w - - 44 1");
			int expectedScore = -17;
			if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
			}
			assertEquals(expectedScore /* passed pawn */, SUT.evaluatePawnStructure(attacks));
		}
	}
	
	@Test
	public void test_won_KP_endgame_oppo_outside_square_of_pawn_white() {
		if (PawnEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/4P3/8/6k1/K7/8/8 b - - 0 1 ");
			int expectedScore = -717;
			if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
			}
			assertEquals(expectedScore /* passed pawn on second rank, can't be caught */, SUT.evaluatePawnStructure(attacks));
		}
	}
	
	@Test
	public void test_drawn_KP_endgame_1_white() {
		if (PawnEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/4P3/7k/8/K7/8/8 b - - 0 1 ");
			System.out.println(SUT.evaluatePawnStructure(attacks));
			int expectedScore = -17;
			if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
			}
			assertEquals(expectedScore /* passed pawn on second rank, can be caught */, SUT.evaluatePawnStructure(attacks));
		}
	}
	
	@Test
	public void test_drawn_KP_endgame_white() {
		if (PawnEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/8/4P1k1/8/8/K7/8/8 b - - 0 1 ");
			int expectedScore = -17;
			if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
			}
			assertEquals(expectedScore /* passed pawn on second rank, can be caught */, SUT.evaluatePawnStructure(attacks));
		}
	}
	
	@Test
	public void test_won_KP_endgame_defended_by_own_king_white() {
		if (PawnEvaluator.ENABLE_KPK_EVALUATION) {
			setUpPosition("8/5K2/4P3/7k/8/8/8/8 b - - 0 1 ");
			int expectedScore = -717;
			if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedScore -= PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
			}
			assertEquals(expectedScore /* own king can block enemy king */, SUT.evaluatePawnStructure(attacks));
		}
	}
	
	@Test
	public void test_fullEval_WhenNoPawns() {
		setUpPosition("8/8/8/8/8/8/8/8 b - - 0 1");
		int score = SUT.evaluatePawnStructure(attacks);
		assertEquals(0, score);
	}
	
	@Test
	public void test_fullEval_OneWhitePawn() {
		setUpPosition("n7/8/8/8/8/8/7P/8 b - - 0 1");
		int score = SUT.evaluatePawnStructure(attacks);
		// white
		int expectedScore = 0;
		expectedScore += -(1*3*(PawnEvaluator.ROOK_FILE_PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) + -PawnEvaluator.ISOLATED_PAWN_HANDICAP); // white has an h file passed pawn
		expectedScore += -PawnEvaluator.NO_PAWNS_HANDICAP_LUT[1]; // black has no pawns
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore -= PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
		}
		assertEquals(expectedScore, score);
	}
	
	@Test
	public void test_fullEval_OneBlackPawn() {
		setUpPosition("n7/7p/8/8/8/8/8/8 b - - 0 1");
		int score = SUT.evaluatePawnStructure(attacks);
		// white
		int expectedScore = 0;
		expectedScore += (1*3*(PawnEvaluator.ROOK_FILE_PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) + -PawnEvaluator.ISOLATED_PAWN_HANDICAP); // black has an h file passed pawn
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[1]; // white has no pawns
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
		}
		assertEquals(expectedScore, score);
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2_rookSupport() {
		setUpPosition("3r4/8/8/8/8/8/3p4/8 b - - 0 1");
		int expectedScore = (7-1)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) + PawnEvaluator.HEAVY_PIECE_BEHIND_PASSED_PAWN - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[1];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
		}
		assertEquals(expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2_rookSupportBlocked() {
		setUpPosition("3r4/8/8/3p4/8/8/3p4/8 b - - 0 1");
		int expectedScore = (7-1)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += ((7-4)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN)) + PawnEvaluator.HEAVY_PIECE_BEHIND_PASSED_PAWN - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore -= PawnEvaluator.DOUBLED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[2];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[2];
		}
		assertEquals(expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2_rookSupportNotBehindPawn() {
		setUpPosition("8/8/8/3p4/8/8/r2p4/8 b - - 0 1");
		int expectedScore = (7-1)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += ((7-4)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN)) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore -= PawnEvaluator.DOUBLED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[2];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[2];
		}
		assertEquals(expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2_rookSupportInFront() {
		setUpPosition("8/8/8/3p4/8/8/3p4/3r4 b - - 0 1");
		int expectedScore = (7-1)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += ((7-4)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN)) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore -= PawnEvaluator.DOUBLED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[2];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[2];
		}
		assertEquals(expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookSupport() {
		setUpPosition("8/8/8/1P6/1R6/8/8/8 b - - 0 1");
		int expectedScore = 4*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) + PawnEvaluator.HEAVY_PIECE_BEHIND_PASSED_PAWN - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[1];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
		}
		assertEquals(-expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookSupportBlocked() {
		setUpPosition("8/8/8/1P6/1K6/8/8/1R6 b - - 0 1");
		int expectedScore = 4*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[1];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
		}
		assertEquals(-expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookSupportNotBehindPawn() {
		setUpPosition("8/8/8/1P5R/8/8/8/8 b - - 0 1");
		int expectedScore = 4*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[1];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
		}
		assertEquals(-expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookSupportInFront() {
		setUpPosition("1R6/8/8/1P6/8/8/8/8 b - - 0 1");
		int expectedScore = 4*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[1];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
		}
		assertEquals(-expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookSupportComplex() {
		setUpPosition("1R6/8/8/1P6/1K6/8/8/1R6 b - - 0 1");
		// Rook in front defends passed pawn, K block rook behind from defending passed pawn
		// so pawn is not directly defended by rook behind pawn
		// two rooks so phase scaling is 2...
		int expectedScore = 4*2*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[1];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
		}
		assertEquals(-expectedScore, SUT.evaluatePawnStructure(attacks));
	}	
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2_rookAttack() {
		setUpPosition("3R4/8/8/8/8/8/3p4/8 b - - 0 1");
		int expectedScore = (7-1)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.HEAVY_PIECE_BEHIND_PASSED_PAWN - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[1];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
		}
		assertEquals(expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2_rookAttackBlocked() {
		setUpPosition("3R4/8/8/3p4/8/8/3p4/8 b - - 0 1");
		int expectedScore = ((7-1)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN)) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += ((7-4)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN)) - PawnEvaluator.HEAVY_PIECE_BEHIND_PASSED_PAWN - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore -= PawnEvaluator.DOUBLED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[2];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[2];
		}
		assertEquals(expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2_rookAttackNotBehindPawn() {
		setUpPosition("8/8/8/3p4/8/8/R2p4/8 b - - 0 1");
		int expectedScore = (7-1)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += ((7-4)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.MOBILE_PASSED_PAWN)) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore -= PawnEvaluator.DOUBLED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[2];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[2];
		}
		assertEquals(expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forBlack_ppOnD2_rookAttackInFront() {
		setUpPosition("8/8/8/3p4/8/8/3p4/3R4 b - - 0 1");
		// Rook blocks the pawn
		int expectedScore = ((7-1)*3*PawnEvaluator.PASSED_PAWN_BOOST) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += ((7-4)*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.MOBILE_PASSED_PAWN)) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore -= PawnEvaluator.DOUBLED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[2];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[2];
		}
		assertEquals(expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookAttack() {
		setUpPosition("8/8/8/1P6/1r6/8/8/8 b - - 0 1");
		int expectedScore = 4*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.HEAVY_PIECE_BEHIND_PASSED_PAWN - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[1];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
		}
		assertEquals(-expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookAttackBlocked() {
		setUpPosition("8/8/8/1P6/1K6/8/8/1r6 b - - 0 1");
		int expectedScore = 4*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[1];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
		}
		assertEquals(-expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookAttackNotBehindPawn() {
		setUpPosition("8/8/8/1P5r/8/8/8/8 b - - 0 1");
		int expectedScore = 4*3*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[1];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
		}
		assertEquals(-expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookAttackInFront() {
		setUpPosition("1r6/8/8/1P6/8/8/8/8 b - - 0 1");
		// Rook blocks the pawn
		int expectedScore = 4*3*PawnEvaluator.PASSED_PAWN_BOOST - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[1];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
		}
		assertEquals(-expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_encouragePassedPawns_forWhite_ppOnB5_rookAttackComplex() {
		setUpPosition("1r6/8/8/1P6/1K6/8/8/1r6 b - - 0 1");
		// Rook in front defends passed pawn, K block rook behind from defending passed pawn
		// so pawn is not directly defended by rook behind pawn
		// two rooks so phase scaling is 2...
		// Rook blocks the pawn
		int expectedScore = 4*2*PawnEvaluator.PASSED_PAWN_BOOST - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[1];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[1];
		}
		assertEquals(-expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_is_a_passed_pawn_present_1() {
		setUpPosition("8/8/8/P7/8/8/8/8 w - - 0 1");
		assertTrue(pm.getTheBoard().isPassedPawnPresent());
	}
	
	@Test
	public void test_is_a_passed_pawn_present_2() {
		setUpPosition("8/8/p7/P7/8/8/8/8 w - - 0 1");
		assertFalse(pm.getTheBoard().isPassedPawnPresent());
	}
	@Test
	public void test_is_a_passed_pawn_present_3() {
		setUpPosition("8/8/8/P7/8/p7/8/8 w - - 0 1");
		assertTrue(pm.getTheBoard().isPassedPawnPresent());
	}
	
	@Test
	public void test_is_a_passed_pawn_present_4() {
		setUpPosition("8/8/1p6/P7/8/8/8/8 w - - 0 1");
		assertFalse(pm.getTheBoard().isPassedPawnPresent());
	}
	
	@Test
	public void test_is_a_passed_pawn_present_5() {
		setUpPosition("8/8/8/3pP3/8/8/8/8 w - - 0 1");
		assertTrue(pm.getTheBoard().isPassedPawnPresent());
	}
	
	@Test
	public void test_is_a_passed_pawn_present_6() {
		setUpPosition("8/8/8/4p3/5P2/8/8/8 w - - 0 1");
		assertFalse(pm.getTheBoard().isPassedPawnPresent());
	}
	
	@Test
	public void test_is_a_passed_pawn_present_7() {
		setUpPosition("8/8/8/p7/8/8/8/8 w - - 0 1");
		assertTrue(pm.getTheBoard().isPassedPawnPresent());
	}
	
	@Test
	public void test_is_a_passed_pawn_present_8() {
		setUpPosition("8/p3pppp/p7/p7/8/8/PPPPPPPP/8 w - - 0 1");
		assertTrue(pm.getTheBoard().isPassedPawnPresent());
	}
	
	@Test
	public void test_is_a_passed_pawn_present_9() {
		setUpPosition("8/pppppppp/8/8/P7/P7/P3PPPP/8 w - - 0 1");
		assertTrue(pm.getTheBoard().isPassedPawnPresent());
	}
	
	@Test
	public void test_evaluate_connected_passed_pawns() {
		setUpPosition("N7/8/6PP/8/8/8/8/n7 w - - 0 1");
		// both pawns are backwards and connected 
		int expectedScore = 5*2*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.BACKWARD_PAWN_HANDICAP;
		expectedScore += 5*2*(PawnEvaluator.ROOK_FILE_PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.BACKWARD_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.CONNECTED_PASSED_PAWN_BOOST*2;
		// black has no pawns
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[2];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[2];
		}
		assertEquals(expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_evaluate_connected_passed_pawns_2() {
		setUpPosition("8/8/1P5P/8/8/8/8/n6N w - - 0 1");
		// as above but both pawns are isolated and their is no connected bonus
		int expectedScore = 5*2*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		expectedScore += 5*2*(PawnEvaluator.ROOK_FILE_PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.ISOLATED_PAWN_HANDICAP;
		// black has no pawns
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[2];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[2];
		}
		assertEquals(expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_evaluate_connected_passed_pawns_1() {
		setUpPosition("N7/8/7P/8/8/6P1/8/n7 w - - 0 1");
		int expectedScore = 2*2*(PawnEvaluator.PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN) - PawnEvaluator.BACKWARD_PAWN_HANDICAP;
		expectedScore += PawnEvaluator.CONNECTED_PASSED_PAWN_BOOST/2;
		expectedScore += 5*2*(PawnEvaluator.ROOK_FILE_PASSED_PAWN_BOOST+PawnEvaluator.SAFE_MOBILE_PASSED_PAWN);
		// black has no pawns
		expectedScore += PawnEvaluator.NO_PAWNS_HANDICAP_LUT[2];
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore += PawnEvaluator.PASSED_PAWN_IMBALANCE_LUT[2];
		}
		assertEquals(expectedScore, SUT.evaluatePawnStructure(attacks));
	}
	
	@Test
	public void test_candidate_passer()  {
		// In this position there are no passed pawns present, so have to work out how we came to store a hash score where we though a passed pawn was present
		setUpPosition("8/pp5p/8/PqP5/3k4/7P/3K4/8 w - - 0 17");
		assertEquals(-42, SUT.evaluatePawnStructure(attacks)); // counted bit board, white attacks frontspan twice, so not a candidate
		// basic attacks, we think that there is a candidate passer.
		SUT = new PawnEvaluator(pm, new PawnEvalHashTable());
		long [][][] basic_attacks = pm.getTheBoard().mae.calculateBasicAttacksAndMobility(pm.getTheBoard().me);
		assertEquals(-52, SUT.evaluatePawnStructure(basic_attacks));	
	}
}
