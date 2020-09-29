package eubos.position;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;

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
		setup("8/8/8/8/8/1pp5/ppp5/Kp6 w - - - -"); // is_stalemate
		assertFalse(classUnderTest.iterator().hasNext());		
	}
	
	@Test
	public void testCreateMoveList_CapturesFirstThenChecks() throws InvalidPieceException, IllegalNotationException {
		setup("8/3k3B/8/1p6/2P5/8/4K3/8 w - - 0 1 ");
		Iterator<Integer> it = classUnderTest.iterator();
		assertEquals(new GenericMove("c4b5"), Move.toGenericMove(it.next()));
		assertEquals(new GenericMove("h7f5"), Move.toGenericMove(it.next()));
	}
	
	@Test
	public void testCreateMoveList_typePromotionIsSet() throws InvalidPieceException, IllegalNotationException {
		setup("8/4P3/8/8/8/8/8/8 w - - - -");
		Iterator<Integer> it = classUnderTest.iterator();
		assertEquals(new GenericMove("e7e8q"), Move.toGenericMove(it.next()));
		assertEquals(new GenericMove("e7e8r"), Move.toGenericMove(it.next()));
	}
	
	@Test
	public void testCreateMoveList_ChecksFirst() throws InvalidPieceException, IllegalNotationException {
		setup( "8/3k3B/8/8/8/8/4K3/8 w - - 0 1");
		Iterator<Integer> it = classUnderTest.iterator();
		assertEquals(new GenericMove("h7f5"), Move.toGenericMove(it.next()));
	}
	
	@Test
	public void testCreateMoveList_ChecksFirstThenCastles() throws InvalidPieceException, IllegalNotationException {
		setup("8/3k3B/8/1p6/8/8/8/4K2R w K - 0 1");
		Iterator<Integer> it = classUnderTest.iterator();
		assertEquals(new GenericMove("h7f5"), Move.toGenericMove(it.next()));
		assertEquals(new GenericMove("e1g1"), Move.toGenericMove(it.next()));
	}
	
	@Test
	public void test_whenNoChecksCapturesOrPromotions() throws IllegalNotationException { 
		setup("8/3p4/8/8/8/5k2/1P6/7K w - - 0 1");
		Iterator<Integer> iter = classUnderTest.getStandardIterator(EXTENDED);
		assertFalse(iter.hasNext());
		iter = classUnderTest.getStandardIterator(NORMAL);
		assertTrue(iter.hasNext());
	}
	
	@Test
	public void test_whenCheckAndCapturePossible() throws IllegalNotationException {
		setup("8/K7/8/8/4B1R1/8/6q1/7k w - - 0 1 ");
		Iterator<Integer> it = classUnderTest.iterator();
		assertEquals(new GenericMove("e4g2"), Move.toGenericMove(it.next())); // Check and capture
		assertEquals(new GenericMove("g4g2"), Move.toGenericMove(it.next())); // capture
		assertEquals(new GenericMove("g4h4"), Move.toGenericMove(it.next())); // check
	}
	
	@Test
	public void test_whenPromotionAndPromoteWithCaptureAndCheckPossible() throws IllegalNotationException {
		setup("q1n5/1P6/8/8/8/8/1K6/7k w - - 0 1 ");
		Iterator<Integer> it = classUnderTest.iterator();
		assertEquals(new GenericMove("b7a8q"), Move.toGenericMove(it.next())); // Promotion with check and capture
		assertEquals(new GenericMove("b7c8q"), Move.toGenericMove(it.next())); // Promotion and capture
		assertEquals(new GenericMove("b7b8q"), Move.toGenericMove(it.next())); // Promotion
	}
	}
