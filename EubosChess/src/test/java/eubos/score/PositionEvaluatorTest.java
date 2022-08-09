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
		assertEquals(-397, SUT.getFullEvaluation());
	}
	
	@Test
	public void test_custom_position_score_reporter()throws IllegalNotationException {
		setUpPosition("r3k2r/pp3p1p/1n6/3p1Q2/3q4/2N1P3/PP6/2K2RR1 b kq - 0 20");
		int full = SUT.getFullEvaluation();
		int crude = SUT.getCrudeEvaluation();
		int delta = full-crude;
		System.out.println("Full:"+full);
		System.out.println("Crude:"+crude);
		System.out.println("Delta:"+delta);
		// Dynamics
		long [][][] attacks = pm.getTheBoard().calculateAttacksAndMobility(pm.getTheBoard().me,false);
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
}
