package eubos.position;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import eubos.board.InvalidPieceException;
import eubos.board.Piece;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

public class PositionManagerTest {
	
	protected PositionManager classUnderTest;
	
	protected int expectedMove;
	protected List<Integer> expectedMoves;
	protected int expectedNumMoves = 0;
	protected MoveList ml;
	protected MoveList theMl;
	
	@Before
	public void setUp() {
		expectedMoves = new LinkedList<Integer>();
	}
	
	private void createSutAndRegisterPe(String fenString) {
		classUnderTest = new PositionManager(fenString);
		try {
			ml = new MoveList(classUnderTest);
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void contains(int move) {
		for (int i=0; i<ml.getList().size(); i++) {
			if (ml.getList().get(i) == move)
				return;
		}
		fail();
	}
	
	private void doesntContain(int move) {
		for (int i=0; i<ml.getList().size(); i++) {
			if (ml.getList().get(i) == move)
				fail();
		}
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
	public void test_UndoPawnMove() throws InvalidPieceException, IllegalNotationException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ....P...
		// 1 ........
		//   abcdefgh
		createSutAndRegisterPe("8/8/8/8/8/8/4P3/8 w - - - -");
		classUnderTest.performMove(Move.valueOf( Position.e2, Piece.WHITE_PAWN, Position.e4, Piece.NONE ));
		classUnderTest.unperformMove();
		int expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( Position.e2 );
		assertTrue( expectPawn==Piece.WHITE_PAWN );		
	}
	
	@Test
	public void test_UndoPawnPromotion() throws InvalidPieceException, IllegalNotationException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ....p...
		// 1 ........
		//   abcdefgh
		createSutAndRegisterPe("8/8/8/8/8/8/4p3/8 b - - - -");
		classUnderTest.performMove( Move.valueOf(Move.TYPE_PROMOTION_MASK, Position.e2, Piece.BLACK_PAWN, Position.e1, Piece.NONE, Piece.QUEEN));
		classUnderTest.unperformMove();
		int expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( Position.e2 );
		assertEquals( Piece.BLACK_PAWN, expectPawn );
	}
	
	@Test
	public void test_UndoPawnCapture() throws InvalidPieceException, IllegalNotationException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ...p....
		// 2 ....P...
		// 1 ........
		//   abcdefgh
		createSutAndRegisterPe("8/8/8/8/8/3p4/4P3/8 w - - - -");
		classUnderTest.performMove( Move.valueOf( Position.e2, Piece.WHITE_PAWN, Position.d3, Piece.BLACK_PAWN ));
		classUnderTest.unperformMove();
		int expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( Position.d3 );
		assertTrue( expectPawn==Piece.BLACK_PAWN );
		expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( Position.e2 );
		assertTrue( expectPawn==Piece.WHITE_PAWN );
	}
	
	@Test
	public void test_enPassantCaptureAtC3() throws InvalidPieceException, IllegalNotationException {
		createSutAndRegisterPe( "r3k2r/1bqpbppp/p1n1p3/3nP3/PpP1N3/3B1N2/1P2QPPP/R4RK1 b kq c3 0 1");
		int en_passant_move =  Move.valueOf( Position.b4, Piece.BLACK_PAWN, Position.c3, Piece.WHITE_PAWN );
		en_passant_move |= Move.MISC_EN_PASSANT_CAPTURE_MASK;
		classUnderTest.performMove(en_passant_move);
		int expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( Position.c3 );
		assertEquals( Piece.BLACK_PAWN, expectPawn );
	}
	
