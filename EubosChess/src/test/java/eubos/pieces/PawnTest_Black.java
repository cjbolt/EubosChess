package eubos.pieces;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import eubos.board.*;

import com.fluxchess.jcpi.models.*;

public class PawnTest_Black extends PawnTest {

	private LinkedList<Piece> pl;
	private Pawn classUnderTest;
	private Board testBoard;
	private GenericMove expectedMove;
	
	@Before
	public void setUp() {
		pl = new LinkedList<Piece>();
	}
	
	private Pawn addBlackPawn( GenericPosition square ) {
		Pawn newPawn = new Pawn( Piece.PieceColour.black, square );
		pl.add( newPawn );
		return newPawn;
	}
	
	private void addWhitePawn( GenericPosition square ) {
		Pawn newPawn = new Pawn( Piece.PieceColour.white, square );
		pl.add( newPawn );
	}
	
	@Test
	public void test_InitialMoveOneSquare() {
		classUnderTest = addBlackPawn( GenericPosition.e7 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e6 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_InitialMoveTwoSquares() {
		classUnderTest = addBlackPawn( GenericPosition.e7 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e5 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_InitialBlocked() {
		classUnderTest = addBlackPawn( GenericPosition.e7 );
		addWhitePawn( GenericPosition.e6 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		assertTrue( ml.isEmpty() );
	}

	@Test
	public void test_CaptureEnPassant() {
		// Black is on e4, white moves f4, then black ml contains capture en passant, exf
		classUnderTest = addBlackPawn( GenericPosition.e5 );
		addWhitePawn( GenericPosition.f2 );
		testBoard = new Board( pl );
		testBoard.performMove( new GenericMove( GenericPosition.f2, GenericPosition.f4 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e4, GenericPosition.f3 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_MoveOneSquare() {
		// After initial move, ensure that a pawn can't move 2 any longer
		classUnderTest = addBlackPawn( GenericPosition.e7 );
		addWhitePawn( GenericPosition.e2 );
		testBoard = new Board( pl );
		testBoard.performMove( new GenericMove( GenericPosition.e7, GenericPosition.e6 ));
		testBoard.performMove( new GenericMove( GenericPosition.f2, GenericPosition.f4 ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e6, GenericPosition.e5 );
		assertTrue( ml.size() == 1 );
		assertTrue( ml.contains( expectedMove ));		
	}

	@Test
	public void test_CaptureLeft() {
		classUnderTest = addBlackPawn( GenericPosition.e7 );
		addWhitePawn( GenericPosition.f6 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.f6 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_CaptureRight() {
		classUnderTest = addBlackPawn( GenericPosition.e7 );
		addWhitePawn( GenericPosition.d6 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.d6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureFork() {
		classUnderTest = addBlackPawn( GenericPosition.e7 );
		addWhitePawn( GenericPosition.d6 );
		addWhitePawn( GenericPosition.f6 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
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
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.a7, GenericPosition.b6 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_CaptureFromHFile() {
		// Can only capture right
		classUnderTest = addBlackPawn( GenericPosition.h7 );
		addWhitePawn( GenericPosition.g6 );
		testBoard = new Board( pl );
		LinkedList<GenericMove> ml = classUnderTest.generateMoveList( testBoard );
		expectedMove = new GenericMove( GenericPosition.h7, GenericPosition.g6 );
		assertTrue( ml.contains( expectedMove ));
	}	
	
	@Test
	@Ignore
	public void test_PromoteQueen() {
	}	

	@Test
	@Ignore
	public void test_PromoteKnight() {
	}

	@Test
	@Ignore
	public void test_PromoteBishop() {
	}

	@Test
	@Ignore
	public void test_PromoteRook() {
	}
}
