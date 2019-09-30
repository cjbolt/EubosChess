package eubos.position;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import eubos.board.pieces.Piece.Colour;

public class MateScoreGeneratorTest {
	
	private MateScoreGenerator classUnderTest;
	
	@Before
	public void setUp() {
	}
	
	private byte convertPlyToMove( byte ply ) { return (byte)((ply+1)/2); }
	
	@Test
	public void testGenerateScoreForCheckmate_fromWhite() {
		classUnderTest = new MateScoreGenerator(new PositionManager("4N3/5P1P/5N1k/6Q1/5PKP/B7/8/1B6 b - - 0 1"));
		// Mate detected on the ply after the move that caused the mate!
		for (byte testPly = 1; testPly <= 30; testPly+=2) {
			if ((testPly % 2) == 1) {
				assertEquals(Short.MAX_VALUE-convertPlyToMove(testPly), classUnderTest.scoreMate(testPly, Colour.white ));
			}
		}
	}
		
	@Test
	public void testGenerateScoreForCheckmate_fromBlack() {
		classUnderTest = new MateScoreGenerator(new PositionManager("8/8/8/8/8/5k2/6q1/7K w - - 0 1"));
		// Mate detected on the ply after the move that caused the mate!
		for (byte testPly = 1; testPly <= 30; testPly+=2) {
			if ((testPly % 2) == 1) {
				assertEquals(Short.MIN_VALUE+convertPlyToMove(testPly), classUnderTest.scoreMate(testPly, Colour.black ));
			} else {
				
			}
		}
	}
}
