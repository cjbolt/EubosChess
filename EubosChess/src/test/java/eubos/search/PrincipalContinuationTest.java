package eubos.search;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

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
	@Ignore
	public void testGetBestMove() {
		fail("Not yet implemented");
	}

	@Test
	@Ignore
	public void testToStringAfter() {
		fail("Not yet implemented");
	}

	@Test
	public void testToPvList_InitialState() {
		List<GenericMove> pv = classUnderTest.toPvList();
		assertTrue(pv != null);
		assertTrue(pv.isEmpty());
	}

	@Test
	public void testUpdate() throws IllegalNotationException {
		List<GenericMove> source_pc = new ArrayList<GenericMove>();
		source_pc.add(new GenericMove("e2e4"));
		source_pc.add(new GenericMove("e7e5"));
		source_pc.add(new GenericMove("d2d4"));
		source_pc.add(new GenericMove("e5d4"));
		classUnderTest.update(3, source_pc);
	}

	@Test
	@Ignore
	public void testClearAfter() {
		fail("Not yet implemented");
	}

}
