package eubos.search;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ScoreTrackerTest {

	private ScoreTracker classUnderTest;
	private static final boolean isWhite = true;
	private static final byte searchDepth = 4;
	private static final byte PLY0 = 0;
	private static final byte PLY1 = 1;
	private static final byte PLY2 = 2;
	private static final byte PLY3 = 3;
	
	/* scores with reference to figure 9.12, pg.171  How Computers Play Chess, Newborn and Levy */
	private static final short Score_A_C_B_D = 9;
	private static final short Score_A_C_B_I = 4;
	private static final short Score_A_C_E_D = 30;
	
	private void initialiseToSearchDepth() {
		classUnderTest.setProvisionalScoreAtPly(PLY0);
		classUnderTest.setProvisionalScoreAtPly(PLY1);
		classUnderTest.setProvisionalScoreAtPly(PLY2);
		classUnderTest.setProvisionalScoreAtPly(PLY3);
	}
	
	private void backup_SearchTree_ACBI() {
		initialiseToSearchDepth();
		int positionScore_A_C_B_I = Score.valueOf(Score_A_C_B_I, Score.exact);
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_B_I);
		classUnderTest.setBackedUpScoreAtPly(PLY2, positionScore_A_C_B_I);
		classUnderTest.setProvisionalScoreAtPly(PLY3);
	}
	
	private void backup_SearchTree_ACED() {
		initialiseToSearchDepth();
		int positionScore_A_C_E_D = Score.valueOf(Score_A_C_E_D, Score.exact);
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_E_D);
		classUnderTest.setBackedUpScoreAtPly(PLY2, positionScore_A_C_E_D);
		classUnderTest.setBackedUpScoreAtPly(PLY1, positionScore_A_C_E_D);
		classUnderTest.setBackedUpScoreAtPly(PLY0, positionScore_A_C_E_D);
	}
	
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
		assertTrue(classUnderTest.onMoveIsWhite(PLY0));
	}

	@Test
	public void testOnMoveIsBlackPly1() {
		assertFalse(classUnderTest.onMoveIsWhite(PLY1));
	}

	@Test
	public void testOnMoveIsWhitePly2() {
		assertTrue(classUnderTest.onMoveIsWhite(PLY2));
	}
	
	@Test
	public void testOnMoveIsBlackPly3() {
		assertFalse(classUnderTest.onMoveIsWhite(PLY3));
	}
	
	@Test
	public void testIsAlphaBetaCutOff_Max_cantBeCutOffBecauseNotDeepEnough() {
		classUnderTest.setBackedUpScoreAtPly(PLY0, Score.valueOf((short)200, Score.exact));
		classUnderTest.setProvisionalScoreAtPly(PLY1);
		assertTrue(classUnderTest.isAlphaBetaCutOff(PLY1, (short)20));
	}
	
	@Test
	public void testIsAlphaBetaCutOff_Min_cantBeCutOffBecauseNotDeepEnough() {
		classUnderTest.setProvisionalScoreAtPly(PLY0);
		assertFalse(classUnderTest.isAlphaBetaCutOff(PLY0, (short)20));
	}
	
	@Test
	public void testIsAlphaBetaCutOff_PlyOne_cantBeCutOffBecauseIsProvisional() {
		classUnderTest.setProvisionalScoreAtPly(PLY0);
		classUnderTest.setBackedUpScoreAtPly(PLY1, (short)-100);
		assertFalse(classUnderTest.isAlphaBetaCutOff(PLY1, (short)20));
	}
		
	@Test
	public void testIsAlphaBetaCutOff_plyTwo() {
		initialiseToSearchDepth();
		short positionScore_A_C_E_D = Score_A_C_E_D;
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_E_D);
		classUnderTest.setBackedUpScoreAtPly(PLY2, positionScore_A_C_E_D);
		classUnderTest.setBackedUpScoreAtPly(PLY1, positionScore_A_C_E_D);
		// now start new limb of tree branching at ply 1
		classUnderTest.setProvisionalScoreAtPly(PLY2);
		// nothing is backed up because all the exact scores are worse than what we already have
		classUnderTest.setProvisionalScoreAtPly(PLY3);
		assertTrue(classUnderTest.isAlphaBetaCutOff(PLY2, positionScore_A_C_E_D));
	}
	
	@Test
	public void testIsAlphaBetaCutOff_PlyThree_IsCutOff() {
		backup_SearchTree_ACED();
		// bring down to ply 3
		classUnderTest.setProvisionalScoreAtPly(PLY1);
		classUnderTest.setProvisionalScoreAtPly(PLY2);
		classUnderTest.setProvisionalScoreAtPly(PLY3);
		// test for refutation, it is one
		assertTrue(classUnderTest.isAlphaBetaCutOff(PLY3, (short)9));
	}
	
	@Test
	public void testIsBackUpRequired_ItIs() {
		initialiseToSearchDepth();
		short positionScore_A_C_B_D = Score_A_C_B_D;
		assertTrue(classUnderTest.isBackUpRequired(PLY3, positionScore_A_C_B_D, Score.upperBound));
	}
	
	@Test
	public void testIsBackUpRequired_ItIsnt() {
		initialiseToSearchDepth();
		int positionScore_A_C_B_D = Score.valueOf(Score_A_C_B_D, Score.exact);
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_B_D);
		assertFalse(classUnderTest.isBackUpRequired(PLY3, (short)15, Score.upperBound));
	}
	
	@Test
	public void testIsBackUpRequired_ItIsAgainAtThisNode() {
		initialiseToSearchDepth();
		int positionScore_A_C_B_D = Score.valueOf(Score_A_C_B_D, Score.exact);
		
		classUnderTest.isBackUpRequired(PLY3, Score.getScore(positionScore_A_C_B_D), Score.upperBound);
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_B_D);
		
		assertTrue(classUnderTest.isBackUpRequired(PLY3, (short)4, Score.upperBound));
	}
	
	@Test
	public void testIsBackUpRequired_Case1() {
		initialiseToSearchDepth();
		int positionScore_A_C_B_D = Score.valueOf(Score_A_C_B_D, Score.exact);
		classUnderTest.isBackUpRequired(PLY3, Score.getScore(positionScore_A_C_B_D), Score.upperBound);
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_B_D);
		
		int positionScore_A_C_B_I = Score.valueOf(Score_A_C_B_I, Score.exact);
		classUnderTest.isBackUpRequired(PLY3, Score.getScore(positionScore_A_C_B_I), Score.upperBound);
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_B_I);
		
		assertTrue(classUnderTest.isBackUpRequired(PLY2, Score.getScore(positionScore_A_C_B_I), Score.lowerBound));
		assertFalse(classUnderTest.isAlphaBetaCutOff(PLY2, Score.getScore(positionScore_A_C_B_I)));
	}	
	
	@Test
	public void testSetProvisionalScoreAtPly_MaxDepthBringsDown() {
		initialiseToSearchDepth();
		backup_SearchTree_ACBI();
		assertTrue(classUnderTest.getBackedUpScoreAtPly(PLY3)==Short.MAX_VALUE);
		int positionScore_A_C_E_D = Score.valueOf(Score_A_C_E_D, Score.exact);
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_E_D);
	}
	
	@Test
	public void testSetProvisionalScoreAtPly_Ply2BringsDown() {
		initialiseToSearchDepth();
		backup_SearchTree_ACBI();
		
		int positionScore_A_C_E_D = Score.valueOf(Score_A_C_E_D, Score.exact);
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_E_D);
		assertTrue(classUnderTest.isBackUpRequired(PLY2, Score.getScore(positionScore_A_C_E_D), Score.lowerBound));
		classUnderTest.setBackedUpScoreAtPly(PLY2, positionScore_A_C_E_D);
		assertTrue(classUnderTest.isBackUpRequired(PLY1, Score.getScore(positionScore_A_C_E_D), Score.upperBound));
		classUnderTest.setBackedUpScoreAtPly(PLY1, positionScore_A_C_E_D);
		classUnderTest.setProvisionalScoreAtPly(PLY2);
		assertTrue(classUnderTest.getBackedUpScoreAtPly(PLY2)==Short.MIN_VALUE);
	}
	
	@Test
	public void testSetProvisionalScoreAtPly_Ply3BringsDown() {
		initialiseToSearchDepth();
		backup_SearchTree_ACBI();
		backup_SearchTree_ACED();
		classUnderTest.setProvisionalScoreAtPly(PLY2);
		classUnderTest.setProvisionalScoreAtPly(PLY3);
		assertTrue(classUnderTest.getBackedUpScoreAtPly(PLY3)==Score_A_C_E_D);
	}	

	@Test
	public void testIsAlphaBetaCutOff_CutOff() {
		initialiseToSearchDepth();
		backup_SearchTree_ACBI();
		backup_SearchTree_ACED();
		classUnderTest.setProvisionalScoreAtPly(PLY2);
		classUnderTest.setProvisionalScoreAtPly(PLY3);
		int positionScore_A_C_E_D = Score.valueOf(Score_A_C_E_D, Score.exact);
		classUnderTest.setBackedUpScoreAtPly(PLY2, positionScore_A_C_E_D);
		assertTrue(classUnderTest.isAlphaBetaCutOff(PLY2, Score.getScore(positionScore_A_C_E_D)));	
	}
	
	public static short WHITE_MATES_IN_1 = Short.MAX_VALUE - 1;
	public static short WHITE_MATES_IN_2 = Short.MAX_VALUE - 2;
	public static short WHITE_MATES_IN_3 = Short.MAX_VALUE - 3;
	public static short WHITE_MATES_IN_4 = Short.MAX_VALUE - 4;
	public static short WHITE_MATES_IN_5 = Short.MAX_VALUE - 5;
	public static short WHITE_MATES_IN_6 = Short.MAX_VALUE - 6;
			
	public static short BLACK_MATES_IN_1 = Short.MIN_VALUE + 1;
	public static short BLACK_MATES_IN_2 = Short.MIN_VALUE + 2;
	public static short BLACK_MATES_IN_3 = Short.MIN_VALUE + 3;
	public static short BLACK_MATES_IN_4 = Short.MIN_VALUE + 4;
	public static short BLACK_MATES_IN_5 = Short.MIN_VALUE + 5;
	public static short BLACK_MATES_IN_6 = Short.MIN_VALUE + 6;		;
	
	@Test
	@Ignore
	public void testadjustHashTableMateInXScore_White() {
		// When white is initial on move, at root node the score is not adjusted
		byte currPly = 0;
		assertEquals(WHITE_MATES_IN_1, classUnderTest.adjustHashTableMateInXScore(currPly, WHITE_MATES_IN_1));
		// at ply 1, when it is blacks move... if black is getting mated in 1, then it is still mate in 1
		currPly = 1;
		assertEquals(WHITE_MATES_IN_1, classUnderTest.adjustHashTableMateInXScore(currPly, WHITE_MATES_IN_1));
		// at ply 2, we have one full move for white already in the tree, so it must be adjusted to mate in 2
		currPly = 2;
		assertEquals(WHITE_MATES_IN_2, classUnderTest.adjustHashTableMateInXScore(currPly, WHITE_MATES_IN_1));
		// at ply 3, we have one full move for white already in the tree, so it must be adjusted to mate in 2
		currPly = 3;
		assertEquals(WHITE_MATES_IN_2, classUnderTest.adjustHashTableMateInXScore(currPly, WHITE_MATES_IN_1));
	}
	
	@Test
	@Ignore
	public void testadjustHashTableMateInXScore_Back() {
		// When black is initial on move, at root node the score is not adjusted
		byte currPly = 0;
		assertEquals(BLACK_MATES_IN_1, classUnderTest.adjustHashTableMateInXScore(currPly, BLACK_MATES_IN_1));
		// at ply 1, when it is whites move... if white is getting mated in 1, then it is still mate in 1
		currPly = 1;
		assertEquals(BLACK_MATES_IN_1, classUnderTest.adjustHashTableMateInXScore(currPly, BLACK_MATES_IN_1));
		// at ply 2, we have one full move for black already in the tree, so it must be adjusted to mate in 2
		currPly = 2;
		assertEquals(BLACK_MATES_IN_2, classUnderTest.adjustHashTableMateInXScore(currPly, BLACK_MATES_IN_1));
		// at ply 3, we have one full move for black already in the tree, so it must be adjusted to mate in 2
		currPly = 3;
		assertEquals(BLACK_MATES_IN_2, classUnderTest.adjustHashTableMateInXScore(currPly, BLACK_MATES_IN_1));
		
		currPly = 2;
		assertEquals(BLACK_MATES_IN_3, classUnderTest.adjustHashTableMateInXScore(currPly, BLACK_MATES_IN_2));
		// at ply 3, we have one full move for black already in the tree, so it must be adjusted to mate in 2
		currPly = 3;
		assertEquals(BLACK_MATES_IN_3, classUnderTest.adjustHashTableMateInXScore(currPly, BLACK_MATES_IN_2));
	}
}
