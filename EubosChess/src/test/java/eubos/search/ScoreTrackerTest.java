package eubos.search;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ScoreTrackerTest {

	private ScoreTracker classUnderTest;
	private static final boolean isWhite = true;
	private static final int searchDepth = 4;
	
	@Before
	public void setUp() throws Exception {
		classUnderTest = new ScoreTracker(searchDepth, isWhite);
	}

	@Test
	public void testScoreTracker() {
		assertNotNull(classUnderTest);
	}

	@Test
	public void testOnMoveIsWhitePly0() {
		assertTrue(classUnderTest.onMoveIsWhite(0));
	}

	@Test
	public void testOnMoveIsBlackPly1() {
		assertFalse(classUnderTest.onMoveIsWhite(1));
	}

	@Test
	public void testOnMoveIsWhitePly2() {
		assertTrue(classUnderTest.onMoveIsWhite(2));
	}
	
	@Test
	public void testOnMoveIsBlackPly3() {
		assertFalse(classUnderTest.onMoveIsWhite(3));
	}
	
	@Test
	public void testIsAlphaBetaCutOff_Max() {
		assertFalse(classUnderTest.isAlphaBetaCutOff(1, Integer.MAX_VALUE, 20));
	}
	
	@Test
	public void testIsAlphaBetaCutOff_Min() {
		assertFalse(classUnderTest.isAlphaBetaCutOff(1, Integer.MIN_VALUE, 20));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testIsAlphaBetaCutOff_plyZero_Exception() {
		classUnderTest.isAlphaBetaCutOff(0, -100, 20);
	}
	
	@Test
	public void testIsAlphaBetaCutOff_PlyOne() {
		assertFalse(classUnderTest.isAlphaBetaCutOff(1, -100, 20));
	}
		
	@Test
	public void testIsAlphaBetaCutOff_plyTwo() {
		assertTrue(classUnderTest.isAlphaBetaCutOff(2, -100, 20));
	}
	
	@Test
	public void testIsAlphaBetaCutOff_PlyThree() {
		assertFalse(classUnderTest.isAlphaBetaCutOff(3, -100, 20));
	}
	
	@Test
	public void testIsBackUpRequired_ItIs() {
		initialiseToSearchDepth();
		assertTrue(classUnderTest.isBackUpRequired(3, 9));
	}
	
	@Test
	public void testIsBackUpRequired_ItIsnt() {
		initialiseToSearchDepth();
		classUnderTest.setBackedUpScoreAtPly(3, 9);
		assertFalse(classUnderTest.isBackUpRequired(3, 15));
	}
	
	@Test
	public void testIsBackUpRequired_ItIs2() {
		initialiseToSearchDepth();
		classUnderTest.isBackUpRequired(3, 9);
		classUnderTest.setBackedUpScoreAtPly(3, 9);
		assertTrue(classUnderTest.isBackUpRequired(3, 4));
	}
	
	@Test
	public void testIsBackUpRequired_Case1() {
		initialiseToSearchDepth();
		classUnderTest.isBackUpRequired(3, 9);
		classUnderTest.setBackedUpScoreAtPly(3, 9);
		classUnderTest.isBackUpRequired(3, 4);
		classUnderTest.setBackedUpScoreAtPly(3, 4);
		assertTrue(classUnderTest.isBackUpRequired(2,4));
		assertFalse(classUnderTest.isAlphaBetaCutOff(2, -100, 4));
	}	
	
	@Test
	public void testSetProvisionalScoreAtPly_MaxDepthBringsDown() {
		initialiseToSearchDepth();
		classUnderTest.setBackedUpScoreAtPly(3, 4);
		classUnderTest.setBackedUpScoreAtPly(2, 4);
		classUnderTest.setProvisionalScoreAtPly(3);
		assertTrue(classUnderTest.getProvisionalScoreAtPly(3)==Integer.MAX_VALUE);
		classUnderTest.setBackedUpScoreAtPly(3, 30);
		assertTrue(classUnderTest.isBackUpRequired(3, 4));
		assertFalse(classUnderTest.isAlphaBetaCutOff(2, -100, 4));
	}
	
	@Test
	public void testSetProvisionalScoreAtPly_Ply2BringsDown() {
		initialiseToSearchDepth();
		classUnderTest.setBackedUpScoreAtPly(3, 4);
		classUnderTest.setBackedUpScoreAtPly(2, 4);
		classUnderTest.setProvisionalScoreAtPly(3);
		classUnderTest.setBackedUpScoreAtPly(3, 30);
		assertTrue(classUnderTest.isBackUpRequired(2, 30));
		classUnderTest.setBackedUpScoreAtPly(2, 30);
		assertTrue(classUnderTest.isBackUpRequired(1, 30));
		classUnderTest.setBackedUpScoreAtPly(1, 30);
		classUnderTest.setProvisionalScoreAtPly(2);
		assertTrue(classUnderTest.getProvisionalScoreAtPly(2)==Integer.MIN_VALUE);
	}
	
	@Test
	public void testSetProvisionalScoreAtPly_Ply3BringsDown() {
		initialiseToSearchDepth();
		classUnderTest.setBackedUpScoreAtPly(3, 4);
		classUnderTest.setBackedUpScoreAtPly(2, 4);
		classUnderTest.setProvisionalScoreAtPly(3);
		classUnderTest.setBackedUpScoreAtPly(3, 30);
		classUnderTest.isBackUpRequired(2, 30);
		classUnderTest.setBackedUpScoreAtPly(2, 30);
		classUnderTest.isBackUpRequired(1, 30);
		classUnderTest.setBackedUpScoreAtPly(1, 30);
		classUnderTest.setProvisionalScoreAtPly(2);
		classUnderTest.setProvisionalScoreAtPly(3);
		assertTrue(classUnderTest.getProvisionalScoreAtPly(3)==30);
		
	}

	private void initialiseToSearchDepth() {
		// Initialise to first backed up score
		classUnderTest.setProvisionalScoreAtPly(0);
		classUnderTest.setProvisionalScoreAtPly(1);
		classUnderTest.setProvisionalScoreAtPly(2);
		classUnderTest.setProvisionalScoreAtPly(3);
	}	
}
