package eubos.score;

import static eubos.score.PositionEvaluator.DOUBLED_PAWN_HANDICAP;
import static eubos.score.PositionEvaluator.PASSED_PAWN_BOOST;
import static eubos.score.PositionEvaluator.ROOK_FILE_PASSED_PAWN_BOOST;
import static eubos.score.PositionEvaluator.ISOLATED_PAWN_HANDICAP;
import static eubos.score.PositionEvaluator.BACKWARD_PAWN_HANDICAP;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.position.MoveList;
import eubos.position.Position;
import eubos.position.PositionManager;
import eubos.search.DrawChecker;
import eubos.search.Score;

public class PositionEvaluatorTest {

	PositionEvaluator SUT;
	PositionManager pm;
	
	@Before
	public void setUp() throws Exception {
	}

	protected void setUpPosition(String fen) {
		pm = new PositionManager(fen, new DrawChecker(), new ReferenceScore(null));
		SUT = (PositionEvaluator) pm.getPositionEvaluator();
	}
	
	@SuppressWarnings("unused")
	@Test
	public void test_evalPosA() {
		setUpPosition("rn2k1nr/1pp2p1p/p7/8/6b1/2P2N2/PPP2PP1/R1BB1RK1 b kq - 0 12");
		if (PositionEvaluator.ENABLE_PAWN_EVALUATION && PositionEvaluator.ENABLE_DYNAMIC_POSITIONAL_EVALUATION) {
			assertEquals(189, Score.getScore(SUT.evaluatePosition())); // Knight good pos, pawn up, doubled pawns, isolated pawn, not endgame, some danger to black king (open file)
		} else if (PositionEvaluator.ENABLE_PAWN_EVALUATION && PositionEvaluator.ENABLE_KING_SAFETY_EVALUATION) {
			assertEquals(159, Score.getScore(SUT.evaluatePosition())); // Knight good pos, pawn up, doubled pawns, not endgame, some danger to black king (open file)
		} else {
			assertEquals(137, Score.getScore(SUT.evaluatePosition())); // Knight good pos, pawn up, not endgame
		}
	}	
	
	@Test
	public void test_DiscourageDoubledPawns_w() {
		setUpPosition("8/pppppp2/8/8/8/1P2P3/1P1P2PP/8 b - - 0 1");
		int score = SUT.evaluatePawnStructure();
		assertEquals(-DOUBLED_PAWN_HANDICAP+ROOK_FILE_PASSED_PAWN_BOOST-2*ISOLATED_PAWN_HANDICAP-3*BACKWARD_PAWN_HANDICAP+6*BACKWARD_PAWN_HANDICAP, score);
	}
	
