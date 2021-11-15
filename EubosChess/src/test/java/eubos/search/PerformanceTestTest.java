package eubos.search;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


import eubos.position.PositionManager;

//@Ignore
public class PerformanceTestTest {

	PerformanceTest sut;
	PositionManager pm;
	int currDepth = 1;
	
	private static final long expectedNodeCount_Original[] = { 20, 400, 8902, 197281, 4865609 };
	private static final long expectedNodeCount_Kiwipete[] = { 48, 2039, 97862, 4085603, 193690690 };
	private static final long expectedNodeCount_Position3[] = { 14, 191, 2812, 43238, 674624, 11030083, 178633661 };
	private static final long expectedNodeCount_Position4[] = { 6, 264, 9467, 422333, 15833292 };
	private static final long expectedNodeCount_Position5[] = { 44, 1486, 62379, 2103487, 89941194 };
	
	@Before
	public void setUp() throws Exception {
		pm = new PositionManager();
		sut = new PerformanceTest(pm, 0);
	}
	
	public void setupPosition(String fen, int depth) {
		pm = new PositionManager(fen);
		sut = new PerformanceTest(pm, depth);
	}
	
	public void runTest(String fen, long[] expectedCounts)  {
		setupPosition(fen, 0);
		for (long expectedCount : expectedCounts) {
			sut = new PerformanceTest(pm, currDepth);
			assertEquals( expectedCount, sut.perft());
			currDepth++;
		}
	}
	
	@Test
	public void perft_depth0()  {
		assertEquals( 1, sut.perft());
	}

	@Test
	public void perft_OriginalPosition()  {
		for (long expectedCount : expectedNodeCount_Original) {
			sut = new PerformanceTest(pm, currDepth);
			assertEquals( expectedCount, sut.perft());
			currDepth++;
		}
	}

	@Test
	@Ignore
	public void perft_OriginalPosition_6()  {
		sut.setRequestedDepthPly(6);
		assertEquals( 119060324, sut.perft());
	}
	
	@Test
	public void perft_Kiwipete()  {
		runTest("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - - -", expectedNodeCount_Kiwipete);
	}
	
	@Test
	public void perft_Position5()  {
		runTest("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", expectedNodeCount_Position5);
	}
	
	@Test
	public void perft_Position4()  {
		runTest("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1", expectedNodeCount_Position4);
	}
	
	@Test
	public void perft_Position3()  {
		runTest("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - - -", expectedNodeCount_Position3);
	}
}
