package eubos.position;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;

public class MateScoreGeneratorTest {
	
	private MateScoreGenerator classUnderTest;
	private PositionManager pm;
	
	@Before
	public void setUp() {
	}
	
	private byte convertPlyToMove( byte ply ) { return (byte)((ply+1)/2); }
	
	@Test
	public void testGenerateScoreForCheckmate_fromWhite() throws InvalidPieceException, IllegalNotationException {
		pm = new PositionManager("7k/8/5K2/8/8/8/8/6Q1 w - - 0 1");
		classUnderTest = new MateScoreGenerator(pm);
		pm.performMove(new GenericMove("g1g7"));
		// Mate detected on the ply after the move that caused the mate!
		for (byte testPly = 1; testPly <= 30; testPly+=2) {
			if ((testPly % 2) == 1) {
				assertEquals(Short.MAX_VALUE-convertPlyToMove(testPly), classUnderTest.scoreMate(testPly));
			}
		}
	}
		
	@Test
	public void testGenerateScoreForCheckmate_fromBlack() throws InvalidPieceException, IllegalNotationException {
		pm = new PositionManager("6q1/8/8/8/8/5k2/8/7K b - - 0 1");
		classUnderTest = new MateScoreGenerator(pm);
		pm.performMove(new GenericMove("g8g2"));
		// Mate detected on the ply after the move that caused the mate!
		for (byte testPly = 1; testPly <= 30; testPly+=2) {
			if ((testPly % 2) == 1) {
				assertEquals(Short.MIN_VALUE+convertPlyToMove(testPly), classUnderTest.scoreMate(testPly));
			}
		}
	}
	
	@Test
	public void testGenerateScoreForCheckmate_fromBlack_matedIn2() {
		classUnderTest = new MateScoreGenerator(new PositionManager("r1r3kQ/pb1p1p2/1p2pBp1/2pPP3/2P5/1P3NP1/n4PBP/b3R1K1 b - - 1 3"));
		// Mate detected on the ply after the move that caused the mate!
		byte testPly = 4;
		assertEquals(Short.MAX_VALUE-convertPlyToMove(testPly), classUnderTest.scoreMate(testPly));
	}
}
