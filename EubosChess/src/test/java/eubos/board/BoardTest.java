package eubos.board;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.Piece.PieceType;

public class BoardTest {
	
	private Board classUnderTest;
	private Map<GenericPosition, PieceType> pl;
	private static final GenericPosition testSq = GenericPosition.a1;
	
	@Before
	public void setUp() throws Exception {
		pl = new HashMap<GenericPosition, PieceType>();
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
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		classUnderTest.setPieceAtSquare(testSq, PieceType.WhitePawn);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
	}

	@Test
	public void testPickUpPieceAtSquare_Exists() throws InvalidPieceException {
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		classUnderTest.setPieceAtSquare(testSq, PieceType.WhitePawn);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
		PieceType pickedUpPiece = classUnderTest.pickUpPieceAtSquare(testSq);
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		assertEquals(PieceType.WhitePawn, pickedUpPiece);
	}
	
	@Test
	public void testPickUpPieceAtSquare_DoesntExist() throws InvalidPieceException {
		assertEquals(PieceType.NONE, classUnderTest.pickUpPieceAtSquare(testSq));
	}	

	@Test
	public void testGetPieceAtSquare_Exists() {
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		classUnderTest.setPieceAtSquare(testSq, PieceType.WhitePawn);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
		PieceType gotPiece = classUnderTest.getPieceAtSquare(testSq);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
		assertTrue(gotPiece.equals(PieceType.BlackPawn) || gotPiece.equals(PieceType.WhitePawn));
	}
	
	@Test
	public void testGetPieceAtSquare_DoesntExist() {
		assertTrue(classUnderTest.getPieceAtSquare(testSq)==PieceType.NONE);
	}
	
	@Test
	public void testCaptureAtSquare() {
		assertTrue(classUnderTest.pickUpPieceAtSquare(testSq)==PieceType.NONE);
	}
	
	@Test
	public void testGetAsFenString() {
		classUnderTest.setPieceAtSquare(testSq, PieceType.WhitePawn);
		assertEquals("8/8/8/8/8/8/8/P7",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString1() {
		classUnderTest.setPieceAtSquare(testSq, PieceType.WhitePawn);
		classUnderTest.setPieceAtSquare(GenericPosition.c1, PieceType.WhiteKing);
		assertEquals("8/8/8/8/8/8/8/P1K5",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString2() {
		classUnderTest.setPieceAtSquare(GenericPosition.h1, PieceType.WhitePawn);
		classUnderTest.setPieceAtSquare(GenericPosition.g1, PieceType.WhiteKing);
		assertEquals("8/8/8/8/8/8/8/6KP",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString3() {
		classUnderTest.setPieceAtSquare(GenericPosition.h1, PieceType.BlackPawn);
		classUnderTest.setPieceAtSquare(GenericPosition.g1, PieceType.BlackKing);
		assertEquals("8/8/8/8/8/8/8/6kp",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString4() {
		classUnderTest.setPieceAtSquare(GenericPosition.h8, PieceType.BlackPawn);
		classUnderTest.setPieceAtSquare(GenericPosition.g8, PieceType.BlackKing);
		assertEquals("6kp/8/8/8/8/8/8/8",classUnderTest.getAsFenString());
	}
}
