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
		classUnderTest.setBackedUpScoreAtPly(PLY3, Score_A_C_B_I);
		classUnderTest.setBackedUpScoreAtPly(PLY2, Score_A_C_B_I);
		classUnderTest.setProvisionalScoreAtPly(PLY3);
	}
	
	private void backup_SearchTree_ACED() {
		classUnderTest.setBackedUpScoreAtPly(PLY3, Score_A_C_E_D);
		classUnderTest.setBackedUpScoreAtPly(PLY2, Score_A_C_E_D);
		classUnderTest.setBackedUpScoreAtPly(PLY1, Score_A_C_E_D);
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
	public void testIsAlphaBetaCutOff_Max() {
		assertFalse(classUnderTest.isAlphaBetaCutOff(PLY1, Short.MAX_VALUE, (short)20));
	}
	
	@Test
	public void testIsAlphaBetaCutOff_Min() {
		assertFalse(classUnderTest.isAlphaBetaCutOff(PLY1, Short.MIN_VALUE, (short)20));
	}
	
	@Test(expected = IllegalArgumentException.class)
	@Ignore
	public void testIsAlphaBetaCutOff_plyZero_Exception() {
		classUnderTest.isAlphaBetaCutOff(PLY0, (short)-100, (short)20);
	}
	
	@Test
	public void testIsAlphaBetaCutOff_PlyOne() {
		assertFalse(classUnderTest.isAlphaBetaCutOff(PLY1, (short)-100, (short)20));
	}
		
	@Test
	public void testIsAlphaBetaCutOff_plyTwo() {
		assertTrue(classUnderTest.isAlphaBetaCutOff(PLY2, (short)-100, (short)20));
	}
	
	@Test
	public void testIsAlphaBetaCutOff_PlyThree() {
		assertFalse(classUnderTest.isAlphaBetaCutOff(PLY3, (short)-100, (short)20));
	}
	
	@Test
	public void testIsBackUpRequired_ItIs() {
		initialiseToSearchDepth();
		assertTrue(classUnderTest.isBackUpRequired(PLY3, Score_A_C_B_D));
	}
	
	@Test
	public void testIsBackUpRequired_ItIsnt() {
		initialiseToSearchDepth();
		classUnderTest.setBackedUpScoreAtPly(PLY3, Score_A_C_B_D);
		assertFalse(classUnderTest.isBackUpRequired(PLY3, (short)15));
	}
	
	@Test
	public void testIsBackUpRequired_ItIsAgainAtThisNode() {
		initialiseToSearchDepth();
		classUnderTest.isBackUpRequired(PLY3, Score_A_C_B_D);
		classUnderTest.setBackedUpScoreAtPly(PLY3, Score_A_C_B_D);
		assertTrue(classUnderTest.isBackUpRequired(PLY3, (short)4));
	}
	
	@Test
	public void testIsBackUpRequired_Case1() {
		initialiseToSearchDepth();
		classUnderTest.isBackUpRequired(PLY3, Score_A_C_B_D);
		classUnderTest.setBackedUpScoreAtPly(PLY3, Score_A_C_B_D);
		classUnderTest.isBackUpRequired(PLY3, Score_A_C_B_I);
		classUnderTest.setBackedUpScoreAtPly(PLY3, Score_A_C_B_I);
		assertTrue(classUnderTest.isBackUpRequired(PLY2, Score_A_C_B_I));
		assertFalse(classUnderTest.isAlphaBetaCutOff(PLY2, (short)-100, Score_A_C_B_I));
	}	
	
	@Test
	public void testSetProvisionalScoreAtPly_MaxDepthBringsDown() {
		initialiseToSearchDepth();
		backup_SearchTree_ACBI();
		assertTrue(classUnderTest.getProvisionalScoreAtPly(PLY3)==Short.MAX_VALUE);
		classUnderTest.setBackedUpScoreAtPly(PLY3, Score_A_C_E_D);
	}
	
	@Test
	public void testSetProvisionalScoreAtPly_Ply2BringsDown() {
		initialiseToSearchDepth();
		backup_SearchTree_ACBI();
		classUnderTest.setBackedUpScoreAtPly(PLY3, Score_A_C_E_D);
		assertTrue(classUnderTest.isBackUpRequired(PLY2, Score_A_C_E_D));
		classUnderTest.setBackedUpScoreAtPly(PLY2, Score_A_C_E_D);
		assertTrue(classUnderTest.isBackUpRequired(PLY1, Score_A_C_E_D));
		classUnderTest.setBackedUpScoreAtPly(PLY1, Score_A_C_E_D);
		classUnderTest.setProvisionalScoreAtPly(PLY2);
		assertTrue(classUnderTest.getProvisionalScoreAtPly(PLY2)==Short.MIN_VALUE);
	}
	
	@Test
	public void testSetProvisionalScoreAtPly_Ply3BringsDown() {
		initialiseToSearchDepth();
		backup_SearchTree_ACBI();
		backup_SearchTree_ACED();
		classUnderTest.setProvisionalScoreAtPly(PLY2);
		classUnderTest.setProvisionalScoreAtPly(PLY3);
		assertTrue(classUnderTest.getProvisionalScoreAtPly(PLY3)==Score_A_C_E_D);
	}	

	@Test
	public void testIsAlphaBetaCutOff_CutOff() {
		initialiseToSearchDepth();
		backup_SearchTree_ACBI();
		backup_SearchTree_ACED();
		classUnderTest.setProvisionalScoreAtPly(PLY2);
		classUnderTest.setProvisionalScoreAtPly(PLY3);
		classUnderTest.setBackedUpScoreAtPly(PLY2,Score_A_C_E_D);
		assertTrue(classUnderTest.isAlphaBetaCutOff(PLY2, Score_A_C_E_D, Score_A_C_E_D));	
	}
}
