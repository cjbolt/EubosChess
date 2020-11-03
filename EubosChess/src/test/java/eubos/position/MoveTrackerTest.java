package eubos.position;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.Piece;

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
}
