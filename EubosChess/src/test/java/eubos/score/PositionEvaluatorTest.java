package eubos.score;

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
	
	@Test
	public void test_evalPosA() {
		setUpPosition("rn2k1nr/1pp2p1p/p7/8/6b1/2P2N2/PPP2PP1/R1BB1RK1 b kq - 0 12");
		assertEquals(-234, SUT.getFullEvaluation()); // Knight good pos, pawn up, doubled pawns, isolated pawn, not endgame, some danger to black king (open file)
	}
	
	@Test
	public void test_EvalPosB() {
		setUpPosition("8/8/1B6/8/8/4Kpk1/8/b7 w - - - 85");
		assertEquals(-398, SUT.getFullEvaluation());
	}
	
	@Test
	public void test_custom_position_score_reporter()throws IllegalNotationException {
		setUpPosition("r1bq2k1/pppp1p2/1b3P1p/1B1N4/Q2Nr2p/8/PP3PPP/R4RK1 w - - 1 15 ");
		int full = SUT.getFullEvaluation();
		int crude = SUT.getCrudeEvaluation();
		int delta = full-crude;
		System.out.println("Full:"+full);
		System.out.println("Crude:"+crude);
		System.out.println("Delta:"+delta);
		// Dynamics
		long [][][] attacks = pm.getTheBoard().mae.calculateBasicAttacksAndMobility(pm.getTheBoard().me);
		System.out.println("MG Mobility+PST:"+pm.getTheBoard().me.getPosition());
		// KS
		System.out.println("KS:"+SUT.evaluateKingSafety(attacks));
		// Pawns
		System.out.println("Pawns:"+SUT.pawn_eval.evaluatePawnStructure(attacks));
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
	public void test_evaluateKingSafety_safe()throws IllegalNotationException {
		setUpPosition("5krr/4pppp/6bq/8/8/6BQ/4PPPP/5KRR b - - 13 1");
		assertEquals(-18, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), true)); // 5 squares, can be attacked by three pieces
		assertEquals(-18, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), false));
	}
	
	@Test
	public void test_evaluateKingSafety_notVerySafe()throws IllegalNotationException {
		setUpPosition("6rr/5ppp/1k4bq/8/8/1K4BQ/5PPP/6RR b - - 13 1 ");
		assertEquals(-178, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), false)); // diagonals 7 squares, can be attacked by two pieces; r'n'f 9 squares can be attacked by three pieces
		assertEquals(-178, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), true));
	}
	
	@Test
	public void test_evaluateKingSafety_No_inEndgame()throws IllegalNotationException {
		setUpPosition("8/8/8/8/8/8/8/K7 w - - 0 1");
		assertEquals(-100, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), true));
	}
	
	@Test
	public void test_evaluateKingSafety_No_opposingBishopWrongColour()throws IllegalNotationException {
		setUpPosition("r4rk1/1p3p2/p7/P2P1p1B/4p3/2b5/3R1PPP/4K2R b K - 13 1 ");
		if (PositionEvaluator.ENABLE_TWEAKED_KING_FLIGHT_SQUARES) {
			assertEquals(-74, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), true)); // 7*2*2 rnf 0 diag = 28
			assertEquals(-66, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), false)); // 7*2*2 rnf 1*2*1 = 30
		} else {
			assertEquals(-116, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), true)); // 7*2*2 rnf 0 diag = 28
			assertEquals(-96, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), false)); // 7*2*2 rnf 1*2*1 = 30
		}

	}
	
	@Test
	public void test_evaluateKingSafety_Yes_opposingBishopRightColour()throws IllegalNotationException {
		setUpPosition("r4rk1/1p6/p7/P2P1p1B/4p3/2b5/3R1PPP/2K4R b - - 13 1 ");
		if (PositionEvaluator.ENABLE_TWEAKED_KING_FLIGHT_SQUARES) {
			assertEquals(-100, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), true));
		} else {
			assertEquals(-184, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), true));
		}
	}
	
	@Test
	public void test_evaluateKingSafety_Yes_opposingQueenBishop()throws IllegalNotationException {
		setUpPosition("r4rk1/1p6/p7/P2P1p1B/4p3/2b5/3R1PPP/Q1K4R b - - 13 1 ");
		if (PositionEvaluator.ENABLE_TWEAKED_KING_FLIGHT_SQUARES) {
		assertEquals(-100, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), true));  // (5 up right + 2 up left) *2 *1bish = 14; (7 up + 2 left + 5 right) * 2 *2rooks = 28*2; 56+14 = 70
		assertEquals(-75, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), false));  // 1*2*2 diag = 4; 7*2*3 = 42 r'n'f; 4+42 = 46 
		} else {
			assertEquals(-184, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), true));  // (5 up right + 2 up left) *2 *1bish = 14; (7 up + 2 left + 5 right) * 2 *2rooks = 28*2; 56+14 = 70
			assertEquals(-167, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), false));  // 1*2*2 diag = 4; 7*2*3 = 42 r'n'f; 4+42 = 46 
		}
	}
	
	@Test
	public void test_evaluateKingSafety_OneKnight_attackBlack()throws IllegalNotationException {
		setUpPosition("K7/8/4k3/8/8/1N4N1/8/8 w - - 1 1 ");
		assertEquals(-100, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), true));
		assertEquals(-168, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), false)); // One knight attacks the black king zone
	}
	
	@Test
	public void test_evaluateKingSafety_TwoKnights_attackBlack()throws IllegalNotationException {
		setUpPosition("K7/8/4k3/8/8/2N3N1/8/8 w - - 1 1 ");
		assertEquals(-100, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), true));
		assertEquals(-187, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), false)); // One knight attacks the black king zone
	}
	
	@Test
	public void test_evaluateKingSafety_OneKnight_attackWhite()throws IllegalNotationException {
		setUpPosition("k7/8/4K3/8/8/1n4n1/8/8 b - - 1 1 ");
		assertEquals(-168, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), true));
		assertEquals(-100, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), false)); // One knight attacks the black king zone
	}
	
	@Test
	public void test_evaluateKingSafety_TwoKnights_attackWhite()throws IllegalNotationException {
		setUpPosition("k7/8/4K3/8/8/2n3n1/8/8 b - - 1 1 ");
		assertEquals(-187, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), true));
		assertEquals(-100, SUT.evaluateKingSafety(SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me), false)); // One knight attacks the black king zone
	}
	
	@Test
	public void test_evaluateKingSafety_ScoreReporter()throws IllegalNotationException {
		setUpPosition("r1bq1r1k/1p1pn2p/p4ppQ/b3pN2/2B1PN2/2P5/PP3PPP/R2R2K1 b - - 10 21");
		SUT.passedPawnPresent = true;
		long [][][] attacks = SUT.bd.mae.calculateCountedAttacksAndMobility(SUT.bd.me);
		assertEquals(-404, SUT.evaluateKingSafety(attacks, false));
	}
}
