package eubos.position;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Piece.Colour;
import eubos.position.Board;
import eubos.position.InvalidPieceException;

public class BoardTest {
	
	private Board classUnderTest;
	private LinkedList<Piece> pl;
	private static final GenericPosition testSq = GenericPosition.a1;
	
	@Before
	public void setUp() throws Exception {
		pl = new LinkedList<Piece>();
		classUnderTest = new Board(pl);
	}

	@Test
	public void testBoard() {
		assertTrue(classUnderTest!=null);
	}

	@Test
	public void testSetEnPassantTargetSq() {
		classUnderTest.setEnPassantTargetSq( testSq );
	}
	
	@Test
	public void testGetEnPassantTargetSq_uninitialised() {
		GenericPosition square = classUnderTest.getEnPassantTargetSq();
		assertTrue(square == null);
	}

	@Test
	public void testGetEnPassantTargetSq_initialised() {
		classUnderTest.setEnPassantTargetSq( testSq );
		GenericPosition square = classUnderTest.getEnPassantTargetSq();
		assertTrue(square == GenericPosition.a1);
	}	

	@Test
	public void testSetPieceAtSquare_and_squareIsEmpty() {
		Pawn pieceToPlace = new Pawn(Colour.white,testSq);
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		classUnderTest.setPieceAtSquare(pieceToPlace);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
	}

	@Test
	public void testPickUpPieceAtSquare_Exists() throws InvalidPieceException {
		Pawn pieceToPlace = new Pawn(Colour.white,testSq);
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		classUnderTest.setPieceAtSquare(pieceToPlace);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
		Piece pickedUpPiece = classUnderTest.pickUpPieceAtSquare(testSq);
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		assertTrue(pickedUpPiece instanceof Pawn);
	}
	
	@Test (expected=InvalidPieceException.class)
	public void testPickUpPieceAtSquare_DoesntExist() throws InvalidPieceException {
		classUnderTest.pickUpPieceAtSquare(testSq);
	}	

	@Test
	public void testGetPieceAtSquare_Exists() {
		Pawn pieceToPlace = new Pawn(Colour.white,testSq);
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		classUnderTest.setPieceAtSquare(pieceToPlace);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
		Piece gotPiece = classUnderTest.getPieceAtSquare(testSq);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
		assertTrue(gotPiece instanceof Pawn);
	}
	
	@Test
	public void testGetPieceAtSquare_DoesntExist() {
		assertTrue(classUnderTest.getPieceAtSquare(testSq)==null);
	}
	
	@Test
	public void testCaptureAtSquare() {
		assertTrue(classUnderTest.captureAtSquare(testSq)==null);
	}
}
