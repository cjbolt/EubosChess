package eubos.position;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import eubos.board.InvalidPieceException;
import eubos.board.Piece.Colour;
import eubos.board.Piece.PieceType;
import eubos.position.PositionManager;
import eubos.position.Position;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;
import com.fluxchess.jcpi.models.IntChessman;

public class PositionManagerTest {
	
	protected PositionManager classUnderTest;
	
	protected int expectedMove;
	protected List<Integer> expectedMoves;
	protected int expectedNumMoves = 0;
	protected List<Integer> ml;
	
	@Before
	public void setUp() {
		expectedMoves = new LinkedList<Integer>();
	}
	
	protected void checkExpectedMoves(List<Integer> ml) {
		assertFalse(ml.isEmpty());
		assertEquals(expectedNumMoves, ml.size());
		for ( int mov : expectedMoves) {
			assertTrue( ml.contains( mov ));
		}
	}
	
	protected void checkNoMovesGenerated(List<GenericMove> ml) {
		assertTrue(ml.isEmpty());
	}
	
	@Test
	public void test_NoLastMoveToUndo() throws InvalidPieceException {
		classUnderTest = new PositionManager();
		classUnderTest.unperformMove();
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
		classUnderTest = new PositionManager("8/8/8/8/8/8/4P3/8 w - - - -");
		classUnderTest.performMove(Move.toMove(new GenericMove("e2e4")));
		classUnderTest.unperformMove();
		PieceType expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( Position.e2 );
		assertTrue( expectPawn==PieceType.WhitePawn );		
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
		classUnderTest = new PositionManager("8/8/8/8/8/8/4p3/8 b - - - -");
		classUnderTest.performMove( Move.toMove(new GenericMove("e2e1Q")));
		classUnderTest.unperformMove();
		PieceType expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( Position.e2 );
		assertTrue( expectPawn==PieceType.BlackPawn );
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
		classUnderTest = new PositionManager("8/8/8/8/8/3p4/4P3/8 w - - - -");
		classUnderTest.performMove( Move.toMove(new GenericMove("d3e2")));
		classUnderTest.unperformMove();
		PieceType expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( Position.d3 );
		assertTrue( expectPawn==PieceType.BlackPawn );
		expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( Position.e2 );
		assertTrue( expectPawn==PieceType.WhitePawn );
	}
	
	@Test
	public void test_enPassantCaptureAtC3() throws InvalidPieceException, IllegalNotationException {
		classUnderTest = new PositionManager( "r3k2r/1bqpbppp/p1n1p3/3nP3/PpP1N3/3B1N2/1P2QPPP/R4RK1 b kq c3 0 1");
		classUnderTest.performMove( Move.toMove(new GenericMove("b4c3")));
		PieceType expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( Position.c3 );
		assertTrue( expectPawn==PieceType.BlackPawn );
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
		classUnderTest = new PositionManager("8/8/8/8/8/8/8/4K2R w K - - -");
		GenericMove expectedMove = new GenericMove("e1g1");
		classUnderTest.performMove(Move.toMove(expectedMove));
		PieceType whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(Position.h1);
		assertTrue(whiteRook == PieceType.NONE);
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(Position.f1);
		assertTrue( whiteRook==PieceType.WhiteRook );
		assertTrue(classUnderTest.castling.everCastled(Colour.white));
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
		classUnderTest = new PositionManager("8/8/8/8/8/8/8/4K2R w K - - -");
		GenericMove expectedMove = new GenericMove("e1g1");
		classUnderTest.performMove(Move.toMove(expectedMove));
		PieceType whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(Position.h1);
		assertTrue(whiteRook == PieceType.NONE);
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(Position.f1);
		assertTrue( whiteRook==PieceType.WhiteRook );
		classUnderTest.unperformMove();
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(Position.f1);
		assertTrue(whiteRook == PieceType.NONE);
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(Position.h1);
		assertTrue( whiteRook==PieceType.WhiteRook );
		PieceType whiteKing = classUnderTest.getTheBoard().getPieceAtSquare(Position.e1);
		assertTrue( whiteKing==PieceType.WhiteKing );
		assertTrue(!classUnderTest.castling.everCastled(Colour.white));
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
		classUnderTest = new PositionManager("7r/8/8/8/8/8/8/4K2R b K - - -");
		classUnderTest.performMove(Move.toMove(new GenericMove("h8h1")));
		assertTrue(classUnderTest.castling.getFenFlags().equals("-"));
	}
	
	@Test
	public void test_FenString() {
		String fenString = "7r/8/8/8/8/8/8/4K2R b K - - 0";
		classUnderTest = new PositionManager(fenString);
		assertEquals(fenString, classUnderTest.getFen());
	}
	
