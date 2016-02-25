package eubos.board;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.pieces.Piece;

public class BoardTest {
	
	private Board classUnderTest;
	private LinkedList<Piece> pl;
	
	@Before
	public void setUp() throws Exception {
		pl = new LinkedList<Piece>();
		classUnderTest = new Board(pl);
	}

	@Test
	public void testBoard() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetEnPassantTargetSq() {
		classUnderTest.setEnPassantTargetSq( GenericPosition.a1 );
	}
	
	@Test
	public void testGetEnPassantTargetSq_uninitialised() {
		GenericPosition square = classUnderTest.getEnPassantTargetSq();
		assertTrue(square == null);
	}

	@Test
	public void testGetEnPassantTargetSq_initialised() {
		classUnderTest.setEnPassantTargetSq( GenericPosition.a1 );
		GenericPosition square = classUnderTest.getEnPassantTargetSq();
		assertTrue(square == GenericPosition.a1);
	}	

	@Test
	public void testSetPieceAtSquare() {
		fail("Not yet implemented");
	}

	@Test
	public void testPickUpPieceAtSquare() {
		fail("Not yet implemented");
	}

	@Test
	public void testCaptureAtSquare() {
		fail("Not yet implemented");
	}

	@Test
	public void testSquareIsEmpty() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetPieceAtSquare() {
		fail("Not yet implemented");
	}

	@Test
	public void testIterator() {
		fail("Not yet implemented");
	}

	@Test
	public void testIterateColour() {
		fail("Not yet implemented");
	}

}
