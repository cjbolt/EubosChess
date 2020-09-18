package eubos.search;

import static org.junit.Assert.*;

import java.util.logging.*;

import org.junit.Before;
import org.junit.Test;

import eubos.main.EubosEngineMain;
import eubos.position.PositionManager;

public class DrawCheckerTest {
	private DrawChecker sut;
	private PositionManager pm;

	private void setupPosition(String fen) {
		pm = new PositionManager(fen);
	}
	
	@Before
	public void setUp() {
		EubosEngineMain.logger.setLevel(Level.OFF);
		sut = new DrawChecker();
	}
	
	@Test
	public void test_CanIncrementCount() {
		setupPosition("5r1k/pp5p/6p1/1N2q3/2P1P1n1/1P6/P2Q2PP/3R2K1 w - - 0 1");
		Long hashCode = pm.getHash();
		assertNull(sut.getPositionReachedCount(hashCode));
		sut.incrementPositionReachedCount(hashCode);
		assertEquals(1, (int)sut.getPositionReachedCount(hashCode));
		sut.incrementPositionReachedCount(hashCode);
		assertEquals(2, (int)sut.getPositionReachedCount(hashCode));
	}
	
	@Test
	public void test_DetectThreeFoldRepetitionDraw() {
		setupPosition("5r1k/pp5p/6p1/1N2q3/2P1P1n1/1P6/P2Q2PP/3R2K1 w - - 0 1");
		Long hashCode = pm.getHash();
		sut.incrementPositionReachedCount(hashCode);
		sut.incrementPositionReachedCount(hashCode);
		sut.incrementPositionReachedCount(hashCode);
		assertTrue(sut.isPositionDraw(hashCode));
	}
	
	@Test
	public void test_DetectThreeFoldRepetitionDraw_NotADraw() {
		setupPosition("5r1k/pp5p/6p1/1N2q3/2P1P1n1/1P6/P2Q2PP/3R2K1 w - - 0 1");
		Long hashCode = pm.getHash();
		sut.incrementPositionReachedCount(hashCode);
		assertFalse(sut.isPositionDraw(hashCode));
	}
}
