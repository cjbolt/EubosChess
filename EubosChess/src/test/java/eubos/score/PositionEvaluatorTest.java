package eubos.score;

import static eubos.score.PositionEvaluator.DOUBLED_PAWN_HANDICAP;
import static eubos.score.PositionEvaluator.HAS_CASTLED_BOOST_CENTIPAWNS;
import static eubos.score.PositionEvaluator.PASSED_PAWN_BOOST;
import static eubos.score.PositionEvaluator.ROOK_FILE_PASSED_PAWN_BOOST;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.position.PositionManager;
import eubos.score.PositionEvaluator;
import eubos.search.DrawChecker;

public class PositionEvaluatorTest {

	PositionEvaluator SUT;
	PositionManager pm;
	
	@Before
	public void setUp() throws Exception {
	}

	protected void setUpPosition(String fen) {
		pm = new PositionManager(fen, new DrawChecker());
		SUT = new PositionEvaluator(pm, new DrawChecker());
	}
	
	@Test
	public void test_evalPosA() {
		setUpPosition("rn2k1nr/1pp2p1p/p7/8/6b1/2P2N2/PPP2PP1/R1BB1RK1 b kq - 0 12");
		assertEquals(80, SUT.evaluatePosition()); // Knight good pos, doubled pawns, pawn up
	}
	
	@Test
	public void test_encourageCastling_notYetCastled() {
		setUpPosition("8/8/8/8/8/8/8/4K2R w K - - -");
		int score = SUT.encourageCastling();
		assertEquals(0, score);
	}
	
