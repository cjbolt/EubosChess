package eubos.score;

import static eubos.score.PositionEvaluator.DOUBLED_PAWN_HANDICAP;
import static eubos.score.PositionEvaluator.PASSED_PAWN_BOOST;
import static eubos.score.PositionEvaluator.ROOK_FILE_PASSED_PAWN_BOOST;
import static eubos.score.PositionEvaluator.ISOLATED_PAWN_HANDICAP;
import static eubos.score.PositionEvaluator.BACKWARD_PAWN_HANDICAP;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.position.PositionManager;
import eubos.search.DrawChecker;

public class PositionEvaluatorTest {

	PositionEvaluator SUT;
	PositionManager pm;
	
	@Before
	public void setUp() throws Exception {
	}

	protected void setUpPosition(String fen) {
		pm = new PositionManager(fen, new DrawChecker());
		SUT = (PositionEvaluator) pm.getPositionEvaluator();
	}
	
	@SuppressWarnings("unused")
	@Test
	public void test_evalPosA() {
		setUpPosition("rn2k1nr/1pp2p1p/p7/8/6b1/2P2N2/PPP2PP1/R1BB1RK1 b kq - 0 12");
		if (PositionEvaluator.ENABLE_PAWN_EVALUATION && PositionEvaluator.ENABLE_DYNAMIC_POSITIONAL_EVALUATION) {
			assertEquals(-214, SUT.getFullEvaluation()); // Knight good pos, pawn up, doubled pawns, isolated pawn, not endgame, some danger to black king (open file)
		} else if (PositionEvaluator.ENABLE_PAWN_EVALUATION && PositionEvaluator.ENABLE_KING_SAFETY_EVALUATION) {
			assertEquals(-159, SUT.getFullEvaluation()); // Knight good pos, pawn up, doubled pawns, not endgame, some danger to black king (open file)
		} else {
			assertEquals(-137, SUT.getFullEvaluation()); // Knight good pos, pawn up, not endgame
		}
	}
	
	@Test
	public void test_EvalPosB() {
		setUpPosition("8/8/1B6/8/8/4Kpk1/8/b7 w - - - 85");
		assertEquals(-182, SUT.getFullEvaluation());
	}
	
