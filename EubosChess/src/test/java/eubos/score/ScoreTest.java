package eubos.score;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import eubos.search.Score;

public class ScoreTest {

	public Score sut;
	
	@Before
	public void setUp() throws Exception {
		
	}

	@Test
	public void isMate_staleMate() {
		sut = new Score((short)2000, Score.exact);
		assertFalse(sut.isMate());
	}

	public static short WHITE_MATES_IN_1 = Short.MAX_VALUE - 1;
	
	@Test
	public void isMate_mateIn1() {
		sut = new Score(WHITE_MATES_IN_1, Score.exact);
		assertTrue(sut.isMate());
	}
	
}
