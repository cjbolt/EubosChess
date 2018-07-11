package eubos.position;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.position.MoveListGenerator;
import eubos.position.PositionManager;

public class LegalMoveListGeneratorTest {

	protected MoveListGenerator classUnderTest;
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testLegalMoveListGenerator() {
		classUnderTest = new MoveListGenerator(new PositionManager());
	}

	@Test
	public void testCreateMoveList() {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 .PP.....
		// 2 PPP.....
		// 1 kP......
		//   abcdefgh
		PositionManager bm = new PositionManager( "8/8/8/8/8/1pp5/ppp5/Kp6 w - - - -" );
		classUnderTest = new MoveListGenerator(bm);
		List<GenericMove> ml;
		try {
			ml = classUnderTest.createMoveList();
			assertTrue(ml.isEmpty());
		} catch (InvalidPieceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		}		
	}

}
