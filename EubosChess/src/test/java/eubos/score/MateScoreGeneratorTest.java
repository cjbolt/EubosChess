package eubos.score;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.position.Move;
import eubos.position.PositionManager;
import eubos.search.Score;

public class MateScoreGeneratorTest {
	
	private PositionManager pm;
	
	@Before
	public void setUp() {
	}
	
	public static byte convertPlyToMove( byte ply ) { return (byte)((ply+1)/2); }
	
	@Test
	public void testGenerateScoreForCheckmate_fromWhite()throws IllegalNotationException {
		pm = new PositionManager("7k/8/5K2/8/8/8/8/6Q1 w - - 0 1");
		pm.performMove(Move.toMove(new GenericMove("g1g7"), pm.getTheBoard()));
		// Mate detected on the ply after the move that caused the mate!
		for (byte testPly = 1; testPly <= 30; testPly+=2) {
			if ((testPly % 2) == 1) {
				assertEquals(-(Short.MAX_VALUE-testPly), Score.getMateScore(testPly));
			}
		}
	}
		
	@Test
	public void testGenerateScoreForCheckmate_fromBlack()throws IllegalNotationException {
		pm = new PositionManager("6q1/8/8/8/8/5k2/8/7K b - - 0 1");
		pm.performMove(Move.toMove(new GenericMove("g8g2"), pm.getTheBoard()));
		// Mate detected on the ply after the move that caused the mate!
		for (byte testPly = 1; testPly <= 30; testPly+=2) {
			if ((testPly % 2) == 1) {
				assertEquals(Score.PROVISIONAL_ALPHA+testPly, Score.getMateScore(testPly));
			}
		}
	}
	
	@Test
	public void testGenerateScoreForCheckmate_fromBlack_matedIn2() {
		//classUnderTest = new MateScoreGenerator(new PositionManager("r1r3kQ/pb1p1p2/1p2pBp1/2pPP3/2P5/1P3NP1/n4PBP/b3R1K1 b - - 1 3"), null);
		// Mate detected on the ply after the move that caused the mate!
		byte testPly = 4;
		assertEquals(-(Score.PROVISIONAL_BETA-testPly), Score.getMateScore(testPly));
	}
}
