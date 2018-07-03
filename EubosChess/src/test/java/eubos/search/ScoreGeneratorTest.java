package eubos.search;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ScoreGeneratorTest {
	
	private ScoreGenerator classUnderTest;
	
	private static final int SEARCH_DEPTH = 8;
	private static final int FIRST_PLY = 0;
	private static final int LAST_PLY = (SEARCH_DEPTH-1);
	private int score = 0;
	private int testPly = FIRST_PLY;
	
	@Before
	public void setUp() {
		classUnderTest = new ScoreGenerator(SEARCH_DEPTH);
		score = 0;
	}
	
	@Test
	public void testConstructor() {
		assertTrue(classUnderTest!=null);
	}
	
	@Test
	public void testGenerateScoreForCheckmate_M1() {
		testPly = FIRST_PLY;
		testPly++; // Mate detected on the ply after the move that caused the mate!
		score = classUnderTest.generateScoreForCheckmate(testPly);
		assertTrue(score==(MaterialEvaluator.KING_VALUE*(SEARCH_DEPTH/ScoreGenerator.PLIES_PER_MOVE)));
	}
	
	@Test
	public void testGenerateScoreForCheckmate_M8() {
		testPly = LAST_PLY;
		score = classUnderTest.generateScoreForCheckmate(testPly);
		assertTrue(score==(MaterialEvaluator.KING_VALUE));
	}
}
