package eubos.position;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.position.MoveListGenerator;
import eubos.position.PositionManager;

public class MoveListGeneratorTest {

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
	
	@Test
	public void testCreateMoveList_CapturesFirstThenChecks() throws InvalidPieceException, IllegalNotationException {
		PositionManager pm = new PositionManager( "8/3k3B/8/1p6/2P5/8/4K3/8 w - - 0 1 " );
		classUnderTest = new MoveListGenerator(pm);
		List<GenericMove> ml= classUnderTest.createMoveList();
		assertEquals(new GenericMove("c4b5"), ml.get(0));
		assertEquals(new GenericMove("h7f5"), ml.get(1));
	}
	
	@Test
	public void testCreateMoveList_ChecksFirst() throws InvalidPieceException, IllegalNotationException {
		PositionManager pm = new PositionManager( "8/3k3B/8/8/8/8/4K3/8 w - - 0 1" );
		classUnderTest = new MoveListGenerator(pm);
		List<GenericMove> ml= classUnderTest.createMoveList();
		assertEquals(new GenericMove("h7f5"), ml.get(0));
	}
	
	
	@Test
	public void testCreateMoveList_CastlesFirstThenChecks() throws InvalidPieceException, IllegalNotationException {
		PositionManager pm = new PositionManager( "8/3k3B/8/1p6/8/8/8/4K2R w K - 0 1" );
		classUnderTest = new MoveListGenerator(pm);
		List<GenericMove> ml= classUnderTest.createMoveList();
		assertEquals(new GenericMove("e1g1"), ml.get(0));
		assertEquals(new GenericMove("h7f5"), ml.get(1));
	}
}
