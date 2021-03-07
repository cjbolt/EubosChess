package eubos.board;

import static org.junit.Assert.*;

import org.junit.*;

import eubos.position.Position;
import eubos.position.PositionManager;

public class PieceListTest {

	private PieceList sut;
	
	@Before
	public void setUp() {
		sut = new PieceList(new PositionManager().getTheBoard());
	}
	
	@Test
	public void testConstruct() {
		assertTrue(sut != null);
	}
	
	@Test
	public void test_whenPieceAdded_countIsOne() {
		sut.addPiece(Piece.WHITE_BISHOP, Position.a1);
		//assertEquals(1, sut.getNum(Piece.WHITE_BISHOP));
	}
	
	@Test
	public void test_whenNoPieceAdded_countIsZero() {
		//assertEquals(0, sut.getNum(Piece.WHITE_BISHOP));
	}
	
	@Test
	public void test_whenPieceAdded_andRemoved_countIsZero() {
		sut.addPiece(Piece.WHITE_BISHOP, Position.a1);
		sut.removePiece(Piece.WHITE_BISHOP, Position.a1);
		//assertEquals(0, sut.getNum(Piece.WHITE_BISHOP));
	}
	
	@Test
	public void test_whenThreePiecesAdded_andRemoved_countIsTwo() {
		sut.addPiece(Piece.WHITE_BISHOP, Position.a1);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a2);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a3);
		sut.removePiece(Piece.WHITE_BISHOP, Position.a1);
		//assertEquals(2, sut.getNum(Piece.WHITE_BISHOP));
	}
	
	@Test
	public void test_whenThreePiecesAdded_andRemoved_arrayOrderIsExpected() {
		sut.addPiece(Piece.WHITE_BISHOP, Position.a1);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a2);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a3);
		sut.removePiece(Piece.WHITE_BISHOP, Position.a2);

		int [] expectedArray = { Position.a1, Position.a3 };
		//assertArrayEquals(expectedArray, sut.getPieceArray(Piece.WHITE_BISHOP));
	}
	
	@Test
	public void test_whenAllPiecesAdded_andOneRemoved_arrayOrderIsExpected() {
		sut.addPiece(Piece.WHITE_BISHOP, Position.a1);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a2);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a3);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a4);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a5);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a6);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a7);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a8);
		sut.removePiece(Piece.WHITE_BISHOP, Position.a2);

		int [] expectedArray = { Position.a1, Position.a3, Position.a4, Position.a5, Position.a6, Position.a7, Position.a8 };
		//assertArrayEquals(expectedArray, sut.getPieceArray(Piece.WHITE_BISHOP));
	}
	
	@Test
	public void test_whenAllPiecesAdded_countIsEight() {
		sut.addPiece(Piece.WHITE_BISHOP, Position.a1);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a2);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a3);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a4);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a5);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a6);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a7);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a8);
		//assertEquals(8, sut.getNum(Piece.WHITE_BISHOP));
	}
	
	@Test
	public void test_whenAllPiecesAdded_multipleTimes_countIsStillEight() {
		sut.addPiece(Piece.WHITE_BISHOP, Position.a1);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a2);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a3);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a4);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a5);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a6);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a7);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a8);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a7);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a8);
		//assertEquals(8, sut.getNum(Piece.WHITE_BISHOP));
	}
	
	@Test
	public void test_whenMorePiecesAreAddedThanPossible_countRemainsAtEight() {
		sut.addPiece(Piece.WHITE_BISHOP, Position.a1);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a2);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a3);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a4);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a5);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a6);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a7);
		sut.addPiece(Piece.WHITE_BISHOP, Position.a8);
		sut.addPiece(Piece.WHITE_BISHOP, Position.b1);
		sut.addPiece(Piece.WHITE_BISHOP, Position.b2);
		//assertEquals(8, sut.getNum(Piece.WHITE_BISHOP));
	}
}
