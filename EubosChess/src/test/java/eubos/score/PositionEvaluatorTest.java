package eubos.score;

import static eubos.score.PositionEvaluator.DOUBLED_PAWN_HANDICAP;
import static eubos.score.PositionEvaluator.PASSED_PAWN_BOOST;
import static eubos.score.PositionEvaluator.ROOK_FILE_PASSED_PAWN_BOOST;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.board.Piece;
import eubos.position.Move;
import eubos.position.Position;
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
		SUT = new PositionEvaluator(pm);
		pm.registerPositionEvaluator(SUT);
	}
	
	@Test
	public void test_evalPosA() {
		setUpPosition("rn2k1nr/1pp2p1p/p7/8/6b1/2P2N2/PPP2PP1/R1BB1RK1 b kq - 0 12");
		assertEquals(170, SUT.evaluatePosition().getScore()); // Knight good pos, pawn up, castled, not endgame
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
		pm.performMove(Move.toMove(new GenericMove("d4e5"), pm.getTheBoard()));
		assertFalse(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_Yes_QueenNoRecapture() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/8/8/4p3/3Q4/8/8/8 w - - 0 1 ");
		pm.performMove(Move.toMove(new GenericMove("d4e5"), pm.getTheBoard()));
		assertTrue(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_Yes_NoCaptures() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/3p4/8/8/3P4/8/8/8 w - - 0 1 ");
		pm.performMove(Move.toMove(new GenericMove("d4d5"), pm.getTheBoard()));
		assertTrue(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_Yes_LastMoveWasntCapture() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/8/4p3/8/3P4/8/8/8 w - - 0 1 ");
		pm.performMove(Move.toMove(new GenericMove("d4d5"), pm.getTheBoard()));
		assertTrue(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_Yes_LastMoveWasCapture_NoRecapturesPossible_Alt() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("rp6/1p6/Pp6/8/1p6/1p6/PP6/QP6 b - - 0 41");
		pm.performMove(Move.toMove(new GenericMove("a8a6"), pm.getTheBoard()));
		assertTrue(SUT.isQuiescent());
	}
	 
	@Test
	public void test_isQuiescent_No_LastMoveWasCheck() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("1r1k1r2/p5Q1/2p3p1/8/1q1p2n1/3P2P1/P3RPP1/4RK2 b - - 0 1");
		pm.performMove(Move.toMove(new GenericMove("f8f2"), pm.getTheBoard()));
		assertFalse(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_No_LastMoveWasCheckMate() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("5r1k/p2R4/1pp2p1p/8/5q2/3Q1bN1/PP3P2/6K1 w - - - -");
		pm.performMove(Move.valueOf(Move.TYPE_CHECK_MASK, Position.d3, Piece.WHITE_QUEEN, Position.h7, Piece.NONE, Piece.NONE));
		assertFalse(SUT.isQuiescent());
	}
	
	@Test
	@Ignore 
	/* not needed as we will only extend searches by having applied moves, and whether they are in check is 
	 * now determined by checking the previous move type. */
	public void test() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("4r3/7P/2k5/1P6/8/6P1/8/6K1 b - - 0 57");
		assertFalse(SUT.isQuiescent());
	}
	
	@Test
	@Ignore 
	/* not needed as we will only extend searches by having applied moves, and whether they are in check is 
	 * now determined by checking the previous move type. */
	public void test2() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("4r3/7P/2k5/1Q5r/P7/6P1/8/6K1 b - - 10 56");
		assertFalse(SUT.isQuiescent());
		assertEquals(371, SUT.evaluatePosition());
	}
	
	@Test
	public void test_custom_position_score_reporter() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("4r1k1/2p2pb1/4Q3/8/3pPB2/1p1P3p/1P3P2/R5K1 b - - 0 42");
		System.out.println(SUT.evaluatePosition());
	}
	
	@Test
	public void test_isQuiescent_No_PromotionPossible() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/4P3/8/8/8/8/8/8 w - - 0 1");
		assertFalse(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_Yes_PromotionPossibleButNotForOnMove() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/4P3/8/8/8/8/8/8 b - - 0 1");
		assertTrue(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_Yes_PromotionPossibleButNotForOnMoveBlack() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/8/8/8/8/8/4p3/8 w - - 0 1");
		assertTrue(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_Yes_PromotionBlockaded() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("4n3/4P3/8/8/8/8/8/8 w - - 0 1");
		assertTrue(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_Yes_PromotionBlockaded_Black() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/8/8/8/8/8/4p3/4N3 b - - 0 1");
		assertTrue(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_No_LastMoveWasPromotionBishop() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/4P3/8/8/8/8/8/8 w - - 0 1");
		pm.performMove(Move.valueOf(Move.TYPE_PROMOTION_PIECE_MASK, Position.e7, Piece.WHITE_PAWN, Position.f8, Piece.NONE, Piece.BISHOP));
		assertFalse(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_No_LastMoveWasPromotionQueenWithCheckAndCapture() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("5q2/4P3/7k/8/8/8/8/8 w - - 0 1");
		pm.performMove(Move.valueOf(Move.TYPE_PROMOTION_QUEEN_MASK | Move.TYPE_CAPTURE_MASK | Move.TYPE_CHECK_MASK, Position.e7, Piece.WHITE_PAWN, Position.f8, Piece.BLACK_QUEEN, Piece.QUEEN));
		assertFalse(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_Yes_LastMoveWasntPromotion() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/4P3/8/8/8/8/8/B7 w - - 0 1");
		pm.performMove(Move.valueOf(Position.a1, Piece.WHITE_BISHOP, Position.b2, Piece.NONE));
		assertTrue(SUT.isQuiescent());
	}
	
	@Test
	public void test_isQuiescent_No_LastMoveWasCheck_alt() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/4P3/7k/8/8/8/1B6/8 w - - 0 1");
		pm.performMove(Move.valueOf(Move.TYPE_CHECK_MASK, Position.b2, Piece.WHITE_BISHOP, Position.c1, Piece.NONE, Piece.NONE));
		assertFalse(SUT.isQuiescent());
	}
	
	@Test
	@Ignore
	public void test_updateMaterialForMove_queen_promotion() throws InvalidPieceException {
		setUpPosition("8/4P3/7k/8/8/8/1B6/8 w - - 0 1");
		PiecewiseEvaluation initialMe = pm.getTheBoard().evaluateMaterial();
		short initial = initialMe.getDelta();
		int promotionMove = Move.valueOf(Move.TYPE_PROMOTION_QUEEN_MASK, Position.e7, Piece.WHITE_PAWN, Position.e8, Piece.NONE, Piece.QUEEN);
		
		pm.performMove(promotionMove);
		
		PiecewiseEvaluation me = pm.getTheBoard().me;
		assertNotEquals(initial, me.getDelta());
		
		pm.unperformMove();
		
		assertEquals(initial, me.getDelta());
	}
	
	@Test
	@Ignore
	public void test_updateMaterialForMove_castle() throws InvalidPieceException {
		setUpPosition("4k2r/2Q2ppp/8/3r4/1P5P/P1p5/4PP2/R3K1N1 b Qk - - -");
		PiecewiseEvaluation initialMe = pm.getTheBoard().evaluateMaterial();
		short initial = initialMe.getDelta();
		int castleMove = Move.valueOf(Move.TYPE_CASTLE_MASK, Position.e8, Piece.BLACK_KING, Position.g8, Piece.NONE, Piece.NONE);
		
		pm.performMove(castleMove);
		PiecewiseEvaluation me = pm.getTheBoard().me;
		
		assertNotEquals(initial, me.getDelta());

		pm.unperformMove();
		assertEquals(initial, me.getDelta());
	}
	
	@Test
	@Ignore // because needs PSTs to be applied in evaluation to make any sense.
	public void test_updateMaterialForMove_rookMove() throws InvalidPieceException {
		setUpPosition("4k3/8/4p3/5b2/8/8/8/4R3 w - - - -");
		PiecewiseEvaluation initialMe = pm.getTheBoard().evaluateMaterial();
		short initial = initialMe.getDelta();
		int rookMove = Move.valueOf(Move.TYPE_REGULAR_NONE, Position.e1, Piece.WHITE_ROOK, Position.e5, Piece.NONE, Piece.NONE);
		
		pm.performMove(rookMove);
		PiecewiseEvaluation me = pm.getTheBoard().me;
		
		assertNotEquals(initial, me.getDelta());
		SUT.evaluatePosition();

		pm.unperformMove();
		assertEquals(initial, me.getDelta());
		
		int rookMove2 = Move.valueOf(Move.TYPE_REGULAR_NONE, Position.e1, Piece.WHITE_ROOK, Position.e2, Piece.NONE, Piece.NONE);
		pm.performMove(rookMove2);
		
		assertNotEquals(initial, me.getDelta());
		SUT.evaluatePosition();

		pm.unperformMove();
		assertEquals(initial, me.getDelta());
	}
	
	@Test
	@Ignore
	public void test_updateMaterialForMove_capture() throws InvalidPieceException {
		setUpPosition("7k/p7/8/8/3n4/4PPP1/8/7K w - - 0 1");
		PiecewiseEvaluation initialMe = pm.getTheBoard().evaluateMaterial();
		short initial = initialMe.getDelta();
		int captureMove = Move.valueOf(Move.TYPE_CAPTURE_MASK, Position.e3, Piece.WHITE_PAWN, Position.d4, Piece.BLACK_KNIGHT, Piece.NONE);
		
		pm.performMove(captureMove);

		PiecewiseEvaluation me = pm.getTheBoard().me;
		assertNotEquals(initial, me.getDelta());
		
		pm.unperformMove();
		assertEquals(initial, me.getDelta());
	}
}
