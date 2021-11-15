package eubos.board;

import static org.junit.Assert.*;

import org.junit.*;

import eubos.position.PositionManager;

public class PieceListTest {

	private PieceList sut;
	
	@Before
	public void setUp() {
		sut = new PieceList(new PositionManager().getTheBoard());
	}
	
	@Test
	public void testConstruct() {
		assertTrue(sut != null);
	}
}