	@Test
	public void test_FenString1() {
		String fenString = "7r/8/8/8/8/4P3/8/4K2R b K e3 - 0";
		classUnderTest = new PositionManager(fenString);
		assertEquals(fenString, classUnderTest.getFen());
	}
	
	/* KING MOVES */
	
	@Test
	public void test_King_MoveGen_CornerTopLeft() throws IllegalNotationException {
		PositionManager classUnderTest = new PositionManager("k7/8/8/8/8/8/8/8 b - - 0 1");
		List<Integer> ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.toMove(new GenericMove( "a8a7" )));
		expectedMoves.add( Move.toMove(new GenericMove( "a8b8" )));
		expectedMoves.add( Move.toMove(new GenericMove( "a8b7" )));
		expectedNumMoves = 3;
		checkExpectedMoves(ml);
	}
	
	/* PAWN MOVES */
	@Test
	public void test_BlackPawn_MoveGen_InitialMoveOneSquare() {
		classUnderTest = new PositionManager("8/4p3/8/8/8/8/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e7, Position.e6 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_BlackPawn_MoveGen_InitialMoveTwoSquares() {
		classUnderTest = new PositionManager("8/4p3/8/8/8/8/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e7, Position.e5 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_BlackPawn_MoveGen_InitialBlocked() {
		classUnderTest = new PositionManager("8/4p3/4P3/8/8/8/8/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		assertTrue( ml.isEmpty() );
	}

	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantLeft() throws InvalidPieceException {
		// Black is on e4, white moves f4, then black ml contains capture en passant, exf
		classUnderTest = new PositionManager("8/8/8/8/4p3/8/5P2/8 w - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.f2, Position.f4 ));
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e4, Position.f3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantLeftFalse() throws InvalidPieceException {
		// Black is on e4, white moves a knight to f4, check black ml doesn't contain a capture en passant, exf
		classUnderTest = new PositionManager("8/8/8/8/4p3/8/5N2/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.f2, Position.f4 ));
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e4, Position.f3 );
		assertFalse( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantRight() throws InvalidPieceException {
		// Black is on e4, white moves d4, then black ml contains capture en passant, exd
		classUnderTest = new PositionManager("8/8/8/8/4p3/8/3P4/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.d2, Position.d4 ));
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e4, Position.d3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantRightFalse() throws InvalidPieceException {
		// Black is on e4, white moves a knight to d4, check black ml doesn't contain a capture en passant, exd
		classUnderTest = new PositionManager("8/8/8/8/4p3/8/3N4/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.d2, Position.d4 ));
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e4, Position.d3 );
		assertFalse( ml.contains( expectedMove ));
	}	
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantFromAFile() throws InvalidPieceException {
		// Black is on a4, white moves b4, then black ml contains capture en passant, axb
		classUnderTest = new PositionManager("8/8/8/8/p7/8/1P6/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.b2, Position.b4 ));
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.a4, Position.b3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantFromAFile_1() throws InvalidPieceException {
		// Black is on a4, white moves b4, then black ml contains capture en passant, axb
		classUnderTest = new PositionManager("8/8/8/8/pP6/8/8/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.b4, Position.b5 ));
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.a4, Position.b3 );
		assertFalse( ml.contains( expectedMove ));
	}	
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantFromHFile() throws InvalidPieceException {
		// Black is on h4, white moves g4, then black ml contains capture en passant, hxg
		classUnderTest = new PositionManager("8/8/8/8/7p/8/6P1/8 w - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.g2, Position.g4 ));
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.h4, Position.g3 );
		assertTrue( ml.contains( expectedMove ));
	}		
	
	@Test
	public void test_BlackPawn_MoveGen_MoveOneSquare() throws InvalidPieceException {
		// After initial move, ensure that a pawn can't move 2 any longer
		classUnderTest = new PositionManager("8/4p3/8/8/8/8/4P3/8 b - - 0 1 ");
		classUnderTest.performMove( Move.valueOf( Position.e7, Position.e6 ));
		classUnderTest.performMove( Move.valueOf( Position.e2, Position.e4 ));
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e6, Position.e5 );
		assertTrue( ml.size() == 1 );
		assertTrue( ml.contains( expectedMove ));		
	}	

	@Test
	public void test_BlackPawn_MoveGen_CaptureLeft() {
		classUnderTest = new PositionManager("8/4p3/5P2/8/8/8/8/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e7, Position.f6 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_BlackPawn_MoveGen_CaptureRight() {
		classUnderTest = new PositionManager("8/4p3/3P4/8/8/8/8/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e7, Position.d6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureFork() {
		classUnderTest = new PositionManager("8/4p3/3P1P2/8/8/8/8/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		int captureLeft = Move.valueOf( Position.e7, Position.d6 );
		int captureRight = Move.valueOf( Position.e7, Position.f6 );
		assertTrue( ml.contains( captureLeft ));
		assertTrue( ml.contains( captureRight ));
	}	
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureFromAFile() {
		// Can only capture left
		classUnderTest = new PositionManager("8/p7/1P6/8/8/8/8/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();;
		expectedMove = Move.valueOf( Position.a7, Position.b6 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_BlackPawn_MoveGen_CaptureFromHFile() {
		// Can only capture right
		classUnderTest = new PositionManager("8/7p/6P1/8/8/8/8/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.h7, Position.g6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_BlackPawn_MoveGen_PromoteQueen() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e2, Position.e1, IntChessman.QUEEN );
		assertTrue( ml.contains( expectedMove ));
	}	

	@Test
	public void test_BlackPawn_MoveGen_PromoteKnight() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e2, Position.e1, IntChessman.KNIGHT );
		assertTrue( ml.contains( expectedMove ));		
	}

	@Test
	public void test_BlackPawn_MoveGen_PromoteBishop() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e2, Position.e1, IntChessman.BISHOP );
		assertTrue( ml.contains( expectedMove ));			
	}

	@Test
	public void test_BlackPawn_MoveGen_PromoteRook() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e2, Position.e1, IntChessman.ROOK );
		assertTrue( ml.contains( expectedMove ));	
	}
	
	@Test
	public void test_WhitePawn_MoveGen_InitialMoveOneSquare() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/4P3/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e2, Position.e4 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_WhitePawn_MoveGen_InitialMoveTwoSquares() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/4P3/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e2, Position.e3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_WhitePawn_MoveGen_InitialBlocked() {
		classUnderTest = new PositionManager("8/8/8/8/8/4p3/4P3/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		assertTrue( ml.isEmpty() );
	}

	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantLeft() throws InvalidPieceException {
		classUnderTest = new PositionManager("8/3p4/8/4P3/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.d7, Position.d5 ));
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e5, Position.d6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantLeftFalse() throws InvalidPieceException {
		classUnderTest = new PositionManager("8/5r2/8/4P3/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.f7, Position.f5 ));
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e5, Position.f6 );
		assertFalse( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantRight() throws InvalidPieceException {
		classUnderTest = new PositionManager("8/5p2/8/4P3/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.f7, Position.f5 ));
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e5, Position.f6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantRightFalse() throws InvalidPieceException {
		classUnderTest = new PositionManager("8/5r2/8/4P3/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.f7, Position.f5 ));
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e5, Position.f6 );
		assertFalse( ml.contains( expectedMove ));
	}	
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantFromAFile() throws InvalidPieceException {
		// white is on a5, black moves b5, then black ml contains capture en passant, axb
		classUnderTest = new PositionManager("8/1p6/8/P7/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.b7, Position.b5 ));
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.a5, Position.b6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantFromHFile() throws InvalidPieceException {
		// Black is on h4, white moves g4, then black ml contains capture en passant, hxg
		classUnderTest = new PositionManager("8/6p1/8/7P/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.g7, Position.g5 ));
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.h5, Position.g6 );
		assertTrue( ml.contains( expectedMove ));
	}		
	
	@Test
	public void test_WhitePawn_MoveGen_MoveOneSquare() throws InvalidPieceException {
		// After initial move, ensure that a pawn can't move 2 any longer
		classUnderTest = new PositionManager("8/5p2/8/8/8/8/4P3/8 w - - 0 1");
		classUnderTest.performMove( Move.valueOf( Position.e2, Position.e4 ));
		classUnderTest.performMove( Move.valueOf( Position.f7, Position.f6 ));
		
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e4, Position.e5 );
		assertTrue( ml.size() == 1 );
		assertTrue( ml.contains( expectedMove ));		
	}	

	@Test
	public void test_WhitePawn_MoveGen_CaptureLeft() {
		classUnderTest = new PositionManager("8/8/8/8/8/5p2/4P3/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e2, Position.f3 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_WhitePawn_MoveGen_CaptureRight() {
		classUnderTest = new PositionManager("8/8/8/8/8/3p4/4P3/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e2, Position.d3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureFork() {
		classUnderTest = new PositionManager("8/8/8/8/8/3p1p2/4P3/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		int captureLeft = Move.valueOf( Position.e2, Position.d3 );
		int captureRight = Move.valueOf( Position.e2, Position.f3 );
		assertTrue( ml.contains( captureLeft ));
		assertTrue( ml.contains( captureRight ));
	}	
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureFromAFile() {
		// Can only capture left
		classUnderTest = new PositionManager("8/8/8/8/8/1p6/P7/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.a2, Position.b3 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_WhitePawn_MoveGen_CaptureFromHFile() {
		// Can only capture right
		classUnderTest = new PositionManager("8/8/8/8/8/6p1/7P/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.h2, Position.g3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_WhitePawn_MoveGen_PromoteQueen() {
		classUnderTest = new PositionManager("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e7, Position.e8, IntChessman.QUEEN );
		assertTrue( ml.contains( expectedMove ));
	}	

	@Test
	public void test_WhitePawn_MoveGen_PromoteKnight() {
		classUnderTest = new PositionManager("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e7, Position.e8, IntChessman.KNIGHT );
		assertTrue( ml.contains( expectedMove ));		
	}

	@Test
	public void test_WhitePawn_MoveGen_PromoteBishop() {
		classUnderTest = new PositionManager("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e7, Position.e8, IntChessman.BISHOP );
		assertTrue( ml.contains( expectedMove ));			
	}

	@Test
	public void test_WhitePawn_MoveGen_PromoteRook() {
		classUnderTest = new PositionManager("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = Move.valueOf( Position.e7, Position.e8, IntChessman.ROOK );
		assertTrue( ml.contains( expectedMove ));	
	}
	
	/* Bishop Moves */
	@Test
	public void test_Bishop_MoveGen_CornerTopLeft() {
		classUnderTest = new PositionManager("b7/8/8/8/8/8/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.a8, Position.b7 ));
		expectedMoves.add( Move.valueOf( Position.a8, Position.h1 ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml);
	}

	@Test
	public void test_Bishop_MoveGen_CornerTopRight() {
		classUnderTest = new PositionManager("7b/8/8/8/8/8/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.h8, Position.g7 ));
		expectedMoves.add( Move.valueOf( Position.h8, Position.a1 ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Bishop_MoveGen_CornerBottomRight() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/8/7b b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.h1, Position.g2 ));
		expectedMoves.add( Move.valueOf( Position.h1, Position.a8 ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Bishop_MoveGen_CornerBottomLeft() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/8/b7 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.a1, Position.b2 ));
		expectedMoves.add( Move.valueOf( Position.a1, Position.h8 ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Bishop_MoveGen_CornerBottomLeft_ObstructedOwnPieces() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/1p6/b7 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedNumMoves = 4;
		checkExpectedMoves(ml);
	}

	@Test
	public void test_Bishop_MoveGen_LeftEdge_PartiallyObstructedOwnPiece() {
		classUnderTest = new PositionManager("8/8/8/8/b7/1p6/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.a4, Position.b5 ));
		expectedMoves.add( Move.valueOf( Position.a4, Position.e8 ));
		expectedNumMoves = 5;
		checkExpectedMoves(ml);		
	}
	
	@Test
	public void test_Bishop_MoveGen_LeftEdge_PartiallyObstructedCapturablePiece() {
		classUnderTest = new PositionManager("8/8/8/8/b7/1P6/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.a4, Position.b5 ));
		expectedMoves.add( Move.valueOf( Position.a4, Position.e8 ));
		expectedMoves.add( Move.valueOf( Position.a4, Position.b3 ));
		expectedNumMoves = 5;
		checkExpectedMoves(ml);	
	}
	
	@Test
	public void test_Bishop_MoveGen_Middle_ObstructedCapturablePieces() {
		classUnderTest = new PositionManager("8/8/8/3P1P2/4b3/3P1P2/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.e4, Position.d3 ));
		expectedMoves.add( Move.valueOf( Position.e4, Position.d5 ));
		expectedMoves.add( Move.valueOf( Position.e4, Position.f3 ));
		expectedMoves.add( Move.valueOf( Position.e4, Position.f5 ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Bishop_MoveGen_Middle_ObstructedMixturePieces() {
		classUnderTest = new PositionManager("8/8/8/3P1p2/4b3/3P1p2/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.e4, Position.d3 ));
		expectedMoves.add( Move.valueOf( Position.e4, Position.d5 ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Bishop_MoveGen_Middle_Unobstructed() {
		classUnderTest = new PositionManager("8/8/8/4P3/3PbP2/4P3/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.e4, Position.d3 ));
		expectedMoves.add( Move.valueOf( Position.e4, Position.d5 ));
		expectedMoves.add( Move.valueOf( Position.e4, Position.f3 ));
		expectedMoves.add( Move.valueOf( Position.e4, Position.f5 ));
		expectedNumMoves = 13;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Bishop_MoveGen_CapturesOnlySinglePiece() {
		classUnderTest = new PositionManager("8/8/8/8/8/2P5/1P6/b7 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.a1, Position.b2 ));
		expectedNumMoves = 1;
		checkExpectedMoves(ml);
		assertFalse(ml.contains( Move.valueOf( Position.a1, Position.c3 )));
	}
	
	/* Knight Moves */
	int startTestOnSq;
	@Test
	public void test_SquareA8() {
		startTestOnSq = Position.a8;
		classUnderTest = new PositionManager("N7/8/8/8/8/8/8/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( startTestOnSq, Position.c7 ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Position.b6 ));
		expectedNumMoves = 2;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_SquareB7() {
		startTestOnSq = Position.b7;
		classUnderTest = new PositionManager("8/1N6/8/8/8/8/8/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( startTestOnSq, Position.d8 ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Position.d6 ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Position.a5 ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Position.c5 ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_SquareC6() {
		startTestOnSq = Position.c6;
		classUnderTest = new PositionManager("8/8/2N5/8/8/8/8/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( startTestOnSq, Position.b8 ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Position.d8 ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Position.e7 ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Position.e5 ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Position.b4 ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Position.d4 ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Position.a7 ));
		expectedMoves.add( Move.valueOf( startTestOnSq, Position.a5 ));
		expectedNumMoves = 8;
		checkExpectedMoves(ml);
	}
	
	/* Rook Moves */
	@Test
	public void test_Rook_MoveGen_CornerTopLeft() {
		classUnderTest = new PositionManager("R7/8/8/8/8/8/8/8 w - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.a8, Position.a7 ));
		expectedMoves.add( Move.valueOf( Position.a8, Position.b8 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}

	@Test
	public void test_Rook_MoveGen_CornerTopRight() {
		classUnderTest = new PositionManager("7R/8/8/8/8/8/8/8 w - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.h8, Position.h7 ));
		expectedMoves.add( Move.valueOf( Position.h8, Position.g8 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Rook_MoveGen_CornerBottomRight() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/8/7R w - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.h1, Position.h2 ));
		expectedMoves.add( Move.valueOf( Position.h1, Position.g1 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Rook_MoveGen_CornerBottomLeft() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/8/R7 w - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.a1, Position.a2 ));
		expectedMoves.add( Move.valueOf( Position.a1, Position.b1 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Rook_MoveGen_CornerBottomLeft_ObstructedOwnPieces() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/P7/RK6 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedNumMoves = 5;
		checkExpectedMoves(ml);
	}

	@Test
	public void test_Rook_MoveGen_CornerBottomLeft_PartiallyObstructedOwnPiece() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/P7/R7 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedNumMoves = 9;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Rook_MoveGen_CornerBottomLeft_PartiallyObstructedCapturablePiece() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/p7/R7 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.a1, Position.a2 ));
		expectedMoves.add( Move.valueOf( Position.a1, Position.b1 ));		
		expectedNumMoves = 8;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Rook_MoveGen_Middle_ObstructedCapturablePieces() {
		classUnderTest = new PositionManager("8/8/8/4P3/3PrP2/4P3/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.e4, Position.f4 ));
		expectedMoves.add( Move.valueOf( Position.e4, Position.d4 ));
		expectedMoves.add( Move.valueOf( Position.e4, Position.e5 ));
		expectedMoves.add( Move.valueOf( Position.e4, Position.e3 ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Rook_MoveGen_Middle_ObstructedMixturePieces() {
		classUnderTest = new PositionManager("8/8/8/4P3/3prp2/4P3/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.e4, Position.e5 ));
		expectedMoves.add( Move.valueOf( Position.e4, Position.e3 ));
		expectedNumMoves = 6;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Rook_MoveGen_Middle_Unobstructed() {
		classUnderTest = new PositionManager("8/8/8/3P1P2/4r3/3P1P2/8/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( Move.valueOf( Position.e4, Position.f4 ));
		expectedMoves.add( Move.valueOf( Position.e4, Position.d4 ));
		expectedMoves.add( Move.valueOf( Position.e4, Position.e5 ));
		expectedMoves.add( Move.valueOf( Position.e4, Position.e3 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Rook_MoveGen_CapturesOnlySinglePiece() {
		classUnderTest = new PositionManager("8/8/8/8/8/P7/P7/r7 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		assertFalse(ml.isEmpty());
		assertTrue( ml.contains( Move.valueOf( Position.a1, Position.a2 )));
		assertFalse(ml.contains( Move.valueOf( Position.a1, Position.a3 )));
	}
}