package eubos.score;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.Piece;
import eubos.position.PositionManager;
import eubos.search.DrawChecker;

public class PositionEvaluatorTest {

	PositionEvaluator SUT;
	PositionManager pm;
	
	@Before
	public void setUp() throws Exception {
	}

	protected void setUpPosition(String fen) {
		pm = new PositionManager(fen, new DrawChecker(), new PawnEvalHashTable());
		SUT = (PositionEvaluator) pm.getPositionEvaluator();
	}
	
	@Test
	public void test_evalPosA() {
		setUpPosition("rn2k1nr/1pp2p1p/p7/8/6b1/2P2N2/PPP2PP1/R1BB1RK1 b kq - 0 12");
		assertEquals(-344, SUT.getFullEvaluation()); // Knight good pos, pawn up, doubled pawns, isolated pawn, not endgame, some danger to black king (open file)
	}
	
	@Test
	public void test_EvalPosB() {
		setUpPosition("8/8/1B6/8/8/4Kpk1/8/b7 w - - - 85");
		int expectedScore = -410;
		if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
			expectedScore -= 15;
		}
		assertEquals(expectedScore, SUT.getFullEvaluation());
	}
	
	@Test
	public void test_kingExposure() {
		setUpPosition("r1nq1rkb/pp1npp2/4b2Q/3p4/3P1R2/P2BP1BP/1PPN2P1/R5K1 b - - - 17");
		assertTrue(SUT.isKingExposed());
	}
	
	// Interesting lazy tune positions
	// rrn2k2/pqb5/2n3Q1/4p1B1/8/2P2NP1/1P1R1P1P/R5K1 b - - - 31
	// r1nq1rkb/pp1npp2/4b2Q/3p4/3P1R2/P2BP1BP/1PPN2P1/R5K1 b - - - 17
	// 1nk5/3r1p2/b1pQ4/1R4B1/p1p3B1/2P5/P4PP1/2K4R b - - - 27
	// 1r3r1k/qb6/pbn3Qp/3pPP2/n6P/2PBB3/N5PK/1R5R b - - - 28
	// r1bq1r2/pppp4/1bn2P1p/5k2/2B4B/6Q1/P4PPP/qN2R1K1 b - - - 17
	// r2qk2r/2p1b2p/p1n4p/2nQ4/R7/2P3PB/1P3P1P/1N2R1K1 b kq - - 20
	
	// passed pawn evaluation - original: k3b2b/1p5P/p3p3/3R1pP1/8/1P2KN2/P3B3/8 b - - 0 38
	// k6b/1p3B1P/p1b3P1/8/3p1p1N/1P6/P4K2/8 w - - 2 44 beowulf terminal
	// k7/1p6/p7/3pNKP1/3b4/1P6/P7/8 w - - 3 44 eubos terminal
	// 
	@Test
	public void test_custom_position_score_reporter()throws IllegalNotationException {
		setUpPosition("2kr3r/pbpp4/3Pp3/n7/5qPp/P1P5/2QNBB1P/R4RK1 b - - 0 22 ");
		int full = SUT.getFullEvaluation();
		int crude = SUT.getCrudeEvaluation();
		int delta = full-crude;
		System.out.println("Full:"+full);
		System.out.println("Crude:"+crude);
		System.out.println("Delta:"+delta);
		// Dynamics
		long [][][] attacks = pm.getTheBoard().mae.calculateBasicAttacksAndMobility(pm.getTheBoard().me);
		System.out.println("MG Mobility+PST:"+pm.getTheBoard().me.getPosition());
		System.out.println("EG Mobility+PST:"+pm.getTheBoard().me.getEndgamePosition());
		// KS
		System.out.println("kingExposed?:"+SUT.isKingExposed());
		System.out.println("KS:"+SUT.ks_eval.evaluateKingSafety(attacks, pm.onMoveIsWhite()));
		// Pawns
		System.out.println("PpPresent:"+SUT.passedPawnPresent);
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
	public void test_threatsWhite() {
		setUpPosition("kr3b2/4ppQ1/8/8/2P5/1P6/P7/7K w - - 1 1 ");
		long [][][] attacks = pm.getTheBoard().mae.calculateBasicAttacksAndMobility(pm.getTheBoard().me);
		assertEquals(-(Piece.MATERIAL_VALUE_QUEEN/10)+Piece.MATERIAL_VALUE_PAWN/10, SUT.evaluateThreats(attacks, true)); // W Queen is attacked, B Pawn is attacked
	}
	
	@Test
	public void test_threatsBlack() {
		setUpPosition("kr3b2/4ppQ1/8/8/2P5/1P6/P7/7K w - - 1 1 ");
		long [][][] attacks = pm.getTheBoard().mae.calculateBasicAttacksAndMobility(pm.getTheBoard().me);
		assertEquals((Piece.MATERIAL_VALUE_QUEEN/10)-Piece.MATERIAL_VALUE_PAWN/10, SUT.evaluateThreats(attacks, false)); // W Queen is attacked, B Pawn is attacked
	}
}