	@Test
	public void test_WhiteKingSideCastle_performMove() throws InvalidPieceException, IllegalNotationException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ....k..r
		//   abcdefgh
		createSutAndRegisterPe("8/8/8/8/8/8/8/4K2R w K - - -");
		classUnderTest.performMove( Move.valueOf(Move.TYPE_REGULAR_NONE, Position.e1, Piece.KING, Position.g1, Piece.NONE, Piece.NONE));
		int whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(Position.h1);
		assertTrue(whiteRook == Piece.NONE);
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(Position.f1);
		assertTrue( whiteRook==Piece.WHITE_ROOK );
		assertTrue(classUnderTest.castling.getFenFlags().equals("-"));
	}
	
	@Test
	public void test_WhiteKingSideCastle_unperformMove() throws InvalidPieceException, IllegalNotationException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ....k..r
		//   abcdefgh
		createSutAndRegisterPe("8/8/8/8/8/8/8/4K2R w K - - -");
		int expectedMove = Move.valueOf( Move.TYPE_REGULAR_NONE, Position.e1, Piece.WHITE_KING, Position.g1, Piece.NONE, Piece.NONE );
		classUnderTest.performMove(expectedMove);
		int whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(Position.h1);
		assertTrue(whiteRook == Piece.NONE);
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(Position.f1);
		assertTrue( whiteRook==Piece.WHITE_ROOK );
		classUnderTest.unperformMove();
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(Position.f1);
		assertTrue(whiteRook == Piece.NONE);
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(Position.h1);
		assertTrue( whiteRook==Piece.WHITE_ROOK );
		int whiteKing = classUnderTest.getTheBoard().getPieceAtSquare(Position.e1);
		assertTrue( whiteKing==Piece.WHITE_KING );
		assertTrue(classUnderTest.castling.getFenFlags().equals("K"));
	}
	
	@Test
	public void test_WhiteKingSideCastleNotAvailWhenRookCaptured_performMove() throws InvalidPieceException, IllegalNotationException {
		// 8 .......r
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ....K..R
		//   abcdefgh
		createSutAndRegisterPe("7r/8/8/8/8/8/8/4K2R b K - - -");
		classUnderTest.performMove(Move.valueOf( Position.h8, Piece.BLACK_ROOK, Position.h1, Piece.WHITE_ROOK ));
		assertTrue(classUnderTest.castling.getFenFlags().equals("-"));
	}
	
	@Test
	public void test_FenString() {
		String fenString = "7r/8/8/8/8/8/8/4K2R b K - - 0";
		createSutAndRegisterPe(fenString);
		assertEquals(fenString, classUnderTest.getFen());
	}
	
	@Test
	public void test_FenString1() {
		String fenString = "7r/8/8/8/8/4P3/8/4K2R b K e3 - 0";
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
		checkExpectedMoves(ml.getList());
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
		
		assertTrue( ml.getList().size() == 0 );
	}

	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantLeft() throws InvalidPieceException {
		// Black is on e4, white moves f4, then black ml contains capture en passant, exf
		createSutAndRegisterPe("8/8/8/8/4p3/8/5P2/8 w - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.f2, Piece.WHITE_PAWN, Position.f4, Piece.NONE ));
		ml = new MoveList(classUnderTest); // regenerate movelist following move
		expectedMove = Move.valueOf( Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.e4, Piece.BLACK_PAWN, Position.f3, Piece.WHITE_PAWN, Piece.NONE );
		contains( expectedMove );
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantLeftFalse() throws InvalidPieceException {
		// Black is on e4, white moves a knight to f4, check black ml doesn't contain a capture en passant, exf
		createSutAndRegisterPe("8/8/8/8/4p3/8/5N2/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.f2, Piece.WHITE_KNIGHT, Position.f4, Piece.NONE ));
		ml = new MoveList(classUnderTest); // regenerate movelist following move
		expectedMove = Move.valueOf( Position.e4, Piece.BLACK_PAWN, Position.f3, Piece.WHITE_PAWN );
		doesntContain( expectedMove );
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantRight() throws InvalidPieceException {
		// Black is on e4, white moves d4, then black ml contains capture en passant, exd
		createSutAndRegisterPe("8/8/8/8/4p3/8/3P4/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.d2, Piece.WHITE_PAWN, Position.d4, Piece.NONE ));
		ml = new MoveList(classUnderTest); // regenerate movelist following move
		expectedMove = Move.valueOf( Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.e4, Piece.BLACK_PAWN, Position.d3, Piece.WHITE_PAWN, Piece.NONE );
		contains( expectedMove );
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantRightFalse() throws InvalidPieceException {
		// Black is on e4, white moves a knight to d4, check black ml doesn't contain a capture en passant, exd
		createSutAndRegisterPe("8/8/8/8/4p3/8/3N4/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.d2, Piece.WHITE_KNIGHT, Position.d4, Piece.NONE ));
		ml = new MoveList(classUnderTest); // regenerate movelist following move
		expectedMove = Move.valueOf( Position.e4, Piece.BLACK_PAWN, Position.d3, Piece.WHITE_PAWN );
		doesntContain( expectedMove );
	}	
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantFromAFile() throws InvalidPieceException {
		// Black is on a4, white moves b4, then black ml contains capture en passant, axb
		createSutAndRegisterPe("8/8/8/8/p7/8/1P6/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.b2, Piece.WHITE_PAWN, Position.b4, Piece.NONE ));
		ml = new MoveList(classUnderTest); // regenerate movelist following move
		expectedMove = Move.valueOf( Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.a4, Piece.BLACK_PAWN, Position.b3, Piece.WHITE_PAWN, Piece.NONE );
		contains( expectedMove );
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantFromAFile_1() throws InvalidPieceException {
		// Black is on a4, white moves b4, then black ml contains capture en passant, axb
		createSutAndRegisterPe("8/8/8/8/pP6/8/8/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.b4, Piece.WHITE_PAWN, Position.b5, Piece.NONE ));
		
		expectedMove = Move.valueOf( Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.a4, Piece.BLACK_PAWN, Position.b3, Piece.WHITE_PAWN, Piece.NONE );
		doesntContain( expectedMove );
	}	
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantFromHFile() throws InvalidPieceException {
		// Black is on h4, white moves g4, then black ml contains capture en passant, hxg
		createSutAndRegisterPe("8/8/8/8/7p/8/6P1/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.g2, Piece.WHITE_PAWN, Position.g4, Piece.NONE ));
		ml = new MoveList(classUnderTest); // regenerate movelist following move
		expectedMove = Move.valueOf( Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.h4, Piece.BLACK_PAWN, Position.g3, Piece.WHITE_PAWN, Piece.NONE );
		contains( expectedMove );
	}		
	
	@Test
	public void test_BlackPawn_MoveGen_MoveOneSquare() throws InvalidPieceException {
		// After initial move, ensure that a pawn can't move 2 any longer
		createSutAndRegisterPe("8/4p3/8/8/8/8/4P3/8 b - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.e7, Piece.BLACK_PAWN, Position.e6, Piece.NONE ));
		classUnderTest.performMove( Move.valueOf( Position.e2, Piece.WHITE_PAWN, Position.e4, Piece.NONE ));
		ml = new MoveList(classUnderTest); // regenerate movelist following move
		
		expectedMove = Move.valueOf( Position.e6, Piece.BLACK_PAWN, Position.e5, Piece.NONE );
		assertTrue( ml.getList().size() == 1 );
		contains( expectedMove );		
	}	

	@Test
	public void test_BlackPawn_MoveGen_CaptureLeft() {
		createSutAndRegisterPe("8/4p3/5P2/8/8/8/8/8 b - - 0 1 ");
		
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
	public void test_BlackPawn_MoveGen_PromoteQueen() throws InvalidPieceException {
		createSutAndRegisterPe("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		theMl = new MoveList(classUnderTest);
		expectedMove = Move.valueOf( Move.TYPE_PROMOTION_MASK, Position.e2, Piece.BLACK_PAWN, Position.e1, Piece.NONE, Piece.QUEEN );
		assertTrue( theMl.contains( expectedMove ));
	}	

	@Test
	public void test_BlackPawn_MoveGen_PromoteKnight() throws InvalidPieceException {
		createSutAndRegisterPe("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		theMl = new MoveList(classUnderTest);
		expectedMove = Move.valueOf( Move.TYPE_PROMOTION_MASK, Position.e2, Piece.BLACK_PAWN, Position.e1, Piece.NONE, Piece.KNIGHT );
		assertTrue( theMl.contains( expectedMove ));		
	}

	@Test
	public void test_BlackPawn_MoveGen_PromoteBishop() throws InvalidPieceException {
		createSutAndRegisterPe("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		theMl = new MoveList(classUnderTest);
		expectedMove = Move.valueOf( Move.TYPE_PROMOTION_MASK, Position.e2, Piece.BLACK_PAWN, Position.e1, Piece.NONE, Piece.BISHOP );
		assertTrue( theMl.contains( expectedMove ));		
	}

	@Test
	public void test_BlackPawn_MoveGen_PromoteRook() throws InvalidPieceException {
		createSutAndRegisterPe("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		theMl = new MoveList(classUnderTest);
		expectedMove = Move.valueOf( Move.TYPE_PROMOTION_MASK, Position.e2, Piece.BLACK_PAWN, Position.e1, Piece.NONE, Piece.ROOK );
		assertTrue( theMl.contains( expectedMove ));
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
		
		assertEquals( 0, ml.getList().size() );
	}

	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantLeft() throws InvalidPieceException {
		createSutAndRegisterPe("8/3p4/8/4P3/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.d7, Piece.BLACK_PAWN, Position.d5, Piece.NONE ));
		ml = new MoveList(classUnderTest); // regenerate movelist following move
		expectedMove = Move.valueOf( Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.e5, Piece.WHITE_PAWN, Position.d6, Piece.BLACK_PAWN, Piece.NONE );
		contains( expectedMove );
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantLeftFalse() throws InvalidPieceException {
		createSutAndRegisterPe("8/5r2/8/4P3/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.f7, Piece.BLACK_ROOK, Position.f5, Piece.NONE ));
		ml = new MoveList(classUnderTest); // regenerate movelist following move
		expectedMove = Move.valueOf( Position.e5, Piece.WHITE_PAWN, Position.f6, Piece.BLACK_PAWN );
		doesntContain( expectedMove );
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantRight() throws InvalidPieceException {
		createSutAndRegisterPe("8/5p2/8/4P3/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.f7, Piece.BLACK_PAWN, Position.f5, Piece.NONE ));
		ml = new MoveList(classUnderTest); // regenerate movelist following move
		expectedMove = Move.valueOf(Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.e5, Piece.WHITE_PAWN, Position.f6, Piece.BLACK_PAWN, Piece.NONE );
		contains( expectedMove );
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantRightFalse() throws InvalidPieceException {
		createSutAndRegisterPe("8/5r2/8/4P3/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.f7, Piece.BLACK_ROOK, Position.f5, Piece.NONE ));
		ml = new MoveList(classUnderTest); // regenerate movelist following move
		expectedMove = Move.valueOf( Position.e5, Piece.WHITE_PAWN, Position.f6, Piece.BLACK_PAWN );
		doesntContain( expectedMove );
	}	
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantFromAFile() throws InvalidPieceException {
		// white is on a5, black moves b5, then black ml contains capture en passant, axb
		createSutAndRegisterPe("8/1p6/8/P7/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.b7, Piece.BLACK_PAWN, Position.b5, Piece.NONE ));
		ml = new MoveList(classUnderTest); // regenerate movelist following move
		expectedMove = Move.valueOf( Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.a5, Piece.WHITE_PAWN, Position.b6, Piece.BLACK_PAWN, Piece.NONE );
		contains( expectedMove );
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantFromHFile() throws InvalidPieceException {
		// Black is on h4, white moves g4, then black ml contains capture en passant, hxg
		createSutAndRegisterPe("8/6p1/8/7P/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.g7, Piece.BLACK_PAWN, Position.g5, Piece.NONE ));
		ml = new MoveList(classUnderTest); // regenerate movelist following move
		expectedMove = Move.valueOf( Move.MISC_EN_PASSANT_CAPTURE_MASK, 0, Position.h5, Piece.WHITE_PAWN, Position.g6, Piece.BLACK_PAWN, Piece.NONE );
		contains( expectedMove );
	}		
	
	@Test
	public void test_WhitePawn_MoveGen_MoveOneSquare() throws InvalidPieceException {
		// After initial move, ensure that a pawn can't move 2 any longer
		createSutAndRegisterPe("8/5p2/8/8/8/8/4P3/8 w - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.e2, Piece.WHITE_PAWN, Position.e4, Piece.NONE ));
		classUnderTest.performMove( Move.valueOf( Position.f7, Piece.BLACK_PAWN, Position.f6, Piece.NONE ));
		ml = new MoveList(classUnderTest); // regenerate movelist following move
		
		expectedMove = Move.valueOf( Position.e4, Piece.WHITE_PAWN, Position.e5, Piece.NONE );
		assertTrue( ml.getList().size() == 1 );
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
	public void test_WhitePawn_MoveGen_PromoteQueen() throws InvalidPieceException {
		createSutAndRegisterPe("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		theMl = new MoveList(classUnderTest);
		expectedMove = Move.valueOf( Move.TYPE_PROMOTION_MASK, Position.e7, Piece.PAWN, Position.e8, Piece.NONE, Piece.QUEEN );
		assertTrue( theMl.contains( expectedMove ));
	}	

	@Test
	public void test_WhitePawn_MoveGen_PromoteKnight() throws InvalidPieceException {
		createSutAndRegisterPe("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		theMl = new MoveList(classUnderTest);
		expectedMove = Move.valueOf( Move.TYPE_PROMOTION_MASK, Position.e7, Piece.PAWN, Position.e8, Piece.NONE, Piece.KNIGHT );
		assertTrue( theMl.contains( expectedMove ));		
	}

	@Test
	public void test_WhitePawn_MoveGen_PromoteBishop() throws InvalidPieceException {
		createSutAndRegisterPe("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		theMl = new MoveList(classUnderTest);
		expectedMove = Move.valueOf( Move.TYPE_PROMOTION_MASK, Position.e7, Piece.PAWN, Position.e8, Piece.NONE, Piece.BISHOP );
		assertTrue( theMl.contains( expectedMove ));			
	}

	@Test
	public void test_WhitePawn_MoveGen_PromoteRook() throws InvalidPieceException {
		createSutAndRegisterPe("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		theMl = new MoveList(classUnderTest);
		expectedMove = Move.valueOf( Move.TYPE_PROMOTION_MASK, Position.e7, Piece.PAWN, Position.e8, Piece.NONE, Piece.ROOK );
		assertTrue( theMl.contains( expectedMove ));	
	}
	
	/* Bishop Moves */
	@Test
	public void test_Bishop_MoveGen_CornerTopLeft() {
		createSutAndRegisterPe("b7/8/8/8/8/8/8/8 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.a8, Piece.BLACK_BISHOP, Position.b7, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.a8, Piece.BLACK_BISHOP, Position.h1, Piece.NONE ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml.getList());
	}

	@Test
	public void test_Bishop_MoveGen_CornerTopRight() {
		createSutAndRegisterPe("7b/8/8/8/8/8/8/8 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.h8, Piece.BLACK_BISHOP, Position.g7, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.h8, Piece.BLACK_BISHOP, Position.a1, Piece.NONE ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml.getList());
	}
	
	@Test
	public void test_Bishop_MoveGen_CornerBottomRight() {
		createSutAndRegisterPe("8/8/8/8/8/8/8/7b b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.h1, Piece.BLACK_BISHOP, Position.g2, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.h1, Piece.BLACK_BISHOP, Position.a8, Piece.NONE ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml.getList());
	}
	
	@Test
	public void test_Bishop_MoveGen_CornerBottomLeft() {
		createSutAndRegisterPe("8/8/8/8/8/8/8/b7 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.a1, Piece.BLACK_BISHOP, Position.b2, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.a1, Piece.BLACK_BISHOP, Position.h8, Piece.NONE ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml.getList());
	}
	
	@Test
	public void test_Bishop_MoveGen_CornerBottomLeft_ObstructedOwnPieces() {
		createSutAndRegisterPe("8/8/8/8/8/8/1p6/b7 b - - 0 1");
		
		expectedNumMoves = 4;
		checkExpectedMoves(ml.getList());
	}

	@Test
	public void test_Bishop_MoveGen_LeftEdge_PartiallyObstructedOwnPiece() {
		createSutAndRegisterPe("8/8/8/8/b7/1p6/8/8 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.a4, Piece.BLACK_BISHOP, Position.b5, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.a4, Piece.BLACK_BISHOP, Position.e8, Piece.NONE ));
		expectedNumMoves = 5;
		checkExpectedMoves(ml.getList());		
	}
	
	@Test
	public void test_Bishop_MoveGen_LeftEdge_PartiallyObstructedCapturablePiece() {
		createSutAndRegisterPe("8/8/8/8/b7/1P6/8/8 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.a4, Piece.BLACK_BISHOP, Position.b5, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.a4, Piece.BLACK_BISHOP, Position.e8, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.a4, Piece.BLACK_BISHOP, Position.b3, Piece.WHITE_PAWN ));
		expectedNumMoves = 5;
		checkExpectedMoves(ml.getList());	
	}
	
	@Test
	public void test_Bishop_MoveGen_Middle_ObstructedCapturablePieces() {
		createSutAndRegisterPe("8/8/8/3P1P2/4b3/3P1P2/8/8 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.d3, Piece.WHITE_PAWN ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.d5, Piece.WHITE_PAWN ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.f3, Piece.WHITE_PAWN ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.f5, Piece.WHITE_PAWN ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml.getList());
	}
	
	@Test
	public void test_Bishop_MoveGen_Middle_ObstructedMixturePieces() {
		createSutAndRegisterPe("8/8/8/3P1p2/4b3/3P1p2/8/8 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.d3, Piece.WHITE_PAWN ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.d5, Piece.WHITE_PAWN ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml.getList());
	}
	
	@Test
	public void test_Bishop_MoveGen_Middle_Unobstructed() {
		createSutAndRegisterPe("8/8/8/4P3/3PbP2/4P3/8/8 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.d3, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.d5, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.f3, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_BISHOP, Position.f5, Piece.NONE ));
		expectedNumMoves = 13;
		checkExpectedMoves(ml.getList());
	}
	
	@Test
	public void test_Bishop_MoveGen_CapturesOnlySinglePiece() {
		createSutAndRegisterPe("8/8/8/8/8/2P5/1P6/b7 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.a1, Piece.BLACK_BISHOP, Position.b2, Piece.WHITE_PAWN ));
		expectedNumMoves = 1;
		checkExpectedMoves(ml.getList());
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
		checkExpectedMoves(ml.getList());
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
		checkExpectedMoves(ml.getList());
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
		checkExpectedMoves(ml.getList());
	}
	
	/* Rook Moves */
	@Test
	public void test_Rook_MoveGen_CornerTopLeft() {
		createSutAndRegisterPe("R7/8/8/8/8/8/8/8 w - - 0 1 ");
		
		expectedMoves.add( Move.valueOf( Position.a8, Piece.WHITE_ROOK, Position.a7, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.a8, Piece.WHITE_ROOK, Position.b8, Piece.NONE ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml.getList());
	}

	@Test
	public void test_Rook_MoveGen_CornerTopRight() {
		createSutAndRegisterPe("7R/8/8/8/8/8/8/8 w - - 0 1 ");
		
		expectedMoves.add( Move.valueOf( Position.h8, Piece.WHITE_ROOK, Position.h7, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.h8, Piece.WHITE_ROOK, Position.g8, Piece.NONE ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml.getList());
	}
	
	@Test
	public void test_Rook_MoveGen_CornerBottomRight() {
		createSutAndRegisterPe("8/8/8/8/8/8/8/7R w - - 0 1 ");
		
		expectedMoves.add( Move.valueOf( Position.h1, Piece.WHITE_ROOK, Position.h2, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.h1, Piece.WHITE_ROOK, Position.g1, Piece.NONE ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml.getList());
	}
	
	@Test
	public void test_Rook_MoveGen_CornerBottomLeft() {
		createSutAndRegisterPe("8/8/8/8/8/8/8/R7 w - - 0 1 ");
		
		expectedMoves.add( Move.valueOf( Position.a1, Piece.WHITE_ROOK, Position.a2, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.a1, Piece.WHITE_ROOK, Position.b1, Piece.NONE ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml.getList());
	}
	
	@Test
	public void test_Rook_MoveGen_CornerBottomLeft_ObstructedOwnPieces() {
		createSutAndRegisterPe("8/8/8/8/8/8/P7/RK6 w - - 0 1");
		
		expectedNumMoves = 5;
		checkExpectedMoves(ml.getList());
	}

	@Test
	public void test_Rook_MoveGen_CornerBottomLeft_PartiallyObstructedOwnPiece() {
		createSutAndRegisterPe("8/8/8/8/8/8/P7/R7 w - - 0 1");
		
		expectedNumMoves = 9;
		checkExpectedMoves(ml.getList());
	}
	
	@Test
	public void test_Rook_MoveGen_CornerBottomLeft_PartiallyObstructedCapturablePiece() {
		createSutAndRegisterPe("8/8/8/8/8/8/p7/R7 w - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.a1, Piece.WHITE_ROOK, Position.a2, Piece.BLACK_PAWN ));
		expectedMoves.add( Move.valueOf( Position.a1, Piece.WHITE_ROOK, Position.b1, Piece.NONE ));		
		expectedNumMoves = 8;
		checkExpectedMoves(ml.getList());
	}
	
	@Test
	public void test_Rook_MoveGen_Middle_ObstructedCapturablePieces() {
		createSutAndRegisterPe("8/8/8/4P3/3PrP2/4P3/8/8 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.f4, Piece.WHITE_PAWN ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.d4, Piece.WHITE_PAWN ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.e5, Piece.WHITE_PAWN ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.e3, Piece.WHITE_PAWN ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml.getList());
	}
	
	@Test
	public void test_Rook_MoveGen_Middle_ObstructedMixturePieces() {
		createSutAndRegisterPe("8/8/8/4P3/3prp2/4P3/8/8 b - - 0 1");
		
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.e5, Piece.WHITE_PAWN ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.e3, Piece.WHITE_PAWN ));
		expectedNumMoves = 6;
		checkExpectedMoves(ml.getList());
	}
	
	@Test
	public void test_Rook_MoveGen_Middle_Unobstructed() {
		createSutAndRegisterPe("8/8/8/3P1P2/4r3/3P1P2/8/8 b - - 0 1 ");
		
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.f4, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.d4, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.e5, Piece.NONE ));
		expectedMoves.add( Move.valueOf( Position.e4, Piece.BLACK_ROOK, Position.e3, Piece.NONE ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml.getList());
	}

	@Test
	public void test_Rook_MoveGen_CapturesOnlySinglePiece() {
		createSutAndRegisterPe("8/8/8/8/8/P7/P7/r7 b - - 0 1 ");
		
		assertFalse(ml.getList().size() == 0);
		contains( Move.valueOf( Position.a1, Piece.BLACK_ROOK, Position.a2, Piece.WHITE_PAWN ));
		doesntContain( Move.valueOf( Position.a1, Piece.BLACK_ROOK, Position.a3, Piece.WHITE_PAWN ));
	}
}