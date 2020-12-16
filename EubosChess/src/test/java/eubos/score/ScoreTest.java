package eubos.score;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import eubos.search.Score;

public class ScoreTest {

	public short sut;
	
	@Before
	public void setUp() throws Exception {
		
	}

	@Test
	public void isMate_staleMate() {
		sut = (short)2000;
		assertFalse(Score.isMate(sut));
	}

	public static short WHITE_MATES_IN_1 = Short.MAX_VALUE - 1;
	
	@Test
	public void isMate_mateIn1() {
		sut = WHITE_MATES_IN_1;
		assertTrue(Score.isMate(sut));
	}
	
}
