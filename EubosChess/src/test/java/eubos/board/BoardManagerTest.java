package eubos.board;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import eubos.pieces.King;
import eubos.pieces.Pawn;
import eubos.pieces.Piece;
import eubos.pieces.Rook;

import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

public class BoardManagerTest {
	
	protected BoardManager classUnderTest;
	
	@Test
	public void test_NoLastMoveToUndo() throws InvalidPieceException {
		classUnderTest = new BoardManager();
		classUnderTest.undoPreviousMove();
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
		classUnderTest = new BoardManager("8/8/8/8/8/8/4P3/8 w - - - -");
		classUnderTest.performMove(new GenericMove("e2e4"));
		classUnderTest.undoPreviousMove();
		Piece expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.e2 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isWhite());		
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
		classUnderTest = new BoardManager("8/8/8/8/8/8/4p3/8 b - - - -");
		classUnderTest.performMove( new GenericMove("e2e1Q"));
		classUnderTest.undoPreviousMove();
		Piece expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.e2 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isBlack());
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
		classUnderTest = new BoardManager("8/8/8/8/8/3p4/4P3/8 w - - - -");
		classUnderTest.performMove( new GenericMove("d3e2"));
		classUnderTest.undoPreviousMove();
		Piece expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.d3 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isBlack());
		expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.e2 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isWhite());
	}
	
	@Test
	public void test_enPassantCaptureAtC3() throws InvalidPieceException, IllegalNotationException {
		classUnderTest = new BoardManager( "r3k2r/1bqpbppp/p1n1p3/3nP3/PpP1N3/3B1N2/1P2QPPP/R4RK1 b kq c3 0 1");
		classUnderTest.performMove( new GenericMove("b4c3"));
		Piece expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.c3 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isBlack());
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
		classUnderTest = new BoardManager("8/8/8/8/8/8/8/4K2R w - - - -");
		GenericMove expectedMove = new GenericMove("e1g1");
		classUnderTest.performMove(expectedMove);
		Piece whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(GenericPosition.h1);
		assertTrue(whiteRook == null);
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(GenericPosition.f1);
		assertTrue(whiteRook instanceof Rook);
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
		classUnderTest = new BoardManager("8/8/8/8/8/8/8/4K2R w K - - -");
		GenericMove expectedMove = new GenericMove("e1g1");
		classUnderTest.performMove(expectedMove);
		Piece whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(GenericPosition.h1);
		assertTrue(whiteRook == null);
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(GenericPosition.f1);
		assertTrue(whiteRook instanceof Rook);
		classUnderTest.undoPreviousMove();
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(GenericPosition.f1);
		assertTrue(whiteRook == null);
		whiteRook = classUnderTest.getTheBoard().getPieceAtSquare(GenericPosition.h1);
		assertTrue(whiteRook instanceof Rook);
		Piece whiteKing = classUnderTest.getTheBoard().getPieceAtSquare(GenericPosition.e1);
		assertTrue(whiteKing instanceof King);
	}
}
