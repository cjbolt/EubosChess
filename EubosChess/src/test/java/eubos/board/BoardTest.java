package eubos.board;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.position.Position;

import eubos.board.Board;
import eubos.board.Piece.PieceType;

public class BoardTest {
	
	private Board classUnderTest;
	private Map<Integer, PieceType> pieceMap;
	private static final int testSq = Position.a1;
	
	@Before
	public void setUp() throws Exception {
		pieceMap = new HashMap<Integer, PieceType>();
		classUnderTest = new Board(pieceMap);
	}

	@Test
	public void testBoard() {
		assertTrue(classUnderTest!=null);
	}

	@Test
	public void testSetEnPassantTargetSq() {
		classUnderTest.setEnPassantTargetSq( Position.toGenericPosition(testSq) );
	}
	
	@Test
	public void testGetEnPassantTargetSq_uninitialised() {
		int square = Position.valueOf(classUnderTest.getEnPassantTargetSq());
		assertTrue(square == Position.NOPOSITION);
	}

	@Test
	public void testGetEnPassantTargetSq_initialised() {
		classUnderTest.setEnPassantTargetSq( Position.toGenericPosition(testSq));
		int square = Position.valueOf(classUnderTest.getEnPassantTargetSq());
		assertTrue(square == Position.a1);
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
		classUnderTest.setPieceAtSquare(Position.c1, PieceType.WhiteKing);
		assertEquals("8/8/8/8/8/8/8/P1K5",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString2() {
		classUnderTest.setPieceAtSquare(Position.h1, PieceType.WhitePawn);
		classUnderTest.setPieceAtSquare(Position.g1, PieceType.WhiteKing);
		assertEquals("8/8/8/8/8/8/8/6KP",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString3() {
		classUnderTest.setPieceAtSquare(Position.h1, PieceType.BlackPawn);
		classUnderTest.setPieceAtSquare(Position.g1, PieceType.BlackKing);
		assertEquals("8/8/8/8/8/8/8/6kp",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString4() {
		classUnderTest.setPieceAtSquare(Position.h8, PieceType.BlackPawn);
		classUnderTest.setPieceAtSquare(Position.g8, PieceType.BlackKing);
		assertEquals("6kp/8/8/8/8/8/8/8",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testOpenFile_isOpen() {
		classUnderTest.setPieceAtSquare(Position.h8, PieceType.BlackPawn);
		assertEquals(14, classUnderTest.getNumRankFileSquaresAvailable(Position.h8));
	}
	
	@Test
	public void testOpenFile_isClosed() {
		classUnderTest.setPieceAtSquare(Position.h7, PieceType.BlackPawn);
		classUnderTest.setPieceAtSquare(Position.h2, PieceType.WhiteRook);
		assertEquals(9, classUnderTest.getNumRankFileSquaresAvailable(Position.h2));
	}
	
	@Test
	public void testOpenFile_isOpen1() {
		classUnderTest.setPieceAtSquare(Position.d7, PieceType.BlackPawn);
		classUnderTest.setPieceAtSquare(Position.e2, PieceType.WhiteRook);
		assertEquals(14, classUnderTest.getNumRankFileSquaresAvailable(Position.e2));
	}
	
	@Test
	public void testisHalfOpenFile_isHalfOpen() {
		classUnderTest.setPieceAtSquare(Position.e7, PieceType.BlackPawn);
		classUnderTest.setPieceAtSquare(Position.e2, PieceType.WhiteRook);
		assertTrue(classUnderTest.isOnHalfOpenFile(GenericPosition.e2, PieceType.WhiteRook));
	}
	
	@Test
	public void testisHalfOpenFile_isNotHalfOpen() {
		classUnderTest.setPieceAtSquare(Position.e7, PieceType.BlackPawn);
		classUnderTest.setPieceAtSquare(Position.e2, PieceType.WhitePawn);
		classUnderTest.setPieceAtSquare(Position.e1, PieceType.WhiteRook);
		assertFalse(classUnderTest.isOnHalfOpenFile(GenericPosition.e1, PieceType.WhiteRook));
	}
	
	@Test
	@Ignore
	public void testisHalfOpenFile_isNotHalfOpen1() {
		classUnderTest.setPieceAtSquare(Position.e7, PieceType.WhitePawn);
		classUnderTest.setPieceAtSquare(Position.e2, PieceType.BlackPawn);
		classUnderTest.setPieceAtSquare(Position.e1, PieceType.WhiteRook);
		assertTrue(classUnderTest.isOnHalfOpenFile(GenericPosition.e1, PieceType.WhiteRook));
	}
	
	@Test
	public void testisOnOpenDiagonal_Yes() {
		classUnderTest.setPieceAtSquare(Position.d5, PieceType.BlackBishop);
		classUnderTest.setPieceAtSquare(Position.e5, PieceType.WhitePawn);
		assertEquals(13,classUnderTest.getNumDiagonalSquaresAvailable(Position.d5));
	}
	
	@Test
	public void testisOnOpenDiagonal_No() {
		classUnderTest.setPieceAtSquare(Position.d5, PieceType.BlackBishop);
		classUnderTest.setPieceAtSquare(Position.e6, PieceType.WhitePawn);
		assertEquals(0,classUnderTest.getNumDiagonalSquaresAvailable(Position.d5));
	}
	
	@Test
	public void testisOnOpenDiagonal_Yes1() {
		classUnderTest.setPieceAtSquare(Position.d5, PieceType.BlackBishop);
		classUnderTest.setPieceAtSquare(Position.e5, PieceType.WhitePawn);
		classUnderTest.setPieceAtSquare(Position.d6, PieceType.WhitePawn);
		classUnderTest.setPieceAtSquare(Position.d4, PieceType.WhitePawn);
		classUnderTest.setPieceAtSquare(Position.c5, PieceType.WhitePawn);
		assertEquals(13,classUnderTest.getNumDiagonalSquaresAvailable(Position.d5));
	}
	
	@Test
	public void testisOnOpenDiagonal_No1() {
		classUnderTest.setPieceAtSquare(Position.a1, PieceType.BlackBishop);
		classUnderTest.setPieceAtSquare(Position.h8, PieceType.WhitePawn);
		assertEquals(6,classUnderTest.getNumDiagonalSquaresAvailable(Position.a1));
	}
	
	@Test
	public void testisOnOpenDiagonal_Yes2() {
		classUnderTest.setPieceAtSquare(Position.a1, PieceType.BlackBishop);
		classUnderTest.setPieceAtSquare(Position.a8, PieceType.WhitePawn);
		assertEquals(7,classUnderTest.getNumDiagonalSquaresAvailable(Position.a1));
	}
}
