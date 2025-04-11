package eubos.position;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import eubos.board.BitBoard;
import eubos.board.Piece;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

public class PositionManagerTest {
	
	protected PositionManager classUnderTest;
	
	protected int expectedMove;
	protected List<Integer> expectedMoves;
	protected int expectedNumMoves = 0;
	protected MoveList ml;
	MoveListIterator it;
	
	@Before
	public void setUp() {
		expectedMoves = new LinkedList<Integer>();
	}
	
	private void createSutAndRegisterPe(String fenString) {
		classUnderTest = new PositionManager(fenString);
		ml = new MoveList(classUnderTest, 0);
		it = ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0);		
	}
	
	private void contains(int move) {
		MoveListIterator it = ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0);
		assertTrue(ml.getList(it).contains(move));
	}
	
	private void doesntContain(int move) {
		MoveListIterator it = ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0);
		assertFalse(ml.getList(it).contains(move));
	}
	
	protected void checkExpectedMoves(List<Integer> ml) {
		assertFalse(ml.size() == 0);
		assertEquals(expectedNumMoves, ml.size());
		for ( int mov : expectedMoves) {
			contains(mov);
		}
	}
	
	protected void checkNoMovesGenerated(List<GenericMove> ml) {
		assertTrue(ml.isEmpty());
	}
	
	@Test
	public void test_UndoPawnMove() throws IllegalNotationException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ....P...
		// 1 ........
		//   abcdefgh
		createSutAndRegisterPe("k6K/8/8/8/8/8/4P3/8 w - - - 1");
		classUnderTest.performMove(Move.valueOf( Position.e2, Piece.WHITE_PAWN, Position.e4, Piece.NONE ));
		classUnderTest.unperformMove();
		int expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( 1L << BitBoard.e2 );
		assertTrue( expectPawn==Piece.WHITE_PAWN );		
	}
	
	@Test
	public void test_UndoPawnPromotion() throws IllegalNotationException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ....p...
		// 1 ........
		//   abcdefgh
		createSutAndRegisterPe("k6K/8/8/8/8/8/4p3/8 b - - - 1");
		classUnderTest.performMove( Move.valueOf(Move.TYPE_PROMOTION_MASK, Position.e2, Piece.BLACK_PAWN, Position.e1, Piece.NONE, Piece.QUEEN));
		classUnderTest.unperformMove();
		int expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( 1L << BitBoard.e2 );
		assertEquals( Piece.BLACK_PAWN, expectPawn );
	}
	
	@Test
	public void test_UndoPawnCapture() throws IllegalNotationException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ...p....
		// 2 ....P...
		// 1 ........
		//   abcdefgh
		createSutAndRegisterPe("k6K/8/8/8/8/3p4/4P3/8 w - - - 1");
		classUnderTest.performMove( Move.valueOf( Position.e2, Piece.WHITE_PAWN, Position.d3, Piece.BLACK_PAWN ));
		classUnderTest.unperformMove();
		int expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( 1L << BitBoard.d3 );
		assertTrue( expectPawn==Piece.BLACK_PAWN );
		expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( 1L << BitBoard.e2 );
		assertTrue( expectPawn==Piece.WHITE_PAWN );
	}
	
	@Test
	public void test_enPassantCaptureAtC3() throws IllegalNotationException {
		createSutAndRegisterPe( "r3k2r/1bqpbppp/p1n1p3/3nP3/PpP1N3/3B1N2/1P2QPPP/R4RK1 b kq c3 0 1");
		int en_passant_move =  Move.valueOf( Position.b4, Piece.BLACK_PAWN, Position.c3, Piece.WHITE_PAWN );
		en_passant_move |= Move.MISC_EN_PASSANT_CAPTURE_MASK;
		classUnderTest.performMove(en_passant_move);
		int expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( 1L << BitBoard.c3 );
		assertEquals( Piece.BLACK_PAWN, expectPawn );
	}
	
	@Test
	public void test_WhiteKingSideCastle_performMove() throws IllegalNotationException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ....k..r
		//   abcdefgh
		createSutAndRegisterPe("k7/8/8/8/8/8/8/4K2R w K - - 1");
		classUnderTest.performMove( Move.valueOfCastlingBit(Move.TYPE_REGULAR_NONE, BitBoard.e1, Piece.KING, BitBoard.g1, Piece.NONE, Piece.NONE));
		int whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(1L << BitBoard.h1);
		assertTrue(whiteRook == Piece.NONE);
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(1L << BitBoard.f1);
		assertTrue( whiteRook==Piece.WHITE_ROOK );
		assertTrue(classUnderTest.castling.getFenFlags().equals("-"));
	}
	
	@Test
	public void test_WhiteKingSideCastle_unperformMove() throws IllegalNotationException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ....k..r
		//   abcdefgh
		createSutAndRegisterPe("k7/8/8/8/8/8/8/4K2R w K - - 1");
		int expectedMove = Move.valueOfCastlingBit( Move.TYPE_REGULAR_NONE, BitBoard.e1, Piece.WHITE_KING, BitBoard.g1, Piece.NONE, Piece.NONE );
		classUnderTest.performMove(expectedMove);
		int whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(1L << BitBoard.h1);
		assertTrue(whiteRook == Piece.NONE);
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(1L << BitBoard.f1);
		assertTrue( whiteRook==Piece.WHITE_ROOK );
		classUnderTest.unperformMove();
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(1L << BitBoard.f1);
		assertTrue(whiteRook == Piece.NONE);
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(1L << BitBoard.h1);
		assertTrue( whiteRook==Piece.WHITE_ROOK );
		int whiteKing = classUnderTest.getTheBoard().getPieceAtSquare(1L << BitBoard.e1);
		assertTrue( whiteKing==Piece.WHITE_KING );
		assertTrue(classUnderTest.castling.getFenFlags().equals("K"));
	}
	
	@Test
	public void test_WhiteKingSideCastleNotAvailWhenRookCaptured_performMove() throws IllegalNotationException {
		// 8 .......r
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ....K..R
		//   abcdefgh
		createSutAndRegisterPe("k6r/8/8/8/8/8/8/4K2R b K - - 1");
		classUnderTest.performMove(Move.valueOf( Position.h8, Piece.BLACK_ROOK, Position.h1, Piece.WHITE_ROOK ));
		assertTrue(classUnderTest.castling.getFenFlags().equals("-"));
	}
	
	@Test
	public void test_FenString() {
		String fenString = "7r/8/8/8/8/8/8/4K2R b K - - 1";
		createSutAndRegisterPe(fenString);
		assertEquals(fenString, classUnderTest.getFen());
	}
	
	@Test
	public void test_FenString1() {
		String fenString = "7r/8/8/8/8/4P3/8/4K2R b K e3 - 1";
		createSutAndRegisterPe(fenString);
		assertEquals(fenString, classUnderTest.getFen());
	}
	
	/* KING MOVES */
	
	@Test
	public void test_King_MoveGen_CornerTopLeft() throws IllegalNotationException {
		createSutAndRegisterPe("k7/8/8/8/8/8/8/8 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.a8, Piece.BLACK_KING, Position.b8, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.a8, Piece.BLACK_KING, Position.b7, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.a8, Piece.BLACK_KING, Position.a7, Piece.NONE ));
		expectedNumMoves = 3;
		checkExpectedMoves(ml.getList(it));
	}
	
	/* PAWN MOVES */
	@Test
	public void test_BlackPawn_MoveGen_InitialMoveOneSquare() {
		createSutAndRegisterPe("8/4p3/8/8/8/8/8/8 b - - 0 1");
		
		expectedMove = Move.valueOf( Position.e7, Piece.BLACK_PAWN, Position.e6, Piece.NONE );
		contains( expectedMove );
	}

	@Test
	public void test_BlackPawn_MoveGen_InitialMoveTwoSquares() {
		createSutAndRegisterPe("8/4p3/8/8/8/8/8/8 b - - 0 1");
		
		expectedMove = Move.valueOf( Position.e7, Piece.BLACK_PAWN, Position.e5, Piece.NONE );
		contains( expectedMove );
	}
	
	@Test
	public void test_BlackPawn_MoveGen_InitialBlocked() {
		createSutAndRegisterPe("8/4p3/4P3/8/8/8/8/8 b - - 0 1 ");
		
		assertTrue( ml.getList(it).size() == 0 );
	}

	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantLeft()  {
		// Black is on e4, white moves f4, then black ml contains capture en passant, exf
		createSutAndRegisterPe("k6K/8/8/8/4p3/8/5P2/8 w - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.f2, Piece.WHITE_PAWN, Position.f4, Piece.NONE ));
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0); // regenerate movelist following move
		expectedMove = Move.valueOfEnPassant( Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.e4, Piece.BLACK_PAWN, Position.f3, Piece.WHITE_PAWN, Piece.NONE );
		contains( expectedMove );
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantLeftFalse()  {
		// Black is on e4, white moves a knight to f4, check black ml doesn't contain a capture en passant, exf
		createSutAndRegisterPe("k6K/8/8/8/4p3/8/5N2/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.f2, Piece.WHITE_KNIGHT, Position.f4, Piece.NONE ));
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0); // regenerate movelist following move
		expectedMove = Move.valueOf( Position.e4, Piece.BLACK_PAWN, Position.f3, Piece.WHITE_PAWN );
		doesntContain( expectedMove );
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantRight()  {
		// Black is on e4, white moves d4, then black ml contains capture en passant, exd
		createSutAndRegisterPe("k6K/8/8/8/4p3/8/3P4/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.d2, Piece.WHITE_PAWN, Position.d4, Piece.NONE ));
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0); // regenerate movelist following move
		expectedMove = Move.valueOfEnPassant( Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.e4, Piece.BLACK_PAWN, Position.d3, Piece.WHITE_PAWN, Piece.NONE );
		contains( expectedMove );
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantRightFalse()  {
		// Black is on e4, white moves a knight to d4, check black ml doesn't contain a capture en passant, exd
		createSutAndRegisterPe("k6K/8/8/8/4p3/8/3N4/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.d2, Piece.WHITE_KNIGHT, Position.d4, Piece.NONE ));
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0); // regenerate movelist following move
		expectedMove = Move.valueOf( Position.e4, Piece.BLACK_PAWN, Position.d3, Piece.WHITE_PAWN );
		doesntContain( expectedMove );
	}	
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantFromAFile()  {
		// Black is on a4, white moves b4, then black ml contains capture en passant, axb
		createSutAndRegisterPe("k6K/8/8/8/p7/8/1P6/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.b2, Piece.WHITE_PAWN, Position.b4, Piece.NONE ));
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0); // regenerate movelist following move
		expectedMove = Move.valueOfEnPassant( Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.a4, Piece.BLACK_PAWN, Position.b3, Piece.WHITE_PAWN, Piece.NONE );
		contains( expectedMove );
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantFromAFile_1()  {
		// Black is on a4, white moves b4, then black ml contains capture en passant, axb
		createSutAndRegisterPe("k6K/8/8/8/pP6/8/8/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.b4, Piece.WHITE_PAWN, Position.b5, Piece.NONE ));
		
		expectedMove = Move.valueOfEnPassant( Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.a4, Piece.BLACK_PAWN, Position.b3, Piece.WHITE_PAWN, Piece.NONE );
		doesntContain( expectedMove );
	}	
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantFromHFile()  {
		// Black is on h4, white moves g4, then black ml contains capture en passant, hxg
		createSutAndRegisterPe("k6K/8/8/8/7p/8/6P1/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.g2, Piece.WHITE_PAWN, Position.g4, Piece.NONE ));
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0); // regenerate movelist following move
		expectedMove = Move.valueOfEnPassant( Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.h4, Piece.BLACK_PAWN, Position.g3, Piece.WHITE_PAWN, Piece.NONE );
		contains( expectedMove );
	}		
	
	@Test
	public void test_BlackPawn_MoveGen_MoveOneSquare()  {
		// After initial move, ensure that a pawn can't move 2 any longer
		createSutAndRegisterPe("8/4p3/8/8/8/8/4P3/K6k b - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.e7, Piece.BLACK_PAWN, Position.e6, Piece.NONE ));
		classUnderTest.performMove( Move.valueOf( Position.e2, Piece.WHITE_PAWN, Position.e4, Piece.NONE ));
		it = ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0); // regenerate movelist following move
		
		expectedMove = Move.valueOf( Position.e6, Piece.BLACK_PAWN, Position.e5, Piece.NONE );
		assertTrue( ml.getList(it).size() == 4 );
		contains( expectedMove );		
	}	

	@Test
	public void test_BlackPawn_MoveGen_CaptureLeft() {
		createSutAndRegisterPe("8/4p3/5P2/8/8/8/8/k6K b - - 0 1 ");
		
		expectedMove = Move.valueOf( Position.e7, Piece.BLACK_PAWN, Position.f6, Piece.WHITE_PAWN );
		contains( expectedMove );
	}

	@Test
	public void test_BlackPawn_MoveGen_CaptureRight() {
		createSutAndRegisterPe("8/4p3/3P4/8/8/8/8/8 b - - 0 1 ");
		
		expectedMove = Move.valueOf( Position.e7, Piece.BLACK_PAWN, Position.d6, Piece.WHITE_PAWN );
		contains( expectedMove );
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureFork() {
		createSutAndRegisterPe("8/4p3/3P1P2/8/8/8/8/8 b - - 0 1 ");
		
		int captureLeft = Move.valueOf( Position.e7, Piece.BLACK_PAWN, Position.d6, Piece.WHITE_PAWN );
		int captureRight = Move.valueOf( Position.e7, Piece.BLACK_PAWN, Position.f6, Piece.WHITE_PAWN );
		contains( captureLeft );
		contains( captureRight );
	}	
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureFromAFile() {
		// Can only capture left
		createSutAndRegisterPe("8/p7/1P6/8/8/8/8/8 b - - 0 1 ");
		;
		expectedMove = Move.valueOf( Position.a7, Piece.BLACK_PAWN, Position.b6, Piece.WHITE_PAWN );
		contains( expectedMove );
	}

	@Test
	public void test_BlackPawn_MoveGen_CaptureFromHFile() {
		// Can only capture right
		createSutAndRegisterPe("8/7p/6P1/8/8/8/8/8 b - - 0 1 ");
		
		expectedMove = Move.valueOf( Position.h7, Piece.BLACK_PAWN, Position.g6, Piece.WHITE_PAWN );
		contains( expectedMove );
	}
	
	@Test
	public void test_BlackPawn_MoveGen_PromoteQueen()  {
		createSutAndRegisterPe("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		expectedMove = Move.valueOf( Move.TYPE_PROMOTION_MASK, Position.e2, Piece.BLACK_PAWN, Position.e1, Piece.NONE, Piece.QUEEN );
		assertTrue( ml.getList(it).contains(expectedMove));
	}	

	@Test
	public void test_BlackPawn_MoveGen_PromoteKnight()  {
		createSutAndRegisterPe("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		expectedMove = Move.valueOf( Move.TYPE_PROMOTION_MASK, Position.e2, Piece.BLACK_PAWN, Position.e1, Piece.NONE, Piece.KNIGHT );
		assertTrue( ml.getList(it).contains(expectedMove));		
	}

	@Test
	@Ignore
	public void test_BlackPawn_MoveGen_PromoteBishop()  {
		createSutAndRegisterPe("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		expectedMove = Move.valueOf( Move.TYPE_PROMOTION_MASK, Position.e2, Piece.BLACK_PAWN, Position.e1, Piece.NONE, Piece.BISHOP );
		assertTrue( ml.getList(it).contains(expectedMove));		
	}

	@Test
	@Ignore
	public void test_BlackPawn_MoveGen_PromoteRook()  {
		createSutAndRegisterPe("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		expectedMove = Move.valueOf( Move.TYPE_PROMOTION_MASK, Position.e2, Piece.BLACK_PAWN, Position.e1, Piece.NONE, Piece.ROOK );
		assertTrue( ml.getList(it).contains(expectedMove));
	}
	
	@Test
	public void test_WhitePawn_MoveGen_InitialMoveOneSquare() {
		createSutAndRegisterPe("8/8/8/8/8/8/4P3/8 w - - 0 1");
		
		expectedMove = Move.valueOf( Position.e2, Piece.WHITE_PAWN, Position.e4, Piece.NONE  );
		contains( expectedMove );
	}

	@Test
	public void test_WhitePawn_MoveGen_InitialMoveTwoSquares() {
		createSutAndRegisterPe("8/8/8/8/8/8/4P3/8 w - - 0 1");
		
		expectedMove = Move.valueOf( Position.e2, Piece.WHITE_PAWN, Position.e3, Piece.NONE );
		contains( expectedMove );
	}
	
	@Test
	public void test_WhitePawn_MoveGen_InitialBlocked() {
		createSutAndRegisterPe("8/8/8/8/8/4p3/4P3/8 w - - 0 1");
		
		assertEquals( 0, ml.getList(it).size() );
	}

	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantLeft()  {
		createSutAndRegisterPe("8/3p4/8/4P3/8/8/8/k6K b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.d7, Piece.BLACK_PAWN, Position.d5, Piece.NONE ));
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0); // regenerate movelist following move
		expectedMove = Move.valueOfEnPassant( Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.e5, Piece.WHITE_PAWN, Position.d6, Piece.BLACK_PAWN, Piece.NONE );
		contains( expectedMove );
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantLeftFalse()  {
		createSutAndRegisterPe("8/5r2/8/4P3/8/8/8/k6K b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.f7, Piece.BLACK_ROOK, Position.f5, Piece.NONE ));
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0); // regenerate movelist following move
		expectedMove = Move.valueOf( Position.e5, Piece.WHITE_PAWN, Position.f6, Piece.BLACK_PAWN );
		doesntContain( expectedMove );
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantRight()  {
		createSutAndRegisterPe("8/5p2/8/4P3/8/8/8/k6K b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.f7, Piece.BLACK_PAWN, Position.f5, Piece.NONE ));
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0); // regenerate movelist following move
		expectedMove = Move.valueOfEnPassant(Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.e5, Piece.WHITE_PAWN, Position.f6, Piece.BLACK_PAWN, Piece.NONE );
		contains( expectedMove );
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantRightFalse()  {
		createSutAndRegisterPe("8/5r2/8/4P3/8/8/8/k6K b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.f7, Piece.BLACK_ROOK, Position.f5, Piece.NONE ));
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0); // regenerate movelist following move
		expectedMove = Move.valueOf( Position.e5, Piece.WHITE_PAWN, Position.f6, Piece.BLACK_PAWN );
		doesntContain( expectedMove );
	}	
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantFromAFile()  {
		// white is on a5, black moves b5, then black ml contains capture en passant, axb
		createSutAndRegisterPe("8/1p6/8/P7/8/8/8/k6K b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.b7, Piece.BLACK_PAWN, Position.b5, Piece.NONE ));
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0); // regenerate movelist following move
		expectedMove = Move.valueOfEnPassant( Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.a5, Piece.WHITE_PAWN, Position.b6, Piece.BLACK_PAWN, Piece.NONE );
		contains( expectedMove );
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantFromHFile()  {
		// Black is on h4, white moves g4, then black ml contains capture en passant, hxg
		createSutAndRegisterPe("8/6p1/8/7P/8/8/8/k6K b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.g7, Piece.BLACK_PAWN, Position.g5, Piece.NONE ));
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0); // regenerate movelist following move
		expectedMove = Move.valueOfEnPassant( Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.h5, Piece.WHITE_PAWN, Position.g6, Piece.BLACK_PAWN, Piece.NONE );
		contains( expectedMove );
	}		
	
	@Test
	public void test_WhitePawn_MoveGen_MoveOneSquare()  {
		// After initial move, ensure that a pawn can't move 2 any longer
		createSutAndRegisterPe("k6K/5p2/8/8/8/8/4P3/8 w - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.e2, Piece.WHITE_PAWN, Position.e4, Piece.NONE ));
		classUnderTest.performMove( Move.valueOf( Position.f7, Piece.BLACK_PAWN, Position.f6, Piece.NONE ));
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0); // regenerate movelist following move
		
		expectedMove = Move.valueOf( Position.e4, Piece.WHITE_PAWN, Position.e5, Piece.NONE );
		assertTrue( ml.getList(it).size() == 4 );
		contains( expectedMove );		
	}	

	@Test
	public void test_WhitePawn_MoveGen_CaptureLeft() {
		createSutAndRegisterPe("8/8/8/8/8/5p2/4P3/8 w - - 0 1");
		
		expectedMove = Move.valueOf( Position.e2, Piece.WHITE_PAWN, Position.f3, Piece.BLACK_PAWN );
		contains( expectedMove );
	}

	@Test
	public void test_WhitePawn_MoveGen_CaptureRight() {
		createSutAndRegisterPe("8/8/8/8/8/3p4/4P3/8 w - - 0 1");
		
		expectedMove = Move.valueOf( Position.e2, Piece.WHITE_PAWN, Position.d3, Piece.BLACK_PAWN );
		contains( expectedMove );
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureFork() {
		createSutAndRegisterPe("8/8/8/8/8/3p1p2/4P3/8 w - - 0 1");
		
		int captureLeft = Move.valueOf( Position.e2, Piece.WHITE_PAWN, Position.d3, Piece.BLACK_PAWN );
		int captureRight = Move.valueOf( Position.e2, Piece.WHITE_PAWN, Position.f3, Piece.BLACK_PAWN );
		contains( captureLeft );
		contains( captureRight );
	}	
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureFromAFile() {
		// Can only capture left
		createSutAndRegisterPe("8/8/8/8/8/1p6/P7/8 w - - 0 1");
		
		expectedMove = Move.valueOf( Position.a2, Piece.WHITE_PAWN, Position.b3, Piece.BLACK_PAWN );
		contains( expectedMove );
	}

	@Test
	public void test_WhitePawn_MoveGen_CaptureFromHFile() {
		// Can only capture right
		createSutAndRegisterPe("8/8/8/8/8/6p1/7P/8 w - - 0 1");
		
		expectedMove = Move.valueOf( Position.h2, Piece.WHITE_PAWN, Position.g3, Piece.BLACK_PAWN );
		contains( expectedMove );
	}
	
	@Test
	public void test_WhitePawn_MoveGen_PromoteQueen()  {
		createSutAndRegisterPe("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		expectedMove = Move.valueOf( Move.TYPE_PROMOTION_MASK, Position.e7, Piece.PAWN, Position.e8, Piece.NONE, Piece.QUEEN );
		assertTrue( ml.getList(it).contains(expectedMove));
	}	

	@Test
	public void test_WhitePawn_MoveGen_PromoteKnight()  {
		createSutAndRegisterPe("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		expectedMove = Move.valueOf( Move.TYPE_PROMOTION_MASK, Position.e7, Piece.PAWN, Position.e8, Piece.NONE, Piece.KNIGHT );
		assertTrue( ml.getList(it).contains(expectedMove));		
	}

	@Test
	@Ignore
	public void test_WhitePawn_MoveGen_PromoteBishop()  {
		createSutAndRegisterPe("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		ml = new MoveList(classUnderTest, 0);
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0);
		expectedMove = Move.valueOf( Move.TYPE_PROMOTION_MASK, Position.e7, Piece.PAWN, Position.e8, Piece.NONE, Piece.BISHOP );
		assertTrue( ml.getList(it).contains(expectedMove));			
	}

	@Test
	@Ignore
	public void test_WhitePawn_MoveGen_PromoteRook()  {
		createSutAndRegisterPe("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		ml = new MoveList(classUnderTest, 0);
		ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0);
		expectedMove = Move.valueOf( Move.TYPE_PROMOTION_MASK, Position.e7, Piece.PAWN, Position.e8, Piece.NONE, Piece.ROOK );
		assertTrue( ml.getList(it).contains(expectedMove));	
	}
	
	/* Bishop Moves */
	@Test
	public void test_Bishop_MoveGen_CornerTopLeft() {
		createSutAndRegisterPe("b7/8/8/8/8/8/8/8 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.a8, Piece.BLACK_BISHOP, Position.b7, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.a8, Piece.BLACK_BISHOP, Position.h1, Piece.NONE ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml.getList(it));
	}

	@Test
	public void test_Bishop_MoveGen_CornerTopRight() {
		createSutAndRegisterPe("7b/8/8/8/8/8/8/8 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.h8, Piece.BLACK_BISHOP, Position.g7, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.h8, Piece.BLACK_BISHOP, Position.a1, Piece.NONE ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml.getList(it));
	}
	
	@Test
	public void test_Bishop_MoveGen_CornerBottomRight() {
		createSutAndRegisterPe("8/8/8/8/8/8/8/7b b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.h1, Piece.BLACK_BISHOP, Position.g2, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.h1, Piece.BLACK_BISHOP, Position.a8, Piece.NONE ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml.getList(it));
	}
	
	@Test
	public void test_Bishop_MoveGen_CornerBottomLeft() {
		createSutAndRegisterPe("8/8/8/8/8/8/8/b7 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.a1, Piece.BLACK_BISHOP, Position.b2, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.a1, Piece.BLACK_BISHOP, Position.h8, Piece.NONE ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml.getList(it));
	}
	
	@Test
	public void test_Bishop_MoveGen_CornerBottomLeft_ObstructedOwnPieces() {
		createSutAndRegisterPe("7k/8/8/8/8/8/1p6/b6K b - - 0 1");
		
		expectedNumMoves = 5;
		checkExpectedMoves(ml.getList(it));
	}

	@Test
	public void test_Bishop_MoveGen_LeftEdge_PartiallyObstructedOwnPiece() {
		createSutAndRegisterPe("7k/8/8/8/b7/1p6/8/7K b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.a4, Piece.BLACK_BISHOP, Position.b5, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.a4, Piece.BLACK_BISHOP, Position.e8, Piece.NONE ));
		expectedNumMoves = 8;
		checkExpectedMoves(ml.getList(it));		
	}
	
	@Test
	public void test_Bishop_MoveGen_LeftEdge_PartiallyObstructedCapturablePiece() {
		createSutAndRegisterPe("8/8/8/8/b7/1P6/8/8 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.a4, Piece.BLACK_BISHOP, Position.b5, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.a4, Piece.BLACK_BISHOP, Position.e8, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.a4, Piece.BLACK_BISHOP, Position.b3, Piece.WHITE_PAWN ));
		expectedNumMoves = 5;
		checkExpectedMoves(ml.getList(it));	
	}
	
	@Test
	public void test_Bishop_MoveGen_Middle_ObstructedCapturablePieces() {
		createSutAndRegisterPe("8/8/8/3P1P2/4b3/3P1P2/8/8 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.d3, Piece.WHITE_PAWN ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.d5, Piece.WHITE_PAWN ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.f3, Piece.WHITE_PAWN ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.f5, Piece.WHITE_PAWN ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml.getList(it));
	}
	
	@Test
	public void test_Bishop_MoveGen_Middle_ObstructedMixturePieces() {
		createSutAndRegisterPe("k7/8/8/3P1p2/4b3/3P1p2/8/K7 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.d3, Piece.WHITE_PAWN ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.d5, Piece.WHITE_PAWN ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml.getList(it));
	}
	
	@Test
	public void test_Bishop_MoveGen_Middle_Unobstructed() {
		createSutAndRegisterPe("8/8/8/4P3/3PbP2/4P3/8/8 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.d3, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.d5, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.f3, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.f5, Piece.NONE ));
		expectedNumMoves = 13;
		checkExpectedMoves(ml.getList(it));
	}
	
	@Test
	public void test_Bishop_MoveGen_CapturesOnlySinglePiece() {
		createSutAndRegisterPe("8/8/8/8/8/2P5/1P6/b7 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.a1, Piece.BLACK_BISHOP, Position.b2, Piece.WHITE_PAWN ));
		expectedNumMoves = 1;
		checkExpectedMoves(ml.getList(it));
		doesntContain( Move.valueOf( Position.a1, Piece.BLACK_BISHOP, Position.c3, Piece.WHITE_PAWN ));
	}
	
	/* Knight Moves */
	int startTestOnSq;
	@Test
	public void test_SquareA8() {
		startTestOnSq = Position.a8;
		createSutAndRegisterPe("N7/8/8/8/8/8/8/8 w - - 0 1");
		expectedMoves.add( Move.valueOf( startTestOnSq, Piece.WHITE_KNIGHT, Position.c7, Piece.NONE ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Piece.WHITE_KNIGHT, Position.b6, Piece.NONE ));
		expectedNumMoves = 2;
		checkExpectedMoves(ml.getList(it));
	}
	
	@Test
	public void test_SquareB7() {
		startTestOnSq = Position.b7;
		createSutAndRegisterPe("8/1N6/8/8/8/8/8/8 w - - 0 1");
		expectedMoves.add( Move.valueOf( startTestOnSq, Piece.WHITE_KNIGHT, Position.d8, Piece.NONE ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Piece.WHITE_KNIGHT, Position.d6, Piece.NONE ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Piece.WHITE_KNIGHT, Position.a5, Piece.NONE ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Piece.WHITE_KNIGHT, Position.c5, Piece.NONE ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml.getList(it));
	}
	
	@Test
	public void test_SquareC6() {
		startTestOnSq = Position.c6;
		createSutAndRegisterPe("8/8/2N5/8/8/8/8/8 w - - 0 1");
		expectedMoves.add( Move.valueOf( startTestOnSq, Piece.WHITE_KNIGHT, Position.b8, Piece.NONE ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Piece.WHITE_KNIGHT, Position.d8, Piece.NONE ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Piece.WHITE_KNIGHT, Position.e7, Piece.NONE ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Piece.WHITE_KNIGHT, Position.e5, Piece.NONE ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Piece.WHITE_KNIGHT, Position.b4, Piece.NONE ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Piece.WHITE_KNIGHT, Position.d4, Piece.NONE ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Piece.WHITE_KNIGHT, Position.a7, Piece.NONE ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Piece.WHITE_KNIGHT, Position.a5, Piece.NONE ));
		expectedNumMoves = 8;
		checkExpectedMoves(ml.getList(it));
	}
	
	/* Rook Moves */
	@Test
	public void test_Rook_MoveGen_CornerTopLeft() {
		createSutAndRegisterPe("R7/8/8/8/8/8/8/8 w - - 0 1 ");
		
		expectedMoves.add( Move.valueOf( Position.a8, Piece.WHITE_ROOK, Position.a7, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.a8, Piece.WHITE_ROOK, Position.b8, Piece.NONE ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml.getList(it));
	}

	@Test
	public void test_Rook_MoveGen_CornerTopRight() {
		createSutAndRegisterPe("7R/8/8/8/8/8/8/8 w - - 0 1 ");
		
		expectedMoves.add( Move.valueOf( Position.h8, Piece.WHITE_ROOK, Position.h7, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.h8, Piece.WHITE_ROOK, Position.g8, Piece.NONE ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml.getList(it));
	}
	
	@Test
	public void test_Rook_MoveGen_CornerBottomRight() {
		createSutAndRegisterPe("8/8/8/8/8/8/8/7R w - - 0 1 ");
		
		expectedMoves.add( Move.valueOf( Position.h1, Piece.WHITE_ROOK, Position.h2, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.h1, Piece.WHITE_ROOK, Position.g1, Piece.NONE ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml.getList(it));
	}
	
	@Test
	public void test_Rook_MoveGen_CornerBottomLeft() {
		createSutAndRegisterPe("8/8/8/8/8/8/8/R7 w - - 0 1 ");
		
		expectedMoves.add( Move.valueOf( Position.a1, Piece.WHITE_ROOK, Position.a2, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.a1, Piece.WHITE_ROOK, Position.b1, Piece.NONE ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml.getList(it));
	}
	
	@Test
	public void test_Rook_MoveGen_CornerBottomLeft_ObstructedOwnPieces() {
		createSutAndRegisterPe("k7/8/8/8/8/8/P7/RK6 w - - 0 1");
		
		expectedNumMoves = 5;
		checkExpectedMoves(ml.getList(it));
	}

	@Test
	public void test_Rook_MoveGen_CornerBottomLeft_PartiallyObstructedOwnPiece() {
		createSutAndRegisterPe("7k/8/8/8/8/8/P7/R6K w - - 0 1");
		
		expectedNumMoves = 11;
		checkExpectedMoves(ml.getList(it));
	}
	
	@Test
	public void test_Rook_MoveGen_CornerBottomLeft_PartiallyObstructedCapturablePiece() {
		createSutAndRegisterPe("8/8/8/8/8/8/p7/R7 w - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.a1, Piece.WHITE_ROOK, Position.a2, Piece.BLACK_PAWN ));
		expectedMoves.add( Move.valueOf( Position.a1, Piece.WHITE_ROOK, Position.b1, Piece.NONE ));		
		expectedNumMoves = 8;
		checkExpectedMoves(ml.getList(it));
	}
	
	@Test
	public void test_Rook_MoveGen_Middle_ObstructedCapturablePieces() {
		createSutAndRegisterPe("8/8/8/4P3/3PrP2/4P3/8/8 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.f4, Piece.WHITE_PAWN ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.d4, Piece.WHITE_PAWN ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.e5, Piece.WHITE_PAWN ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.e3, Piece.WHITE_PAWN ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml.getList(it));
	}
	
	@Test
	public void test_Rook_MoveGen_Middle_ObstructedMixturePieces() {
		createSutAndRegisterPe("k7/8/8/4P3/3prp2/4P3/8/K7 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.e5, Piece.WHITE_PAWN ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.e3, Piece.WHITE_PAWN ));
		expectedNumMoves = 9;
		checkExpectedMoves(ml.getList(it));
	}
	
	@Test
	public void test_Rook_MoveGen_Middle_Unobstructed() {
		createSutAndRegisterPe("8/8/8/3P1P2/4r3/3P1P2/8/8 b - - 0 1 ");
		
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.f4, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.d4, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.e5, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.e3, Piece.NONE ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml.getList(it));
	}

	@Test
	public void test_Rook_MoveGen_CapturesOnlySinglePiece() {
		createSutAndRegisterPe("8/8/8/8/8/P7/P7/r7 b - - 0 1 ");
		
		assertFalse(ml.getList(it).size() == 0);
		contains( Move.valueOf( Position.a1, Piece.BLACK_ROOK, Position.a2, Piece.WHITE_PAWN ));
		doesntContain( Move.valueOf( Position.a1, Piece.BLACK_ROOK, Position.a3, Piece.WHITE_PAWN ));
	}
	
	@Test
	public void test_take_repetition() throws IllegalNotationException {
		createSutAndRegisterPe("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
		String moves = "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6 c1e3 e7e6 f2f3 b7b5 d1d3 b5b4 c3e2 e6e5 d4b3 "
		+ "f8e7 d3c4 d6d5 e4d5 f6d5 e1c1 c8e6 c4e4 b8c6 c2c4 f7f5 e4b1 d5e3 d1d8 a8d8 b3d2 e6c4 e2g3 e7g5 "
		+ "f1c4 e3c4 b1f5 g5d2 c1b1 c6d4 f5d3 d8c8 g3f5 d4f5 d3f5 e8e7 a2a3 g7g6 f5e4 b4a3 b2b3 c4e3 e4e5 "
		+ "e7f7 e5f4 f7g8 f4d6 c8c2 d6e6 g8g7 e6e7 g7g8 e7d8 g8f7 d8d7 f7f8 d7d6 f8e8 d6e6 e8d8 e6d6 d8c8 "
		+ "d6a6 c8d7 a6a3 h8c8 a3a4 d7e6 a4h4 c8c3 h4a4 e6e7 g2g3 e7f8 f3f4 h7h6 a4b5 d2c1 b5b8 f8f7 b1a1 "
		+ "c1b2 a1b1 c3c7 b8c7 c2c7 b1b2 c7c2 b2a3 e3g4 h2h3 g4f6 g3g4 c2f2 g4g5 h6g5 f4g5 f6e4 h3h4 f2f3 "
		+ "a3a2 e4d2 b3b4 d2c4 a2b1 f3f2 b1c1 f2b2 h1f1 f7g7 f1e1 b2b4 e1e4 g7f7 c1c2 c4a3 c2d3 b4b3 d3d4 "
		+ "a3c2 d4c5 b3c3 c5d5 c3d3 d5e5 d3d7 e4f4 f7g7 e5e6 d7a7 f4e4 a7a1 e4c4 a1e1 e6d6 e1d1 d6e5 c2e3 "
		+ "c4c7 g7g8 c7c8 g8f7 c8c7 f7f8 e5e6 e3d5 c7c8 f8g7 c8c4 d1d2 c4c6 d5f4 e6e5 f4d3 e5e4 d3b4 c6c7 "
		+ "g7f8 e4e5 d2e2 e5d6 b4d3 c7c4 f8f7 " 
		/* The repetition starts from here, when rook goes to c7 and gives check. */
		+ "c4c7 f7g8 c7c8 g8f7 c8c7 f7g8 c7c8 g8f7";
		String [] move_array = moves.split(" ");
		for (String move: move_array) {
			classUnderTest.performMove(Move.toMove(new GenericMove(move), classUnderTest.getTheBoard()));
		}
		// At this point the draw checker should detect a draw on the next move
		classUnderTest.performMove(Move.toMove(new GenericMove("c8c7"), classUnderTest.getTheBoard()));
		assertTrue(classUnderTest.repetitionPossible);
	}
	
	@Test
	public void test_take_repetition_with_other_moves() throws IllegalNotationException {
		createSutAndRegisterPe("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
		String moves = "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6 c1e3 e7e6 f2f3 b7b5 d1d3 b5b4 c3e2 e6e5 d4b3 "
		+ "f8e7 d3c4 d6d5 e4d5 f6d5 e1c1 c8e6 c4e4 b8c6 c2c4 f7f5 e4b1 d5e3 d1d8 a8d8 b3d2 e6c4 e2g3 e7g5 "
		+ "f1c4 e3c4 b1f5 g5d2 c1b1 c6d4 f5d3 d8c8 g3f5 d4f5 d3f5 e8e7 a2a3 g7g6 f5e4 b4a3 b2b3 c4e3 e4e5 "
		+ "e7f7 e5f4 f7g8 f4d6 c8c2 d6e6 g8g7 e6e7 g7g8 e7d8 g8f7 d8d7 f7f8 d7d6 f8e8 d6e6 e8d8 e6d6 d8c8 "
		+ "d6a6 c8d7 a6a3 h8c8 a3a4 d7e6 a4h4 c8c3 h4a4 e6e7 g2g3 e7f8 f3f4 h7h6 a4b5 d2c1 b5b8 f8f7 b1a1 "
		+ "c1b2 a1b1 c3c7 b8c7 c2c7 b1b2 c7c2 b2a3 e3g4 h2h3 g4f6 g3g4 c2f2 g4g5 h6g5 f4g5 f6e4 h3h4 f2f3 "
		+ "a3a2 e4d2 b3b4 d2c4 a2b1 f3f2 b1c1 f2b2 h1f1 f7g7 f1e1 b2b4 e1e4 g7f7 c1c2 c4a3 c2d3 b4b3 d3d4 "
		+ "a3c2 d4c5 b3c3 c5d5 c3d3 d5e5 d3d7 e4f4 f7g7 e5e6 d7a7 f4e4 a7a1 e4c4 a1e1 e6d6 e1d1 d6e5 c2e3 "
		+ "c4c7 g7g8 c7c8 g8f7 c8c7 f7f8 e5e6 e3d5 c7c8 f8g7 c8c4 d1d2 c4c6 d5f4 e6e5 f4d3 e5e4 d3b4 c6c7 "
		+ "g7f8 e4e5 d2e2 e5d6 b4d3 c7c4 f8f7 " 
		/* The repetition starts from here, when rook goes to c7 and gives check. */
		+ "c4c7 f7g8 c7c8 g8f7 c8c7 f7g8 c7c8 g8f7";
		String [] move_array = moves.split(" ");
		for (String move: move_array) {
			classUnderTest.performMove(Move.toMove(new GenericMove(move), classUnderTest.getTheBoard()));
		}
		// Apply moves mimicking search
		moves = "d6d7 d6d5 d6c6 d6c7";
		move_array = moves.split(" ");
		for (String move: move_array) {
			classUnderTest.performMove(Move.toMove(new GenericMove(move), classUnderTest.getTheBoard()));
			classUnderTest.unperformMove();
		}
		classUnderTest.performMove(Move.toMove(new GenericMove("h4h5"), classUnderTest.getTheBoard()));
		classUnderTest.performMove(Move.toMove(new GenericMove("g6h5"), classUnderTest.getTheBoard()));
		classUnderTest.unperformMove();
		classUnderTest.unperformMove();
		// At this point the draw checker should detect a draw on the next move
		classUnderTest.performMove(Move.toMove(new GenericMove("c8c7"), classUnderTest.getTheBoard()));
		assertTrue(classUnderTest.repetitionPossible);
	}
	
	
}
