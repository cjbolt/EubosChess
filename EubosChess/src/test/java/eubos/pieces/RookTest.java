package eubos.pieces;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.BoardManager;
import eubos.pieces.Piece.Colour;

public class RookTest extends PieceTest {
	
	List<GenericMove> ml;
	
	@Test
	public void test_CornerTopLeft() {
		classUnderTest = new Rook( Colour.black, GenericPosition.a8 );
		pl.add(classUnderTest);
		bm = new BoardManager( new Board( pl ), Colour.black );
		ml = classUnderTest.generateMoves( bm );
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.a7 ));
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.b8 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}

	@Test
	public void test_CornerTopRight() {
		classUnderTest = new Rook( Colour.white, GenericPosition.h8 );
		pl.add(classUnderTest);
		bm = new BoardManager( new Board( pl ), Colour.white );
		ml = classUnderTest.generateMoves( bm );
		expectedMoves.add( new GenericMove( GenericPosition.h8, GenericPosition.h7 ));
		expectedMoves.add( new GenericMove( GenericPosition.h8, GenericPosition.g8 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CornerBottomRight() {
		classUnderTest = new Rook( Colour.white, GenericPosition.h1 );
		pl.add(classUnderTest);
		bm = new BoardManager( new Board( pl ), Colour.white );
		ml = classUnderTest.generateMoves( bm );
		expectedMoves.add( new GenericMove( GenericPosition.h1, GenericPosition.h2 ));
		expectedMoves.add( new GenericMove( GenericPosition.h1, GenericPosition.g1 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CornerBottomLeft() {
		classUnderTest = new Rook( Colour.black, GenericPosition.a1 );
		pl.add(classUnderTest);
		bm = new BoardManager( new Board( pl ), Colour.black );
		ml = classUnderTest.generateMoves( bm );
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.a2 ));
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.b1 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CornerBottomLeft_ObstructedOwnPieces() {
		classUnderTest = new Rook( Colour.black, GenericPosition.a1 );
		pl.add(classUnderTest);
		pl.add(new Pawn( Colour.black, GenericPosition.a2));
		pl.add(new Pawn( Colour.black, GenericPosition.b1));
		bm = new BoardManager( new Board( pl ), Colour.black );
		ml = classUnderTest.generateMoves( bm );
		checkNoMovesGenerated(ml);
	}

	@Test
	public void test_CornerBottomLeft_PartiallyObstructedOwnPiece() {
		classUnderTest = new Rook( Colour.black, GenericPosition.a1 );
		pl.add(classUnderTest);
		pl.add(new Pawn( Colour.black, GenericPosition.a2));
		bm = new BoardManager( new Board( pl ), Colour.black );
		ml = classUnderTest.generateMoves( bm );
		assertFalse(ml.isEmpty());
		assertTrue(ml.size()==7);
	}
	
	@Test
	public void test_CornerBottomLeft_PartiallyObstructedCapturablePiece() {
		classUnderTest = new Rook( Colour.black, GenericPosition.a1 );
		pl.add(classUnderTest);
		pl.add(new Pawn( Colour.white, GenericPosition.a2));
		bm = new BoardManager( new Board( pl ), Colour.black );
		ml = classUnderTest.generateMoves( bm );
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.a2 ));
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.b1 ));		
		expectedNumMoves = 8;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Middle_ObstructedCapturablePieces() {
		classUnderTest = new Rook( Colour.black, GenericPosition.e4 );
		pl.add(classUnderTest);
		pl.add(new Pawn( Colour.white, GenericPosition.e5));
		pl.add(new Pawn( Colour.white, GenericPosition.e3));
		pl.add(new Pawn( Colour.white, GenericPosition.d4));
		pl.add(new Pawn( Colour.white, GenericPosition.f4));
		bm = new BoardManager( new Board( pl ), Colour.black );
		ml = classUnderTest.generateMoves( bm );
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f4 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d4 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e5 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e3 ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Middle_ObstructedMixturePieces() {
		classUnderTest = new Rook( Colour.black, GenericPosition.e4 );
		pl.add(classUnderTest);
		pl.add(new Pawn( Colour.white, GenericPosition.e5));
		pl.add(new Pawn( Colour.white, GenericPosition.e3));
		pl.add(new Pawn( Colour.black, GenericPosition.d4));
		pl.add(new Pawn( Colour.black, GenericPosition.f4));
		bm = new BoardManager( new Board( pl ), Colour.black );
		ml = classUnderTest.generateMoves( bm );
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e5 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e3 ));
		expectedNumMoves = 2;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Middle_Unobstructed() {
		classUnderTest = new Rook( Colour.black, GenericPosition.e4 );
		pl.add(new Pawn( Colour.white, GenericPosition.d3));
		pl.add(new Pawn( Colour.white, GenericPosition.d5));
		pl.add(new Pawn( Colour.white, GenericPosition.f3));
		pl.add(new Pawn( Colour.white, GenericPosition.f5));
		ml = completeSetupAndGenerateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f4 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d4 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e5 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e3 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CapturesOnlySinglePiece() {
		classUnderTest = new Rook( Colour.black, GenericPosition.a1 );
		pl.add(classUnderTest);
		pl.add(new Pawn( Colour.white, GenericPosition.a2));
		pl.add(new Pawn( Colour.white, GenericPosition.a3));
		bm = new BoardManager( new Board( pl ), Colour.black );
		ml = classUnderTest.generateMoves( bm );
		assertFalse(ml.isEmpty());
		assertTrue( ml.contains( new GenericMove( GenericPosition.a1, GenericPosition.a2 )));
		assertFalse(ml.contains( new GenericMove( GenericPosition.a1, GenericPosition.a3 )));
	}
}