	@Test
	public void test_DiscourageDoubledPawns_b() {
		setUpPosition("8/pp2p1p1/3p2p1/8/8/8/2PPPPPP/8 w - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		assertEquals(DOUBLED_PAWN_HANDICAP-ROOK_FILE_PASSED_PAWN_BOOST+2*ISOLATED_PAWN_HANDICAP+3*BACKWARD_PAWN_HANDICAP-6*BACKWARD_PAWN_HANDICAP, score);
	}
	
	@Test
	public void test_DiscourageTripledPawns_w() {
		setUpPosition("8/8/8/8/2P5/2P5/2P5/8 w - - 0 38 ");
		int score = SUT.evaluatePawnStructure();
		// black
		int expectedScore = 0;
		// white
		expectedScore += 1*PASSED_PAWN_BOOST-ISOLATED_PAWN_HANDICAP;
		expectedScore += 2*PASSED_PAWN_BOOST-ISOLATED_PAWN_HANDICAP;
		expectedScore += 3*PASSED_PAWN_BOOST-ISOLATED_PAWN_HANDICAP;
		expectedScore += -2*DOUBLED_PAWN_HANDICAP;
		assertEquals(expectedScore, score);
	}

	@Test
	public void test_DiscourageTripledPawns_b() {
		setUpPosition("8/8/8/8/2p5/2p5/2p5/8 w - - 0 38 ");
		int score = SUT.evaluatePawnStructure();
		// black
		int expectedScore = 0;
		expectedScore -= ((7-1)*PASSED_PAWN_BOOST-ISOLATED_PAWN_HANDICAP);
		expectedScore -= ((7-2)*PASSED_PAWN_BOOST-ISOLATED_PAWN_HANDICAP);
		expectedScore -= ((7-3)*PASSED_PAWN_BOOST-ISOLATED_PAWN_HANDICAP);
		expectedScore -= -2*DOUBLED_PAWN_HANDICAP;
		assertEquals(expectedScore, score);
	}
	
	@Test
	public void test_encouragePassedPawns_PassedPawn() {
		setUpPosition("8/8/3pp3/8/3p4/8/2P5/8 b - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		// black
		int expectedScore = 0;
		expectedScore -= -BACKWARD_PAWN_HANDICAP; // black d pawn about to queen
		expectedScore -= (7-5)*PASSED_PAWN_BOOST; // black e pawn 6th rank
		expectedScore -= -BACKWARD_PAWN_HANDICAP; // black d pawn 6th rank
		expectedScore -= -DOUBLED_PAWN_HANDICAP; // black doubled pawns
		// white
		expectedScore += -ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, score);
	}
	
	@Test
	public void test_encouragePassedPawns_TwoPassedPawnsForBlack() {
		setUpPosition("8/8/3pp3/8/8/8/2Pp4/8 b - - 0 1");
		int score = SUT.evaluatePawnStructure();
		// black
		int expectedScore = 0;
		expectedScore -= (7-1)*PASSED_PAWN_BOOST; // black d pawn about to queen
		expectedScore -= ((7-5)*PASSED_PAWN_BOOST - BACKWARD_PAWN_HANDICAP); // black e pawn 6th rank
		expectedScore -= -BACKWARD_PAWN_HANDICAP; // black d pawn 6th rank
		expectedScore -= -DOUBLED_PAWN_HANDICAP; // black doubled pawns
		// white
		expectedScore += -ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, score);
	}
	
	@Test
	public void test_encouragePassedPawns_PassedPawnForWhite() {
		setUpPosition("8/2pPp3/8/2P1P3/8/8/8/8 w - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		// white
		int expectedScore = 0;
		expectedScore += 6*PASSED_PAWN_BOOST;
		expectedScore += -BACKWARD_PAWN_HANDICAP;
		expectedScore += -BACKWARD_PAWN_HANDICAP;
		// white
		expectedScore -= -ISOLATED_PAWN_HANDICAP;
		expectedScore -= -ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, score);
	}
	 
