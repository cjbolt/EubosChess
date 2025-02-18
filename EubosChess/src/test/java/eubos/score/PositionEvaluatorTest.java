package eubos.score;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.BitBoard;
import eubos.board.Piece;
import eubos.position.Move;
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
		assertEquals(-338, SUT.getFullEvaluation()); // Knight good pos, pawn up, doubled pawns, isolated pawn, not endgame, some danger to black king (open file)
	}
	
	@Test
	public void test_EvalPosB() {
		setUpPosition("8/8/1B6/8/8/4Kpk1/8/b7 w - - - 85");
		int expectedScore = -408;
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
		assertEquals(-(Piece.MATERIAL_VALUE_QUEEN/2)+Piece.MATERIAL_VALUE_PAWN/2, SUT.evaluateThreats(attacks, true)); // W Queen is attacked, B Pawn is attacked
	}
	
	@Test
	public void test_threatsBlack() {
		setUpPosition("kr3b2/4ppQ1/8/8/2P5/1P6/P7/7K w - - 1 1 ");
		long [][][] attacks = pm.getTheBoard().mae.calculateBasicAttacksAndMobility(pm.getTheBoard().me);
		assertEquals((Piece.MATERIAL_VALUE_QUEEN/2)-Piece.MATERIAL_VALUE_PAWN/2, SUT.evaluateThreats(attacks, false)); // W Queen is attacked, B Pawn is attacked
	}
	
	@Test
	public void test_movePositionalContribution_WhitePawn() {
		setUpPosition("K7/8/1P6/3p4/2P5/8/8/7k w - - 0 1");
		int move = Move.valueOfBit(BitBoard.c4, Piece.WHITE_PAWN, BitBoard.c5, Piece.NONE);
		int pos = SUT.estimateMovePositionalContribution(move);
		assertEquals(250, pos);
	}
	
	@Test
	public void test_movePositionalContribution_BlackPawn() {
		setUpPosition("7K/8/8/2p5/3P4/1p6/8/k7 b - - 0 1 ");
		int move = Move.valueOfBit(BitBoard.c5, Piece.BLACK_PAWN, BitBoard.c4, Piece.NONE);
		int pos = SUT.estimateMovePositionalContribution(move);
		assertEquals(250, pos);
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
