package eubos.pieces;

import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertTrue;


import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.pieces.Piece.Colour;

public class BishopTest extends PieceTest {
	
	@Test
	public void test_CornerTopLeft() {
		classUnderTest = new Bishop( Colour.black, GenericPosition.a8 );
		ml = completeSetupAndGenerateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.b7 ));
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.h1 ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml);
	}

	@Test
	public void test_CornerTopRight() {
		classUnderTest = new Bishop( Colour.white, GenericPosition.h8 );
		ml = completeSetupAndGenerateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.h8, GenericPosition.g7 ));
		expectedMoves.add( new GenericMove( GenericPosition.h8, GenericPosition.a1 ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CornerBottomRight() {
		classUnderTest = new Bishop( Colour.white, GenericPosition.h1 );
		ml = completeSetupAndGenerateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.h1, GenericPosition.g2 ));
		expectedMoves.add( new GenericMove( GenericPosition.h1, GenericPosition.a8 ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CornerBottomLeft() {
		classUnderTest = new Bishop( Colour.black, GenericPosition.a1 );
		ml = completeSetupAndGenerateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.b2 ));
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.h8 ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CornerBottomLeft_ObstructedOwnPieces() {
		classUnderTest = new Bishop( Colour.black, GenericPosition.a1 );
		pl.add(new Pawn( Colour.black, GenericPosition.b2));
		ml = completeSetupAndGenerateMoves();
		checkNoMovesGenerated(ml);
	}

	@Test
	public void test_LeftEdge_PartiallyObstructedOwnPiece() {
		classUnderTest = new Bishop( Colour.black, GenericPosition.a4 );
		pl.add(new Pawn( Colour.black, GenericPosition.b3));
		ml = completeSetupAndGenerateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.a4, GenericPosition.b5 ));
		expectedMoves.add( new GenericMove( GenericPosition.a4, GenericPosition.e8 ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml);		
	}
	
	@Test
	public void test_LeftEdge_PartiallyObstructedCapturablePiece() {
		classUnderTest = new Bishop( Colour.black, GenericPosition.a4 );
		pl.add(new Pawn( Colour.white, GenericPosition.b3));
		ml = completeSetupAndGenerateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.a4, GenericPosition.b5 ));
		expectedMoves.add( new GenericMove( GenericPosition.a4, GenericPosition.e8 ));
		expectedMoves.add( new GenericMove( GenericPosition.a4, GenericPosition.b3 ));
		expectedNumMoves = 5;
		checkExpectedMoves(ml);	
	}
	
	@Test
	public void test_Middle_ObstructedCapturablePieces() {
		classUnderTest = new Bishop( Colour.black, GenericPosition.e4 );
		pl.add(new Pawn( Colour.white, GenericPosition.d3));
		pl.add(new Pawn( Colour.white, GenericPosition.d5));
		pl.add(new Pawn( Colour.white, GenericPosition.f3));
		pl.add(new Pawn( Colour.white, GenericPosition.f5));
		ml = completeSetupAndGenerateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d3 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d5 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f3 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f5 ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Middle_ObstructedMixturePieces() {
		classUnderTest = new Bishop( Colour.black, GenericPosition.e4 );
		pl.add(new Pawn( Colour.white, GenericPosition.d3));
		pl.add(new Pawn( Colour.white, GenericPosition.d5));
		pl.add(new Pawn( Colour.black, GenericPosition.f3));
		pl.add(new Pawn( Colour.black, GenericPosition.f5));
		ml = completeSetupAndGenerateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d3 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d5 ));
		expectedNumMoves = 2;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Middle_Unobstructed() {
		classUnderTest = new Bishop( Colour.black, GenericPosition.e4 );
		pl.add(new Pawn( Colour.white, GenericPosition.e5));
		pl.add(new Pawn( Colour.white, GenericPosition.e3));
		pl.add(new Pawn( Colour.white, GenericPosition.d4));
		pl.add(new Pawn( Colour.white, GenericPosition.f4));
		ml = completeSetupAndGenerateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d3 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d5 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f3 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f5 ));
		expectedNumMoves = 13;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CapturesOnlySinglePiece() {
		classUnderTest = new Bishop( Colour.black, GenericPosition.a1 );
		pl.add(new Pawn( Colour.white, GenericPosition.b2));
		pl.add(new Pawn( Colour.white, GenericPosition.c3));
		ml = completeSetupAndGenerateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.b2 ));
		expectedNumMoves = 1;
		checkExpectedMoves(ml);
		assertFalse(ml.contains( new GenericMove( GenericPosition.a1, GenericPosition.c3 )));
	}
}
