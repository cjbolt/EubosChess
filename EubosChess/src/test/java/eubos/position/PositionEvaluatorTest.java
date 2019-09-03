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
	
	@Before
	public void setUp() throws Exception {
		SUT = new PositionEvaluator();
	}

	@Test
	public void test_encourageCastling_notYetCastled() {
		int score = SUT.encourageCastling(new PositionManager("8/8/8/8/8/8/8/4K2R w K - - -"));
		assertEquals(0, score);
	}
	
	@Test
	public void test_encourageCastling_castled() throws InvalidPieceException, IllegalNotationException {
		PositionManager pm = new PositionManager("k7/8/8/8/8/8/8/4K2R w K - - -");
	    pm.performMove(new GenericMove("e1g1"));
		int score = SUT.encourageCastling(pm);
		assertEquals(HAS_CASTLED_BOOST_CENTIPAWNS, score);
	}	

	@Test
	public void test_encourageCastling_castled_fewMoveLater() throws InvalidPieceException, IllegalNotationException {
		PositionManager pm = new PositionManager("k7/8/8/8/8/8/8/4K2R w K - - -");
		pm.performMove(new GenericMove("e1g1"));
		pm.performMove(new GenericMove("a8b8"));
		pm.performMove(new GenericMove("f1d1"));
		pm.performMove(new GenericMove("b8a8"));
		pm.performMove(new GenericMove("d1d8"));
		int score = SUT.encourageCastling(pm);
		assertEquals(HAS_CASTLED_BOOST_CENTIPAWNS, score);
	}	
	
	@Test
	public void test_DiscourageDoubledPawns_w() {
		PositionManager pm = new PositionManager("8/pppppp2/8/8/8/1P2P3/1P1P2PP/8 b - - 0 1");
		int score = SUT.discourageDoubledPawns(pm);
		assertEquals(-DOUBLED_PAWN_HANDICAP, score);
	}
	
	@Test
	public void test_DiscourageDoubledPawns_b() {
		PositionManager pm = new PositionManager("8/pp2p1p1/3p2p1/8/8/8/2PPPPPP/8 w - - 0 1 ");
		int score = SUT.discourageDoubledPawns(pm);
		assertEquals(DOUBLED_PAWN_HANDICAP, score);
	}

	
	@Test
	public void test_encouragePassedPawns_PassedPawn() {
		PositionManager pm = new PositionManager("8/8/3pp3/8/3p4/8/2P5/8 b - - 0 1 ");
		int score = SUT.encouragePassedPawns(pm);
		assertEquals(-PASSED_PAWN_BOOST /* passed e pawn */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_TwoPassedPawnsForBlack() {
		PositionManager pm = new PositionManager("8/8/3pp3/8/8/8/2Pp4/8 b - - 0 1");
		int score = SUT.encouragePassedPawns(pm);
		assertEquals(-2*PASSED_PAWN_BOOST /* passed d and e pawns */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_PassedPawnForWhite() {
		PositionManager pm = new PositionManager("8/2pPp3/8/2P1P3/8/8/8/8 w - - 0 1 ");
		int score = SUT.encouragePassedPawns(pm);
		assertEquals(PASSED_PAWN_BOOST /* passed d pawn */, score);
	}
	 
	@Test
	public void test_encouragePassedPawns_PassedPawnForBlack1() {
		PositionManager pm = new PositionManager("8/8/8/8/8/2p1p3/2PpP3/8 b - - 0 1 ");
		int score = SUT.encouragePassedPawns(pm);
		assertEquals(-PASSED_PAWN_BOOST /* passed d pawn */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_TwoPassedPawnsForBlackOneRookFile() {
		PositionManager pm = new PositionManager("8/8/3p3p/8/8/8/2Pp4/8 b - - 0 1");
		int score = SUT.encouragePassedPawns(pm);
		assertEquals(-(PASSED_PAWN_BOOST+ROOK_FILE_PASSED_PAWN_BOOST) /* passed d and e pawns */, score);
	} 
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn() {
		PositionManager pm = new PositionManager("8/8/8/8/8/5p2/6P1/8 w - - 0 1 ");
		int score = SUT.encouragePassedPawns(pm);
		assertEquals(0 /* no passed f pawn, can be taken */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn1() {
		PositionManager pm = new PositionManager("8/8/8/8/5p2/8/6P1/8 w - - 0 1 ");
		int score = SUT.encouragePassedPawns(pm);
		assertEquals(0 /* no passed f pawn, fully passed */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn2() {
		PositionManager pm = new PositionManager("8/8/8/8/6p1/8/6P1/8 w - - 0 1 ");
		int score = SUT.encouragePassedPawns(pm);
		assertEquals(0 /* blocked g pawn not passed */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_NotPassedPawn2_w() {
		PositionManager pm = new PositionManager("8/8/8/8/6p1/8/6P1/8 b - - 0 1 ");
		int score = SUT.encouragePassedPawns(pm);
		assertEquals(0 /* blocked g pawn not passed */, score);
	}
	
	@Test
	public void test_encouragePassedPawns_BothPassedPawns() {
		PositionManager pm = new PositionManager("8/8/8/8/6P1/8/6p1/8 b - - 0 1 ");
		int score = SUT.encouragePassedPawns(pm);
		assertEquals(0 /* both pawns on the same file, passed */, score);
	}
}
