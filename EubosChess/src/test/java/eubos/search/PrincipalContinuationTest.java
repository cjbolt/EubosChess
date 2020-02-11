package eubos.search;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.Piece;
import eubos.position.Move;
import eubos.position.Position;
import eubos.search.PrincipalContinuation;

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
		List<GenericMove> pv = classUnderTest.toPvList();
		assertEquals(new GenericMove("a2a3"), pv.get(0));
		assertEquals(new GenericMove("e7e5"), pv.get(1));
		assertEquals(new GenericMove("d2d4"), pv.get(2));
		assertEquals(new GenericMove("e5d4"), pv.get(3));		
	}

	@Test
	@Ignore
	public void test_clearContinuationBeyondPly() throws IllegalNotationException {
		classUnderTest.update(3, Move.valueOf(Position.e5, Piece.BLACK_PAWN, Position.d4, Piece.WHITE_PAWN ));
		classUnderTest.update(2, Move.valueOf(Position.d2, Piece.WHITE_PAWN, Position.d4, Piece.NONE ));
		classUnderTest.update(1, Move.valueOf(Position.e7, Piece.BLACK_PAWN, Position.e5, Piece.NONE ));
		classUnderTest.update(0, Move.valueOf(Position.a2, Piece.WHITE_PAWN, Position.a3, Piece.NONE ));
		classUnderTest.clearContinuationsBeyondPly(1);
		List<GenericMove> pv = classUnderTest.toPvList();
		assertEquals(new GenericMove("a2a3"), pv.get(0));
		assertEquals(new GenericMove("e7e5"), pv.get(1));
		assertEquals(2, pv.size());		
	}

	@Test
	public void testToPvList_InitialState() {
		List<GenericMove> pv = classUnderTest.toPvList();
		assertTrue(pv != null);
		assertTrue(pv.isEmpty());
	}

	@Test
	public void testUpdate(){
		List<Integer> source_pc = new ArrayList<Integer>();
		source_pc.add(Move.valueOf(Position.e2, Piece.NONE, Position.e4, Piece.NONE ));
		source_pc.add(Move.valueOf(Position.e7, Piece.NONE, Position.e5, Piece.NONE ));
		source_pc.add(Move.valueOf(Position.d2, Piece.NONE, Position.d4, Piece.NONE ));
		source_pc.add(Move.valueOf(Position.e5, Piece.NONE, Position.d4, Piece.NONE ));
		classUnderTest.update(3, source_pc);
	}

	@Test
	@Ignore
	public void testClearAfter() {
		fail("Not yet implemented");
	}

}
