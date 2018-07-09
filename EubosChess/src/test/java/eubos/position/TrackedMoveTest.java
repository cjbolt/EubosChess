package eubos.position;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.pieces.King;
import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Piece.Colour;
import eubos.position.TrackedMove;

public class TrackedMoveTest {

	private TrackedMove classUnderTest;
	
	private static final GenericMove pawnCapture = new GenericMove(GenericPosition.a2,GenericPosition.b3);
	private static final GenericMove pawnAdvance = new GenericMove(GenericPosition.a2,GenericPosition.a4);
	private static final Pawn capturedBlackPawn = new Pawn(Colour.black, GenericPosition.b3);
	private static final GenericPosition targetSq = GenericPosition.b3;
	
	@Before
	public void setUp() throws Exception {
		classUnderTest = new TrackedMove(pawnCapture, capturedBlackPawn, null);
	}

	@Test
	public void testTrackedMoveGenericMove() throws IllegalNotationException {
		classUnderTest = new TrackedMove(new GenericMove("a2a4"));
		assertTrue(classUnderTest!=null);
	}

	@Test
	public void testTrackedMoveGenericMovePieceGenericPosition() throws IllegalNotationException {
		assertTrue(classUnderTest!=null);
	}

	@Test
	public void testIsCapture() throws IllegalNotationException {
		assertTrue(classUnderTest.isCapture());
	}

	@Test
	public void testGetMove() throws IllegalNotationException {
		assertTrue(classUnderTest.getMove().equals(pawnCapture));
	}

	@Test
	public void testSetMove() {
		classUnderTest.setMove(pawnAdvance);
		assertTrue(classUnderTest.getMove().equals(pawnAdvance));
	}

	@Test
	public void testGetCapturedPiece() {
		assertTrue(classUnderTest.getCapturedPiece().equals(capturedBlackPawn));
	}

	@Test
	public void testSetCapturedPiece() {
		classUnderTest.setCapturedPiece(new King(Colour.white,GenericPosition.b3));
		Piece captured = classUnderTest.getCapturedPiece();
		assertTrue(captured.isWhite());
		assertTrue(captured instanceof King);
		assertTrue(captured.getSquare().equals(GenericPosition.b3));
	}

	@Test
	public void testGetEnPassantTarget() {
		classUnderTest.setEnPassantTarget(targetSq);
		assertTrue(classUnderTest.getEnPassantTarget().equals(targetSq));
	}

	@Test
	public void testSetEnPassantTarget() {
		classUnderTest.setEnPassantTarget(targetSq);
		assertFalse(classUnderTest.getEnPassantTarget().equals(null));
	}

}
