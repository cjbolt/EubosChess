package eubos.search;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
		List<Integer> pv = Arrays.stream(classUnderTest.toPvList(0)).boxed().collect(Collectors.toList());
		assertEquals(Move.valueOf(Position.a2, Piece.WHITE_PAWN, Position.a3, Piece.NONE ),(int) pv.get(0));
		assertEquals(Move.valueOf(Position.e7, Piece.BLACK_PAWN, Position.e5, Piece.NONE ),(int) pv.get(1));
		assertEquals(Move.valueOf(Position.d2, Piece.WHITE_PAWN, Position.d4, Piece.NONE ), (int)pv.get(2));
		assertEquals(Move.valueOf(Position.e5, Piece.BLACK_PAWN, Position.d4, Piece.WHITE_PAWN ), (int)pv.get(3));		
	}

	@Test
	@Ignore
	public void test_clearContinuationBeyondPly() throws IllegalNotationException {
		classUnderTest.update(3, Move.valueOf(Position.e5, Piece.BLACK_PAWN, Position.d4, Piece.WHITE_PAWN ));
		classUnderTest.update(2, Move.valueOf(Position.d2, Piece.WHITE_PAWN, Position.d4, Piece.NONE ));
		classUnderTest.update(1, Move.valueOf(Position.e7, Piece.BLACK_PAWN, Position.e5, Piece.NONE ));
		classUnderTest.update(0, Move.valueOf(Position.a2, Piece.WHITE_PAWN, Position.a3, Piece.NONE ));
		classUnderTest.clearContinuationBeyondPly(1);
		List<Integer> pv = Arrays.stream(classUnderTest.toPvList(0)).boxed().collect(Collectors.toList());
		assertEquals(Move.valueOf(Position.a2, Piece.WHITE_PAWN, Position.a3, Piece.NONE ),(int) pv.get(0));
		assertEquals(Move.valueOf(Position.e7, Piece.BLACK_PAWN, Position.e5, Piece.NONE ),(int) pv.get(1));
		assertEquals(2, pv.size());		
	}

	@Test
	public void testToPvList_InitialState() {
		List<Integer> pv = Arrays.stream(classUnderTest.toPvList(0)).boxed().collect(Collectors.toList());
		assertTrue(pv != null);
		assertTrue(pv.isEmpty());
	}

	@Test
	@Ignore
	public void testClearAfter() {
		fail("Not yet implemented");
	}
	
	@Test
	public void testUpdateAtPly0WhenEnpty() {
		classUnderTest.update(0, Move.valueOf(Position.e2, Piece.NONE, Position.e4, Piece.NONE ));
		List<Integer> updated_pc = Arrays.stream(classUnderTest.toPvList(0)).boxed().collect(Collectors.toList());
		assertFalse(updated_pc.isEmpty());
		assertEquals((int)Move.valueOf(Position.e2, Piece.NONE, Position.e4, Piece.NONE ), (int)updated_pc.get(0));
	}
}
