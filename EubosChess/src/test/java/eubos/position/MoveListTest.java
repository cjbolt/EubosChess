package eubos.position;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.position.PositionManager;

public class MoveListTest {

	public static final boolean EXTENDED = true;
	public static final boolean NORMAL = false;
	
	protected MoveList classUnderTest;
	
	private void setup(String fen) {
		PositionManager pm = new PositionManager( fen );
		classUnderTest = new MoveList(pm);
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testLegalMoveListGenerator() {
		classUnderTest = new MoveList(new PositionManager());
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
		classUnderTest = new MoveList(bm);
		List<GenericMove> ml;
		ml = classUnderTest.getList();
		assertTrue(ml.isEmpty());		
	}
	
	@Test
	public void testCreateMoveList_CapturesFirstThenChecks() throws InvalidPieceException, IllegalNotationException {
		PositionManager pm = new PositionManager( "8/3k3B/8/1p6/2P5/8/4K3/8 w - - 0 1 " );
		classUnderTest = new MoveList(pm);
		List<GenericMove> ml= classUnderTest.getList();
		assertEquals(new GenericMove("c4b5"), ml.get(0));
		assertEquals(new GenericMove("h7f5"), ml.get(1));
	}
	
	@Test
	public void testCreateMoveList_ChecksFirst() throws InvalidPieceException, IllegalNotationException {
		PositionManager pm = new PositionManager( "8/3k3B/8/8/8/8/4K3/8 w - - 0 1" );
		classUnderTest = new MoveList(pm);
		List<GenericMove> ml= classUnderTest.getList();
		assertEquals(new GenericMove("h7f5"), ml.get(0));
	}
	
	
	@Test
	public void testCreateMoveList_CastlesFirstThenChecks() throws InvalidPieceException, IllegalNotationException {
		PositionManager pm = new PositionManager( "8/3k3B/8/1p6/8/8/8/4K2R w K - 0 1" );
		classUnderTest = new MoveList(pm);
		List<GenericMove> ml= classUnderTest.getList();
		assertEquals(new GenericMove("e1g1"), ml.get(0));
		assertEquals(new GenericMove("h7f5"), ml.get(1));
	}
	
	@Test
	public void test_setBestMove() throws IllegalNotationException {
		GenericMove expected = new GenericMove("g3f2"); 
		setup("8/8/4n1p1/1R3p1p/3k3P/2rB2K1/2P3P1/8 w - - 15 51");
		assertNotEquals(expected, classUnderTest.getList().get(0));
		classUnderTest.adjustForBestMove(expected);
		assertEquals(expected, classUnderTest.getList().get(0));
	}
	
	@Test
	public void test_whenNoChecksCapturesOrPromotions() throws IllegalNotationException { 
		setup("8/3p4/8/8/8/5k2/1P6/7K w - - 0 1");
		Iterator<GenericMove> iter = classUnderTest.getIterator(EXTENDED);
		assertFalse(iter.hasNext());
		iter = classUnderTest.getIterator(NORMAL);
		assertTrue(iter.hasNext());
	}
	
	@Test
	public void test_whenChangedBestCapture_BothIteratorsAreUpdated() throws IllegalNotationException {
		setup("8/1B6/8/3q1r2/4P3/8/8/8 w - - 0 1");
		GenericMove first = classUnderTest.getList().get(0);
		GenericMove newBestCapture = new GenericMove("e4f5");
		assertNotEquals(first, newBestCapture);
		
		classUnderTest.adjustForBestMove(newBestCapture);
		
		Iterator<GenericMove> iter = classUnderTest.getIterator(NORMAL);
		assertTrue(iter.hasNext());
		assertEquals(newBestCapture, iter.next());
		
		iter = classUnderTest.getIterator(EXTENDED);
		assertTrue(iter.hasNext());
		assertEquals(newBestCapture, iter.next());
	}
	
	// Consider when move is check and capture
	// Consider tests for move list order
}
