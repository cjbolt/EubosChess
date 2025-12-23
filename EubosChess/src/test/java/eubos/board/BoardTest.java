package eubos.board;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.position.Move;
import eubos.position.Position;
import eubos.position.PositionManager;
import eubos.search.DrawChecker;
import eubos.search.transposition.Transposition;

public class BoardTest {
	
	private Board classUnderTest;
	private static final int testSq = Position.a1;
	
	@Before
	public void setUp() throws Exception {
		// Use a valid position, i.e. that has both kings on board
		setUpPosition("8/8/8/8/8/8/k7/7K w - - 0 1");
	}

	@Test
	public void testBoard() {
		assertTrue(classUnderTest!=null);
	}

	@Test
	public void testSetEnPassantTargetSq() {
		classUnderTest.setEnPassantTargetSq(BitBoard.d3);
	}
	
	@Test
	public void testGetEnPassantTargetSq_uninitialised() {
		int square = classUnderTest.getEnPassantTargetSq();
		assertTrue(square == BitBoard.INVALID);
	}

	@Test
	public void testGetEnPassantTargetSq_initialised() {
		classUnderTest.setEnPassantTargetSq(BitBoard.d3);
		int square = classUnderTest.getEnPassantTargetSq();
		assertTrue(square == BitBoard.d3);
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
		classUnderTest.pickUpPieceAtSquare(BitBoard.positionToMask_Lut[testSq], BitBoard.positionToBit_Lut[testSq], Piece.WHITE_PAWN);
		assertTrue(classUnderTest.squareIsEmpty(testSq));
	}
	
	@Test
	public void testGetPieceAtSquare_Exists() {
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		classUnderTest.setPieceAtSquare(testSq, Piece.WHITE_PAWN);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
		int gotPiece = classUnderTest.getPieceAtSquare(1L << testSq);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
		assertTrue(gotPiece==Piece.BLACK_PAWN || gotPiece==Piece.WHITE_PAWN);
	}
	
	@Test
	public void testGetPieceAtSquare_DoesntExist() {
		assertTrue(classUnderTest.getPieceAtSquare(1L << testSq)==Piece.NONE);
	}
	
