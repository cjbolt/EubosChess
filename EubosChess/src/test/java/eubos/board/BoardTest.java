package eubos.board;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.position.Move;
import eubos.position.Position;
import eubos.position.PositionManager;
import eubos.score.PiecewiseEvaluation;
import eubos.search.DrawChecker;

public class BoardTest {
	
	private Board classUnderTest;
	private Map<Integer, Integer> pieceMap;
	private static final int testSq = Position.a1;
	
	@Before
	public void setUp() throws Exception {
		pieceMap = new HashMap<Integer, Integer>();
		classUnderTest = new Board(pieceMap, Piece.Colour.white);
	}

	@Test
	public void testBoard() {
		assertTrue(classUnderTest!=null);
	}

	@Test
	public void testSetEnPassantTargetSq() {
		classUnderTest.setEnPassantTargetSq( testSq );
	}
	
	@Test
	public void testGetEnPassantTargetSq_uninitialised() {
		int square = classUnderTest.getEnPassantTargetSq();
		assertTrue(square == Position.NOPOSITION);
	}

	@Test
	public void testGetEnPassantTargetSq_initialised() {
		classUnderTest.setEnPassantTargetSq(testSq);
		int square = classUnderTest.getEnPassantTargetSq();
		assertTrue(square == Position.a1);
	}	

	@Test
	public void testSetPieceAtSquare_and_squareIsEmpty() {
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		classUnderTest.setPieceAtSquare(testSq, Piece.WHITE_PAWN);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
	}

	@Test
	public void testPickUpPieceAtSquare_Exists()  {
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		classUnderTest.setPieceAtSquare(testSq, Piece.WHITE_PAWN);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
		int pickedUpPiece = classUnderTest.pickUpPieceAtSquare(testSq, Piece.WHITE_PAWN);
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		assertEquals(Piece.WHITE_PAWN, pickedUpPiece);
	}
	
	@Test
	public void testPickUpPieceAtSquare_DoesntExist()  {
		assertEquals(Piece.NONE, classUnderTest.pickUpPieceAtSquare(testSq, Piece.NONE));
	}	

	@Test
	public void testGetPieceAtSquare_Exists() {
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		classUnderTest.setPieceAtSquare(testSq, Piece.WHITE_PAWN);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
		int gotPiece = classUnderTest.getPieceAtSquare(testSq);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
		assertTrue(gotPiece==Piece.BLACK_PAWN || gotPiece==Piece.WHITE_PAWN);
	}
	
	@Test
	public void testGetPieceAtSquare_DoesntExist() {
		assertTrue(classUnderTest.getPieceAtSquare(testSq)==Piece.NONE);
	}
	
