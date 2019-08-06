package eubos.board;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.pieces.King;
import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Piece.Colour;

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
	
	@Test
	public void testGetAsFenString() {
		Pawn pieceToPlace = new Pawn(Colour.white,testSq);
		classUnderTest.setPieceAtSquare(pieceToPlace);
		assertEquals("8/8/8/8/8/8/8/P7",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString1() {
		Piece pieceToPlace = new Pawn(Colour.white,testSq);
		classUnderTest.setPieceAtSquare(pieceToPlace);
		pieceToPlace = new King(Colour.white, GenericPosition.c1);
		classUnderTest.setPieceAtSquare(pieceToPlace);
		assertEquals("8/8/8/8/8/8/8/P1K5",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString2() {
		Piece pieceToPlace = new Pawn(Colour.white,GenericPosition.h1);
		classUnderTest.setPieceAtSquare(pieceToPlace);
		pieceToPlace = new King(Colour.white, GenericPosition.g1);
		classUnderTest.setPieceAtSquare(pieceToPlace);
		assertEquals("8/8/8/8/8/8/8/6KP",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString3() {
		Piece pieceToPlace = new Pawn(Colour.black,GenericPosition.h1);
		classUnderTest.setPieceAtSquare(pieceToPlace);
		pieceToPlace = new King(Colour.black, GenericPosition.g1);
		classUnderTest.setPieceAtSquare(pieceToPlace);
		assertEquals("8/8/8/8/8/8/8/6kp",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString4() {
		Piece pieceToPlace = new Pawn(Colour.black,GenericPosition.h8);
		classUnderTest.setPieceAtSquare(pieceToPlace);
		pieceToPlace = new King(Colour.black, GenericPosition.g8);
		classUnderTest.setPieceAtSquare(pieceToPlace);
		assertEquals("6kp/8/8/8/8/8/8/8",classUnderTest.getAsFenString());
	}
}
