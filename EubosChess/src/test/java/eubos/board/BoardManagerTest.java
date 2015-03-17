package eubos.board;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import static org.junit.Assert.assertTrue;
import eubos.pieces.Bishop;
import eubos.pieces.King;
import eubos.pieces.Pawn;
import eubos.pieces.Piece;
import eubos.pieces.Queen;
import eubos.pieces.Rook;
import eubos.pieces.Piece.Colour;

import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericChessman;

public class BoardManagerTest {
	
	protected LinkedList<Piece> pl;
	protected BoardManager classUnderTest;
	
	@Before
	public void setUp() {
		pl = new LinkedList<Piece>();
	}
	
	@Test(expected=java.util.EmptyStackException.class)
	@Ignore
	public void test_NoLastMoveToUndo() {
		classUnderTest = new BoardManager();
		classUnderTest.undoPreviousMove();
	}
	
	@Test
	public void test_UndoPawnMove() {
		pl.add( new Pawn( Piece.Colour.white, GenericPosition.e2 ));
		classUnderTest = new BoardManager( new Board( pl ));
		classUnderTest.performMove( new GenericMove( GenericPosition.e2, GenericPosition.e4 ));
		classUnderTest.undoPreviousMove();
		Piece expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.e2 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isWhite());		
	}
	
	@Test
	public void test_UndoPawnPromotion() {
		pl.add( new Pawn( Piece.Colour.black, GenericPosition.e2 ));
		classUnderTest = new BoardManager( new Board( pl ));
		classUnderTest.performMove( new GenericMove( GenericPosition.e2, GenericPosition.e1, GenericChessman.QUEEN ));
		classUnderTest.undoPreviousMove();
		Piece expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.e2 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isBlack());
	}
	
	@Test
	public void test_UndoPawnCapture() {
		pl.add( new Pawn( Piece.Colour.black, GenericPosition.d3 ));
		pl.add( new Pawn( Piece.Colour.white, GenericPosition.e2 ));
		classUnderTest = new BoardManager( new Board( pl ));
		classUnderTest.performMove( new GenericMove( GenericPosition.d3, GenericPosition.e2 ));
		classUnderTest.undoPreviousMove();
		Piece expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.d3 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isBlack());
		expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.e2 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isWhite());
	}
	
	@Test
	public void test_WhiteKingSideCastle() {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ....k..r
		//   abcdefgh
		pl.add(new King( Colour.white, GenericPosition.e1 ));
		pl.add(new Rook( Colour.white, GenericPosition.h1 ));
		classUnderTest = new BoardManager( new Board( pl ));
		GenericMove kscMove = classUnderTest.addKingSideCastle( Colour.white );
		GenericMove expectedMove = new GenericMove( GenericPosition.e1, GenericPosition.g1 );
		assertTrue(kscMove != null);
		assertTrue(expectedMove.equals(kscMove));
	}
	
	@Test
	public void test_WhiteKingSideCastle_Check() {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ..B.....
		// 2 ........
		// 1 ....k..r
		//   abcdefgh
		pl.add(new King( Colour.white, GenericPosition.e1 ));
		pl.add(new Rook( Colour.white, GenericPosition.h1 ));
		pl.add(new Bishop( Colour.black, GenericPosition.c3 ));
		classUnderTest = new BoardManager( new Board( pl ));
		GenericMove kscMove = classUnderTest.addKingSideCastle( Colour.white );
		assertTrue(kscMove == null);
	}
	
	@Test
	public void test_WhiteKingSideCastle_MovesThroughCheckAtF1() {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ...B....
		// 2 ........
		// 1 ....k..r
		//   abcdefgh
		pl.add(new King( Colour.white, GenericPosition.e1 ));
		pl.add(new Rook( Colour.white, GenericPosition.h1 ));
		pl.add(new Bishop( Colour.black, GenericPosition.d3 ));
		classUnderTest = new BoardManager( new Board( pl ));
		GenericMove kscMove = classUnderTest.addKingSideCastle( Colour.white );
		assertTrue(kscMove == null);
	}
	
	@Test
	public void test_WhiteKingSideCastle_MovesThroughCheckAtG1() {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ....B...
		// 2 ........
		// 1 ....k..r
		//   abcdefgh
		pl.add(new King( Colour.white, GenericPosition.e1 ));
		pl.add(new Rook( Colour.white, GenericPosition.h1 ));
		pl.add(new Bishop( Colour.black, GenericPosition.e3 ));
		classUnderTest = new BoardManager( new Board( pl ));
		GenericMove kscMove = classUnderTest.addKingSideCastle( Colour.white );
		assertTrue(kscMove == null);
	}
	
	@Test
	public void test_WhiteKingSideCastle_BlockedOwnPieceAtF1() {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ....kb.r
		//   abcdefgh
		pl.add(new King( Colour.white, GenericPosition.e1 ));
		pl.add(new Rook( Colour.white, GenericPosition.h1 ));
		pl.add(new Bishop( Colour.white, GenericPosition.f1 ));
		classUnderTest = new BoardManager( new Board( pl ));
		GenericMove kscMove = classUnderTest.addKingSideCastle( Colour.white );
		assertTrue(kscMove == null);
	}
	
	@Test
	public void test_WhiteKingSideCastle_BlockedOwnPieceAtG1() {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ....k.br
		//   abcdefgh
		pl.add(new King( Colour.white, GenericPosition.e1 ));
		pl.add(new Rook( Colour.white, GenericPosition.h1 ));
		pl.add(new Bishop( Colour.white, GenericPosition.g1 ));
		classUnderTest = new BoardManager( new Board( pl ));
		GenericMove kscMove = classUnderTest.addKingSideCastle( Colour.white );
		assertTrue(kscMove == null);
	}	
	
	@Test
	public void test_WhiteKingSideCastle_RookIsAttackedAtH1() {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 .....B..
		// 2 ........
		// 1 ....k..r
		//   abcdefgh
		pl.add(new King( Colour.white, GenericPosition.e1 ));
		pl.add(new Rook( Colour.white, GenericPosition.h1 ));
		pl.add(new Bishop( Colour.black, GenericPosition.f3 ));
		classUnderTest = new BoardManager( new Board( pl ));
		GenericMove kscMove = classUnderTest.addKingSideCastle( Colour.white );
		GenericMove expectedMove = new GenericMove( GenericPosition.e1, GenericPosition.g1 );
		assertTrue(kscMove != null);
		assertTrue(expectedMove.equals(kscMove));
	}
	
	
	@Test
	public void test_BlackQueenSideCastle() {
		// 8 R...K...
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ........
		//   abcdefgh
		pl.add(new King( Colour.black, GenericPosition.e8 ));
		pl.add(new Rook( Colour.black, GenericPosition.a8 ));
		classUnderTest = new BoardManager( new Board( pl ));
		GenericMove qscMove = classUnderTest.addQueenSideCastle( Colour.black );
		GenericMove expectedMove = new GenericMove( GenericPosition.e8, GenericPosition.c8 );
		assertTrue(qscMove != null);
		assertTrue(expectedMove.equals(qscMove));
	}
	
	@Test
	public void test_BlackQueenSideCastle_Check() {
		// 8 R...K...
		// 7 ........
		// 6 ......b.
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ........
		//   abcdefgh
		pl.add(new King( Colour.black, GenericPosition.e8 ));
		pl.add(new Rook( Colour.black, GenericPosition.a8 ));
		pl.add(new Bishop( Colour.white, GenericPosition.g6 ));
		classUnderTest = new BoardManager( new Board( pl ));
		GenericMove qscMove = classUnderTest.addQueenSideCastle( Colour.black );
		assertTrue(qscMove == null);
	}
	
	@Test
	public void test_BlackQueenSideCastle_MovesThroughCheckAtD8() {
			// 8 R...K...
			// 7 ........
			// 6 .....b..
			// 5 ........
			// 4 ........
			// 3 ........
			// 2 ........
			// 1 ........
			//   abcdefgh
			pl.add(new King( Colour.black, GenericPosition.e8 ));
			pl.add(new Rook( Colour.black, GenericPosition.a8 ));
			pl.add(new Bishop( Colour.white, GenericPosition.f6 ));
			classUnderTest = new BoardManager( new Board( pl ));
			GenericMove qscMove = classUnderTest.addQueenSideCastle( Colour.black );
			assertTrue(qscMove == null);
		}
		
	@Test
	public void test_BlackQueenSideCastle_MovesThroughCheckAtC8() {
		// 8 R...K...
		// 7 ........
		// 6 ....b...
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ........
		//   abcdefgh
		pl.add(new King( Colour.black, GenericPosition.e8 ));
		pl.add(new Rook( Colour.black, GenericPosition.a8 ));
		pl.add(new Bishop( Colour.white, GenericPosition.e6 ));
		classUnderTest = new BoardManager( new Board( pl ));
		GenericMove qscMove = classUnderTest.addQueenSideCastle( Colour.black );
		assertTrue(qscMove == null);
	}
	
	@Test
	public void test_BlackQueenSideCastle_BlockedOwnPieceAtD8() {
		// 8 R..QK...
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ........
		//   abcdefgh
		pl.add(new King( Colour.black, GenericPosition.e8 ));
		pl.add(new Rook( Colour.black, GenericPosition.a8 ));
		pl.add(new Queen( Colour.black, GenericPosition.d8 ));
		classUnderTest = new BoardManager( new Board( pl ));
		GenericMove qscMove = classUnderTest.addQueenSideCastle( Colour.black );
		assertTrue(qscMove == null);
	}
	
	@Test
	public void test_BlackQueenSideCastle_RookIsAttackedAtA8() {
		// 8 R...K...
		// 7 ........
		// 6 R.......
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ........
		//   abcdefgh
		pl.add(new King( Colour.black, GenericPosition.e8 ));
		pl.add(new Rook( Colour.black, GenericPosition.a8 ));
		pl.add(new Rook( Colour.white, GenericPosition.a6 ));
		classUnderTest = new BoardManager( new Board( pl ));
		GenericMove qscMove = classUnderTest.addQueenSideCastle( Colour.black );
		GenericMove expectedMove = new GenericMove( GenericPosition.e8, GenericPosition.c8 );
		assertTrue(qscMove != null);
		assertTrue(expectedMove.equals(qscMove));
	}
}
