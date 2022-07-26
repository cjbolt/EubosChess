package eubos.board;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.position.Position;
import eubos.position.PositionManager;
import eubos.score.PiecewiseEvaluation;
import eubos.score.PositionEvaluator;
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
		if (!EubosEngineMain.ENABLE_ASSERTS) {
			assertEquals(Piece.NONE, classUnderTest.pickUpPieceAtSquare(testSq, Piece.NONE));
		}
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
	public void testRookMobility_ExcludePawnAttacks() {
		classUnderTest.setPieceAtSquare(Position.d4, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.a3, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.b3, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.c3, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.d3, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.f3, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.g3, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.h3, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.e2, Piece.WHITE_ROOK);
		assertEquals(14, classUnderTest.calculateRankFileMobility(classUnderTest.getWhiteRooks(), 0));
	}
	
	@Test
	public void testRookMobility_ExcludePawnAttacks_1() {
		classUnderTest.setPieceAtSquare(Position.d7, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.d6, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.d5, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.d4, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.d3, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.d2, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.e2, Piece.WHITE_ROOK);
		assertEquals(10, classUnderTest.calculateRankFileMobility(classUnderTest.getWhiteRooks(), 0));
	}
	
	@Test
	public void testRookMobility_ExcludeWhitePawnAttacks() {
		// Max 8 pawns in the PieceList limitation
		classUnderTest.setPieceAtSquare(Position.d5, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.a6, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.b6, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.c6, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d6, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.f6, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.g6, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.h6, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.e7, Piece.BLACK_ROOK);
		assertEquals(14, classUnderTest.calculateRankFileMobility(classUnderTest.getBlackRooks(), 0));
	}
	
	@Test
	public void testRookMobility_ExcludeWhitePawnAttacks_1() {
		// Max 8 pawns in the PieceLit limitation
		classUnderTest.setPieceAtSquare(Position.d2, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d3, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d4, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d5, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d6, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d7, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.e7, Piece.BLACK_ROOK);
		assertEquals(10, classUnderTest.calculateRankFileMobility(classUnderTest.getBlackRooks(), 0));
	}
	
	@Test
	public void testRookMobility_ExcludeWhiteKnightAttacks() {
		classUnderTest.setPieceAtSquare(Position.f6, Piece.WHITE_KNIGHT);
		classUnderTest.setPieceAtSquare(Position.h8, Piece.BLACK_ROOK);
		assertEquals(14, classUnderTest.calculateRankFileMobility(classUnderTest.getBlackRooks(), 0));
	}
	
	@Test
	public void testRookMobility_ExcludeWhiteKnightAndPawnAttacks() {
		classUnderTest.setPieceAtSquare(Position.f6, Piece.WHITE_KNIGHT);
		classUnderTest.setPieceAtSquare(Position.b7, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.g2, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.h8, Piece.BLACK_ROOK);
		assertEquals(14, classUnderTest.calculateRankFileMobility(classUnderTest.getBlackRooks(), 0));
	}
	
	@Test
	public void testRookMobility_ExcludeBlackKnightAttacks() {
		classUnderTest.setPieceAtSquare(Position.c3, Piece.BLACK_KNIGHT);
		classUnderTest.setPieceAtSquare(Position.a1, Piece.WHITE_ROOK);
		assertEquals(14, classUnderTest.calculateRankFileMobility(classUnderTest.getWhiteRooks(), 0));
	}
	
	@Test
	public void testRookMobility_IgnoreOwnKnight() {
		classUnderTest.setPieceAtSquare(Position.f6, Piece.BLACK_KNIGHT);
		classUnderTest.setPieceAtSquare(Position.h8, Piece.BLACK_ROOK);
		assertEquals(14, classUnderTest.calculateRankFileMobility(classUnderTest.getBlackRooks(), 0));
	}
	
	@Test
	public void testRookMobility_IgnoreOwnKnight_1() {
		classUnderTest.setPieceAtSquare(Position.c3, Piece.WHITE_KNIGHT);
		classUnderTest.setPieceAtSquare(Position.a1, Piece.WHITE_ROOK);
		assertEquals(14, classUnderTest.calculateRankFileMobility(classUnderTest.getWhiteRooks(), 0));
	}
	
	@Test
	public void testBishopMobility_ExcludePawnAttacks() {
		classUnderTest.setPieceAtSquare(Position.e7, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.d6, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.c5, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.b4, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.a3, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.a1, Piece.WHITE_BISHOP);
		// Can move to g7 and h8, that can't be attacked by a black pawn
		assertEquals(7, classUnderTest.calculateDiagonalMobility(classUnderTest.getWhiteBishops(), 0));
	}
	
	@Test
	public void testBishopMobility_ExcludePawnAttacks_1() {
		classUnderTest.setPieceAtSquare(Position.e7, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.c5, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.a3, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.a1, Piece.WHITE_BISHOP);
		// Can move to g7 and h8, that can't be attacked by a black pawn, and c3 and e5 which aren't attacked
		assertEquals(7, classUnderTest.calculateDiagonalMobility(classUnderTest.getWhiteBishops(), 0));
	}
	
	@Test
	public void testBishopQueenMobility_ExcludePawnAttacks() {
		classUnderTest.setPieceAtSquare(Position.e7, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.c5, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.a3, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.a1, Piece.WHITE_BISHOP);
		classUnderTest.setPieceAtSquare(Position.e6, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.b1, Piece.WHITE_QUEEN);
		assertEquals(14, classUnderTest.calculateDiagonalMobility(classUnderTest.getWhiteBishops(), classUnderTest.getWhiteQueens()));
	}
	
	@Test
	public void testBishopMobility_ExcludeWhitePawnAttacks() {
		classUnderTest.setPieceAtSquare(Position.a6, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.b5, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.c4, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d3, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.e2, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.a8, Piece.BLACK_BISHOP);
		assertEquals(7, classUnderTest.calculateDiagonalMobility(classUnderTest.getBlackBishops(), 0));
	}
	
	@Test
	public void testBishopMobility_ExcludeWhitePawnAttacks_1() {
		// Max 8 pawns in the PieceList limitation
		classUnderTest.setPieceAtSquare(Position.d2, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.e3, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.f4, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.g5, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.h6, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.h8, Piece.BLACK_BISHOP);
		assertEquals(7, classUnderTest.calculateDiagonalMobility(classUnderTest.getBlackBishops(), 0));
	}
	
	@Test
	public void testRookandQueenMobility_ExcludeBlackPawnandKnightAttacks() {	
		setUpPosition("5K1k/2n5/2n5/8/2n5/2nn4/5pp1/R3Q3 w - - 0 1");
		assertEquals(23, classUnderTest.calculateRankFileMobility(classUnderTest.getWhiteRooks(), classUnderTest.getWhiteQueens()));
	}
	
	@Test
	public void testRookandQueenMobility_ExcludeBlackPawnandKnightAttacks_1() {	
		setUpPosition("5K1k/2n5/2n5/8/2n5/2nn4/5pp1/R3Q2R w - - 0 1");
		assertEquals(30, classUnderTest.calculateRankFileMobility(classUnderTest.getWhiteRooks(), classUnderTest.getWhiteQueens()));
	}
	
	@Test
	public void testRookandQueenMobility_ExcludeBlackPawnandKnightAttacks_2() {	
		setUpPosition("5K1k/2n5/2n5/8/2n5/2nn4/5pp1/R3Q2R w - - 0 1");
		classUnderTest.me.dynamicPosition = 0;
		classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		assertEquals(62, classUnderTest.me.dynamicPosition);
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
		classUnderTest.setPieceAtSquare(Position.d8, Piece.BLACK_ROOK);
		classUnderTest.setPieceAtSquare(Position.d2, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d1, Piece.WHITE_KING);
		classUnderTest.evaluateMaterial(classUnderTest.me);
		int move = Move.valueOf(Position.d2, Piece.WHITE_PAWN, Position.d4, Piece.NONE);
		assertTrue(classUnderTest.moveCouldLeadToOwnKingDiscoveredCheck(move, Position.d1, true));
	}
	
	@Test
	public void testCouldLeadToCheck_Yes1() {
		classUnderTest.setPieceAtSquare(Position.h5, Piece.BLACK_BISHOP);
		classUnderTest.setPieceAtSquare(Position.e2, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d1, Piece.WHITE_KING);
		classUnderTest.evaluateMaterial(classUnderTest.me);
		int move = Move.valueOf(Position.e2, Piece.WHITE_PAWN, Position.d3, Piece.NONE);
		assertTrue(classUnderTest.moveCouldLeadToOwnKingDiscoveredCheck(move, Position.d1, true));
	}
	
	@Test
	public void testCouldLeadToCheck_No() {
		classUnderTest.setPieceAtSquare(Position.e3, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d1, Piece.WHITE_KING);
		int move = Move.valueOf(Position.e3, Piece.WHITE_PAWN, Position.e4, Piece.NONE);
		assertFalse(classUnderTest.moveCouldLeadToOwnKingDiscoveredCheck(move, Position.d1, true));
	}
	
	@Test
	public void testCouldLeadToCheck_No1() {
		classUnderTest.setPieceAtSquare(Position.d1, Piece.WHITE_KNIGHT);
		classUnderTest.setPieceAtSquare(Position.e4, Piece.WHITE_KING);
		int move = Move.valueOf(Position.d1, Piece.WHITE_KNIGHT, Position.c3, Piece.NONE);
		assertFalse(classUnderTest.moveCouldLeadToOwnKingDiscoveredCheck(move, Position.e4, true));
	}
	
	PositionManager pm;
	protected void setUpPosition(String fen) {
		pm = new PositionManager(fen, new DrawChecker());
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
		assertEquals(-28, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), true)); // 5 squares, can be attacked by three pieces
		assertEquals(-45, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), false));
	}
	
	@Test
	public void test_evaluateKingSafety_notVerySafe()throws IllegalNotationException {
		setUpPosition("6rr/5ppp/1k4bq/8/8/1K4BQ/5PPP/6RR b - - 13 1 ");
		assertEquals(-230, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), false)); // diagonals 7 squares, can be attacked by two pieces; r'n'f 9 squares can be attacked by three pieces
		assertEquals(-230, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), true));
	}
	
	@Test
	public void test_evaluateKingSafety_No_inEndgame()throws IllegalNotationException {
		setUpPosition("8/8/8/8/8/8/8/K7 w - - 0 1");
		assertEquals(-50, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), true));
	}
	
	@Test
	public void test_evaluateKingSafety_No_opposingBishopWrongColour()throws IllegalNotationException {
		setUpPosition("r4rk1/1p3p2/p7/P2P1p1B/4p3/2b5/3R1PPP/4K2R b K - 13 1 ");
		if (PositionEvaluator.ENABLE_TWEAKED_KING_FLIGHT_SQUARES) {
			assertEquals(-74, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), true)); // 7*2*2 rnf 0 diag = 28
			assertEquals(-66, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), false)); // 7*2*2 rnf 1*2*1 = 30
		} else {
			assertEquals(-80, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), true)); // 7*2*2 rnf 0 diag = 28
			assertEquals(-32, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), false)); // 7*2*2 rnf 1*2*1 = 30
		}

	}
	
	@Test
	public void test_evaluateKingSafety_Yes_opposingBishopRightColour()throws IllegalNotationException {
		setUpPosition("r4rk1/1p6/p7/P2P1p1B/4p3/2b5/3R1PPP/2K4R b - - 13 1 ");
		if (PositionEvaluator.ENABLE_TWEAKED_KING_FLIGHT_SQUARES) {
			assertEquals(-100, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), true));
		} else {
			assertEquals(-80, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), true));
		}
	}
	
	@Test
	public void test_evaluateKingSafety_Yes_opposingQueenBishop()throws IllegalNotationException {
		setUpPosition("r4rk1/1p6/p7/P2P1p1B/4p3/2b5/3R1PPP/Q1K4R b - - 13 1 ");
		if (PositionEvaluator.ENABLE_TWEAKED_KING_FLIGHT_SQUARES) {
		assertEquals(-100, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), true));  // (5 up right + 2 up left) *2 *1bish = 14; (7 up + 2 left + 5 right) * 2 *2rooks = 28*2; 56+14 = 70
		assertEquals(-75, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), false));  // 1*2*2 diag = 4; 7*2*3 = 42 r'n'f; 4+42 = 46 
		} else {
			assertEquals(-80, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), true));  // (5 up right + 2 up left) *2 *1bish = 14; (7 up + 2 left + 5 right) * 2 *2rooks = 28*2; 56+14 = 70
			assertEquals(-93, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), false));  // 1*2*2 diag = 4; 7*2*3 = 42 r'n'f; 4+42 = 46 
		}
	}
	
	@Test
	public void test_evaluateKingSafety_OneKnight_attackBlack()throws IllegalNotationException {
		setUpPosition("K7/8/4k3/8/8/1N4N1/8/8 w - - 1 1 ");
		assertEquals(-50, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), true));
		assertEquals(-110, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), false)); // One knight attacks the black king zone
	}
	
	@Test
	public void test_evaluateKingSafety_TwoKnights_attackBlack()throws IllegalNotationException {
		setUpPosition("K7/8/4k3/8/8/2N3N1/8/8 w - - 1 1 ");
		assertEquals(-50, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), true));
		assertEquals(-105, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), false)); // One knight attacks the black king zone
	}
	
	@Test
	public void test_evaluateKingSafety_OneKnight_attackWhite()throws IllegalNotationException {
		setUpPosition("k7/8/4K3/8/8/1n4n1/8/8 b - - 1 1 ");
		assertEquals(-100, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), true));
		assertEquals(-50, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), false)); // One knight attacks the black king zone
	}
	
	@Test
	public void test_evaluateKingSafety_TwoKnights_attackWhite()throws IllegalNotationException {
		setUpPosition("k7/8/4K3/8/8/2n3n1/8/8 b - - 1 1 ");
		assertEquals(-100, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), true));
		assertEquals(-50, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), false)); // One knight attacks the black king zone
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
		PiecewiseEvaluation me = new PiecewiseEvaluation();
		classUnderTest.calculateAttacksAndMobility(me);
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
						classUnderTest.calculateAttacksAndMobility(me);
						classUnderTest.calculateAttacksAndMobility(old_me);
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
						classUnderTest.calculateAttacksAndMobility(me);
						classUnderTest.calculateAttacksAndMobility(old_me);
						assertEquals(old_me.getPosition(), me.getPosition());
						classUnderTest.pickUpPieceAtSquare(atPos, Piece.WHITE_ROOK);
					}
				}
				classUnderTest.pickUpPieceAtSquare(Position.valueOf(outer_file, outer_rank), Piece.WHITE_QUEEN);
			}
		}
	}
	
	@Test
	public void test_is_playable_misc() {
		setUpPosition("5Q2/P5K1/8/3k4/5n2/8/8/8 w - - 1 113");
		int move = Move.valueOf(Move.TYPE_PROMOTION_MASK, Position.a7, Piece.WHITE_PAWN, Position.a8, Piece.NONE, Piece.QUEEN);
		boolean inCheck = false;
		assertTrue(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
		// Origin doesn't exist
		move = Move.valueOf(Position.a6, Piece.WHITE_PAWN, Position.a7, Piece.NONE);
		assertFalse(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
		// Target doesn't exist
		move = Move.valueOf(Move.TYPE_PROMOTION_MASK, Position.a7, Piece.WHITE_PAWN, Position.b8, Piece.BLACK_KNIGHT, Piece.QUEEN);
		assertFalse(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
		// Move into check
		move = Move.valueOf(Position.g7, Piece.WHITE_KING, Position.g6, Piece.NONE);
		assertFalse(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test
	public void test_is_playable_castling() {
		setUpPosition("8/8/8/3k4/8/8/8/R3K2R w K - 1 10");
		int move = Move.valueOf(Position.e1, Piece.WHITE_KING, Position.g1, Piece.NONE);
		boolean inCheck = false;
		assertTrue(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
		// Castle move not valid
		move = Move.valueOf(Position.e1, Piece.WHITE_KING, Position.c1, Piece.NONE);
		assertFalse(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test
	public void test_is_playable_castling_target_square_is_attacked() {
		setUpPosition("8/8/8/2bk4/8/8/8/R3K2R w K - 1 10");
		int move = Move.valueOf(Position.e1, Piece.WHITE_KING, Position.g1, Piece.NONE);
		boolean inCheck = false;
		assertFalse(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test
	public void test_is_playable_castling_king_in_check() {
		setUpPosition("8/8/8/1b1k4/8/8/8/R3K2R w K - 1 10");
		int move = Move.valueOf(Position.e1, Piece.WHITE_KING, Position.g1, Piece.NONE);
		boolean inCheck = false;
		assertFalse(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test
	public void test_is_playable_valid_en_passant() {
		setUpPosition("8/8/8/3k1pP1/8/8/8/4K3 w - f6 - 10");
		int move = Move.valueOfEnPassant(Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.g5, Piece.WHITE_PAWN, Position.f6, Piece.BLACK_PAWN, Piece.NONE);
		boolean inCheck = false;
		assertTrue(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
		assertEquals("8/8/8/3k1pP1/8/8/8/4K3 w - f6 - 10", pm.getFen());
	}
	
	@Test
	public void test_is_playable_invalid_en_passant() {
		setUpPosition("8/8/8/3k1pP1/8/8/8/4K3 w - - 1 10");
		int move = Move.valueOfEnPassant(Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.g5, Piece.WHITE_PAWN, Position.f6, Piece.BLACK_PAWN, Piece.NONE);
		boolean inCheck = false;
		assertFalse(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test 
	public void test_is_playable_under_promotion() {
		setUpPosition("6nn/6P1/8/3k1p2/8/8/8/4K3 w - - 1 10");
		int move = Move.valueOf(Move.TYPE_PROMOTION_MASK, Position.g7, Piece.WHITE_PAWN, Position.h8, Piece.BLACK_KNIGHT, Piece.KNIGHT);
		boolean inCheck = false;
		assertTrue(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test 
	public void test_is_playable_initial_pawn_move() {
		setUpPosition("r1b1kb1r/ppq1pppp/8/3pN3/3Q4/8/PPP2PPP/RNB1K2R b KQkq - 0 1");
		int move = Move.valueOf(Position.a7, Piece.BLACK_PAWN, Position.a5, Piece.NONE);
		boolean inCheck = false;
		assertTrue(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test 
	public void test_is_playable_initial_pawn_move_blocked() {
		setUpPosition("r1b1kb1r/ppq1pppp/P7/3pN3/3Q4/8/PPP2PPP/RNB1K2R b KQkq - 0 1");
		int move = Move.valueOf(Position.a7, Piece.BLACK_PAWN, Position.a5, Piece.NONE);
		boolean inCheck = false;
		assertFalse(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test
	public void test_frontspan_isBlocked() {
		setUpPosition("2k5/8/8/8/2P5/3K4/8/8 w - - 1 10 ");
		assertTrue(classUnderTest.isPawnFrontspanBlocked(Position.c4, true, pm.getTheBoard().getAttackedSquares()[0][3], pm.getTheBoard().getAttackedSquares()[1][3], false));
	}
	
	@Test
	public void test_frontspan_NotBlocked() {
		setUpPosition("8/k7/8/8/2P5/3K4/8/8 w - - 1 10 ");
		assertFalse(classUnderTest.isPawnFrontspanBlocked(Position.c4, true, pm.getTheBoard().getAttackedSquares()[0][3], pm.getTheBoard().getAttackedSquares()[1][3], false));
	}
	
	@Test
	public void test_frontspan_isAttackedAndDefended() {
		setUpPosition("8/1B1b4/8/8/2P5/3K4/8/8 w - - 1 10 ");
		assertFalse(classUnderTest.isPawnFrontspanBlocked(Position.c4, true, pm.getTheBoard().getAttackedSquares()[0][3], pm.getTheBoard().getAttackedSquares()[1][3], false));
	}
	
	@Test
	public void test_frontspan_IsAttacked() {
		setUpPosition("8/3b4/8/8/2P5/3K4/8/8 w - - 1 10 ");
		assertTrue(classUnderTest.isPawnFrontspanBlocked(Position.c4, true, pm.getTheBoard().getAttackedSquares()[0][3], pm.getTheBoard().getAttackedSquares()[1][3], false));
	}
	
	@Test
	public void test_frontspan_IsAttackedTwiceDefendedOnce() {
		setUpPosition("8/3b4/2P6/2P5/3K4/8/8 w - - 1 10 ");
		assertTrue(classUnderTest.isPawnFrontspanBlocked(Position.c4, true, pm.getTheBoard().getAttackedSquares()[0][3], pm.getTheBoard().getAttackedSquares()[1][3], false));
	}
	
	@Test
	public void test_frontspan_IsAttackedTwiceDefendedTwice() {
		setUpPosition("R7/3b4/8/1PP5/3K4/8/8 w - - 1 10 ");
		assertFalse(classUnderTest.isPawnFrontspanBlocked(Position.c4, true, pm.getTheBoard().getAttackedSquares()[0][3], pm.getTheBoard().getAttackedSquares()[1][3], false));
	}
	
	@Test
	public void test_frontspan_IsAttackedOnceDefendedOnceByRookToRear() {
		setUpPosition("8/3b4/8/2P5/7K/8/2R5 w - - 1 10 ");
		assertFalse(classUnderTest.isPawnFrontspanBlocked(Position.c4, true, pm.getTheBoard().getAttackedSquares()[0][3], pm.getTheBoard().getAttackedSquares()[1][3], true));
	}
	
	@Test
	public void test_frontspan_IsAttackedOnceDefendedTwice_soBlocked() {
		setUpPosition("4B2K/8/1b1Bn3/8/2P5/8/8/7k w - - 1 1");
		assertTrue(classUnderTest.isPawnFrontspanBlocked(Position.c4, true, pm.getTheBoard().getAttackedSquares()[0][3], pm.getTheBoard().getAttackedSquares()[1][3], false));
	}
	
	@Test
	public void test_evaluateKingSafety_ScoreReporter()throws IllegalNotationException {
		setUpPosition("4rbk1/1pr2p2/2p2Qp1/p2p4/6RP/2P1PN1q/PP3P2/2K3R1 b - - 9 30 ");
		if (PositionEvaluator.ENABLE_TWEAKED_KING_FLIGHT_SQUARES) {
			assertEquals(-257, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), false));
		} else {
			assertEquals(-132, classUnderTest.evaluateKingSafety(pm.getTheBoard().getAttackedSquares(), false));
		}
	}
	
	@Test
	public void test_blockedBishopAttacksOwnPieces() throws IllegalNotationException {
		setUpPosition("7k/8/8/2P1P3/3B4/2P1P3/8/7K w - - 0 1");
		classUnderTest.getAttackedSquares();
		// bishop attacks 4 pawns
		assertEquals(4, Long.bitCount(classUnderTest.attacks[0][2][0]));
	}
	
	@Test
	public void test_blockedRookAttacksOwnPieces() throws IllegalNotationException {
		setUpPosition("7k/8/8/3P4/2PRP3/3P4/8/7K w - - 0 1");
		classUnderTest.getAttackedSquares();
		// rook attacks 4 pawns
		assertEquals(4, Long.bitCount(classUnderTest.attacks[0][2][0]));
	}
	
	@Test
	public void test_Attacks() throws IllegalNotationException {
		setUpPosition("1b2r1k1/5ppp/1qb1p3/p1p5/2P3rP/P5P1/1PQ1NP2/R1B1R1K1 w - - 9 28 ");
		classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		// white sliders
		int [] positions = {Position.a2, Position.a3, Position.b1, Position.c1, // rook
				Position.b2, Position.d2, Position.e3, Position.f4, Position.g5, Position.h6, // bishop
				Position.c1, Position.d1, Position.f1, Position.g1, Position.e2, // rook
				Position.c1, Position.b1, Position.d1, Position.b2, Position.d2, Position.e2, Position.b3, Position.a4, 
				Position.c3, Position.c4, Position.d3, Position.e4, Position.f5, Position.g6, Position.h7 // queen
		}; 
		long expectedMask = BitBoard.valueOf(positions);
		assertEquals(expectedMask, classUnderTest.attacks[0][2][0]);
		// white pawns
		positions = new int[] {Position.b4, Position.a3, Position.c3, Position.b5,
				Position.d5, Position.e3, Position.g3, Position.f4, Position.h4, Position.g5,
		}; 
		expectedMask = BitBoard.valueOf(positions);
		assertEquals(expectedMask, classUnderTest.attacks[0][0][0]);
		// white knight
		positions = new int[] {Position.c1, Position.c3, Position.d4, Position.f4,
				Position.g3, Position.g1
		}; 
		expectedMask = BitBoard.valueOf(positions);
		assertEquals(expectedMask, classUnderTest.attacks[0][1][0]);

		// black sliders
		positions = new int[] {Position.a5, Position.b5, Position.c5, Position.a6, Position.c6, Position.b4, Position.b3, Position.b2, 
				Position.a7, Position.b7, Position.c7, Position.a8, Position.d8, Position.b8, // queen
				Position.h4, Position.g3, Position.g5, Position.g6, Position.g7, // rook
				Position.f4, Position.e4, Position.d4, Position.c4,
			
				Position.b8, Position.c8, Position.d8, Position.f8, Position.g8, Position.e7, Position.e6, // other rook
				
				Position.a7, Position.c7, Position.d6, Position.e5, Position.f4, Position.g3, // bishop
				
				Position.a8, Position.b7, Position.d7, Position.e8, Position.b5, Position.a4, 
				Position.d5, Position.e4, Position.f3, Position.g2, Position.h1 // bishop
		}; 
		expectedMask = BitBoard.valueOf(positions);
		assertEquals(expectedMask, classUnderTest.attacks[1][2][0]);
		// black pawns
		positions = new int[] {Position.b4, Position.d4, Position.d5, Position.f5,
				Position.h6, Position.g6, Position.f6, Position.e6
		}; 
		expectedMask = BitBoard.valueOf(positions);
		assertEquals(expectedMask, classUnderTest.attacks[1][0][0]);
		// black knight
		assertEquals(0, classUnderTest.attacks[1][1][0]);
	}	
	
	@Test
	public void test_evaluateSquareControlRoundKing() {
		setUpPosition("4q1k1/5ppp/4N3/7Q/4B3/8/8/6K1 b - - 0 1 ");
		long [][][] attacks = classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		int numSquares = CountedBitBoard.evaluate(attacks[1][3], attacks[0][3], SquareAttackEvaluator.KingZone_Lut[1][Position.g8]);
		assertEquals(3, numSquares);
	}
	
	@Test
	public void test_evaluateSquareControlRoundKing_NoPawns() {
		setUpPosition("6k1/8/8/7R/4BR1Q/8/8/6K1 b - - 0 1 ");
		long [][][] attacks = classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		int numSquares = CountedBitBoard.evaluate(attacks[1][3], attacks[0][3], SquareAttackEvaluator.KingZone_Lut[1][Position.g8]);
		assertEquals(8, numSquares);
	}
	
	@Test
	public void test_evaluateSquareControlRoundKing_NoPawnsCrazy() {
		setUpPosition("6k1/8/5PP1/4N2P/4B3/3Q4/8/1K3R2 w - - 99 1");
		long [][][] attacks = classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		int numSquares = CountedBitBoard.evaluate(attacks[1][3], attacks[0][3], SquareAttackEvaluator.KingZone_Lut[1][Position.g8]);
		assertEquals(4, numSquares);
	}
	
	@Test
	public void test_RookBattery_OnFile() {
		setUpPosition("8/8/8/8/8/8/R7/R7 w - - 0 1");
		long [][][] attacks = classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a2));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a3));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a4));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a5));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a6));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a7));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a8));

		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.d1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.e1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.f1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.g1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h1));
		
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.d2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.e2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.f2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.g2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h2));
	}
	
	@Test
	public void test_RookQueenBattery_OnFile() {
		setUpPosition("8/8/8/8/8/8/R7/Q7 w - - 0 1");
		long [][][] attacks = classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a2));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a3));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a4));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a5));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a6));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a7));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a8));

		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.d1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.e1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.f1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.g1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h1));
		
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.b2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.d2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.e2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.f2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.g2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h2));
	}
	
	@Test
	public void test_QueenRookRookBattery_OnFile() {
		setUpPosition("8/8/8/8/8/R7/R7/Q7 w - - 0 1");
		long [][][] attacks = classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		assertEquals(3, CountedBitBoard.count(attacks[0][3], Position.a1));
		assertEquals(4, CountedBitBoard.count(attacks[0][3], Position.a2)); // overcounting up and down adjacent attacks?
		assertEquals(3, CountedBitBoard.count(attacks[0][3], Position.a3));
		assertEquals(3, CountedBitBoard.count(attacks[0][3], Position.a4));
		assertEquals(3, CountedBitBoard.count(attacks[0][3], Position.a5));
		assertEquals(3, CountedBitBoard.count(attacks[0][3], Position.a6));
		assertEquals(3, CountedBitBoard.count(attacks[0][3], Position.a7));
		assertEquals(3, CountedBitBoard.count(attacks[0][3], Position.a8));

		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.d1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.e1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.f1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.g1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h1));
		
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.b2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.d2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.e2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.f2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.g2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h2));
		
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b3));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.c3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.d3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.e3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.f3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.g3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h3));
	}
	
	@Test
	public void test_QueenRookRookBattery_Separated_OnFile() {
		setUpPosition("8/R7/8/p7/p7/8/R7/Q7 w - - 0 1");
		long [][][] attacks = classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a2)); // overcounting up and down adjacent attacks?
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a3));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a6));
		assertEquals(0, CountedBitBoard.count(attacks[0][3], Position.a7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a8));

		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.d1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.e1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.f1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.g1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h1));
		
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.b2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.d2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.e2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.f2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.g2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h2));
		
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.d7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.e7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.f7));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.g7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h7));
	}
	
	@Test
	public void test_RookBattery_OnRank() {
		setUpPosition("8/8/8/8/8/8/8/RR6 w - - 0 1");
		long [][][] attacks = classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a1));
		
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a8));
		
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b8));

		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.b1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.c1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.d1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.e1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.f1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.g1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.h1));
	}
	
	@Test
	public void test_RookQueenBattery_OnRank() {
		setUpPosition("8/8/8/8/8/8/8/QR6 w - - 0 1");
		long [][][] attacks = classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a1));
		
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a8));
		
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.b2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b8));

		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.b1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.c1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.d1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.e1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.f1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.g1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.h1));
	}
	
	@Test
	public void test_QueenRookRookBattery_OnRank() {
		setUpPosition("8/8/8/8/8/8/8/QRR5 w - - 0 1");
		long [][][] attacks = classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		assertEquals(3, CountedBitBoard.count(attacks[0][3], Position.a1));
		assertEquals(4, CountedBitBoard.count(attacks[0][3], Position.b1)); // comes as 4! overcounting L and R
		assertEquals(3, CountedBitBoard.count(attacks[0][3], Position.c1));
		assertEquals(3, CountedBitBoard.count(attacks[0][3], Position.d1));
		assertEquals(3, CountedBitBoard.count(attacks[0][3], Position.e1));
		assertEquals(3, CountedBitBoard.count(attacks[0][3], Position.f1));
		assertEquals(3, CountedBitBoard.count(attacks[0][3], Position.g1));
		assertEquals(3, CountedBitBoard.count(attacks[0][3], Position.h1));
		
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a8));
		
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.b2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b8));
		
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c2));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.c3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c8));
	}
	
	@Test
	public void test_QueenRookBattery_AndOddRook_OnRank() {
		setUpPosition("8/8/8/8/8/8/8/QR4nR w - - 0 1");
		long [][][] attacks = classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a8));
		
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.b2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b8));
		
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h7));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.h8));

		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.b1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.c1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.d1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.e1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.f1));
		assertEquals(3, CountedBitBoard.count(attacks[0][3], Position.g1));
		assertEquals(0, CountedBitBoard.count(attacks[0][3], Position.h1));
	}
	
	@Test
	public void test_QueenRookBattery_AndOddRook_OnRank_alt() {
		setUpPosition("8/8/8/8/8/8/8/QR1nnn1R w - - 0 1");
		long [][][] attacks = classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a1));
		
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a8));
		
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.b2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b8));
		
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h2));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h7));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.h8));

		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.b1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.c1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.d1));
		assertEquals(0, CountedBitBoard.count(attacks[0][3], Position.e1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.f1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.g1));
		assertEquals(0, CountedBitBoard.count(attacks[0][3], Position.h1));
	}
	
	@Test
	public void test_QueenBishopBattery() {
		setUpPosition("8/8/8/8/8/8/1B6/Q7 w - - 0 1");
		long [][][] attacks = classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a1));
		
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a2));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a8));
		
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.b2));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.c3));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.d4));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.e5));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.f6));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.g7));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.h8));

		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.c1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.d1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.e1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.f1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.g1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h1));
	}
	
	@Test
	public void test_QueenBishopBattery_SeparatedBishop() {
		setUpPosition("7B/8/5p2/8/3p4/8/1B6/Q7 w - - 0 1");
		long [][][] attacks = classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a1));
		
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a2));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.a3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a8));
		
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.b2));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.c3));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.d4));
		assertEquals(0, CountedBitBoard.count(attacks[0][3], Position.e5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.f6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.g7));
		assertEquals(0, CountedBitBoard.count(attacks[0][3], Position.h8));

		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.c1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.d1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.e1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.f1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.g1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h1));
	}
	
	@Test
	public void test_QueenBishopBattery_SeparatedBishop_UpLeft() {
		setUpPosition("B7/8/2p5/8/4p3/8/6B1/7Q w - - 0 1");
		long [][][] attacks = classUnderTest.calculateAttacksAndMobility(classUnderTest.me);
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.h1));
		
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h2));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.h3));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h4));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h7));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.h8));
		
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.g2));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.f3));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.e4));
		assertEquals(0, CountedBitBoard.count(attacks[0][3], Position.d5));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c6));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b7));
		assertEquals(0, CountedBitBoard.count(attacks[0][3], Position.a8));

		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.b1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.c1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.d1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.e1));
		assertEquals(2, CountedBitBoard.count(attacks[0][3], Position.f1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.g1));
		assertEquals(1, CountedBitBoard.count(attacks[0][3], Position.a1));
	}
	
	@Test
	public void test_pawnIsBlockaded() {
		setUpPosition("8/8/8/8/8/8/1p6/1N6 w - - 0 1");
		assertTrue(classUnderTest.isPawnBlockaded(Position.b2, false));
	}
	
	@Test
	public void test_pawnIsNotBlockaded() {
		setUpPosition("8/8/8/8/8/8/1p6/8 w - - 0 1");
		assertFalse(classUnderTest.isPawnBlockaded(Position.b2, false));
	}
	
	@Test
	public void test_pawnIsBlockaded_white() {
		setUpPosition("8/8/8/8/8/1n6/1P6/8 w - - 0 1");
		assertTrue(classUnderTest.isPawnBlockaded(Position.b2, true));
	}
	
	@Test
	public void test_pawnIsNotBlockaded_white() {
		setUpPosition("8/8/8/8/8/8/1P6/8 w - - 0 1");
		assertFalse(classUnderTest.isPawnBlockaded(Position.b2, true));
	}
	
	@Test
	public void test_pawnIsNotBlockaded_ownColour() {
		setUpPosition("8/8/8/8/8/8/1p6/1n6 w - - 0 1");
		assertFalse(classUnderTest.isPawnBlockaded(Position.b2, false));
	}
	
	@Test
	public void test_pawnIsNotBlockaded_ownColour_white() {
		setUpPosition("8/8/8/8/8/1N6/1P6/8 w - - 0 1");
		assertFalse(classUnderTest.isPawnBlockaded(Position.b2, true));
	}
	
	@Test
	public void test_heavy_behind_pawn() {
		setUpPosition("8/8/8/P7/8/8/8/Q7 w - - 0 1");
		assertEquals(1, classUnderTest.checkForHeavyPieceBehindPassedPawn(Position.a5, true));
	}
	
	@Test
	public void test_heavy_behind_pawn_but_blocked_by_enemy() {
		setUpPosition("8/8/8/P7/8/n7/8/Q7 w - - 0 1");
		assertEquals(0, classUnderTest.checkForHeavyPieceBehindPassedPawn(Position.a5, true));
	}
	
	@Test
	public void test_enemy_heavy_behind_pawn_() {
		setUpPosition("8/8/8/P7/r7/n7/8/R7 w - - 0 1");
		assertEquals(-1, classUnderTest.checkForHeavyPieceBehindPassedPawn(Position.a5, true));
	}
	
	@Test
	public void test_enemy_heavy_behind_pawn_simple() {
		setUpPosition("8/8/8/P7/8/8/8/q7 w - - 0 1");
		assertEquals(-1, classUnderTest.checkForHeavyPieceBehindPassedPawn(Position.a5, true));
	}
}