	@Test
	public void test_DiscourageDoubledPawns_w() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/pppppp2/8/8/8/1P2P3/1P1P2PP/8 b - - 0 1");
		int score = SUT.evaluatePawnStructure();
		assertEquals(DOUBLED_PAWN_HANDICAP-3*ROOK_FILE_PASSED_PAWN_BOOST+2*ISOLATED_PAWN_HANDICAP+3*BACKWARD_PAWN_HANDICAP-6*BACKWARD_PAWN_HANDICAP, score);
	}
	
	@Test
	public void test_DiscourageDoubledPawns_b() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/pp2p1p1/3p2p1/8/8/8/2PPPPPP/8 w - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		assertEquals(DOUBLED_PAWN_HANDICAP-3*ROOK_FILE_PASSED_PAWN_BOOST+2*ISOLATED_PAWN_HANDICAP+3*BACKWARD_PAWN_HANDICAP-6*BACKWARD_PAWN_HANDICAP, score);
	}
	
	@Test
	public void test_DiscourageTripledPawns_w() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/8/8/2P5/2P5/2P5/8 w - - 0 38 ");
		int score = SUT.evaluatePawnStructure();
		// black
		int expectedScore = 0;
		// white
		expectedScore += 1*3*PASSED_PAWN_BOOST-ISOLATED_PAWN_HANDICAP;
		expectedScore += 2*3*PASSED_PAWN_BOOST-ISOLATED_PAWN_HANDICAP;
		expectedScore += 3*3*PASSED_PAWN_BOOST-ISOLATED_PAWN_HANDICAP;
		expectedScore += -2*DOUBLED_PAWN_HANDICAP;
		assertEquals(expectedScore, score);
	}

	@Test
	public void test_DiscourageTripledPawns_b() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/8/8/2p5/2p5/2p5/8 w - - 0 38 ");
		int score = SUT.evaluatePawnStructure();
		// black
		int expectedScore = 0;
		expectedScore -= ((7-1)*3*PASSED_PAWN_BOOST-ISOLATED_PAWN_HANDICAP);
		expectedScore -= ((7-2)*3*PASSED_PAWN_BOOST-ISOLATED_PAWN_HANDICAP);
		expectedScore -= ((7-3)*3*PASSED_PAWN_BOOST-ISOLATED_PAWN_HANDICAP);
		expectedScore -= -2*DOUBLED_PAWN_HANDICAP;
		assertEquals(expectedScore, score);
	}
	
	@Test
	public void test_encouragePassedPawns_PassedPawn() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/3pp3/8/3p4/8/2P5/8 b - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		// black
		int expectedScore = 0;
		expectedScore += -BACKWARD_PAWN_HANDICAP; // black d pawn about to queen
		expectedScore += (7-5)*3*PASSED_PAWN_BOOST; // black e pawn 6th rank
		expectedScore += -BACKWARD_PAWN_HANDICAP; // black d pawn 6th rank
		expectedScore += -DOUBLED_PAWN_HANDICAP; // black doubled pawns
		// white
		expectedScore -= -ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, score);
	}
	
	@Test
	public void test_encouragePassedPawns_TwoPassedPawnsForBlack() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/3pp3/8/8/8/2Pp4/8 b - - 0 1");
		int score = SUT.evaluatePawnStructure();
		// black
		int expectedScore = 0;
		expectedScore += (7-1)*3*PASSED_PAWN_BOOST; // black d pawn about to queen
		expectedScore += ((7-5)*3*PASSED_PAWN_BOOST - BACKWARD_PAWN_HANDICAP); // black e pawn 6th rank
		expectedScore += -BACKWARD_PAWN_HANDICAP; // black d pawn 6th rank
		expectedScore += -DOUBLED_PAWN_HANDICAP; // black doubled pawns
		// white
		expectedScore += ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, score);
	}
	
	@Test
	public void test_encouragePassedPawns_PassedPawnForWhite() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("8/2pPp3/8/2P1P3/8/8/8/7n w - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		// white
		int expectedScore = 0;
		expectedScore += 6*3*PASSED_PAWN_BOOST;
		expectedScore += -BACKWARD_PAWN_HANDICAP;
		expectedScore += -BACKWARD_PAWN_HANDICAP;
		// white
		expectedScore -= -ISOLATED_PAWN_HANDICAP;
		expectedScore -= -ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, score);
	}
	 
	@Test
	public void test_encouragePassedPawns_PassedPawnForBlack1() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/8/8/8/2p1p3/2PpP3/8 b - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		// black
		int expectedScore = 0;
		expectedScore += (7-1)*3*PASSED_PAWN_BOOST;
		expectedScore += -BACKWARD_PAWN_HANDICAP;
		expectedScore += -BACKWARD_PAWN_HANDICAP;
		// white
		expectedScore -= -ISOLATED_PAWN_HANDICAP;
		expectedScore -= -ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, score);
	}
	
	@Test
	public void test_encouragePassedPawns_TwoPassedPawnsForBlackOneRookFile() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/3p3p/8/8/8/2Pp4/8 b - - 0 1");
		int score = SUT.evaluatePawnStructure();
		// black
		int expectedScore = 0;
		expectedScore += ((7-1)*3*PASSED_PAWN_BOOST - ISOLATED_PAWN_HANDICAP); // black d pawn about to queen
		expectedScore += ((7-5)*3*ROOK_FILE_PASSED_PAWN_BOOST - ISOLATED_PAWN_HANDICAP); // black h pawn 6th rank
		expectedScore += -ISOLATED_PAWN_HANDICAP; // black d pawn 6th rank
		expectedScore += -DOUBLED_PAWN_HANDICAP; // black doubled pawns
		// white
		expectedScore -= -ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, score);
	} 
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn() {
		setUpPosition("8/8/8/8/8/5p2/6P1/8 w - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		assertEquals(-72 /* two candidate passed pawns, blacks is more valuable, but white can take it on the next move! */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn1() {
		setUpPosition("8/8/8/8/5p2/8/6P1/8 w - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		assertEquals(0 /* no passed f pawn, fully passed */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn2() {
		setUpPosition("8/8/8/8/6p1/8/6P1/8 w - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		assertEquals(0 /* blocked g pawn not passed */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn2_w() {
		setUpPosition("8/8/8/8/6p1/8/6P1/8 b - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		assertEquals(0 /* blocked g pawn not passed */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_BothPassedPawns() {
		// addition of a knight means it doesn't count as KPK endgame
		setUpPosition("n7/8/8/8/6P1/8/6p1/8 b - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		/* both pawns on the same file, passed, white at 3rd rank, black at 1st rank. */
		int expected_eval = (((7-3)*3*PASSED_PAWN_BOOST)-ISOLATED_PAWN_HANDICAP)-(1*3*PASSED_PAWN_BOOST-ISOLATED_PAWN_HANDICAP);
		assertEquals(expected_eval, score);
	}
	 
	@Test
	public void test_encouragePassedPawns_CandidatePasserAtB5() {
		setUpPosition("8/p7/8/PP6/8/8/8/8 w - - 0 1");
		int score = SUT.evaluatePawnStructure();
		assertEquals(81 /* b5 pawn will queen, not including material */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_CandidatePasserAtB5NotSupportedByDefender() {
		setUpPosition("8/p7/8/1P6/8/8/8/8 w - - 0 1");
		int score = SUT.evaluatePawnStructure();
		assertEquals(0 /* b5 pawn will not queen, because b6 is not defended by our pawn, not including material */, score);
	}
	
	@Test
	public void test_custom_position_score_reporter()throws IllegalNotationException {
		setUpPosition("4r1k1/2p2pb1/4Q3/8/3pPB2/1p1P3p/1P3P2/R5K1 b - - 0 42");
		System.out.println(SUT.getFullEvaluation());
	}
	
	@Test
	public void test_game_phase() throws IllegalNotationException {
		setUpPosition("4r1k1/2p2pb1/4Q3/8/3pPB2/1p1P3p/1P3P2/R5K1 b - - 0 42");
		int phase1 = pm.getTheBoard().me.getPhase();
		setUpPosition("4r1k1/2p2pb1/4Q3/8/3pP3/1p1P3p/1P3P2/R5K1 b - - 0 42");
		int phase2 = pm.getTheBoard().me.getPhase();
		System.out.println(String.format("%d %d %d", phase1, phase2, phase2 - phase1));
	}
	
	@Test
	public void test_won_KP_endgame_oppo_outside_square_of_pawn() {
		setUpPosition("8/8/k7/6K1/8/4p3/8/8 w - - 0 1");
		assertEquals(-767 /* passed pawn on second rank, can't be caught */, SUT.getFullEvaluation());
	}
	
	@Test
	public void test_drawn_KP_endgame_1() {
		setUpPosition("8/8/k7/8/7K/4p3/8/8 w - - 0 1 ");
		System.out.println(SUT.getFullEvaluation());
		assertEquals(-67 /* passed pawn on second rank, can be caught */, SUT.getFullEvaluation());
	}
	
	@Test
	public void test_drawn_KP_endgame() {
		setUpPosition("8/8/k7/8/8/4p1K1/8/8 w - - 0 1");
		assertEquals(-67 /* passed pawn on second rank, can be caught */, SUT.getFullEvaluation());
	}
	
	@Test
	public void test_won_KP_endgame_defended_by_own_king() {
		setUpPosition("8/8/8/8/7K/4p3/5k2/8 w - - 0 1");
		assertEquals(-767 /* own king can block enemy king */, SUT.getFullEvaluation());
	}
	
	@Test
	public void test_won_KP_endgame_one() {
		setUpPosition("8/8/8/8/8/2k1p1K1/8/8 w - - 44 1");
		/* In this test, the white king cannot get to the queening square in time,
		   because it is blocked by the square that the pawn is attacking, at f2. */
		assertEquals(-767 /* passed pawn */, SUT.getFullEvaluation());
	}
	
	@Test
	public void test_won_KP_endgame_two() {
		setUpPosition("8/8/8/8/8/2k1p3/6K1/8 w - - 44 1");
		assertEquals(-767 /* passed pawn */, SUT.getFullEvaluation());
	}
	
	@Test
	public void test_won_KP_endgame_three() {
		setUpPosition("8/8/8/8/8/1k2p3/6K1/8 b - - 44 1");
		assertEquals(767 /* passed pawn */, SUT.getFullEvaluation());
	}
	
	@Test
	public void test_drawn_KP_endgame_one() {
		setUpPosition("8/8/8/8/8/2k1p3/8/5K2 w - - 44 1");
		assertEquals(-67 /* passed pawn */, SUT.getFullEvaluation());
	}
	
	@Test
	public void test_drawn_KP_endgame_two() {
		setUpPosition("8/8/8/8/8/1k2p3/6K1/8 w - - 44 1");
		assertEquals(-67 /* passed pawn */, SUT.getFullEvaluation());
	}
	
	@Test
	public void test_won_KP_endgame_oppo_outside_square_of_pawn_white() {
		setUpPosition("8/8/4P3/8/6k1/K7/8/8 b - - 0 1 ");
		assertEquals(-767 /* passed pawn on second rank, can't be caught */, SUT.getFullEvaluation());
	}
	
	@Test
	public void test_drawn_KP_endgame_1_white() {
		setUpPosition("8/8/4P3/7k/8/K7/8/8 b - - 0 1 ");
		System.out.println(SUT.getFullEvaluation());
		assertEquals(-67 /* passed pawn on second rank, can be caught */, SUT.getFullEvaluation());
	}
	
	@Test
	public void test_drawn_KP_endgame_white() {
		setUpPosition("8/8/4P1k1/8/8/K7/8/8 b - - 0 1 ");
		assertEquals(-67 /* passed pawn on second rank, can be caught */, SUT.getFullEvaluation());
	}
	
	@Test
	public void test_won_KP_endgame_defended_by_own_king_white() {
		setUpPosition("8/5K2/4P3/7k/8/8/8/8 b - - 0 1 ");
		assertEquals(-767 /* own king can block enemy king */, SUT.getFullEvaluation());
	}
}
