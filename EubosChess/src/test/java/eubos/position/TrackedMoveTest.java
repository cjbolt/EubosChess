package eubos.position;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.Piece;

public class TrackedMoveTest {

	private TrackedMove classUnderTest;
	
	private static final int pawnCapture = Move.toMove(new GenericMove(GenericPosition.a2,GenericPosition.b3));
	private static final int pawnAdvance = Move.toMove(new GenericMove(GenericPosition.a2,GenericPosition.a4));
	private static final CaptureData capturedBlackPawn = new CaptureData(Piece.BLACK_PAWN, Position.b3);
	private static final GenericPosition targetSq = GenericPosition.b3;
	
	@Before
	public void setUp() throws Exception {
		classUnderTest = new TrackedMove(pawnCapture, capturedBlackPawn, Position.NOPOSITION, 0xF);
	}

	@Test
	public void testTrackedMoveGenericMove() throws IllegalNotationException {
		classUnderTest = new TrackedMove(Move.toMove(new GenericMove("a2a4")));
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
		assertTrue(classUnderTest.getMove() == pawnCapture);
	}

	@Test
	public void testSetMove() {
		classUnderTest.setMove(pawnAdvance);
		assertTrue(classUnderTest.getMove()==pawnAdvance);
	}

	@Test
	public void testGetCapturedPiece() {
		assertTrue(classUnderTest.getCaptureData().equals(capturedBlackPawn));
	}

	@Test
	public void testSetCapturedPiece() {
		classUnderTest.setCaptureData(new CaptureData(Piece.WHITE_KING, Position.b3));
		CaptureData captured = classUnderTest.getCaptureData();
		assertTrue(captured.target == Piece.WHITE_KING);
		assertTrue(captured.square==Position.b3);
	}

	@Test
	public void testGetEnPassantTarget() {
		classUnderTest.setEnPassantTarget(Position.valueOf(targetSq));
		assertTrue(Position.toGenericPosition(classUnderTest.getEnPassantTarget()).equals(targetSq));
	}

	@Test
	public void testSetEnPassantTarget() {
		classUnderTest.setEnPassantTarget(Position.valueOf(targetSq));
		assertFalse(Position.toGenericPosition(classUnderTest.getEnPassantTarget()).equals(null));
	}

}
