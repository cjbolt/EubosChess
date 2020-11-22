package eubos.position;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Ignore;
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
	@Ignore // Eubos no longer privileges castling in move ordering
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
	
	@Test
	public void test_mvv_lva_order() throws IllegalNotationException {
		setup("8/N2B4/Q3q3/1r3PN1/2P3B1/4Rp2/6P1/1R6 w - - 0 1 ");
		Iterator<Integer> it = classUnderTest.iterator();
		
		// gaining material
		assertEquals(new GenericMove("f5e6"), Move.toGenericMove(it.next())); // PxQ delta 4 gains 8
		assertEquals(new GenericMove("g5e6"), Move.toGenericMove(it.next())); // NxQ delta 2 gains 6
		assertEquals(new GenericMove("d7e6"), Move.toGenericMove(it.next())); // BxQ delta 2 gains 6
		assertEquals(new GenericMove("e3e6"), Move.toGenericMove(it.next())); // RxQ delta 1 gains 4
		assertEquals(new GenericMove("c4b5"), Move.toGenericMove(it.next())); // PxR delta 3 gains 4
		assertEquals(new GenericMove("a7b5"), Move.toGenericMove(it.next())); // NxR delta 2 gains 2
		assertEquals(new GenericMove("d7b5"), Move.toGenericMove(it.next())); // BxR delta 1 gains 2
		
		// neutral exchanges
		assertEquals(new GenericMove("g2f3"), Move.toGenericMove(it.next())); // PxP
		assertEquals(new GenericMove("b1b5"), Move.toGenericMove(it.next())); // RxR
		assertEquals(new GenericMove("a6e6"), Move.toGenericMove(it.next())); // QxQ
		
		// losing material
		assertEquals(new GenericMove("g5f3"), Move.toGenericMove(it.next())); // NxP delta -1 loses 2
		assertEquals(new GenericMove("g4f3"), Move.toGenericMove(it.next())); // BxP delta -2 loses 2
		assertEquals(new GenericMove("e3f3"), Move.toGenericMove(it.next())); // RxP delta -3 loses 4
		assertEquals(new GenericMove("a6b5"), Move.toGenericMove(it.next())); // QxR delta -1 loses 4
		
		// add more losing moves???
	}
	
	@Test
	public void test_mvv_lva_order_for_captures_with_check() throws IllegalNotationException {
		// as prior test but adds a king into the mix
		setup("8/N2Bk3/Q3p3/1r3PN1/2P3B1/4Rp2/6P1/1R6 w - - 0 1 ");
		Iterator<Integer> it = classUnderTest.iterator();
		
		// gaining material
		assertEquals(new GenericMove("c4b5"), Move.toGenericMove(it.next())); // PxR delta 3 gains 4
		assertEquals(new GenericMove("a7b5"), Move.toGenericMove(it.next())); // NxR delta 2 gains 2
		assertEquals(new GenericMove("d7b5"), Move.toGenericMove(it.next())); // BxR delta 1 gains 2
		
		// neutral exchanges
		assertEquals(new GenericMove("g2f3"), Move.toGenericMove(it.next())); // PxP
		assertEquals(new GenericMove("f5e6"), Move.toGenericMove(it.next())); // PxP
		assertEquals(new GenericMove("b1b5"), Move.toGenericMove(it.next())); // RxR
		
		// losing material
		assertEquals(new GenericMove("g5f3"), Move.toGenericMove(it.next())); // NxP delta -1 loses 2
		assertEquals(new GenericMove("g5e6"), Move.toGenericMove(it.next())); // NxP delta -1 loses 2
		assertEquals(new GenericMove("g4f3"), Move.toGenericMove(it.next())); // BxP delta -2 loses 2
		assertEquals(new GenericMove("d7e6"), Move.toGenericMove(it.next())); // BxP delta -2 loses 2
		assertEquals(new GenericMove("e3e6"), Move.toGenericMove(it.next())); // RxP delta -3 loses 4 losing material but checks
		assertEquals(new GenericMove("e3f3"), Move.toGenericMove(it.next())); // RxP delta -3 loses 4
		assertEquals(new GenericMove("a6b5"), Move.toGenericMove(it.next())); // QxR delta -1 loses 4
		assertEquals(new GenericMove("a6e6"), Move.toGenericMove(it.next())); // QxP delta -4 loses 8 losing material but checks
		
		// add more losing moves???
	}
	
	@Test
	public void test_move_ordering_when_mix_of_captures_and_checks() throws IllegalNotationException {
		// as prior test but adds a king into the mix
		setup("8/4k3/4p3/5PN1/8/4R1q1/8/8 w - - 0 1");
		Iterator<Integer> it = classUnderTest.iterator();
		
		// gaining material
		assertEquals(new GenericMove("e3g3"), Move.toGenericMove(it.next())); // RxQ delta 1 gains 9
		
		// neutral material
		assertEquals(new GenericMove("f5e6"), Move.toGenericMove(it.next())); // PxP
		
		// losing material
		assertEquals(new GenericMove("g5e6"), Move.toGenericMove(it.next())); // NxP delta -1 loses 2
		assertEquals(new GenericMove("e3e6"), Move.toGenericMove(it.next())); // RxP delta -3 loses 4 losing material (happens to check, but that is ignored)
		
		// checks
		assertEquals(new GenericMove("f5f6"), Move.toGenericMove(it.next())); // Check
		
		// regular moves
		assertEquals(new GenericMove("e3e2"), Move.toGenericMove(it.next())); // Regular move
	}
	
	@Test
	public void test_move_ordering_when_mix_of_promotions_captures_and_checks() throws IllegalNotationException {
		// as prior test but adds a king into the mix
		setup("1n6/P3kP2/8/1Pp2P2/8/8/8/8 w - c6 0 1");
		Iterator<Integer> it = classUnderTest.iterator();
		
		// Queen promotions
		assertEquals(new GenericMove("a7b8q"), Move.toGenericMove(it.next())); // Queen promotion with capture, PxN
		assertEquals(new GenericMove("f7f8q"), Move.toGenericMove(it.next())); // Queen promotion with check
		assertEquals(new GenericMove("a7a8q"), Move.toGenericMove(it.next())); // Queen promotion
		
		// Piece promotions with captures should be considered before queen promotions??
		assertEquals(new GenericMove("a7b8r"), Move.toGenericMove(it.next())); // Rook promotion with capture, PxN
		assertEquals(new GenericMove("a7b8b"), Move.toGenericMove(it.next())); // Bishop promotion with capture, PxN
		assertEquals(new GenericMove("a7b8n"), Move.toGenericMove(it.next())); // Knight promotion with capture, PxN
		
		// Piece promotions with check
		assertEquals(new GenericMove("f7f8b"), Move.toGenericMove(it.next())); // Bishop promotion with check
		
		// Piece promotions
		assertEquals(new GenericMove("a7a8r"), Move.toGenericMove(it.next())); // Rook promotion
		assertEquals(new GenericMove("a7a8b"), Move.toGenericMove(it.next())); // Bishop promotion
		assertEquals(new GenericMove("a7a8n"), Move.toGenericMove(it.next())); // Knight promotion
		assertEquals(new GenericMove("f7f8r"), Move.toGenericMove(it.next())); // Bishop promotion
		assertEquals(new GenericMove("f7f8n"), Move.toGenericMove(it.next())); // Knight promotion
		
		// Captures
		assertEquals(new GenericMove("b5c6"), Move.toGenericMove(it.next())); // En Passant capture, PxP
		
		// Checks
		assertEquals(new GenericMove("f5f6"), Move.toGenericMove(it.next())); // Pawn check
		
		// Regular moves
		assertEquals(new GenericMove("b5b6"), Move.toGenericMove(it.next())); // Regular pawn move
		assertFalse(it.hasNext());
	}
	
	@Test
	public void test_best_and_killer_ordering() throws IllegalNotationException {
		PositionManager pm = new PositionManager( "8/3k3B/8/1p6/2P5/8/4K3/8 w - - 0 1 " );
		
		// fake best and killers in this position just to check correct move ordering is used.
		GenericMove best_gen = new GenericMove("c4c5");
		int best = Move.toMove(best_gen, pm.getTheBoard());
		GenericMove killer1_gen = new GenericMove("h7e4");
		int killer1 = Move.toMove(killer1_gen, pm.getTheBoard());
		GenericMove killer2_gen = new GenericMove("e2d1");
		int killer2 = Move.toMove(killer2_gen, pm.getTheBoard());
		
		classUnderTest = new MoveList(pm, best, killer1, killer2);
		Iterator<Integer> it = classUnderTest.iterator();
		
		// best
		assertEquals(best_gen, Move.toGenericMove(it.next()));
		
		// killers
		assertEquals(killer1_gen, Move.toGenericMove(it.next()));
		assertEquals(killer2_gen, Move.toGenericMove(it.next()));
		
		// capture
		assertEquals(new GenericMove("c4b5"), Move.toGenericMove(it.next()));
		// check
		assertEquals(new GenericMove("h7f5"), Move.toGenericMove(it.next()));
	}
}
