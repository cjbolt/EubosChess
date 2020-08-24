package eubos.search;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

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
		classUnderTest = new PrincipalContinuation(searchDepth);
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
		List<Integer> pv = classUnderTest.toPvList(0);
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
		classUnderTest.clearContinuationsBeyondPly(1);
		List<Integer> pv = classUnderTest.toPvList(0);
		assertEquals(Move.valueOf(Position.a2, Piece.WHITE_PAWN, Position.a3, Piece.NONE ),(int) pv.get(0));
		assertEquals(Move.valueOf(Position.e7, Piece.BLACK_PAWN, Position.e5, Piece.NONE ),(int) pv.get(1));
		assertEquals(2, pv.size());		
	}

	@Test
	public void testToPvList_InitialState() {
		List<Integer> pv = classUnderTest.toPvList(0);
		assertTrue(pv != null);
		assertTrue(pv.isEmpty());
	}

	@Test
	public void testUpdateFromHashHit(){
		List<Integer> source_pc = new ArrayList<Integer>();
		source_pc.add(Move.valueOf(Position.e2, Piece.NONE, Position.e4, Piece.NONE ));
		source_pc.add(Move.valueOf(Position.e7, Piece.NONE, Position.e5, Piece.NONE ));
		source_pc.add(Move.valueOf(Position.d2, Piece.NONE, Position.d4, Piece.NONE ));
		source_pc.add(Move.valueOf(Position.e5, Piece.NONE, Position.d4, Piece.NONE ));
		classUnderTest.update(3, source_pc);
		classUnderTest.update(2, Move.valueOf(Position.a7, Piece.NONE, Position.a6, Piece.NONE ));
		List<Integer> updated_pc = classUnderTest.toPvList(2);
		assertEquals(source_pc, updated_pc.subList(1, updated_pc.size()));
		assertEquals((int)Move.valueOf(Position.a7, Piece.NONE, Position.a6, Piece.NONE ), (int)updated_pc.get(0));
	}

	@Test
	@Ignore
	public void testClearAfter() {
		fail("Not yet implemented");
	}
	
	@Test
	public void testUpdateAtPly0WhenEnpty() {
		classUnderTest.update(0, Move.valueOf(Position.e2, Piece.NONE, Position.e4, Piece.NONE ));
		List<Integer> updated_pc = classUnderTest.toPvList(0);
		assertFalse(updated_pc.isEmpty());
		assertEquals((int)Move.valueOf(Position.e2, Piece.NONE, Position.e4, Piece.NONE ), (int)updated_pc.get(0));
	}
}
