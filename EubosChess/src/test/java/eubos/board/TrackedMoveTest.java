package eubos.board;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece.Colour;

public class TrackedMoveTest {

	private TrackedMove classUnderTest;
	
	@Before
	public void setUp() throws Exception {
		classUnderTest = new TrackedMove(null);
	}

	@Test
	public void testTrackedMoveGenericMove() throws IllegalNotationException {
		classUnderTest = new TrackedMove(new GenericMove("a2a4"));
		assertTrue(classUnderTest!=null);
	}

	@Test
	public void testTrackedMoveGenericMovePieceGenericPosition() throws IllegalNotationException {
		classUnderTest = new TrackedMove(new GenericMove("a2b3"), new Pawn(Colour.black, GenericPosition.b3), null);
		assertTrue(classUnderTest!=null);
	}

	@Test
	public void testIsCapture() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMove() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetMove() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCapturedPiece() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetCapturedPiece() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetEnPassantTarget() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetEnPassantTarget() {
		fail("Not yet implemented");
	}

}
