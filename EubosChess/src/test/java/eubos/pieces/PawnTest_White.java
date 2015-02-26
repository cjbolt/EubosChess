package eubos.pieces;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.*;

import eubos.board.*;

import com.fluxchess.jcpi.models.*;

public class PawnTest_White extends PawnTest {
	
	@Test
	public void test_InitialMoveOneSquare() {
		classUnderTest = addWhitePawn( GenericPosition.e2 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e4 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_InitialMoveTwoSquares() {
		classUnderTest = addWhitePawn( GenericPosition.e2 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_InitialBlocked() {
		classUnderTest = addWhitePawn( GenericPosition.e2 );
		addBlackPawn( GenericPosition.e3 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		assertTrue( ml.isEmpty() );
	}

	@Test
	public void test_CaptureEnPassantLeft() {
		classUnderTest = addWhitePawn( GenericPosition.e5 );
		addBlackPawn( GenericPosition.f7 );
		testBoard = new Board( pl );
		testBoard.performMove( new GenericMove( GenericPosition.f7, GenericPosition.f5 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e5, GenericPosition.f6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureEnPassantLeftFalse() {
		classUnderTest = addWhitePawn( GenericPosition.e5 );
		pl.add( new Rook( Piece.PieceColour.white, GenericPosition.f7 ));
		testBoard = new Board( pl );
		testBoard.performMove( new GenericMove( GenericPosition.f7, GenericPosition.f5 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e5, GenericPosition.f6 );
		assertFalse( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureEnPassantRight() {
		classUnderTest = addWhitePawn( GenericPosition.e5 );
		addBlackPawn( GenericPosition.d7 );
		testBoard = new Board( pl );
		testBoard.performMove( new GenericMove( GenericPosition.d7, GenericPosition.d5 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e5, GenericPosition.d6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureEnPassantRightFalse() {
		classUnderTest = addWhitePawn( GenericPosition.e5 );
		pl.add( new Rook( Piece.PieceColour.white, GenericPosition.d7 ));
		testBoard = new Board( pl );
		testBoard.performMove( new GenericMove( GenericPosition.d7, GenericPosition.d5 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e5, GenericPosition.d6 );
		assertFalse( ml.contains( expectedMove ));
	}	
	
	@Test
	public void test_CaptureEnPassantFromAFile() {
		// Black is on a4, white moves b4, then black ml contains capture en passant, axb
		classUnderTest = addWhitePawn( GenericPosition.a5 );
		addBlackPawn( GenericPosition.b7 );
		testBoard = new Board( pl );
		testBoard.performMove( new GenericMove( GenericPosition.b7, GenericPosition.b5 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.a5, GenericPosition.b6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureEnPassantFromHFile() {
		// Black is on h4, white moves g4, then black ml contains capture en passant, hxg
		classUnderTest = addWhitePawn( GenericPosition.h5 );
		addBlackPawn( GenericPosition.g7 );
		testBoard = new Board( pl );
		testBoard.performMove( new GenericMove( GenericPosition.g7, GenericPosition.g5 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.h5, GenericPosition.g6 );
		assertTrue( ml.contains( expectedMove ));
	}		
	
	@Test
	public void test_MoveOneSquare() {
		// After initial move, ensure that a pawn can't move 2 any longer
		classUnderTest = addWhitePawn( GenericPosition.e2 );
		addBlackPawn( GenericPosition.f7 );
		testBoard = new Board( pl );
		testBoard.performMove( new GenericMove( GenericPosition.e2, GenericPosition.e4 ));
		testBoard.performMove( new GenericMove( GenericPosition.f7, GenericPosition.f6 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e4, GenericPosition.e5 );
		assertTrue( ml.size() == 1 );
		assertTrue( ml.contains( expectedMove ));		
	}	

	@Test
	public void test_CaptureLeft() {
		classUnderTest = addWhitePawn( GenericPosition.e2 );
		addBlackPawn( GenericPosition.f3 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.f3 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_CaptureRight() {
		classUnderTest = addWhitePawn( GenericPosition.e2 );
		addBlackPawn( GenericPosition.d3 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.d3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureFork() {
		classUnderTest = addWhitePawn( GenericPosition.e2 );
		addBlackPawn( GenericPosition.d3 );
		addBlackPawn( GenericPosition.f3 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		GenericMove captureLeft = new GenericMove( GenericPosition.e2, GenericPosition.d3 );
		GenericMove captureRight = new GenericMove( GenericPosition.e2, GenericPosition.f3 );
		assertTrue( ml.contains( captureLeft ));
		assertTrue( ml.contains( captureRight ));
	}	
	
	@Test
	public void test_CaptureFromAFile() {
		// Can only capture left
		classUnderTest = addWhitePawn( GenericPosition.a2 );
		addBlackPawn( GenericPosition.b3 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.a2, GenericPosition.b3 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_CaptureFromHFile() {
		// Can only capture right
		classUnderTest = addWhitePawn( GenericPosition.h2 );
		addBlackPawn( GenericPosition.g3 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.h2, GenericPosition.g3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_PromoteQueen() {
		classUnderTest = addWhitePawn( GenericPosition.e7 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e8, GenericChessman.QUEEN );
		assertTrue( ml.contains( expectedMove ));
	}	

	@Test
	public void test_PromoteKnight() {
		classUnderTest = addWhitePawn( GenericPosition.e7 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e8, GenericChessman.KNIGHT );
		assertTrue( ml.contains( expectedMove ));		
	}

	@Test
	public void test_PromoteBishop() {
		classUnderTest = addWhitePawn( GenericPosition.e7 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e8, GenericChessman.BISHOP );
		assertTrue( ml.contains( expectedMove ));			
	}

	@Test
	public void test_PromoteRook() {
		classUnderTest = addWhitePawn( GenericPosition.e7 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e8, GenericChessman.ROOK );
		assertTrue( ml.contains( expectedMove ));	
	}
}

