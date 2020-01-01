package eubos.search;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import eubos.search.Score.ScoreType;

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
		Score positionScore_A_C_B_I = new Score(Score_A_C_B_I, ScoreType.exact);
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_B_I);
		classUnderTest.setBackedUpScoreAtPly(PLY2, positionScore_A_C_B_I);
		classUnderTest.setProvisionalScoreAtPly(PLY3);
	}
	
	private void backup_SearchTree_ACED() {
		Score positionScore_A_C_E_D = new Score(Score_A_C_E_D, ScoreType.exact);
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_E_D);
		classUnderTest.setBackedUpScoreAtPly(PLY2, positionScore_A_C_E_D);
		classUnderTest.setBackedUpScoreAtPly(PLY1, positionScore_A_C_E_D);
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
		classUnderTest.setProvisionalScoreAtPly(PLY1);
		assertFalse(classUnderTest.isAlphaBetaCutOff(PLY1, (short)20));
	}
	
	@Test
	public void testIsAlphaBetaCutOff_Min() {
		classUnderTest.setProvisionalScoreAtPly(PLY0);
		assertFalse(classUnderTest.isAlphaBetaCutOff(PLY0, (short)20));
	}
	
	@Test
	public void testIsAlphaBetaCutOff_PlyOne() {
		classUnderTest.setBackedUpScoreAtPly(PLY1, new Score((short)-100, ScoreType.exact));
		assertFalse(classUnderTest.isAlphaBetaCutOff(PLY1, (short)20));
	}
		
	@Test
	public void testIsAlphaBetaCutOff_plyTwo() {
		classUnderTest.setBackedUpScoreAtPly(PLY2, new Score((short)-100, ScoreType.exact));
		assertTrue(classUnderTest.isAlphaBetaCutOff(PLY2, (short)20));
	}
	
	@Test
	public void testIsAlphaBetaCutOff_PlyThree() {
		classUnderTest.setBackedUpScoreAtPly(PLY3, new Score((short)-100, ScoreType.exact));
		assertFalse(classUnderTest.isAlphaBetaCutOff(PLY3, (short)20));
	}
	
	@Test
	public void testIsBackUpRequired_ItIs() {
		initialiseToSearchDepth();
		Score positionScore_A_C_B_D = new Score(Score_A_C_B_D, ScoreType.exact);
		assertTrue(classUnderTest.isBackUpRequired(PLY3, positionScore_A_C_B_D));
	}
	
	@Test
	public void testIsBackUpRequired_ItIsnt() {
		initialiseToSearchDepth();
		Score positionScore_A_C_B_D = new Score(Score_A_C_B_D, ScoreType.exact);
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_B_D);
		assertFalse(classUnderTest.isBackUpRequired(PLY3, new Score((short)15, ScoreType.exact)));
	}
	
	@Test
	public void testIsBackUpRequired_ItIsAgainAtThisNode() {
		initialiseToSearchDepth();
		Score positionScore_A_C_B_D = new Score(Score_A_C_B_D, ScoreType.exact);
		
		classUnderTest.isBackUpRequired(PLY3, positionScore_A_C_B_D);
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_B_D);
		
		assertTrue(classUnderTest.isBackUpRequired(PLY3, new Score((short)4, ScoreType.exact)));
	}
	
	@Test
	public void testIsBackUpRequired_Case1() {
		initialiseToSearchDepth();
		Score positionScore_A_C_B_D = new Score(Score_A_C_B_D, ScoreType.exact);
		classUnderTest.isBackUpRequired(PLY3, positionScore_A_C_B_D);
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_B_D);
		
		Score positionScore_A_C_B_I = new Score(Score_A_C_B_I, ScoreType.exact);
		classUnderTest.isBackUpRequired(PLY3, positionScore_A_C_B_I);
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_B_I);
		
		assertTrue(classUnderTest.isBackUpRequired(PLY2, positionScore_A_C_B_I));
		assertFalse(classUnderTest.isAlphaBetaCutOff(PLY2, positionScore_A_C_B_I.getScore()));
	}	
	
	@Test
	public void testSetProvisionalScoreAtPly_MaxDepthBringsDown() {
		initialiseToSearchDepth();
		backup_SearchTree_ACBI();
		assertTrue(classUnderTest.getBackedUpScoreAtPly(PLY3).getScore()==Short.MAX_VALUE);
		Score positionScore_A_C_E_D = new Score(Score_A_C_E_D, ScoreType.exact);
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_E_D);
	}
	
	@Test
	public void testSetProvisionalScoreAtPly_Ply2BringsDown() {
		initialiseToSearchDepth();
		backup_SearchTree_ACBI();
		
		Score positionScore_A_C_E_D = new Score(Score_A_C_E_D, ScoreType.exact);
		classUnderTest.setBackedUpScoreAtPly(PLY3, positionScore_A_C_E_D);
		assertTrue(classUnderTest.isBackUpRequired(PLY2, positionScore_A_C_E_D));
		classUnderTest.setBackedUpScoreAtPly(PLY2, positionScore_A_C_E_D);
		assertTrue(classUnderTest.isBackUpRequired(PLY1, positionScore_A_C_E_D));
		classUnderTest.setBackedUpScoreAtPly(PLY1, positionScore_A_C_E_D);
		classUnderTest.setProvisionalScoreAtPly(PLY2);
		assertTrue(classUnderTest.getBackedUpScoreAtPly(PLY2).getScore()==Short.MIN_VALUE);
	}
	
	@Test
	public void testSetProvisionalScoreAtPly_Ply3BringsDown() {
		initialiseToSearchDepth();
		backup_SearchTree_ACBI();
		backup_SearchTree_ACED();
		classUnderTest.setProvisionalScoreAtPly(PLY2);
		classUnderTest.setProvisionalScoreAtPly(PLY3);
		assertTrue(classUnderTest.getBackedUpScoreAtPly(PLY3).getScore()==Score_A_C_E_D);
	}	

	@Test
	public void testIsAlphaBetaCutOff_CutOff() {
		initialiseToSearchDepth();
		backup_SearchTree_ACBI();
		backup_SearchTree_ACED();
		classUnderTest.setProvisionalScoreAtPly(PLY2);
		classUnderTest.setProvisionalScoreAtPly(PLY3);
		Score positionScore_A_C_E_D = new Score(Score_A_C_E_D, ScoreType.exact);
		classUnderTest.setBackedUpScoreAtPly(PLY2, positionScore_A_C_E_D);
		assertTrue(classUnderTest.isAlphaBetaCutOff(PLY2, Score_A_C_E_D));	
	}
}