	@Test
	public void test_encouragePassedPawns_PassedPawnForBlack1() {
		setUpPosition("8/8/8/8/8/2p1p3/2PpP3/8 b - - 0 1 ");
		int score = SUT.evaluatePawnStructure();
		// black
		int expectedScore = 0;
		expectedScore -= (7-1)*PASSED_PAWN_BOOST;
		expectedScore -= -BACKWARD_PAWN_HANDICAP;
		expectedScore -= -BACKWARD_PAWN_HANDICAP;
		// white
		expectedScore += -ISOLATED_PAWN_HANDICAP;
		expectedScore += -ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, score);
	}
	
	@Test
	public void test_encouragePassedPawns_TwoPassedPawnsForBlackOneRookFile() {
		setUpPosition("8/8/3p3p/8/8/8/2Pp4/8 b - - 0 1");
		int score = SUT.evaluatePawnStructure();
		// black
		int expectedScore = 0;
		expectedScore -= ((7-1)*PASSED_PAWN_BOOST - ISOLATED_PAWN_HANDICAP); // black d pawn about to queen
		expectedScore -= ((7-5)*ROOK_FILE_PASSED_PAWN_BOOST - ISOLATED_PAWN_HANDICAP); // black h pawn 6th rank
		expectedScore -= -ISOLATED_PAWN_HANDICAP; // black d pawn 6th rank
		expectedScore -= -DOUBLED_PAWN_HANDICAP; // black doubled pawns
		// white
		expectedScore += -ISOLATED_PAWN_HANDICAP;
		assertEquals(expectedScore, score);
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
		assertEquals(-((7-3)*PASSED_PAWN_BOOST)+PASSED_PAWN_BOOST/* both pawns on the same file, passed */, score);
	}
	
	@Test
	public void test_isQuiescent_No_QueenRecapture() throws InvalidPieceException, IllegalNotationException {
		if (!EubosEngineMain.ENABLE_QUIESCENCE_CHECK) {
			setUpPosition("8/8/5p2/4p3/3Q4/8/8/8 w - - 0 1 ");
			int currMove = Move.toMove(new GenericMove("d4e5"), pm.getTheBoard());
			pm.performMove(currMove);
			assertFalse(SUT.isQuiescent(currMove));
		}
	}
	
	@Test
	public void test_isQuiescent_Yes_QueenNoRecapture() throws InvalidPieceException, IllegalNotationException {
		if (!EubosEngineMain.ENABLE_QUIESCENCE_CHECK) {
			setUpPosition("8/8/8/4p3/3Q4/8/8/8 w - - 0 1 ");
			int currMove = Move.toMove(new GenericMove("d4e5"), pm.getTheBoard());
			pm.performMove(currMove);
			assertFalse(SUT.isQuiescent(currMove));
			MoveList ml = new MoveList(pm, Move.NULL_MOVE, null, 0);
			assertTrue(ml.isMateOccurred());
		}
	}
	
	@Test
	public void test_isQuiescent_Yes_NoCaptures() throws InvalidPieceException, IllegalNotationException {
		if (!EubosEngineMain.ENABLE_QUIESCENCE_CHECK) {
			setUpPosition("8/3p4/8/8/3P4/8/8/8 w - - 0 1 ");
			int currMove = Move.toMove(new GenericMove("d4d5"), pm.getTheBoard());
			pm.performMove(currMove);
			assertTrue(SUT.isQuiescent(currMove));
		}
	}
	
	@Test
	public void test_isQuiescent_Yes_LastMoveWasntCapture() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/8/4p3/8/3P4/8/8/8 w - - 0 1 ");
		int currMove = Move.toMove(new GenericMove("d4d5"), pm.getTheBoard());
		pm.performMove(currMove);
		assertTrue(SUT.isQuiescent(currMove));
	}
	
	@Test
	public void test_isQuiescent_Yes_LastMoveWasCapture_NoRecapturesPossible_Alt() throws InvalidPieceException, IllegalNotationException {
		if (!EubosEngineMain.ENABLE_QUIESCENCE_CHECK) {
			setUpPosition("rp6/1p6/Pp6/8/1p6/1p6/PP6/QP6 b - - 0 41");
			int currMove = Move.toMove(new GenericMove("a8a6"), pm.getTheBoard());
			pm.performMove(currMove);
			assertFalse(SUT.isQuiescent(currMove));
			// Shall now do an extended search and see that the move list is empty, so stand PAT.
			MoveList ml = new MoveList(pm, Move.NULL_MOVE, null, 0);
			assertFalse(ml.getStandardIterator(true, Position.a6).hasNext());
		}
	}
	 
	@Test
	public void test_isQuiescent_No_LastMoveWasCheck() throws InvalidPieceException, IllegalNotationException {
		if (EubosEngineMain.ENABLE_QUIESCENCE_CHECK) {
			setUpPosition("1r1k1r2/p5Q1/2p3p1/8/1q1p2n1/3P2P1/P3RPP1/4RK2 b - - 0 1");
			int currMove = Move.toMove(new GenericMove("f8f2"), pm.getTheBoard());
			pm.performMove(currMove);
			assertFalse(SUT.isQuiescent(currMove));
		}
	}
	
	@Test
	public void test_isQuiescent_Yes_LastMoveWasCheckMate() throws InvalidPieceException, IllegalNotationException {
		if (EubosEngineMain.ENABLE_QUIESCENCE_CHECK) {
			setUpPosition("5r1k/p2R4/1pp2p1p/8/5q2/3Q1bN1/PP3P2/6K1 w - - - -");
			int currMove = Move.valueOf(0, 0, Position.d3, Piece.WHITE_QUEEN, Position.h7, Piece.NONE, Piece.NONE);
			pm.performMove(currMove);
			assertFalse(SUT.isQuiescent(currMove));
		}
	}
	
	@Test
	public void test_custom_position_score_reporter() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("4r1k1/2p2pb1/4Q3/8/3pPB2/1p1P3p/1P3P2/R5K1 b - - 0 42");
		System.out.println(SUT.evaluatePosition());
	}
	
	@Test
	public void test_isQuiescent_No_PromotionPossible() throws InvalidPieceException, IllegalNotationException {
		if (EubosEngineMain.ENABLE_QUIESCENCE_CHECK) {
			setUpPosition("8/4P3/8/8/8/8/8/8 w - - 0 1");
			assertFalse(SUT.isQuiescent(Move.NULL_MOVE));
		}
	}
	
	@Test
	public void test_isQuiescent_Yes_PromotionPossibleButNotForOnMove() throws InvalidPieceException, IllegalNotationException {
		if (EubosEngineMain.ENABLE_QUIESCENCE_CHECK) {
			setUpPosition("8/4P3/8/8/8/8/8/8 b - - 0 1");
			assertTrue(SUT.isQuiescent(Move.NULL_MOVE));
		}
	}
	
	@Test
	public void test_isQuiescent_Yes_PromotionPossibleButNotForOnMoveBlack() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/8/8/8/8/8/4p3/8 w - - 0 1");
		assertTrue(SUT.isQuiescent(Move.NULL_MOVE));
	}
	
	@Test
	public void test_isQuiescent_Yes_PromotionBlockaded() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("4n3/4P3/8/8/8/8/8/8 w - - 0 1");
		assertTrue(SUT.isQuiescent(Move.NULL_MOVE));
	}
	
	@Test
	public void test_isQuiescent_Yes_PromotionBlockaded_Black() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/8/8/8/8/8/4p3/4N3 b - - 0 1");
		assertTrue(SUT.isQuiescent(Move.NULL_MOVE));
	}
	
	@Test
	public void test_isQuiescent_No_LastMoveWasPromotionBishop() throws InvalidPieceException, IllegalNotationException {
		if (EubosEngineMain.ENABLE_QUIESCENCE_CHECK) {
			setUpPosition("8/4P3/8/8/8/8/8/8 w - - 0 1");
			int currMove = Move.valueOf(Move.TYPE_PROMOTION_MASK, Position.e7, Piece.WHITE_PAWN, Position.f8, Piece.NONE, Piece.BISHOP);
			pm.performMove(currMove);
			assertTrue(SUT.isQuiescent(currMove)); // only care about queen promotions for now
		}
	}
	
	@Test
	public void test_isQuiescent_No_LastMoveWasPromotionQueenWithCheckAndCapture() throws InvalidPieceException, IllegalNotationException {
		if (EubosEngineMain.ENABLE_QUIESCENCE_CHECK) {
			setUpPosition("5q2/4P3/7k/8/8/8/8/8 w - - 0 1");
			int currMove = Move.valueOf(0, Move.TYPE_PROMOTION_MASK | Move.TYPE_CAPTURE_MASK, Position.e7, Piece.WHITE_PAWN, Position.f8, Piece.BLACK_QUEEN, Piece.QUEEN);
			pm.performMove(currMove);
			assertFalse(SUT.isQuiescent(currMove));
		}
	}
	
	@Test
	public void test_isQuiescent_Yes_LastMoveWasntPromotion() throws InvalidPieceException, IllegalNotationException {
		if (EubosEngineMain.ENABLE_QUIESCENCE_CHECK) {
			setUpPosition("8/4P3/8/8/8/8/8/B7 w - - 0 1");
			int currMove = Move.valueOf(Position.a1, Piece.WHITE_BISHOP, Position.b2, Piece.NONE);
			pm.performMove(currMove);
			assertTrue(SUT.isQuiescent(currMove));
		}
	}
	
	@Test
	public void test_isQuiescent_Yes_LastMoveWasCheck_alt() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("8/4P3/7k/8/8/8/1B6/8 w - - 0 1");
		int currMove = Move.valueOf(0, 0, Position.b2, Piece.WHITE_BISHOP, Position.c1, Piece.NONE, Piece.NONE);
		pm.performMove(currMove);
		assertFalse(SUT.isQuiescent(currMove));
	}
	
	@Test
	@Ignore
	public void test_updateMaterialForMove_queen_promotion() throws InvalidPieceException {
		setUpPosition("8/4P3/7k/8/8/8/1B6/8 w - - 0 1");
		PiecewiseEvaluation initialMe = pm.getTheBoard().evaluateMaterial();
		short initial = initialMe.getDelta();
		int promotionMove = Move.valueOf(Move.TYPE_PROMOTION_MASK, Position.e7, Piece.WHITE_PAWN, Position.e8, Piece.NONE, Piece.QUEEN);
		
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
		int castleMove = Move.valueOf(Move.TYPE_REGULAR_NONE, Position.e8, Piece.BLACK_KING, Position.g8, Piece.NONE, Piece.NONE);
		
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
	
	@Test
	@Ignore
	public void test_investigate_bad_evaluation() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("r1b1k3/1p1p1p1p/p3pR2/8/3KP3/1PN3r1/P1PQB3/2q5 b q - 4 21");
		// black good rook, white bad queen, but this isn't quiet! In the position (dubious continuation) white is about to lose queen!
		// this happens because one of the continuations goes into an extended search and immediately runs out of moves, backing up a bad score as exact :(
		// I could write some unit tests for this as it is a good test of the evaluation that happens when we terminate an extended search.
		assertEquals(-441, Score.getScore(SUT.evaluatePosition()));
		// bishop interpose
		setUpPosition("r1b1k3/1p1p1p1p/p3pR2/8/4P3/1PN1KBr1/P1PQ4/2q5 b q - 4 21");
		assertEquals(-419, Score.getScore(SUT.evaluatePosition()));
		// rook interpose
		setUpPosition("r1b1k3/1p1p1p1p/p3p3/8/4P3/1PN1KRr1/P1PQB3/2q5 b q - 4 21");
		assertEquals(-419, Score.getScore(SUT.evaluatePosition()));
		// alternate King move 1
		setUpPosition("r1b1k3/1p1p1p1p/p3pR2/8/4PK2/1PN3r1/P1PQB3/2q5 b q - 4 21 ");
		assertEquals(-431, Score.getScore(SUT.evaluatePosition()));
		// alternate King move 2
		setUpPosition("r1b1k3/1p1p1p1p/p3pR2/8/4P3/1PN3r1/P1PQBK2/2q5 b q - 4 21 ");
		assertEquals(-391, Score.getScore(SUT.evaluatePosition()));
	}
	
	
