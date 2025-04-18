package eubos.score;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import eubos.evaluation.PositionEvaluator;
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
	@Ignore // With NN eval, absolute c.p, scores are too volatile between nets to be worth testing
	public void test_evalPosA() {
		setUpPosition("rn2k1nr/1pp2p1p/p7/8/6b1/2P2N2/PPP2PP1/R1BB1RK1 b kq - 0 12");
		assertEquals(-426, SUT.getFullEvaluation()); // Knight good pos, pawn up, doubled pawns, isolated pawn, not endgame, some danger to black king (open file)
	}
	
	@Test
	@Ignore // With NN eval, absolute c.p, scores are too volatile between nets to be worth testing
	public void test_EvalPosB() {
		setUpPosition("8/8/1B6/8/8/4Kpk1/8/b7 w - - - 85");
		int expectedScore = -377;
		assertEquals(expectedScore, SUT.getFullEvaluation());
	}
	
	private void checkSymmetryOfPosition(String fen) {
		int white_eval = 0, black_eval = 0;
		setUpPosition(fen);
		white_eval = SUT.getFullEvaluation();
		int onMoveIndex = fen.indexOf('w');
		char[] myNameChars = fen.toCharArray();
		myNameChars[onMoveIndex] = 'b';
		String black_fen = String.valueOf(myNameChars);
		setUpPosition(black_fen);
		black_eval = SUT.getFullEvaluation();
		assertEquals(white_eval, -black_eval);
	}
	
	@Test
	@Ignore // With NN eval, can't guarantee symmetry of evaluation function
	public void test_evaluationSymmetry() {
		checkSymmetryOfPosition("4q3/P4b2/2R3Bp/KPn5/2p3P1/2r2n2/kP6/5N2 w - - 0 1");
		checkSymmetryOfPosition("4b3/1Q6/1B1K1n1k/2p5/5r2/Pp1P1N2/6q1/NR5B w - - 0 1");
		checkSymmetryOfPosition("6k1/1Q6/K5n1/p1P1N3/2P5/pb2r3/N3pPpP/8 w - - 0 1");
		checkSymmetryOfPosition("2n2Bn1/2K4p/8/6p1/2QP4/NP2p1kq/p5p1/5B2 w - - 0 1");
		checkSymmetryOfPosition("1b5B/P1pq1P1p/5PpP/r6k/n7/6P1/6RK/8 w - - 0 1");
		checkSymmetryOfPosition("4K3/1P2B3/2P2P1q/1p1P3r/k3p3/4r3/pP4p1/5b2 w - - 0 1");
		checkSymmetryOfPosition("8/P3p1k1/qpN5/2n3pn/1P6/3R1p2/5Rr1/5Kb1 w - - 0 1");
		checkSymmetryOfPosition("8/5R2/R6K/4P3/QPpP1p1k/1p1p4/1p2B3/2nr4 w - - 0 1");
		checkSymmetryOfPosition("k6K/3R4/3P3P/6b1/3pPP1n/3p3p/P3p3/3rR3 w - - 0 1");
		checkSymmetryOfPosition("7k/P4p2/2N1B3/PR5P/1K1R3B/4rbN1/1b3n2/8 w - - 0 1");
		checkSymmetryOfPosition("8/PK1pp3/rP3rb1/k2pNP2/pR2P2p/BR4b1/1P2p3/2q1Q3 w - - 0 1");
		checkSymmetryOfPosition("1n3b2/1RP4P/p2pP1rQ/1p2P1N1/1B3P2/P3p3/B3qkpP/1K6 w - - 0 1");
		checkSymmetryOfPosition("R1r5/1kB5/q1pPKPp1/1P2N3/pnPP1p2/P1Pn4/2B5/1R5b w - - 0 1");
		checkSymmetryOfPosition("R3Qq/nKB1P1Rr/3pPN2/pk6/2p3Bp/1pPpPP2/bP1p2P1/2br3n w - - 0 1");
		checkSymmetryOfPosition("3Nn3/krPB1p2/P1Pp3q/2b2Ppp/p2P3Q/np1RBKp1/2P3R1/Nb4r1 w - - 0 1");
		checkSymmetryOfPosition("3r4/3RPpP1/1Np5/2K2ppr/1pn3PR/2QpPb2/p3PPqp/1k1nN1BB w - - 0 1");
		checkSymmetryOfPosition("r7/N7/3k4/6Kp/8/4Pb1r/8/R7 w - - 0 1");
		checkSymmetryOfPosition("5q2/1k6/4K3/1b6/P7/5Pb1/4Q3/1n6 w - - 0 1");
		checkSymmetryOfPosition("1b3r2/2P5/nK2P2p/1n6/p3pNpR/PP2p1PR/P1p2p2/4k3 w - - 0 1");
		checkSymmetryOfPosition("r5R1/3r3N/1P1p2p1/1P4pQ/4P3/N3Kn2/1k1pppRP/2nb4 w - - 0 1");
		checkSymmetryOfPosition("7N/2p2P2/2BRp3/Pb2P2q/2kb1Kp1/2np1Pp1/p5Pr/5n2 w - - 0 1");
		checkSymmetryOfPosition("R7/P4P2/pbP1NpP1/3KQpR1/5pPp/P3p2p/r4PpB/2k2qn1 w - - 0 1");
		checkSymmetryOfPosition("5r2/P5PP/Nnp4B/p2rkpP1/2P3p1/1PpP1pP1/bn2pK2/1R1B4 w - - 0 1");
		checkSymmetryOfPosition("8/3R4/N2r4/5p2/4P3/1bn2k2/1BPPp3/7K w - - 0 1");
		checkSymmetryOfPosition("8/B7/K2p3p/2P5/7k/4P3/p2R2r1/rRb5 w - - 0 1");
		checkSymmetryOfPosition("6r1/B1nP1pnN/b3qpQ1/pPK1pP2/k1pp2Pr/p1bNP2p/P2RR2P/7B w - - 0 1");
		checkSymmetryOfPosition("7b/2R1Pp1p/b1p1p1p1/1n1B1k1N/Pp1Q1PNP/1Kp1BPPP/3pPq2/R3n2r w - - 0 1");
		checkSymmetryOfPosition("b1q5/8/1nR5/r5k1/p1R5/B4K2/Qn2p1pp/8 w - - 0 1");
		checkSymmetryOfPosition("4k3/5p2/5P2/pb3p2/5N2/1N4r1/P1PK4/2R2rQ1 w - - 0 1");
	}
}
