package eubos.position;

import static eubos.position.PositionEvaluator.DOUBLED_PAWN_HANDICAP;
import static eubos.position.PositionEvaluator.HAS_CASTLED_BOOST_CENTIPAWNS;
import static eubos.position.PositionEvaluator.PASSED_PAWN_BOOST;
import static eubos.position.PositionEvaluator.ROOK_FILE_PASSED_PAWN_BOOST;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;

public class PositionEvaluatorTest {

	PositionEvaluator SUT;
	PositionManager pm;
	
	@Before
	public void setUp() throws Exception {
	}

	protected void setUpPosition(String fen) {
		pm = new PositionManager(fen);
		SUT = new PositionEvaluator(pm);
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
		int score = SUT.discourageDoubledPawns();
		assertEquals(-DOUBLED_PAWN_HANDICAP, score);
	}
	
	@Test
	public void test_DiscourageDoubledPawns_b() {
		setUpPosition("8/pp2p1p1/3p2p1/8/8/8/2PPPPPP/8 w - - 0 1 ");
		int score = SUT.discourageDoubledPawns();
		assertEquals(DOUBLED_PAWN_HANDICAP, score);
	}

	
	@Test
	public void test_encouragePassedPawns_PassedPawn() {
		setUpPosition("8/8/3pp3/8/3p4/8/2P5/8 b - - 0 1 ");
		int score = SUT.encouragePassedPawns();
		assertEquals(-PASSED_PAWN_BOOST /* passed e pawn */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_TwoPassedPawnsForBlack() {
		setUpPosition("8/8/3pp3/8/8/8/2Pp4/8 b - - 0 1");
		int score = SUT.encouragePassedPawns();
		assertEquals(-2*PASSED_PAWN_BOOST /* passed d and e pawns */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_PassedPawnForWhite() {
		setUpPosition("8/2pPp3/8/2P1P3/8/8/8/8 w - - 0 1 ");
		int score = SUT.encouragePassedPawns();
		assertEquals(PASSED_PAWN_BOOST /* passed d pawn */, score);
	}
	 
	@Test
	public void test_encouragePassedPawns_PassedPawnForBlack1() {
		setUpPosition("8/8/8/8/8/2p1p3/2PpP3/8 b - - 0 1 ");
		int score = SUT.encouragePassedPawns();
		assertEquals(-PASSED_PAWN_BOOST /* passed d pawn */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_TwoPassedPawnsForBlackOneRookFile() {
		setUpPosition("8/8/3p3p/8/8/8/2Pp4/8 b - - 0 1");
		int score = SUT.encouragePassedPawns();
		assertEquals(-(PASSED_PAWN_BOOST+ROOK_FILE_PASSED_PAWN_BOOST) /* passed d and e pawns */, score);
	} 
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn() {
		setUpPosition("8/8/8/8/8/5p2/6P1/8 w - - 0 1 ");
		int score = SUT.encouragePassedPawns();
		assertEquals(0 /* no passed f pawn, can be taken */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn1() {
		setUpPosition("8/8/8/8/5p2/8/6P1/8 w - - 0 1 ");
		int score = SUT.encouragePassedPawns();
		assertEquals(0 /* no passed f pawn, fully passed */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn2() {
		setUpPosition("8/8/8/8/6p1/8/6P1/8 w - - 0 1 ");
		int score = SUT.encouragePassedPawns();
		assertEquals(0 /* blocked g pawn not passed */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn2_w() {
		setUpPosition("8/8/8/8/6p1/8/6P1/8 b - - 0 1 ");
		int score = SUT.encouragePassedPawns();
		assertEquals(0 /* blocked g pawn not passed */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_BothPassedPawns() {
		setUpPosition("8/8/8/8/6P1/8/6p1/8 b - - 0 1 ");
		int score = SUT.encouragePassedPawns();
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
}
