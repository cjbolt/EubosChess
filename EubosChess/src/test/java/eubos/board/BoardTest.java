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
		classUnderTest.setEnPassantTargetSq( testSq );
	}
	
	@Test
	public void testGetEnPassantTargetSq_uninitialised() {
		int square = classUnderTest.getEnPassantTargetSq();
		assertTrue(square == Position.NOPOSITION);
	}

	@Test
	public void testGetEnPassantTargetSq_initialised() {
		classUnderTest.setEnPassantTargetSq(testSq);
		int square = classUnderTest.getEnPassantTargetSq();
		assertTrue(square == Position.a1);
	}	

	@Test
	public void testSetPieceAtSquare_and_squareIsEmpty() {
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		classUnderTest.setPieceAtSquare(testSq, Piece.WHITE_PAWN);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
	}

	@Test
	public void testPickUpPieceAtSquare_Exists() throws InvalidPieceException {
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		classUnderTest.setPieceAtSquare(testSq, Piece.WHITE_PAWN);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
		int pickedUpPiece = classUnderTest.pickUpPieceAtSquare(testSq);
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		assertEquals(Piece.WHITE_PAWN, pickedUpPiece);
	}
	
	@Test
	public void testPickUpPieceAtSquare_DoesntExist() throws InvalidPieceException {
		assertEquals(Piece.PIECE_NONE, classUnderTest.pickUpPieceAtSquare(testSq));
	}	

	@Test
	public void testGetPieceAtSquare_Exists() {
		assertTrue(classUnderTest.squareIsEmpty(testSq));
		classUnderTest.setPieceAtSquare(testSq, Piece.WHITE_PAWN);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
		int gotPiece = classUnderTest.getPieceAtSquare(testSq);
		assertFalse(classUnderTest.squareIsEmpty(testSq));
		assertTrue(gotPiece==Piece.BLACK_PAWN || gotPiece==Piece.WHITE_PAWN);
	}
	
	@Test
	public void testGetPieceAtSquare_DoesntExist() {
		assertTrue(classUnderTest.getPieceAtSquare(testSq)==Piece.PIECE_NONE);
	}
	
	@Test
	public void testCaptureAtSquare() {
		assertTrue(classUnderTest.pickUpPieceAtSquare(testSq)==Piece.PIECE_NONE);
	}
	
	@Test
	public void testGetAsFenString() {
		classUnderTest.setPieceAtSquare(testSq, Piece.WHITE_PAWN);
		assertEquals("8/8/8/8/8/8/8/P7",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString1() {
		classUnderTest.setPieceAtSquare(testSq, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.c1, Piece.WHITE_KING);
		assertEquals("8/8/8/8/8/8/8/P1K5",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString2() {
		classUnderTest.setPieceAtSquare(Position.h1, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.g1, Piece.WHITE_KING);
		assertEquals("8/8/8/8/8/8/8/6KP",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString3() {
		classUnderTest.setPieceAtSquare(Position.h1, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.g1, Piece.BLACK_KING);
		assertEquals("8/8/8/8/8/8/8/6kp",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testGetAsFenString4() {
		classUnderTest.setPieceAtSquare(Position.h8, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.g8, Piece.BLACK_KING);
		assertEquals("6kp/8/8/8/8/8/8/8",classUnderTest.getAsFenString());
	}
	
	@Test
	public void testOpenFile_isOpen() {
		classUnderTest.setPieceAtSquare(Position.h8, Piece.BLACK_PAWN);
		assertEquals(14, classUnderTest.getNumRankFileSquaresAvailable(Position.h8));
	}
	
	@Test
	public void testOpenFile_isClosed() {
		classUnderTest.setPieceAtSquare(Position.h7, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.h2, Piece.WHITE_ROOK);
		assertEquals(9, classUnderTest.getNumRankFileSquaresAvailable(Position.h2));
	}
	
	@Test
	public void testOpenFile_isOpen1() {
		classUnderTest.setPieceAtSquare(Position.d7, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.e2, Piece.WHITE_ROOK);
		assertEquals(14, classUnderTest.getNumRankFileSquaresAvailable(Position.e2));
	}
	
	@Test
	public void testisHalfOpenFile_isHalfOpen() {
		classUnderTest.setPieceAtSquare(Position.e7, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.e2, Piece.WHITE_ROOK);
		assertTrue(classUnderTest.isOnHalfOpenFile(GenericPosition.e2, Piece.PIECE_TABLE[Piece.WHITE_ROOK]));
	}
	
	@Test
	public void testisHalfOpenFile_isNotHalfOpen() {
		classUnderTest.setPieceAtSquare(Position.e7, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.e2, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.e1, Piece.WHITE_ROOK);
		assertFalse(classUnderTest.isOnHalfOpenFile(GenericPosition.e1, Piece.PIECE_TABLE[Piece.WHITE_ROOK]));
	}
	
	@Test
	@Ignore
	public void testisHalfOpenFile_isNotHalfOpen1() {
		classUnderTest.setPieceAtSquare(Position.e7, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.e2, Piece.BLACK_PAWN);
		classUnderTest.setPieceAtSquare(Position.e1, Piece.WHITE_ROOK);
		assertTrue(classUnderTest.isOnHalfOpenFile(GenericPosition.e1, Piece.PIECE_TABLE[Piece.WHITE_ROOK]));
	}
	
	@Test
	public void testisOnOpenDiagonal_Yes() {
		classUnderTest.setPieceAtSquare(Position.d5, Piece.BLACK_BISHOP);
		classUnderTest.setPieceAtSquare(Position.e5, Piece.WHITE_PAWN);
		assertEquals(13,classUnderTest.getNumDiagonalSquaresAvailable(Position.d5));
	}
	
	@Test
	public void testisOnOpenDiagonal_No() {
		classUnderTest.setPieceAtSquare(Position.d5, Piece.BLACK_BISHOP);
		classUnderTest.setPieceAtSquare(Position.e6, Piece.WHITE_PAWN);
		assertEquals(0,classUnderTest.getNumDiagonalSquaresAvailable(Position.d5));
	}
	
	@Test
	public void testisOnOpenDiagonal_Yes1() {
		classUnderTest.setPieceAtSquare(Position.d5, Piece.BLACK_BISHOP);
		classUnderTest.setPieceAtSquare(Position.e5, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d6, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.d4, Piece.WHITE_PAWN);
		classUnderTest.setPieceAtSquare(Position.c5, Piece.WHITE_PAWN);
		assertEquals(13,classUnderTest.getNumDiagonalSquaresAvailable(Position.d5));
	}
	
	@Test
	public void testisOnOpenDiagonal_No1() {
		classUnderTest.setPieceAtSquare(Position.a1, Piece.BLACK_BISHOP);
		classUnderTest.setPieceAtSquare(Position.h8, Piece.WHITE_PAWN);
		assertEquals(6,classUnderTest.getNumDiagonalSquaresAvailable(Position.a1));
	}
	
	@Test
	public void testisOnOpenDiagonal_Yes2() {
		classUnderTest.setPieceAtSquare(Position.a1, Piece.BLACK_BISHOP);
		classUnderTest.setPieceAtSquare(Position.a8, Piece.WHITE_PAWN);
		assertEquals(7,classUnderTest.getNumDiagonalSquaresAvailable(Position.a1));
	}
}
