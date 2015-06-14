package eubos.main;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

public class OpeningBookTest {
	private OpeningBook classUnderTest = null;
	private ArrayList<GenericMove> prevMoves = null;
	private GenericMove nextBookMove = null;

	@Before
	public void setUp() throws Exception {
		classUnderTest = new OpeningBook();
		prevMoves = new ArrayList<GenericMove>();
	}

	@Test
	public void testGetMove() throws IllegalNotationException {
		prevMoves.add(new GenericMove("e2e4"));
		nextBookMove = classUnderTest.getMove(prevMoves);
		assertTrue(nextBookMove.equals(new GenericMove("e7e5")));
	}

}
