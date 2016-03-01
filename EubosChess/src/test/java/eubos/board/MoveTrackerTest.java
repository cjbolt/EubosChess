package eubos.board;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece.Colour;

public class MoveTrackerTest {
	
	private MoveTracker classUnderTest;

	@Before
	public void setUp() throws Exception {
		classUnderTest = new MoveTracker();
	}

	@Test
	public void testMoveTracker() {
		assertTrue(classUnderTest!=null);
	}

	@Test
	public void testLastMoveWasCapture_NoTrackedMoves() {
		assertFalse(classUnderTest.lastMoveWasCapture());
	}

	@Test
	public void testLastMoveWasCapture_NotACapture() throws IllegalNotationException {
		TrackedMove tm = new TrackedMove(new GenericMove("a2a4"));
		classUnderTest.add(tm);		
		assertFalse(classUnderTest.lastMoveWasCapture());
	}

	@Test
	public void testLastMoveWasCapture_WasACapture() throws IllegalNotationException {
		TrackedMove tm = new TrackedMove(new GenericMove("a2b3"), new Pawn(Colour.black, GenericPosition.b3), null);
		classUnderTest.add(tm);		
		assertTrue(classUnderTest.lastMoveWasCapture());
	}
}