/* Test surrounding the anomalous score in the below SDA log....

	undo(g3a3:r, L:510) @3
do(g3h3:r) @3
	search @:4 prov:4 @time 19-34-24.689453
	fen:6k1/6pp/B3b3/P1B1Rn2/2P5/7r/1r6/R6K w - - - 37
	do(h1g1:K) @4
		search @:5 prov:43 @time 19-34-24.689453
		fen:6k1/6pp/B3b3/P1B1Rn2/2P5/7r/1r6/R5K1 b - - - 37
		hash:7120664301139237326 seed:trans best=b2h2:r, dep=5, sc=43, type=2 object:eubos.search.transposition.Transposition@537d6635 @5
		do(b2h2:r) @5
		undo(b2h2:r, X:95) @5
		do(e6c4:b:P) @5
			search @:6 prov:4 @time 19-34-24.689453
			fen:6k1/6pp/B7/P1B1Rn2/2b5/7r/1r6/R5K1 w - - - 38
			hash:-7551081746862432276 seed:trans best=a6c4:B:b, dep=4, sc=480, type=3 object:eubos.search.transposition.Transposition@79f8e8cc @6
			do(a6c4:B:b) @6
				search @:7 prov:43 @time 19-34-24.689453
				fen:6k1/6pp/8/P1B1Rn2/2B5/7r/1r6/R5K1 b - - - 38
				hash:761058831192763197 seed:trans best=g8h8:k, dep=3, sc=480, type=2 object:eubos.search.transposition.Transposition@1a939857 @7
				extSearch NoMoves term @7 score:243
				backedUp was:E:43 now:U:243 @7
			undo(a6c4:B:b, U:243) @6
			ab cmp prev:43 curr:243 @6
			ref now:U:243 @6
		undo(e6c4:b:P, L:243) @5
		do(f5d4:n) @5
		undo(f5d4:n, X:65) @5
		
		Killer? Perhaps print move flags to help identify such?
		
		do(g7g5:p) @5
		undo(g7g5:p, X:9) @5
		backedUp was:E:43 now:E:9 @5
		pc:g7g5:p 
		do(g8f7:k) @5
		undo(g8f7:k, X:94) @5
		do(b2b1:r) @5
		undo(b2b1:r, X:77) @5
		do(b2b3:r) @5
		undo(b2b3:r, X:85) @5
		do(b2b4:r) @5
		undo(b2b4:r, X:87) @5
		do(b2b5:r) @5
		undo(b2b5:r, X:91) @5
		do(b2b6:r) @5
		undo(b2b6:r, X:85) @5
		do(b2b7:r) @5
		undo(b2b7:r, X:79) @5
		do(b2b8:r) @5
		undo(b2b8:r, X:79) @5
		do(b2a2:r) @5
		undo(b2a2:r, X:83) @5
		do(b2c2:r) @5
		undo(b2c2:r, X:89) @5
		do(b2d2:r) @5
		undo(b2d2:r, X:79) @5
		do(b2e2:r) @5
		undo(b2e2:r, X:83) @5
		do(b2f2:r) @5
		undo(b2f2:r, X:85) @5
		do(b2g2:r) @5
		undo(b2g2:r, X:95) @5
		do(h3h2:r) @5
		undo(h3h2:r, X:85) @5
		do(h3h1:r) @5
		undo(h3h1:r, X:95) @5
		do(h3h4:r) @5
		undo(h3h4:r, X:85) @5
		do(h3h5:r) @5
		undo(h3h5:r, X:91) @5
		do(h3h6:r) @5
		undo(h3h6:r, X:89) @5
		do(h3g3:r) @5
		undo(h3g3:r, X:-11) @5
		ab cmp prev:4 curr:-11 @5
		ref now:X:-11 @5
	undo(h1g1:K, U:-11) @4
	trans create, hash:-8931909632838107069
	trans best=h1g1:K, dep=2, sc=-11, type=3 hash:-8931909632838107069 object:eubos.search.transposition.Transposition@3b8e54fd
undo(g3h3:r, L:-11) @3
ab cmp prev:4 curr:-11 @3
trans best=g3h3:r, dep=3, sc=-11, type=2 hash:-5530230281005173188 object:eubos.search.transposition.Transposition@15d1517
ref now:L:-11 @3
undo(g1h1:K, U:-11) @2
trans create, hash:6168375866375925681
trans best=g1h1:K, dep=4, sc=-11, type=3 hash:6168375866375925681 object:eubos.search.transposition.Transposition@2f931baa

My theory is that it could only be more in favour of black like this if the material balance was altered by a capture!?!

No, it was caused by returning a drawn position when that is inconsistent with our search goal.
*/
	@Test
	@Ignore
	public void test_pos_last_good() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("6k1/6pp/B3b3/P1B1Rn2/2P5/7r/1r6/R5K1 b - - - 37");
		pm.performMove(Move.toMove(new GenericMove("h3h6"), pm.getTheBoard()));
		assertEquals(89, Score.getScore(SUT.evaluatePosition()));
	}
	
	@Test
	@Ignore
	public void test_pos_bad_unexpected_eval_in_eubos_run_repeatable_apply_move() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("6k1/6pp/B3b3/P1B1Rn2/2P5/7r/1r6/R5K1 b - - - 37");
		pm.performMove(Move.toMove(new GenericMove("h3g3"), pm.getTheBoard()));
		assertEquals(-11, Score.getScore(SUT.evaluatePosition()));
	}
	
	@Test
	@Ignore
	public void test_pos_bad_unexpected_eval_in_eubos_run_repeatable_exact_pos_fen() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("6k1/6pp/B3b3/P1B1Rn2/2P5/6r1/1r6/R5K1 w - - 1 38");
		assertEquals(-11, Score.getScore(SUT.evaluatePosition()));
	}
	
	@Test
	@Ignore
	public void test_pos() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("2b3k1/6pp/8/P3R3/2P5/6nr/5K2/R7 b - - 0 38");
		assertEquals(-8, Score.getScore(SUT.evaluatePosition()));
	}
	
	@Test
	@Ignore
	public void test_defect_board_state_when_making_moves_from_last_good() throws InvalidPieceException, IllegalNotationException {
		setUpPosition("6k1/6pp/B3b3/P1B1Rn2/2P5/7r/1r6/R5K1 b - - - 37");
		pm.performMove(Move.toMove(new GenericMove("h3h6"), pm.getTheBoard()));
		pm.unperformMove();
		pm.performMove(Move.toMove(new GenericMove("h3g3"), pm.getTheBoard()));
		assertEquals(-11, Score.getScore(SUT.evaluatePosition()));
	}
}
