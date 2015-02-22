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
	private Pawn pieceUnderTest;
	private Board testPosition;
	private GenericMove expectedMove;
	
	@Before
	public void setUp() {
		pl = new LinkedList<Piece>();
	}
	
	private Pawn addBlackPawnE7() {
		Pawn e7Pawn = new Pawn( Piece.PieceColour.black, GenericPosition.e7 );
		pl.add( e7Pawn );
		return e7Pawn;
	}
	
	@Test
	public void testInitial_MoveOneSquare() {
		pieceUnderTest = addBlackPawnE7();
		testPosition = new Board( pl );
		LinkedList<GenericMove> ml = pieceUnderTest.generateMoveList( testPosition );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e6 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void testInitial_MoveTwoSquares() {
		pieceUnderTest = addBlackPawnE7();
		testPosition = new Board( pl );
		LinkedList<GenericMove> ml = pieceUnderTest.generateMoveList( testPosition );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e5 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void testInitial_Blocked() {
		pieceUnderTest = addBlackPawnE7();
		pl.add( new Pawn( Piece.PieceColour.black, GenericPosition.e6 ));
		testPosition = new Board( pl );
		LinkedList<GenericMove> ml = pieceUnderTest.generateMoveList( testPosition );
		assertTrue( ml.isEmpty() );
	}

	@Test
	@Ignore
	public void testInitial_EnPassant() {
	}

	@Test
	@Ignore
	public void test_MoveOneSquare() {
	}

	@Test
	@Ignore
	public void test_CaptureLeft() {
	}

	@Test
	@Ignore
	public void test_CaptureRight() {
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
