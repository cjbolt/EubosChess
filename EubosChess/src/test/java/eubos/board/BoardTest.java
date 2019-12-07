package eubos.board;

import static org.junit.Assert.*;

import java.util.EnumMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.Piece.PieceType;

public class BoardTest {
	
	private Board classUnderTest;
	private Map<GenericPosition, PieceType> pieceMap;
	private static final GenericPosition testSq = GenericPosition.a1;
	
	@Before
	public void setUp() throws Exception {
		pieceMap = new EnumMap<GenericPosition, PieceType>(GenericPosition.class);
		classUnderTest = new Board(pieceMap);
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
	
	@Test
	public void testOpenFile_isOpen() {
		classUnderTest.setPieceAtSquare(GenericPosition.h8, PieceType.BlackPawn);
		assertTrue(classUnderTest.isOnOpenFile(GenericPosition.h8));
	}
	
	@Test
	public void testOpenFile_isClosed() {
		classUnderTest.setPieceAtSquare(GenericPosition.h7, PieceType.BlackPawn);
		classUnderTest.setPieceAtSquare(GenericPosition.h2, PieceType.WhiteRook);
		assertFalse(classUnderTest.isOnOpenFile(GenericPosition.h2));
	}
	
	@Test
	public void testOpenFile_isOpen1() {
		classUnderTest.setPieceAtSquare(GenericPosition.d7, PieceType.BlackPawn);
		classUnderTest.setPieceAtSquare(GenericPosition.e2, PieceType.WhiteRook);
		assertTrue(classUnderTest.isOnOpenFile(GenericPosition.e2));
	}
	
	@Test
	public void testisHalfOpenFile_isHalfOpen() {
		classUnderTest.setPieceAtSquare(GenericPosition.e7, PieceType.BlackPawn);
		classUnderTest.setPieceAtSquare(GenericPosition.e2, PieceType.WhiteRook);
		assertTrue(classUnderTest.isOnHalfOpenFile(GenericPosition.e2, PieceType.WhiteRook));
	}
	
	@Test
	public void testisHalfOpenFile_isNotHalfOpen() {
		classUnderTest.setPieceAtSquare(GenericPosition.e7, PieceType.BlackPawn);
		classUnderTest.setPieceAtSquare(GenericPosition.e2, PieceType.WhitePawn);
		classUnderTest.setPieceAtSquare(GenericPosition.e1, PieceType.WhiteRook);
		assertFalse(classUnderTest.isOnHalfOpenFile(GenericPosition.e1, PieceType.WhiteRook));
	}
	
	@Test
	@Ignore
	public void testisHalfOpenFile_isNotHalfOpen1() {
		classUnderTest.setPieceAtSquare(GenericPosition.e7, PieceType.WhitePawn);
		classUnderTest.setPieceAtSquare(GenericPosition.e2, PieceType.BlackPawn);
		classUnderTest.setPieceAtSquare(GenericPosition.e1, PieceType.WhiteRook);
		assertTrue(classUnderTest.isOnHalfOpenFile(GenericPosition.e1, PieceType.WhiteRook));
	}
	
	@Test
	public void testisOnOpenDiagonal_Yes() {
		classUnderTest.setPieceAtSquare(GenericPosition.d5, PieceType.BlackBishop);
		classUnderTest.setPieceAtSquare(GenericPosition.e5, PieceType.WhitePawn);
		assertEquals(4,classUnderTest.isOnOpenDiagonal(GenericPosition.d5));
	}
	
	@Test
	public void testisOnOpenDiagonal_No() {
		classUnderTest.setPieceAtSquare(GenericPosition.d5, PieceType.BlackBishop);
		classUnderTest.setPieceAtSquare(GenericPosition.e6, PieceType.WhitePawn);
		assertEquals(0,classUnderTest.isOnOpenDiagonal(GenericPosition.d5));
	}
	
	@Test
	public void testisOnOpenDiagonal_Yes1() {
		classUnderTest.setPieceAtSquare(GenericPosition.d5, PieceType.BlackBishop);
		classUnderTest.setPieceAtSquare(GenericPosition.e5, PieceType.WhitePawn);
		classUnderTest.setPieceAtSquare(GenericPosition.d6, PieceType.WhitePawn);
		classUnderTest.setPieceAtSquare(GenericPosition.d4, PieceType.WhitePawn);
		classUnderTest.setPieceAtSquare(GenericPosition.c5, PieceType.WhitePawn);
		assertEquals(4,classUnderTest.isOnOpenDiagonal(GenericPosition.d5));
	}
	
	@Test
	public void testisOnOpenDiagonal_No1() {
		classUnderTest.setPieceAtSquare(GenericPosition.a1, PieceType.BlackBishop);
		classUnderTest.setPieceAtSquare(GenericPosition.h8, PieceType.WhitePawn);
		assertEquals(6,classUnderTest.isOnOpenDiagonal(GenericPosition.a1));
	}
	
	@Test
	public void testisOnOpenDiagonal_Yes2() {
		classUnderTest.setPieceAtSquare(GenericPosition.a1, PieceType.BlackBishop);
		classUnderTest.setPieceAtSquare(GenericPosition.a8, PieceType.WhitePawn);
		assertEquals(7,classUnderTest.isOnOpenDiagonal(GenericPosition.a1));
	}
}