	@Test
	public void testGetAsFenString() {
		classUnderTest.setPieceAtSquare(testSq, Piece.WHITE_PAWN);
		assertEquals("8/8/8/8/8/8/8/P7",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString1() {
		classUnderTest.setPieceAtSquare(testSq, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.c1, Piece.WHITE_KING);
		assertEquals("8/8/8/8/8/8/8/P1K5",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString2() {
		classUnderTest.setPieceAtSquare(Position.h1, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.g1, Piece.WHITE_KING);
		assertEquals("8/8/8/8/8/8/8/6KP",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString3() {
		classUnderTest.setPieceAtSquare(Position.h1, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.g1, Piece.BLACK_KING);
		assertEquals("8/8/8/8/8/8/8/6kp",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString4() {
		classUnderTest.setPieceAtSquare(Position.h8, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.g8, Piece.BLACK_KING);
		assertEquals("6kp/8/8/8/8/8/8/8",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testOpenFile_isOpen() {
		classUnderTest.setPieceAtSquare(Position.h8, Piece.WHITE_ROOK);
		assertEquals(14, classUnderTest.calculateRankFileMobility(classUnderTest.getWhiteRooks(), 0));
	}
	
	@Test
	public void testOpenFile_isClosed() {
		classUnderTest.setPieceAtSquare(Position.h7, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.h2, Piece.WHITE_ROOK);
		assertEquals(12, classUnderTest.calculateRankFileMobility(classUnderTest.getWhiteRooks(), 0));
	}
	
	@Test
	public void testOpenFile_isOpen1() {
		classUnderTest.setPieceAtSquare(Position.d7, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.e2, Piece.WHITE_ROOK);
		assertEquals(14, classUnderTest.calculateRankFileMobility(classUnderTest.getWhiteRooks(), 0));
	}
	
	@Test
	public void testisHalfOpenFile_isHalfOpen() {
		classUnderTest.setPieceAtSquare(Position.e7, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.e2, Piece.WHITE_ROOK);
		assertTrue(classUnderTest.isOnHalfOpenFile(GenericPosition.e2, Piece.WHITE_ROOK));
	}
	
	@Test
	public void testisHalfOpenFile_isNotHalfOpen() {
		classUnderTest.setPieceAtSquare(Position.e7, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.e2, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.e1, Piece.WHITE_ROOK);
		assertFalse(classUnderTest.isOnHalfOpenFile(GenericPosition.e1, Piece.WHITE_ROOK));
	}
	
	@Test
	@Ignore
	public void testisHalfOpenFile_isNotHalfOpen1() {
		classUnderTest.setPieceAtSquare(Position.e7, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.e2, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.e1, Piece.WHITE_ROOK);
		assertTrue(classUnderTest.isOnHalfOpenFile(GenericPosition.e1, Piece.WHITE_ROOK));
	}
	
	@Test
	public void testisOnOpenDiagonal_Yes() {
		classUnderTest.setPieceAtSquare(Position.d5, Piece.BLACK_BISHOP);
		classUnderTest.setPieceAtSquare(Position.e5, Piece.WHITE_PAWN);
		assertEquals(13, classUnderTest.calculateDiagonalMobility(classUnderTest.getBlackBishops(), 0));
	}
	
	@Test
	public void testisOnOpenDiagonal_No() {
		classUnderTest.setPieceAtSquare(Position.d5, Piece.BLACK_BISHOP);
		classUnderTest.setPieceAtSquare(Position.e6, Piece.WHITE_PAWN);
		assertEquals(10, classUnderTest.calculateDiagonalMobility(classUnderTest.getBlackBishops(), 0));
	}
	
	@Test
	public void testisOnOpenDiagonal_Yes1() {
		classUnderTest.setPieceAtSquare(Position.d5, Piece.BLACK_BISHOP);
		classUnderTest.setPieceAtSquare(Position.e5, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d6, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d4, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.c5, Piece.WHITE_PAWN);
		assertEquals(13, classUnderTest.calculateDiagonalMobility(classUnderTest.getBlackBishops(), 0));
	}
	
	@Test
	public void testisOnOpenDiagonal_No1() {
		classUnderTest.setPieceAtSquare(Position.a1, Piece.BLACK_BISHOP);
		classUnderTest.setPieceAtSquare(Position.h8, Piece.WHITE_PAWN);
		assertEquals(6, classUnderTest.calculateDiagonalMobility(classUnderTest.getBlackBishops(), 0));
	}
	
	@Test
	public void testisOnOpenDiagonal_Yes2() {
		classUnderTest.setPieceAtSquare(Position.a1, Piece.BLACK_BISHOP);
		classUnderTest.setPieceAtSquare(Position.a8, Piece.WHITE_PAWN);
		assertEquals(7, classUnderTest.calculateDiagonalMobility(classUnderTest.getBlackBishops(), 0));
	}
	
	@Test
	public void testCouldLeadToCheck_Yes() {
		classUnderTest.setPieceAtSquare(Position.d2, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d1, Piece.WHITE_KING);
		int move = Move.valueOf(Position.d2, Piece.WHITE_PAWN, Position.d4, Piece.NONE);
		assertTrue(classUnderTest.moveCouldLeadToOwnKingDiscoveredCheck(move, Piece.WHITE_PAWN));
	}
	
	@Test
	public void testCouldLeadToCheck_Yes1() {
		classUnderTest.setPieceAtSquare(Position.e2, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d1, Piece.WHITE_KING);
		int move = Move.valueOf(Position.e2, Piece.WHITE_PAWN, Position.d3, Piece.NONE);
		assertTrue(classUnderTest.moveCouldLeadToOwnKingDiscoveredCheck(move, Piece.WHITE_PAWN));
	}
	
	@Test
	public void testCouldLeadToCheck_No() {
		classUnderTest.setPieceAtSquare(Position.e3, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d1, Piece.WHITE_KING);
		int move = Move.valueOf(Position.e3, Piece.WHITE_PAWN, Position.e4, Piece.NONE);
		assertFalse(classUnderTest.moveCouldLeadToOwnKingDiscoveredCheck(move, Piece.WHITE_PAWN));
	}
	
	@Test
	public void testCouldLeadToCheck_No1() {
		classUnderTest.setPieceAtSquare(Position.d1, Piece.WHITE_KNIGHT);
		classUnderTest.setPieceAtSquare(Position.e4, Piece.WHITE_KING);
		int move = Move.valueOf(Position.d1, Piece.WHITE_KNIGHT, Position.c3, Piece.NONE);
		assertFalse(classUnderTest.moveCouldLeadToOwnKingDiscoveredCheck(move, Piece.WHITE_PAWN));
	}
	
	protected void setUpPosition(String fen) {
		PositionManager pm = new PositionManager(fen, new DrawChecker(), null);
		classUnderTest = pm.getTheBoard();
	}
	
	@Test
	public void test_isInsufficientMaterial_JustKings()throws IllegalNotationException {
		setUpPosition("8/8/8/8/8/8/k/7K w - - 0 1");
		assertTrue(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_RookOnBoard()throws IllegalNotationException {
		setUpPosition("8/R7/8/8/8/8/k/7K w - - 0 1");
		assertFalse(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_QueenOnBoard()throws IllegalNotationException {
		setUpPosition("8/Q7/8/8/8/8/k/7K w - - 0 1");
		assertFalse(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_TwoKnights()throws IllegalNotationException {
		setUpPosition("8/K7/8/K7/8/8/k/7K w - - 0 1");
		assertTrue(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_BishopKnight()throws IllegalNotationException {
		setUpPosition("8/B7/8/4N3/8/8/k/7K w - - 0 1");
		assertFalse(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_BishopKnightDifferentSides()throws IllegalNotationException {
		setUpPosition("8/B7/8/4n3/8/8/k/7K w - - 0 1");
		assertTrue(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_TwoBishops()throws IllegalNotationException {
		setUpPosition("BB6/8/8/8/8/8/k/7K w - - 0 1");
		assertFalse(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_TwoBishopsDifferentSides()throws IllegalNotationException {
		setUpPosition("Bb6/8/8/8/8/8/k/7K w - - 0 1");
		assertTrue(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_SingleBishop()throws IllegalNotationException {
		setUpPosition("B7/8/8/8/8/8/k/7K w - - 0 1");
		assertTrue(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_PawnOnBoard()throws IllegalNotationException {
		setUpPosition("8/P/8/8/8/8/k/7K w - - 0 1");
		assertFalse(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_evaluateKingSafety_safe()throws IllegalNotationException {
		setUpPosition("5krr/4pppp/6bq/8/8/6BQ/4PPPP/5KRR b - - 13 1");
		assertEquals(-30, classUnderTest.evaluateKingSafety(Piece.Colour.white)); // 5 squares, can be attacked by three pieces
		assertEquals(-30, classUnderTest.evaluateKingSafety(Piece.Colour.black));
	}
	
	@Test
	public void test_evaluateKingSafety_notVerySafe()throws IllegalNotationException {
		setUpPosition("6rr/5ppp/1k4bq/8/8/1K4BQ/5PPP/6RR b - - 13 1 ");
		assertEquals(-120, classUnderTest.evaluateKingSafety(Piece.Colour.black)); // diagonals 7 squares, can be attacked by two pieces; r'n'f 9 squares can be attacked by three pieces
		assertEquals(-120, classUnderTest.evaluateKingSafety(Piece.Colour.white));
	}
	
	@Test
	public void test_evaluateKingSafety_No_inEndgame()throws IllegalNotationException {
		setUpPosition("8/8/8/8/8/8/8/K7 w - - 0 1");
		assertEquals(0, classUnderTest.evaluateKingSafety(Piece.Colour.white));
	}
	
	@Test
	public void test_evaluateKingSafety_No_opposingBishopWrongColour()throws IllegalNotationException {
		setUpPosition("r4rk1/1p3p2/p7/P2P1p1B/4p3/2b5/3R1PPP/4K2R b K - 13 1 ");
		assertEquals(-50, classUnderTest.evaluateKingSafety(Piece.Colour.white)); // 7*2*2 rnf 0 diag = 28
		assertEquals(-34, classUnderTest.evaluateKingSafety(Piece.Colour.black)); // 7*2*2 rnf 1*2*1 = 30
	}
	
	@Test
	public void test_evaluateKingSafety_Yes_opposingBishopRightColour()throws IllegalNotationException {
		setUpPosition("r4rk1/1p6/p7/P2P1p1B/4p3/2b5/3R1PPP/2K4R b - - 13 1 ");
		assertEquals(-50, classUnderTest.evaluateKingSafety(Piece.Colour.white));
	}
	
	@Test
	public void test_evaluateKingSafety_Yes_opposingQueenBishop()throws IllegalNotationException {
		setUpPosition("r4rk1/1p6/p7/P2P1p1B/4p3/2b5/3R1PPP/Q1K4R b - - 13 1 ");
		assertEquals(-50, classUnderTest.evaluateKingSafety(Piece.Colour.white));  // (5 up right + 2 up left) *2 *1bish = 14; (7 up + 2 left + 5 right) * 2 *2rooks = 28*2; 56+14 = 70
		assertEquals(-76, classUnderTest.evaluateKingSafety(Piece.Colour.black));  // 1*2*2 diag = 4; 7*2*3 = 42 r'n'f; 4+42 = 46 
	}
	
	@Test
	public void test_evaluateKingSafety_OneKnight_attackBlack()throws IllegalNotationException {
		setUpPosition("8/8/4k3/8/8/1N4N1/8/8 w - - 1 1 ");
		classUnderTest.isEndgame = false;
		assertEquals(0, classUnderTest.evaluateKingSafety(Piece.Colour.white));
		assertEquals(-8, classUnderTest.evaluateKingSafety(Piece.Colour.black)); // One knight attacks the black king zone
	}
	
	@Test
	public void test_evaluateKingSafety_TwoKnights_attackBlack()throws IllegalNotationException {
		setUpPosition("8/8/4k3/8/8/2N3N1/8/8 w - - 1 1 ");
		classUnderTest.isEndgame = false;
		assertEquals(0, classUnderTest.evaluateKingSafety(Piece.Colour.white));
		assertEquals(-16, classUnderTest.evaluateKingSafety(Piece.Colour.black)); // One knight attacks the black king zone
	}
	
	@Test
	public void test_evaluateKingSafety_OneKnight_attackWhite()throws IllegalNotationException {
		setUpPosition("8/8/4K3/8/8/1n4n1/8/8 b - - 1 1 ");
		classUnderTest.isEndgame = false;
		assertEquals(-8, classUnderTest.evaluateKingSafety(Piece.Colour.white));
		assertEquals(0, classUnderTest.evaluateKingSafety(Piece.Colour.black)); // One knight attacks the black king zone
	}
	
	@Test
	public void test_evaluateKingSafety_TwoKnights_attackWhite()throws IllegalNotationException {
		setUpPosition("8/8/4K3/8/8/2n3n1/8/8 b - - 1 1 ");
		classUnderTest.isEndgame = false;
		assertEquals(-16, classUnderTest.evaluateKingSafety(Piece.Colour.white));
		assertEquals(0, classUnderTest.evaluateKingSafety(Piece.Colour.black)); // One knight attacks the black king zone
	}
	
	@Test
	public void test_verify_empty_squares_mask_lut() {
		int j=0, k=0;
		for (int i : Position.values) {
			long [][] atSquare = Board.emptySquareMask_Lut[i];
			j = 0;
			for (long [] inDirection : atSquare) {
				k = 0;
				for (long mask : inDirection) {
					int position = SquareAttackEvaluator.directPieceMove_Lut[i][j][k];
					assertEquals(BitBoard.positionToMask_Lut[position], mask);
					k++;
				}
				j++;
			}
		}
	}
	
	@Test
	public void test_slider_refactor_eval() {
		setUpPosition("r1b1kb1r/ppq1pppp/8/3pN3/3Q4/8/PPP2PPP/RNB1K2R b KQkq - 0 1");
		classUnderTest.isEndgame = false;
		PiecewiseEvaluation me = new PiecewiseEvaluation();
		classUnderTest.calculateDynamicMobility(me);
		assertEquals(8, me.getPosition());
	}
	
	@Test
	public void test_optimised_mobility_func() {
		for (int outer_rank=0; outer_rank<8; outer_rank++) {
			for (int outer_file=0; outer_file<8; outer_file++) {
				classUnderTest.setPieceAtSquare(Position.valueOf(outer_file, outer_rank), Piece.WHITE_QUEEN);
				for (int rank=0; rank<8; rank++) {
					for (int file=0; file<8; file++) {
						if (file==outer_file && rank==outer_rank) continue;
						int atPos = Position.valueOf(file, rank);
						PiecewiseEvaluation me = new PiecewiseEvaluation();
						PiecewiseEvaluation old_me = new PiecewiseEvaluation();
						classUnderTest.setPieceAtSquare(atPos, Piece.WHITE_BISHOP);
						classUnderTest.calculateDynamicMobility(me);
						classUnderTest.calculateDynamicMobility(old_me);
						assertEquals(old_me.getPosition(), me.getPosition());
						classUnderTest.pickUpPieceAtSquare(atPos, Piece.WHITE_BISHOP);
					}
				}
				classUnderTest.pickUpPieceAtSquare(Position.valueOf(outer_file, outer_rank), Piece.WHITE_QUEEN);
			}
		}
		for (int outer_rank=0; outer_rank<8; outer_rank++) {
			for (int outer_file=0; outer_file<8; outer_file++) {
				classUnderTest.setPieceAtSquare(Position.valueOf(outer_file, outer_rank), Piece.WHITE_QUEEN);
				for (int rank=0; rank<8; rank++) {
					for (int file=0; file<8; file++) {
						if (file==outer_file && rank==outer_rank) continue;
						int atPos = Position.valueOf(file, rank);
						PiecewiseEvaluation me = new PiecewiseEvaluation();
						PiecewiseEvaluation old_me = new PiecewiseEvaluation();
						classUnderTest.setPieceAtSquare(atPos, Piece.WHITE_ROOK);
						classUnderTest.calculateDynamicMobility(me);
						classUnderTest.calculateDynamicMobility(old_me);
						assertEquals(old_me.getPosition(), me.getPosition());
						classUnderTest.pickUpPieceAtSquare(atPos, Piece.WHITE_ROOK);
					}
				}
				classUnderTest.pickUpPieceAtSquare(Position.valueOf(outer_file, outer_rank), Piece.WHITE_QUEEN);
			}
		}
	}
}
