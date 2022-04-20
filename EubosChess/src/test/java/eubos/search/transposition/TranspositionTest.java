package eubos.search.transposition;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TranspositionTest {

	@Test
	void testValueOfByteShortByteGenericMove() {
		fail("Not yet implemented");
	}

	@Test
	void testValueOfByteShortByteIntListOfInteger() {
		fail("Not yet implemented");
	}

	@Test
	void testGetDepthSearchedInPly() {
		fail("Not yet implemented");
	}

	@Test
	void testSetDepthSearchedInPly() {
		fail("Not yet implemented");
	}

	@Test
	void testGetType() {
		fail("Not yet implemented");
	}

	@Test
	void testSetType() {
		fail("Not yet implemented");
	}

	@Test
	void testReadbackScore() {
		long trans = Transposition.setScore(0L, (short)100);
		assertEquals(100, Transposition.getScore(trans));
	}

	@Test
	void testModifyReadbackScore() {
		long trans = Transposition.setScore(0xFFL, (short)100);
		assertEquals(100, Transposition.getScore(trans));
		assertEquals((100<<32)+0xFFL, trans);
	}

	@Test
	void testGetBestMoveLongBoard() {
		fail("Not yet implemented");
	}

	@Test
	void testGetBestMoveLong() {
		fail("Not yet implemented");
	}

	@Test
	void testSetBestMove() {
		fail("Not yet implemented");
	}

	@Test
	void testReport() {
		fail("Not yet implemented");
	}

	@Test
	void testCheckUpdate() {
		fail("Not yet implemented");
	}

	@Test
	void testGetPv() {
		fail("Not yet implemented");
	}

}
