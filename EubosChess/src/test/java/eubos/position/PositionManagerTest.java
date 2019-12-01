package eubos.position;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import eubos.board.InvalidPieceException;
import eubos.board.pieces.Piece.Colour;
import eubos.board.pieces.Piece.PieceType;
import eubos.position.PositionManager;

import com.fluxchess.jcpi.models.GenericChessman;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

public class PositionManagerTest {
	
	protected PositionManager classUnderTest;
	
	protected GenericMove expectedMove;
	protected List<GenericMove> expectedMoves;
	protected int expectedNumMoves = 0;
	protected List<GenericMove> ml;
	
	@Before
	public void setUp() {
		expectedMoves = new LinkedList<GenericMove>();
	}
	
	protected void checkExpectedMoves(List<GenericMove> ml) {
		assertFalse(ml.isEmpty());
		assertEquals(expectedNumMoves, ml.size());
		for ( GenericMove mov : expectedMoves) {
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
		classUnderTest.performMove(new GenericMove("e2e4"));
		classUnderTest.unperformMove();
		PieceType expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.e2 );
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
		classUnderTest.performMove( new GenericMove("e2e1Q"));
		classUnderTest.unperformMove();
		PieceType expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.e2 );
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
		classUnderTest.performMove( new GenericMove("d3e2"));
		classUnderTest.unperformMove();
		PieceType expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.d3 );
		assertTrue( expectPawn==PieceType.BlackPawn );
		expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.e2 );
		assertTrue( expectPawn==PieceType.WhitePawn );
	}
	
	@Test
	public void test_enPassantCaptureAtC3() throws InvalidPieceException, IllegalNotationException {
		classUnderTest = new PositionManager( "r3k2r/1bqpbppp/p1n1p3/3nP3/PpP1N3/3B1N2/1P2QPPP/R4RK1 b kq c3 0 1");
		classUnderTest.performMove( new GenericMove("b4c3"));
		PieceType expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.c3 );
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
		classUnderTest.performMove(expectedMove);
		PieceType whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(GenericPosition.h1);
		assertTrue(whiteRook == PieceType.NONE);
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(GenericPosition.f1);
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
		classUnderTest.performMove(expectedMove);
		PieceType whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(GenericPosition.h1);
		assertTrue(whiteRook == PieceType.NONE);
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(GenericPosition.f1);
		assertTrue( whiteRook==PieceType.WhiteRook );
		classUnderTest.unperformMove();
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(GenericPosition.f1);
		assertTrue(whiteRook == PieceType.NONE);
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(GenericPosition.h1);
		assertTrue( whiteRook==PieceType.WhiteRook );
		PieceType whiteKing = classUnderTest.getTheBoard().getPieceAtSquare(GenericPosition.e1);
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
		classUnderTest.performMove(new GenericMove("h8h1"));
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
	public void test_King_MoveGen_CornerTopLeft() {
		PositionManager classUnderTest = new PositionManager("k7/8/8/8/8/8/8/8 b - - 0 1");
		List<GenericMove> ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.a7 ));
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.b8 ));
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.b7 ));
		expectedNumMoves = 3;
		checkExpectedMoves(ml);
	}
	
	/* PAWN MOVES */
	@Test
	public void test_BlackPawn_MoveGen_InitialMoveOneSquare() {
		classUnderTest = new PositionManager("8/4p3/8/8/8/8/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e6 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_BlackPawn_MoveGen_InitialMoveTwoSquares() {
		classUnderTest = new PositionManager("8/4p3/8/8/8/8/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e5 );
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
		classUnderTest.performMove( new GenericMove( GenericPosition.f2, GenericPosition.f4 ));
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e4, GenericPosition.f3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantLeftFalse() throws InvalidPieceException {
		// Black is on e4, white moves a knight to f4, check black ml doesn't contain a capture en passant, exf
		classUnderTest = new PositionManager("8/8/8/8/4p3/8/5N2/8 w - - 0 1 ");
		classUnderTest.performMove( new GenericMove( GenericPosition.f2, GenericPosition.f4 ));
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e4, GenericPosition.f3 );
		assertFalse( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantRight() throws InvalidPieceException {
		// Black is on e4, white moves d4, then black ml contains capture en passant, exd
		classUnderTest = new PositionManager("8/8/8/8/4p3/8/3P4/8 w - - 0 1 ");
		classUnderTest.performMove( new GenericMove( GenericPosition.d2, GenericPosition.d4 ));
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e4, GenericPosition.d3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantRightFalse() throws InvalidPieceException {
		// Black is on e4, white moves a knight to d4, check black ml doesn't contain a capture en passant, exd
		classUnderTest = new PositionManager("8/8/8/8/4p3/8/3N4/8 w - - 0 1 ");
		classUnderTest.performMove( new GenericMove( GenericPosition.d2, GenericPosition.d4 ));
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e4, GenericPosition.d3 );
		assertFalse( ml.contains( expectedMove ));
	}	
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantFromAFile() throws InvalidPieceException {
		// Black is on a4, white moves b4, then black ml contains capture en passant, axb
		classUnderTest = new PositionManager("8/8/8/8/p7/8/1P6/8 w - - 0 1 ");
		classUnderTest.performMove( new GenericMove( GenericPosition.b2, GenericPosition.b4 ));
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.a4, GenericPosition.b3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantFromAFile_1() throws InvalidPieceException {
		// Black is on a4, white moves b4, then black ml contains capture en passant, axb
		classUnderTest = new PositionManager("8/8/8/8/pP6/8/8/8 w - - 0 1 ");
		classUnderTest.performMove( new GenericMove( GenericPosition.b4, GenericPosition.b5 ));
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.a4, GenericPosition.b3 );
		assertFalse( ml.contains( expectedMove ));
	}	
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureEnPassantFromHFile() throws InvalidPieceException {
		// Black is on h4, white moves g4, then black ml contains capture en passant, hxg
		classUnderTest = new PositionManager("8/8/8/8/7p/8/6P1/8 w - - 0 1 ");
		classUnderTest.performMove( new GenericMove( GenericPosition.g2, GenericPosition.g4 ));
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.h4, GenericPosition.g3 );
		assertTrue( ml.contains( expectedMove ));
	}		
	
	@Test
	public void test_BlackPawn_MoveGen_MoveOneSquare() throws InvalidPieceException {
		// After initial move, ensure that a pawn can't move 2 any longer
		classUnderTest = new PositionManager("8/4p3/8/8/8/8/4P3/8 b - - 0 1 ");
		classUnderTest.performMove( new GenericMove( GenericPosition.e7, GenericPosition.e6 ));
		classUnderTest.performMove( new GenericMove( GenericPosition.e2, GenericPosition.e4 ));
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e6, GenericPosition.e5 );
		assertTrue( ml.size() == 1 );
		assertTrue( ml.contains( expectedMove ));		
	}	

	@Test
	public void test_BlackPawn_MoveGen_CaptureLeft() {
		classUnderTest = new PositionManager("8/4p3/5P2/8/8/8/8/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.f6 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_BlackPawn_MoveGen_CaptureRight() {
		classUnderTest = new PositionManager("8/4p3/3P4/8/8/8/8/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.d6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureFork() {
		classUnderTest = new PositionManager("8/4p3/3P1P2/8/8/8/8/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		GenericMove captureLeft = new GenericMove( GenericPosition.e7, GenericPosition.d6 );
		GenericMove captureRight = new GenericMove( GenericPosition.e7, GenericPosition.f6 );
		assertTrue( ml.contains( captureLeft ));
		assertTrue( ml.contains( captureRight ));
	}	
	
	@Test
	public void test_BlackPawn_MoveGen_CaptureFromAFile() {
		// Can only capture left
		classUnderTest = new PositionManager("8/p7/1P6/8/8/8/8/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();;
		expectedMove = new GenericMove( GenericPosition.a7, GenericPosition.b6 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_BlackPawn_MoveGen_CaptureFromHFile() {
		// Can only capture right
		classUnderTest = new PositionManager("8/7p/6P1/8/8/8/8/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.h7, GenericPosition.g6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_BlackPawn_MoveGen_PromoteQueen() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e1, GenericChessman.QUEEN );
		assertTrue( ml.contains( expectedMove ));
	}	

	@Test
	public void test_BlackPawn_MoveGen_PromoteKnight() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e1, GenericChessman.KNIGHT );
		assertTrue( ml.contains( expectedMove ));		
	}

	@Test
	public void test_BlackPawn_MoveGen_PromoteBishop() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e1, GenericChessman.BISHOP );
		assertTrue( ml.contains( expectedMove ));			
	}

	@Test
	public void test_BlackPawn_MoveGen_PromoteRook() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e1, GenericChessman.ROOK );
		assertTrue( ml.contains( expectedMove ));	
	}
	
	@Test
	public void test_WhitePawn_MoveGen_InitialMoveOneSquare() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/4P3/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e4 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_WhitePawn_MoveGen_InitialMoveTwoSquares() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/4P3/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e3 );
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
		classUnderTest.performMove( new GenericMove( GenericPosition.d7, GenericPosition.d5 ));
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e5, GenericPosition.d6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantLeftFalse() throws InvalidPieceException {
		classUnderTest = new PositionManager("8/5r2/8/4P3/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( new GenericMove( GenericPosition.f7, GenericPosition.f5 ));
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e5, GenericPosition.f6 );
		assertFalse( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantRight() throws InvalidPieceException {
		classUnderTest = new PositionManager("8/5p2/8/4P3/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( new GenericMove( GenericPosition.f7, GenericPosition.f5 ));
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e5, GenericPosition.f6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantRightFalse() throws InvalidPieceException {
		classUnderTest = new PositionManager("8/5r2/8/4P3/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( new GenericMove( GenericPosition.f7, GenericPosition.f5 ));
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e5, GenericPosition.f6 );
		assertFalse( ml.contains( expectedMove ));
	}	
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantFromAFile() throws InvalidPieceException {
		// white is on a5, black moves b5, then black ml contains capture en passant, axb
		classUnderTest = new PositionManager("8/1p6/8/P7/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( new GenericMove( GenericPosition.b7, GenericPosition.b5 ));
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.a5, GenericPosition.b6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureEnPassantFromHFile() throws InvalidPieceException {
		// Black is on h4, white moves g4, then black ml contains capture en passant, hxg
		classUnderTest = new PositionManager("8/6p1/8/7P/8/8/8/8 b - - 0 1");
		classUnderTest.performMove( new GenericMove( GenericPosition.g7, GenericPosition.g5 ));
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.h5, GenericPosition.g6 );
		assertTrue( ml.contains( expectedMove ));
	}		
	
	@Test
	public void test_WhitePawn_MoveGen_MoveOneSquare() throws InvalidPieceException {
		// After initial move, ensure that a pawn can't move 2 any longer
		classUnderTest = new PositionManager("8/5p2/8/8/8/8/4P3/8 w - - 0 1");
		classUnderTest.performMove( new GenericMove( GenericPosition.e2, GenericPosition.e4 ));
		classUnderTest.performMove( new GenericMove( GenericPosition.f7, GenericPosition.f6 ));
		
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e4, GenericPosition.e5 );
		assertTrue( ml.size() == 1 );
		assertTrue( ml.contains( expectedMove ));		
	}	

	@Test
	public void test_WhitePawn_MoveGen_CaptureLeft() {
		classUnderTest = new PositionManager("8/8/8/8/8/5p2/4P3/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.f3 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_WhitePawn_MoveGen_CaptureRight() {
		classUnderTest = new PositionManager("8/8/8/8/8/3p4/4P3/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.d3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureFork() {
		classUnderTest = new PositionManager("8/8/8/8/8/3p1p2/4P3/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		GenericMove captureLeft = new GenericMove( GenericPosition.e2, GenericPosition.d3 );
		GenericMove captureRight = new GenericMove( GenericPosition.e2, GenericPosition.f3 );
		assertTrue( ml.contains( captureLeft ));
		assertTrue( ml.contains( captureRight ));
	}	
	
	@Test
	public void test_WhitePawn_MoveGen_CaptureFromAFile() {
		// Can only capture left
		classUnderTest = new PositionManager("8/8/8/8/8/1p6/P7/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.a2, GenericPosition.b3 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_WhitePawn_MoveGen_CaptureFromHFile() {
		// Can only capture right
		classUnderTest = new PositionManager("8/8/8/8/8/6p1/7P/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.h2, GenericPosition.g3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_WhitePawn_MoveGen_PromoteQueen() {
		classUnderTest = new PositionManager("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e8, GenericChessman.QUEEN );
		assertTrue( ml.contains( expectedMove ));
	}	

	@Test
	public void test_WhitePawn_MoveGen_PromoteKnight() {
		classUnderTest = new PositionManager("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e8, GenericChessman.KNIGHT );
		assertTrue( ml.contains( expectedMove ));		
	}

	@Test
	public void test_WhitePawn_MoveGen_PromoteBishop() {
		classUnderTest = new PositionManager("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e8, GenericChessman.BISHOP );
		assertTrue( ml.contains( expectedMove ));			
	}

	@Test
	public void test_WhitePawn_MoveGen_PromoteRook() {
		classUnderTest = new PositionManager("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e8, GenericChessman.ROOK );
		assertTrue( ml.contains( expectedMove ));	
	}
	
	/* Bishop Moves */
	@Test
	public void test_Bishop_MoveGen_CornerTopLeft() {
		classUnderTest = new PositionManager("b7/8/8/8/8/8/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.b7 ));
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.h1 ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml);
	}

	@Test
	public void test_Bishop_MoveGen_CornerTopRight() {
		classUnderTest = new PositionManager("7b/8/8/8/8/8/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.h8, GenericPosition.g7 ));
		expectedMoves.add( new GenericMove( GenericPosition.h8, GenericPosition.a1 ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Bishop_MoveGen_CornerBottomRight() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/8/7b b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.h1, GenericPosition.g2 ));
		expectedMoves.add( new GenericMove( GenericPosition.h1, GenericPosition.a8 ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Bishop_MoveGen_CornerBottomLeft() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/8/b7 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.b2 ));
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.h8 ));
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
		expectedMoves.add( new GenericMove( GenericPosition.a4, GenericPosition.b5 ));
		expectedMoves.add( new GenericMove( GenericPosition.a4, GenericPosition.e8 ));
		expectedNumMoves = 5;
		checkExpectedMoves(ml);		
	}
	
	@Test
	public void test_Bishop_MoveGen_LeftEdge_PartiallyObstructedCapturablePiece() {
		classUnderTest = new PositionManager("8/8/8/8/b7/1P6/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.a4, GenericPosition.b5 ));
		expectedMoves.add( new GenericMove( GenericPosition.a4, GenericPosition.e8 ));
		expectedMoves.add( new GenericMove( GenericPosition.a4, GenericPosition.b3 ));
		expectedNumMoves = 5;
		checkExpectedMoves(ml);	
	}
	
	@Test
	public void test_Bishop_MoveGen_Middle_ObstructedCapturablePieces() {
		classUnderTest = new PositionManager("8/8/8/3P1P2/4b3/3P1P2/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d3 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d5 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f3 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f5 ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Bishop_MoveGen_Middle_ObstructedMixturePieces() {
		classUnderTest = new PositionManager("8/8/8/3P1p2/4b3/3P1p2/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d3 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d5 ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Bishop_MoveGen_Middle_Unobstructed() {
		classUnderTest = new PositionManager("8/8/8/4P3/3PbP2/4P3/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d3 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d5 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f3 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f5 ));
		expectedNumMoves = 13;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Bishop_MoveGen_CapturesOnlySinglePiece() {
		classUnderTest = new PositionManager("8/8/8/8/8/2P5/1P6/b7 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.b2 ));
		expectedNumMoves = 1;
		checkExpectedMoves(ml);
		assertFalse(ml.contains( new GenericMove( GenericPosition.a1, GenericPosition.c3 )));
	}
	
	/* Knight Moves */
	GenericPosition startTestOnSq;
	@Test
	public void test_SquareA8() {
		startTestOnSq = GenericPosition.a8;
		classUnderTest = new PositionManager("N7/8/8/8/8/8/8/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.c7 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.b6 ));
		expectedNumMoves = 2;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_SquareB7() {
		startTestOnSq = GenericPosition.b7;
		classUnderTest = new PositionManager("8/1N6/8/8/8/8/8/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.d8 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.d6 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.a5 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.c5 ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_SquareC6() {
		startTestOnSq = GenericPosition.c6;
		classUnderTest = new PositionManager("8/8/2N5/8/8/8/8/8 w - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.b8 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.d8 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.e7 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.e5 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.b4 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.d4 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.a7 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.a5 ));
		expectedNumMoves = 8;
		checkExpectedMoves(ml);
	}
	
	/* Rook Moves */
	@Test
	public void test_Rook_MoveGen_CornerTopLeft() {
		classUnderTest = new PositionManager("R7/8/8/8/8/8/8/8 w - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.a7 ));
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.b8 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}

	@Test
	public void test_Rook_MoveGen_CornerTopRight() {
		classUnderTest = new PositionManager("7R/8/8/8/8/8/8/8 w - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.h8, GenericPosition.h7 ));
		expectedMoves.add( new GenericMove( GenericPosition.h8, GenericPosition.g8 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Rook_MoveGen_CornerBottomRight() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/8/7R w - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.h1, GenericPosition.h2 ));
		expectedMoves.add( new GenericMove( GenericPosition.h1, GenericPosition.g1 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Rook_MoveGen_CornerBottomLeft() {
		classUnderTest = new PositionManager("8/8/8/8/8/8/8/R7 w - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.a2 ));
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.b1 ));
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
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.a2 ));
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.b1 ));		
		expectedNumMoves = 8;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Rook_MoveGen_Middle_ObstructedCapturablePieces() {
		classUnderTest = new PositionManager("8/8/8/4P3/3PrP2/4P3/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f4 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d4 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e5 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e3 ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Rook_MoveGen_Middle_ObstructedMixturePieces() {
		classUnderTest = new PositionManager("8/8/8/4P3/3prp2/4P3/8/8 b - - 0 1");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e5 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e3 ));
		expectedNumMoves = 6;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Rook_MoveGen_Middle_Unobstructed() {
		classUnderTest = new PositionManager("8/8/8/3P1P2/4r3/3P1P2/8/8 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f4 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d4 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e5 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e3 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Rook_MoveGen_CapturesOnlySinglePiece() {
		classUnderTest = new PositionManager("8/8/8/8/8/P7/P7/r7 b - - 0 1 ");
		ml = classUnderTest.generateMoves();
		assertFalse(ml.isEmpty());
		assertTrue( ml.contains( new GenericMove( GenericPosition.a1, GenericPosition.a2 )));
		assertFalse(ml.contains( new GenericMove( GenericPosition.a1, GenericPosition.a3 )));
	}
}