	@Test
	public void testGetAsFenString() {
		classUnderTest.setPieceAtSquare(testSq, Piece.WHITE_PAWN);
		assertEquals("8/8/8/8/8/8/k7/P6K",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString1() {
		classUnderTest.setPieceAtSquare(testSq, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(BitBoard.c1, Piece.WHITE_KING);
		assertEquals("8/8/8/8/8/8/k7/P1K4K",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString2() {
		classUnderTest.setPieceAtSquare(BitBoard.h1, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(BitBoard.g1, Piece.WHITE_KING);
		assertEquals("8/8/8/8/8/8/k7/6KP",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString3() {
		classUnderTest.setPieceAtSquare(BitBoard.h1, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(BitBoard.g1, Piece.BLACK_KING);
		assertEquals("8/8/8/8/8/8/k7/6kp",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString4() {
		classUnderTest.setPieceAtSquare(BitBoard.h8, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(BitBoard.g8, Piece.BLACK_KING);
		assertEquals("6kp/8/8/8/8/8/k7/7K",classUnderTest.getAsFenString());
	}

//	@Test
//	public void testCouldLeadToCheck_Yes() {
//		classUnderTest.setPieceAtSquare(BitBoard.d8, Piece.BLACK_ROOK);
//		classUnderTest.setPieceAtSquare(BitBoard.d2, Piece.WHITE_PAWN);
//		classUnderTest.setPieceAtSquare(BitBoard.d1, Piece.WHITE_KING);
//		int move = Move.valueOf(Position.d2, Piece.WHITE_PAWN, Position.d4, Piece.NONE);
//		assertFalse(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.d1, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_Yes1() {
//		classUnderTest.setPieceAtSquare(BitBoard.h5, Piece.BLACK_BISHOP);
//		classUnderTest.setPieceAtSquare(BitBoard.e2, Piece.WHITE_PAWN);
//		classUnderTest.setPieceAtSquare(BitBoard.d1, Piece.WHITE_KING);
//		int move = Move.valueOf(Position.e2, Piece.WHITE_PAWN, Position.d3, Piece.NONE);
//		assertTrue(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.d1, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_No() {
//		classUnderTest.setPieceAtSquare(BitBoard.e3, Piece.WHITE_PAWN);
//		classUnderTest.setPieceAtSquare(BitBoard.d1, Piece.WHITE_KING);
//		int move = Move.valueOf(Position.e3, Piece.WHITE_PAWN, Position.e4, Piece.NONE);
//		assertFalse(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.d1, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_No1() {
//		classUnderTest.setPieceAtSquare(BitBoard.d1, Piece.WHITE_KNIGHT);
//		classUnderTest.setPieceAtSquare(BitBoard.e4, Piece.WHITE_KING);
//		int move = Move.valueOf(Position.d1, Piece.WHITE_KNIGHT, Position.c3, Piece.NONE);
//		assertFalse(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.e4, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_PinnedUpLeft() {
//		setUpPosition("8/8/1q6/2P5/8/4K3/8/8 w - - 0 1");
//		int move = Move.valueOf(Position.c5, Piece.WHITE_PAWN, Position.c6, Piece.NONE);
//		assertTrue(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.e3, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_NotPinnedUpLeft() {
//		setUpPosition("8/8/1q6/2P5/3P4/4K3/8/8 w - - 0 1 ");
//		int move = Move.valueOf(Position.c5, Piece.WHITE_PAWN, Position.c6, Piece.NONE);
//		assertFalse(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.e3, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_PinnedUpRight() {
//		setUpPosition("8/8/7q/6P1/8/4K3/8/8 w - - 0 1 ");
//		int move = Move.valueOf(Position.g5, Piece.WHITE_PAWN, Position.g6, Piece.NONE);
//		assertTrue(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.e3, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_NotPinnedUpRight() {
//		setUpPosition("8/8/7q/6P1/5P2/4K3/8/8 w - - 0 1 ");
//		int move = Move.valueOf(Position.g5, Piece.WHITE_PAWN, Position.g6, Piece.NONE);
//		assertFalse(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.e3, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_PinnedDownLeft() {
//		setUpPosition("8/8/3K4/8/1P6/q7/8/8 w - - 0 1 ");
//		int move = Move.valueOf(Position.b4, Piece.WHITE_PAWN, Position.b5, Piece.NONE);
//		assertTrue(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.d6, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_NotPinnedDownLeft() {
//		setUpPosition("8/8/3K4/2P5/1P6/q7/8/8 w - - 0 1 ");
//		int move = Move.valueOf(Position.b4, Piece.WHITE_PAWN, Position.b5, Piece.NONE);
//		assertFalse(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.d6, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_PinnedDownRight() {
//		setUpPosition("8/8/3K4/8/5P2/6q1/8/8 w - - 0 1 ");
//		int move = Move.valueOf(Position.f4, Piece.WHITE_PAWN, Position.f5, Piece.NONE);
//		assertTrue(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.d6, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_NotPinnedDownRight() {
//		setUpPosition("8/8/3K4/4P3/5P2/6q1/8/8 w - - 0 1 ");
//		int move = Move.valueOf(Position.f4, Piece.WHITE_PAWN, Position.f5, Piece.NONE);
//		assertFalse(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.d6, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_PinnedUp() {
//		setUpPosition("3q4/3R4/8/8/3K4/8/8/8 w - - 0 1");
//		int move = Move.valueOf(Position.d7, Piece.WHITE_ROOK, Position.e7, Piece.NONE);
//		assertTrue(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.d4, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_CapturePinningPieceUp() {
//		setUpPosition("3q4/3R4/8/8/3K4/8/8/8 w - - 0 1");
//		int move = Move.valueOf(Position.d7, Piece.WHITE_ROOK, Position.d8, Piece.BLACK_QUEEN);
//		assertFalse(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.d4, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_NotPinnedUp() {
//		setUpPosition("3q4/3R4/8/3P4/3K4/8/8/8 w - - 0 1");
//		int move = Move.valueOf(Position.d7, Piece.WHITE_ROOK, Position.e7, Piece.NONE);
//		assertFalse(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.d4, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_NotPinnedRight() {
//		setUpPosition("8/8/8/8/3KP1Rq/8/8/8 w - - 0 1 ");
//		int move = Move.valueOf(Position.g4, Piece.WHITE_ROOK, Position.g5, Piece.NONE);
//		assertFalse(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.d4, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_PinnedRight() {
//		setUpPosition("8/8/8/8/3K2Rq/8/8/8 w - - 0 1 ");
//		int move = Move.valueOf(Position.g4, Piece.WHITE_ROOK, Position.g5, Piece.NONE);
//		assertTrue(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.d4, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_NotPinnedLeft() {
//		setUpPosition("8/8/8/8/qRPK4/8/8/8 w - - 0 1 ");
//		int move = Move.valueOf(Position.b4, Piece.WHITE_ROOK, Position.b5, Piece.NONE);
//		assertFalse(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.d4, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_PinnedLeft() {
//		setUpPosition("8/8/8/8/qR1K4/8/8/8 w - - 0 1 ");
//		int move = Move.valueOf(Position.b4, Piece.WHITE_ROOK, Position.b5, Piece.NONE);
//		assertTrue(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.d4, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_NotPinnedDown() {
//		setUpPosition("8/8/8/8/3K4/3P4/3R4/3q4 w - - 0 1 ");
//		int move = Move.valueOf(Position.d2, Piece.WHITE_ROOK, Position.e2, Piece.NONE);
//		assertFalse(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.d4, true));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_PinnedDown() {
//		setUpPosition("8/8/8/8/3K4/8/3R4/3q4 w - - 0 1 ");
//		int move = Move.valueOf(Position.d2, Piece.WHITE_ROOK, Position.e2, Piece.NONE);
//		assertTrue(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.d4, true));
//	}
//	 
//	@Test
//	public void testCouldLeadToCheck_FromGame() {
//		setUpPosition("7k/8/5p1p/6R1/3Q4/5PP1/6KP/1r6 b - - 0 47 ");
//		int move = Move.valueOf(Position.f6, Piece.BLACK_PAWN, Position.g5, Piece.WHITE_ROOK);
//		assertTrue(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.h8, false));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_Position3EnPassant() {
//		setUpPosition("8/2p5/3p4/KP5r/1R3pPk/8/4P3/8 b - g3 0 1 ");
//		int move = Move.valueOfEnPassant(Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.f4, Piece.BLACK_PAWN, Position.g3, Piece.WHITE_PAWN, Piece.NONE);
//		assertTrue(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.h4, false));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_EnPassantDiagonal() {
//		setUpPosition("7K/B7/8/8/3pP3/8/8/6k1 b - e3 0 1");
//		int move = Move.valueOfEnPassant(Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.d4, Piece.BLACK_PAWN, Position.e3, Piece.WHITE_PAWN, Piece.NONE);
//		assertFalse(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.g1, false));
//	}
//	
//	@Test
//	public void testCouldLeadToCheck_EnPassantDiagonalOtherSide() {
//		setUpPosition("7K/B7/8/8/2Pp4/8/8/6k1 b - c3 0 1 ");
//		int move = Move.valueOfEnPassant(Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.d4, Piece.BLACK_PAWN, Position.c3, Piece.WHITE_PAWN, Piece.NONE);
//		assertTrue(classUnderTest.moveCausesDiscoveredCheck(move, BitBoard.g1, false));
//	}
	
//	@Test
//	public void test_enPassant_mateInOne6() {
//		// http://open-chess.org/viewtopic.php?f=7&t=997
//		setUpPosition("1rk2N2/1p6/8/B1Pp4/B6Q/K7/8/2R5 w - d6 0 1");
//		int move = Move.valueOfEnPassant(Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.c5, Piece.WHITE_PAWN, Position.d6, Piece.BLACK_PAWN, Piece.NONE);
//		assertEquals(Piece.NONE, classUnderTest.getPieceAtSquare(1L << BitBoard.d6));
//	}
	
	PositionManager pm;
	protected void setUpPosition(String fen) {
		pm = new PositionManager(fen, new DrawChecker());
		classUnderTest = pm.getTheBoard();
	}
	
	@Test
	public void test_isInsufficientMaterial_JustKings()throws IllegalNotationException {
		setUpPosition("8/8/8/8/8/8/k7/7K w - - 0 1");
		assertTrue(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_RookOnBoard()throws IllegalNotationException {
		setUpPosition("8/R7/8/8/8/8/k7/7K w - - 0 1");
		assertFalse(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_QueenOnBoard()throws IllegalNotationException {
		setUpPosition("8/Q7/8/8/8/8/k7/7K w - - 0 1");
		assertFalse(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_TwoKnights()throws IllegalNotationException {
		setUpPosition("8/K7/8/K7/8/8/k7/7K w - - 0 1");
		assertTrue(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_BishopKnight()throws IllegalNotationException {
		setUpPosition("8/B7/8/4N3/8/8/k7/7K w - - 0 1");
		assertFalse(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_BishopKnightDifferentSides()throws IllegalNotationException {
		setUpPosition("8/B7/8/4n3/8/8/k7/7K w - - 0 1");
		assertTrue(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_TwoBishops()throws IllegalNotationException {
		setUpPosition("BB6/8/8/8/8/8/k7/7K w - - 0 1");
		assertFalse(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_TwoBishopsDifferentSides()throws IllegalNotationException {
		setUpPosition("Bb6/8/8/8/8/8/k7/7K w - - 0 1");
		assertTrue(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_SingleBishop()throws IllegalNotationException {
		setUpPosition("B7/8/8/8/8/8/k7/7K w - - 0 1");
		assertTrue(classUnderTest.isInsufficientMaterial());
	}
	
	@Test
	public void test_isInsufficientMaterial_PawnOnBoard()throws IllegalNotationException {
		setUpPosition("8/P/8/8/8/8/k7/7K w - - 0 1");
		assertFalse(classUnderTest.isInsufficientMaterial());
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
		assertTrue(classUnderTest.isPlayableMove(move, inCheck, pm.castling)); // doesn't check legality
	}
	
	@Test
	public void test_is_playable_castling() {
		setUpPosition("8/8/8/3k4/8/8/8/R3K2R w K - 1 10");
		int move = Move.valueOf(Position.e1, Piece.WHITE_KING, Position.g1, Piece.NONE);
		boolean inCheck = false;
		assertTrue(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
		// Castle move not valid
		move = Move.valueOfCastling(Position.e1, Piece.WHITE_KING, Position.c1, Piece.NONE, Piece.NONE, Piece.NONE);
		assertFalse(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test
	public void test_is_playable_castling_target_square_is_attacked() {
		setUpPosition("8/8/8/2bk4/8/8/8/R3K2R w K - 1 10");
		int move = Move.valueOf(Position.e1, Piece.WHITE_KING, Position.g1, Piece.NONE);
		boolean inCheck = false;
		assertTrue(classUnderTest.isPlayableMove(move, inCheck, pm.castling)); // doesn't check legality
	}
	
	@Test
	public void test_is_playable_castling_king_in_check() {
		setUpPosition("8/8/8/1b1k4/8/8/8/R3K2R w K - 1 10");
		int move = Move.valueOfCastling(Position.e1, Piece.WHITE_KING, Position.g1, Piece.NONE, Piece.NONE, Piece.NONE);
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
	public void test_is_playable_weird_fen() {
		setUpPosition("r3k2r/1p1n1pp1/2p1p2p/3pP1b1/1R1P1P2/P1Nq2BP/2P3P1/5RK1 w kq - - 19");
		int move = Move.valueOf(Position.c2, Piece.WHITE_PAWN, Position.d3, Piece.BLACK_QUEEN);
		boolean inCheck = false;
		assertTrue(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test
	public void test_is_playable_move_refactor_issue() {
		setUpPosition("8/8/3p3k/1P6/3p3K/8/8/8 w - - - 3");
		int move = Move.valueOf(Position.h4, Piece.WHITE_KING, Position.g3, Piece.NONE);
		long trans = Transposition.valueOf((byte)1, (short)0, (byte)1, (short)move, 1);
		int hash_move = Move.valueOfFromTransposition(trans, pm.getTheBoard());
		boolean inCheck = false;
		//assertEquals(move, hash_move);
		assertTrue(classUnderTest.isPlayableMove(hash_move, inCheck, pm.castling));
	}	
	
	@Test 
	public void test_is_playable_handles_castling() {
		setUpPosition("r1bqkb1r/1p1n1ppp/p2ppn2/6B1/3NP3/2N2Q2/PPP2PPP/R3KB1R w KQkq - 0 1 ");
		int move = Move.valueOfCastlingBit(Move.TYPE_REGULAR_NONE, Position.e1, Piece.WHITE_KING, Position.c1, Piece.NONE, Piece.NONE);
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
	public void test_is_playable_rook_lift_blocked() {
		setUpPosition("k7/8/8/8/8/8/4p3/4R2K w - - 0 1 ");
		int move = Move.valueOf(Position.e1, Piece.WHITE_ROOK, Position.e5, Piece.NONE);
		boolean inCheck = false;
		assertFalse(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test 
	public void test_is_playable_rook_lift() {
		setUpPosition("k7/8/8/8/8/8/8/4R2K w - - 0 1 ");
		int move = Move.valueOf(Position.e1, Piece.WHITE_ROOK, Position.e5, Piece.NONE);
		boolean inCheck = false;
		assertTrue(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test 
	public void test_is_playable_rook_left_blocked() {
		setUpPosition("k7/8/8/8/8/8/8/3pR2K w - - 0 1 ");
		int move = Move.valueOf(Position.e1, Piece.WHITE_ROOK, Position.a1, Piece.NONE);
		boolean inCheck = false;
		assertFalse(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test 
	public void test_is_playable_rook_left() {
		setUpPosition("k7/8/8/8/8/8/8/4R2K w - - 0 1 ");
		int move = Move.valueOf(Position.e1, Piece.WHITE_ROOK, Position.a1, Piece.NONE);
		boolean inCheck = false;
		assertTrue(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test 
	public void test_is_playable_bishop_up_left_blocked() {
		setUpPosition("k7/8/8/8/8/8/3p4/4B2K w - - 0 1 ");
		int move = Move.valueOf(Position.e1, Piece.WHITE_BISHOP, Position.c3, Piece.NONE);
		boolean inCheck = false;
		assertFalse(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test 
	public void test_is_playable_bishop_up_left() {
		setUpPosition("k7/8/8/8/8/8/8/4B2K w - - 0 1 ");
		int move = Move.valueOf(Position.e1, Piece.WHITE_BISHOP, Position.c3, Piece.NONE);
		boolean inCheck = false;
		assertTrue(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test 
	public void test_is_playable_bishop_up_right_blocked() {
		setUpPosition("k7/8/8/8/8/8/5p2/4B2K w - - 0 1 ");
		int move = Move.valueOf(Position.e1, Piece.WHITE_BISHOP, Position.g3, Piece.NONE);
		boolean inCheck = false;
		assertFalse(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test 
	public void test_is_playable_bishop_up_right() {
		setUpPosition("k7/8/8/8/8/8/8/4B2K w - - 0 1 ");
		int move = Move.valueOf(Position.e1, Piece.WHITE_BISHOP, Position.g3, Piece.NONE);
		boolean inCheck = false;
		assertTrue(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test 
	public void test_is_playable_bishop_down_right_blocked() {
		setUpPosition("k3B3/5p2/8/8/8/8/8/7K w - - 0 1 ");
		int move = Move.valueOf(Position.e8, Piece.WHITE_BISHOP, Position.g6, Piece.NONE);
		boolean inCheck = false;
		assertFalse(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test 
	public void test_is_playable_bishop_down_right() {
		setUpPosition("k3B3/8/8/8/8/8/8/7K w - - 0 1 ");
		int move = Move.valueOf(Position.e8, Piece.WHITE_BISHOP, Position.g6, Piece.NONE);
		boolean inCheck = false;
		assertTrue(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
	
	@Test 
	public void test_is_playable_bishop_mate_in_four() {
		setUpPosition("r1r3k1/pb1p1p2/1p2p1p1/2pPP1B1/1nP4Q/1Pq2NP1/P4PBP/b2R2K1 w - - 0 1 ");
		int move = Move.valueOf(Position.g5, Piece.WHITE_BISHOP, Position.f6, Piece.NONE);
		boolean inCheck = false;
		assertTrue(classUnderTest.isPlayableMove(move, inCheck, pm.castling));
	}
			
	@Test
	public void knightUnderPromotion() {
		// Caused an error in assert builds as the combined position wasn't updated properly
		setUpPosition("8/8/8/8/8/k7/4p1K1/8 b - - 1 1");
		classUnderTest.doMoveBlack(Move.valueOfBit(Move.TYPE_PROMOTION_MASK, BitBoard.e2, Piece.BLACK_PAWN, BitBoard.e1, Piece.NONE, Piece.KNIGHT));
	}
}

