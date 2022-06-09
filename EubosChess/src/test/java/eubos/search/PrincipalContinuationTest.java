package eubos.search;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.Piece;
import eubos.position.Move;
import eubos.position.Position;

public class PrincipalContinuationTest {

	private PrincipalContinuation classUnderTest;
	private static final int searchDepth = 4;
	
	@Before
	public void setUp() {
		classUnderTest = new PrincipalContinuation(searchDepth, new SearchDebugAgent(0, true));
	}
	
	@Test
	public void testPrincipalContinuation() {
		assertTrue(classUnderTest != null);
	}

	@Test
	public void test_update()  throws IllegalNotationException {
		classUnderTest.update(3, Move.valueOf(Position.e5, Piece.BLACK_PAWN, Position.d4, Piece.WHITE_PAWN ));
		classUnderTest.update(2, Move.valueOf(Position.d2, Piece.WHITE_PAWN, Position.d4, Piece.NONE ));
		classUnderTest.update(1, Move.valueOf(Position.e7, Piece.BLACK_PAWN, Position.e5, Piece.NONE ));
		classUnderTest.update(0, Move.valueOf(Position.a2, Piece.WHITE_PAWN, Position.a3, Piece.NONE ));
		assertEquals(Move.valueOf(Position.a2, Piece.WHITE_PAWN, Position.a3, Piece.NONE ),(int) classUnderTest.getBestMove((byte)0));
		assertEquals(Move.valueOf(Position.e7, Piece.BLACK_PAWN, Position.e5, Piece.NONE ),(int) classUnderTest.getBestMove((byte)1));
		assertEquals(Move.valueOf(Position.d2, Piece.WHITE_PAWN, Position.d4, Piece.NONE ), (int)classUnderTest.getBestMove((byte)2));
		assertEquals(Move.valueOf(Position.e5, Piece.BLACK_PAWN, Position.d4, Piece.WHITE_PAWN ), (int)classUnderTest.getBestMove((byte)3));		
	}

	@Test
	@Ignore
	public void test_clearContinuationBeyondPly() throws IllegalNotationException {
		classUnderTest.update(3, Move.valueOf(Position.e5, Piece.BLACK_PAWN, Position.d4, Piece.WHITE_PAWN ));
		classUnderTest.update(2, Move.valueOf(Position.d2, Piece.WHITE_PAWN, Position.d4, Piece.NONE ));
		classUnderTest.update(1, Move.valueOf(Position.e7, Piece.BLACK_PAWN, Position.e5, Piece.NONE ));
		classUnderTest.update(0, Move.valueOf(Position.a2, Piece.WHITE_PAWN, Position.a3, Piece.NONE ));
		classUnderTest.clearContinuationBeyondPly(1);
		assertEquals(Move.valueOf(Position.a2, Piece.WHITE_PAWN, Position.a3, Piece.NONE ),(int) classUnderTest.getBestMove((byte)0));
		assertEquals(Move.valueOf(Position.e7, Piece.BLACK_PAWN, Position.e5, Piece.NONE ),(int) classUnderTest.getBestMove((byte)1));	
	}

	@Test
	@Ignore
	public void testToPvList_InitialState() {
	}

	@Test
	@Ignore
	public void testClearAfter() {
		fail("Not yet implemented");
	}
	
	@Test
	public void testUpdateAtPly0WhenEnpty() {
		classUnderTest.update(0, Move.valueOf(Position.e2, Piece.NONE, Position.e4, Piece.NONE ));
		assertEquals((int)Move.valueOf(Position.e2, Piece.NONE, Position.e4, Piece.NONE ), (int)classUnderTest.getBestMove((byte)0));
	}
}
