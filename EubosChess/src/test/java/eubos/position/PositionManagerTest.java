package eubos.position;

import org.junit.Test;

import static org.junit.Assert.*;
import eubos.board.InvalidPieceException;
import eubos.board.pieces.Piece.Colour;
import eubos.board.pieces.Piece.PieceType;
import eubos.position.PositionManager;

import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

public class PositionManagerTest {
	
	protected PositionManager classUnderTest;
	
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
}