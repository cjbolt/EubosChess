package eubos.pieces;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.*;

import eubos.board.*;

import com.fluxchess.jcpi.models.*;

public class PawnTest_Black extends PawnTest {

	@Test
	public void test_InitialMoveOneSquare() {
		classUnderTest = addBlackPawn( GenericPosition.e7 );
		bm = new BoardManager( new Board( pl ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e6 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_InitialMoveTwoSquares() {
		classUnderTest = addBlackPawn( GenericPosition.e7 );
		bm = new BoardManager( new Board( pl ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e5 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_InitialBlocked() {
		classUnderTest = addBlackPawn( GenericPosition.e7 );
		addWhitePawn( GenericPosition.e6 );
		bm = new BoardManager( new Board( pl ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		assertTrue( ml.isEmpty() );
	}

	@Test
	public void test_CaptureEnPassantLeft() {
		// Black is on e4, white moves f4, then black ml contains capture en passant, exf
		classUnderTest = addBlackPawn( GenericPosition.e4 );
		addWhitePawn( GenericPosition.f2 );
		bm = new BoardManager( new Board( pl ));
		bm.performMove( new GenericMove( GenericPosition.f2, GenericPosition.f4 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.e4, GenericPosition.f3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureEnPassantLeftFalse() {
		// Black is on e4, white moves a knight to f4, check black ml doesn't contain a capture en passant, exf
		classUnderTest = addBlackPawn( GenericPosition.e4 );
		pl.add( new Knight( Piece.Colour.white, GenericPosition.f2 ));
		bm = new BoardManager( new Board( pl ));
		bm.performMove( new GenericMove( GenericPosition.f2, GenericPosition.f4 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.e4, GenericPosition.f3 );
		assertFalse( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureEnPassantRight() {
		// Black is on e4, white moves d4, then black ml contains capture en passant, exd
		classUnderTest = addBlackPawn( GenericPosition.e4 );
		addWhitePawn( GenericPosition.d2 );
		bm = new BoardManager( new Board( pl ));
		bm.performMove( new GenericMove( GenericPosition.d2, GenericPosition.d4 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.e4, GenericPosition.d3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureEnPassantRightFalse() {
		// Black is on e4, white moves a knight to d4, check black ml doesn't contain a capture en passant, exd
		classUnderTest = addBlackPawn( GenericPosition.e4 );
		pl.add( new Knight( Piece.Colour.white, GenericPosition.d2 ));
		bm = new BoardManager( new Board( pl ));
		bm.performMove( new GenericMove( GenericPosition.d2, GenericPosition.d4 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.e4, GenericPosition.d3 );
		assertFalse( ml.contains( expectedMove ));
	}	
	
	@Test
	public void test_CaptureEnPassantFromAFile() {
		// Black is on a4, white moves b4, then black ml contains capture en passant, axb
		classUnderTest = addBlackPawn( GenericPosition.a4 );
		addWhitePawn( GenericPosition.b2 );
		bm = new BoardManager( new Board( pl ));
		bm.performMove( new GenericMove( GenericPosition.b2, GenericPosition.b4 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.a4, GenericPosition.b3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureEnPassantFromAFile_1() {
		// Black is on a4, white moves b4, then black ml contains capture en passant, axb
		classUnderTest = addBlackPawn( GenericPosition.a4 );
		addWhitePawn( GenericPosition.b4 );
		bm = new BoardManager( new Board( pl ));
		bm.performMove( new GenericMove( GenericPosition.b4, GenericPosition.b5 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.a4, GenericPosition.b3 );
		assertFalse( ml.contains( expectedMove ));
	}	
	
	@Test
	public void test_CaptureEnPassantFromHFile() {
		// Black is on h4, white moves g4, then black ml contains capture en passant, hxg
		classUnderTest = addBlackPawn( GenericPosition.h4 );
		addWhitePawn( GenericPosition.g2 );
		bm = new BoardManager( new Board( pl ));
		bm.performMove( new GenericMove( GenericPosition.g2, GenericPosition.g4 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.h4, GenericPosition.g3 );
		assertTrue( ml.contains( expectedMove ));
	}		
	
	@Test
	public void test_MoveOneSquare() {
		// After initial move, ensure that a pawn can't move 2 any longer
		classUnderTest = addBlackPawn( GenericPosition.e7 );
		addWhitePawn( GenericPosition.e2 );
		bm = new BoardManager( new Board( pl ));
		bm.performMove( new GenericMove( GenericPosition.e7, GenericPosition.e6 ));
		bm.performMove( new GenericMove( GenericPosition.f2, GenericPosition.f4 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.e6, GenericPosition.e5 );
		assertTrue( ml.size() == 1 );
		assertTrue( ml.contains( expectedMove ));		
	}	

	@Test
	public void test_CaptureLeft() {
		classUnderTest = addBlackPawn( GenericPosition.e7 );
		addWhitePawn( GenericPosition.f6 );
		bm = new BoardManager( new Board( pl ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.f6 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_CaptureRight() {
		classUnderTest = addBlackPawn( GenericPosition.e7 );
		addWhitePawn( GenericPosition.d6 );
		bm = new BoardManager( new Board( pl ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.d6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureFork() {
		classUnderTest = addBlackPawn( GenericPosition.e7 );
		addWhitePawn( GenericPosition.d6 );
		addWhitePawn( GenericPosition.f6 );
		bm = new BoardManager( new Board( pl ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		GenericMove captureLeft = new GenericMove( GenericPosition.e7, GenericPosition.d6 );
		GenericMove captureRight = new GenericMove( GenericPosition.e7, GenericPosition.f6 );
		assertTrue( ml.contains( captureLeft ));
		assertTrue( ml.contains( captureRight ));
	}	
	
	@Test
	public void test_CaptureFromAFile() {
		// Can only capture left
		classUnderTest = addBlackPawn( GenericPosition.a7 );
		addWhitePawn( GenericPosition.b6 );
		bm = new BoardManager( new Board( pl ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.a7, GenericPosition.b6 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_CaptureFromHFile() {
		// Can only capture right
		classUnderTest = addBlackPawn( GenericPosition.h7 );
		addWhitePawn( GenericPosition.g6 );
		bm = new BoardManager( new Board( pl ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.h7, GenericPosition.g6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_PromoteQueen() {
		classUnderTest = addBlackPawn( GenericPosition.e2 );
		bm = new BoardManager( new Board( pl ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e1, GenericChessman.QUEEN );
		assertTrue( ml.contains( expectedMove ));
	}	

	@Test
	public void test_PromoteKnight() {
		classUnderTest = addBlackPawn( GenericPosition.e2 );
		bm = new BoardManager( new Board( pl ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e1, GenericChessman.KNIGHT );
		assertTrue( ml.contains( expectedMove ));		
	}

	@Test
	public void test_PromoteBishop() {
		classUnderTest = addBlackPawn( GenericPosition.e2 );
		bm = new BoardManager( new Board( pl ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e1, GenericChessman.BISHOP );
		assertTrue( ml.contains( expectedMove ));			
	}

	@Test
	public void test_PromoteRook() {
		classUnderTest = addBlackPawn( GenericPosition.e2 );
		bm = new BoardManager( new Board( pl ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e1, GenericChessman.ROOK );
		assertTrue( ml.contains( expectedMove ));	
	}
}