	@Test
	public void test_encourageCastling_castled() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("k7/8/8/8/8/8/8/4K2R w K - - -");
	    pm.performMove(new GenericMove("e1g1"));
		int score = SUT.encourageCastling();
		assertEquals(HAS_CASTLED_BOOST_CENTIPAWNS, score);
	}

	

	@Test
	public void test_encourageCastling_castled_fewMoveLater() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("k7/8/8/8/8/8/8/4K2R w K - - -");
		pm.performMove(new GenericMove("e1g1"));
		pm.performMove(new GenericMove("a8b8"));
		pm.performMove(new GenericMove("f1d1"));
		pm.performMove(new GenericMove("b8a8"));
		pm.performMove(new GenericMove("d1d8"));
		int score = SUT.encourageCastling();
		assertEquals(HAS_CASTLED_BOOST_CENTIPAWNS, score);
	}	
	
	@Test
	public void test_DiscourageDoubledPawns_w() {
		setUpPosition("8/pppppp2/8/8/8/1P2P3/1P1P2PP/8 b - - 0 1");
		int score = SUT.evaluatePawnStructure();
		assertEquals(-DOUBLED_PAWN_HANDICAP+ROOK_FILE_PASSED_PAWN_BOOST, score);
	}
	
	@Test
	public void test_DiscourageDoubledPawns_b() {
		setUpPosition("8/pp2p1p1/3p2p1/8/8/8/2PPPPPP/8 w - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		assertEquals(DOUBLED_PAWN_HANDICAP-ROOK_FILE_PASSED_PAWN_BOOST, score);
	}
	
	@Test
	public void test_DiscourageTripledPawns_w() {
		setUpPosition("8/8/8/8/2P5/2P5/2P5/8 w - - 0 38 ");
		int score = SUT.evaluatePawnStructure();
		assertEquals(-2*DOUBLED_PAWN_HANDICAP+3*PASSED_PAWN_BOOST, score);
	}

	@Test
	public void test_DiscourageTripledPawns_b() {
		setUpPosition("8/8/8/8/2p5/2p5/2p5/8 w - - 0 38 ");
		int score = SUT.evaluatePawnStructure();
		assertEquals(2*DOUBLED_PAWN_HANDICAP-3*PASSED_PAWN_BOOST, score);
	}
	
	@Test
	public void test_encouragePassedPawns_PassedPawn() {
		setUpPosition("8/8/3pp3/8/3p4/8/2P5/8 b - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		assertEquals(-PASSED_PAWN_BOOST+DOUBLED_PAWN_HANDICAP /* passed e pawn */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_TwoPassedPawnsForBlack() {
		setUpPosition("8/8/3pp3/8/8/8/2Pp4/8 b - - 0 1");
		int score = SUT.evaluatePawnStructure();
		assertEquals(-2*PASSED_PAWN_BOOST+DOUBLED_PAWN_HANDICAP /* passed d and e pawns */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_PassedPawnForWhite() {
		setUpPosition("8/2pPp3/8/2P1P3/8/8/8/8 w - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		assertEquals(PASSED_PAWN_BOOST /* passed d pawn */, score);
	}
	 
	@Test
	public void test_encouragePassedPawns_PassedPawnForBlack1() {
		setUpPosition("8/8/8/8/8/2p1p3/2PpP3/8 b - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		assertEquals(-PASSED_PAWN_BOOST /* passed d pawn */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_TwoPassedPawnsForBlackOneRookFile() {
		setUpPosition("8/8/3p3p/8/8/8/2Pp4/8 b - - 0 1");
		int score = SUT.evaluatePawnStructure();
		assertEquals(-(PASSED_PAWN_BOOST+ROOK_FILE_PASSED_PAWN_BOOST-DOUBLED_PAWN_HANDICAP) /* passed d and e pawns */, score);
	} 
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn() {
		setUpPosition("8/8/8/8/8/5p2/6P1/8 w - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		assertEquals(0 /* no passed f pawn, can be taken */, score);
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
		setUpPosition("8/8/8/8/6P1/8/6p1/8 b - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		assertEquals(0 /* both pawns on the same file, passed */, score);
	}
	
	@Test
	public void test_isQuiescent_No_QueenRecapture() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/8/5p2/4p3/3Q4/8/8/8 w - - 0 1 ");
		pm.performMove(new GenericMove("d4e5"));
		assertFalse(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_Yes_QueenNoRecapture() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/8/8/4p3/3Q4/8/8/8 w - - 0 1 ");
		pm.performMove(new GenericMove("d4e5"));
		assertTrue(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_Yes_NoCaptures() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/3p4/8/8/3P4/8/8/8 w - - 0 1 ");
		pm.performMove(new GenericMove("d4d5"));
		assertTrue(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_Yes_LastMoveWasntCapture() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/8/4p3/8/3P4/8/8/8 w - - 0 1 ");
		pm.performMove(new GenericMove("d4d5"));
		assertTrue(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_Yes_LastMoveWasCapture_NoRecapturesPossible_Alt() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("rp6/1p6/Pp6/8/1p6/1p6/PP6/QP6 b - - 0 41");
		pm.performMove(new GenericMove("a8a6"));
		assertTrue(SUT.isQuiescent());
	}
	 
	@Test
	public void test_isQuiescent_No_LastMoveWasCheck() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("1r1k1r2/p5Q1/2p3p1/8/1q1p2n1/3P2P1/P3RPP1/4RK2 b - - 0 1");
		pm.performMove(new GenericMove("f8f2"));
		assertFalse(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_No_LastMoveWasCheckMate() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("5r1k/p2R4/1pp2p1p/8/5q2/3Q1bN1/PP3P2/6K1 w - - - -");
		pm.performMove(new GenericMove("d3h7"));
		assertFalse(SUT.isQuiescent());
	}
	
	@Test
	public void test() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("4r3/7P/2k5/1P6/8/6P1/8/6K1 b - - 0 57");
		assertFalse(SUT.isQuiescent());
	}
	
	@Test
	public void test2() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("4r3/7P/2k5/1Q5r/P7/6P1/8/6K1 b - - 10 56");
		assertFalse(SUT.isQuiescent());
		assertEquals(273, SUT.evaluatePosition());
	}
	
	@Test
	public void test_custom_position_score_reporter() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("4r1k1/2p2pb1/4Q3/8/3pPB2/1p1P3p/1P3P2/R5K1 b - - 0 42");
		System.out.println(SUT.evaluatePosition());
	}
}
