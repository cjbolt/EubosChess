package eubos.position;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.Piece;
import eubos.search.KillerList;

public class MoveListTest {

	public static final boolean EXTENDED = true;
	public static final boolean NORMAL = false;
	
	protected MoveList classUnderTest;
	
	private void setup(String fen)  {
		PositionManager pm = new PositionManager( fen );
		classUnderTest = new MoveList(pm, 1);;
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testLegalMoveListGenerator()  {
		classUnderTest = new MoveList(new PositionManager(), 0);
	}

	@Test
	public void testCreateMoveList()  {
		setup("8/8/8/8/8/1pp5/ppp5/Kp6 w - - - -"); // is_stalemate
		assertFalse(classUnderTest.iterator().hasNext());		
	}
	
	@Test
	public void testCreateMoveList_CapturesFirst()throws IllegalNotationException {
		setup("8/3k3B/8/1p6/2P5/8/4K3/8 w - - 0 1 ");
		MoveListIterator it = classUnderTest.createForPly(Move.NULL_MOVE, null, true, false, 0);
		assertEquals(new GenericMove("c4b5"), Move.toGenericMove(it.nextInt()));
	}
	
	@Test
	public void testCreateMoveList_typePromotionIsSet()throws IllegalNotationException {
		setup("8/4P3/8/8/8/8/8/8 w - - - -");
		MoveListIterator it = classUnderTest.createForPly(Move.NULL_MOVE, null, false, false, 0);
		assertEquals(new GenericMove("e7e8q"), Move.toGenericMove(it.nextInt()));
		assertEquals(new GenericMove("e7e8r"), Move.toGenericMove(it.nextInt()));
	}
	
	@Test
	public void test_whenNoChecksCapturesOrPromotions()throws IllegalNotationException { 
		setup("8/3p4/8/8/8/5k2/1P6/7K w - - 0 1");
		MoveListIterator iter =  classUnderTest.createForPly(Move.NULL_MOVE, null, false, false, 0);
		MoveListIterator extended_iter = classUnderTest.createForPly(Move.NULL_MOVE, true, 0);
		assertFalse(extended_iter.hasNext());
		assertTrue(iter.hasNext());
	}
	
	@Test
	public void test_whenCheckAndCapturePossible() throws IllegalNotationException {
		setup("8/K7/8/8/4B1R1/8/6q1/7k w - - 0 1 ");
		MoveListIterator it =  classUnderTest.createForPly(Move.NULL_MOVE, null, false, false, 0);
		assertEquals(new GenericMove("e4g2"), Move.toGenericMove(it.nextInt())); // capture (happens to have check)
		assertEquals(new GenericMove("g4g2"), Move.toGenericMove(it.nextInt())); // capture
	}
	
	@Test
	public void test_whenPromotionAndPromoteWithCaptureAndCheckPossible()throws IllegalNotationException {
		setup("q1n5/1P6/8/8/8/8/1K6/7k w - - 0 1 ");
		MoveListIterator it =  classUnderTest.createForPly(Move.NULL_MOVE, null, false, false, 0);
		assertEquals(new GenericMove("b7a8q"), Move.toGenericMove(it.nextInt())); // Promotion with check and capture
		assertEquals(new GenericMove("b7c8q"), Move.toGenericMove(it.nextInt())); // Promotion and capture
		
		assertEquals(new GenericMove("b7a8r"), Move.toGenericMove(it.nextInt())); // Promotion (piece) and capture
		assertEquals(new GenericMove("b7c8r"), Move.toGenericMove(it.nextInt())); // Promotion (piece) and capture
		assertEquals(new GenericMove("b7a8b"), Move.toGenericMove(it.nextInt())); // Promotion (piece) and capture
		assertEquals(new GenericMove("b7c8b"), Move.toGenericMove(it.nextInt())); // Promotion (piece) and capture
		assertEquals(new GenericMove("b7a8n"), Move.toGenericMove(it.nextInt())); // Promotion (piece) and capture
		assertEquals(new GenericMove("b7c8n"), Move.toGenericMove(it.nextInt())); // Promotion (piece) and capture
		assertEquals(new GenericMove("b7b8q"), Move.toGenericMove(it.nextInt())); // Promotion
		assertEquals(new GenericMove("b7b8r"), Move.toGenericMove(it.nextInt())); // Promotion (piece)
		assertEquals(new GenericMove("b7b8b"), Move.toGenericMove(it.nextInt())); // Promotion (piece)
		assertEquals(new GenericMove("b7b8n"), Move.toGenericMove(it.nextInt())); // Promotion (piece)
	}
	
	@Test
	public void test_mvv_lva_order()throws IllegalNotationException {
		setup("8/N2B4/Q3q3/1r3PN1/2P3B1/4Rp2/6P1/1R6 w - - 0 1 ");
		MoveListIterator it =  classUnderTest.createForPly(Move.NULL_MOVE, null, false, false, 0);
		
		// gaining material
		assertEquals(new GenericMove("f5e6"), Move.toGenericMove(it.nextInt())); // PxQ delta 4 gains 8
		assertEquals(new GenericMove("g5e6"), Move.toGenericMove(it.nextInt())); // NxQ delta 2 gains 6
		assertEquals(new GenericMove("d7e6"), Move.toGenericMove(it.nextInt())); // BxQ delta 2 gains 6
		assertEquals(new GenericMove("e3e6"), Move.toGenericMove(it.nextInt())); // RxQ delta 1 gains 4
		assertEquals(new GenericMove("c4b5"), Move.toGenericMove(it.nextInt())); // PxR delta 3 gains 4
		assertEquals(new GenericMove("a7b5"), Move.toGenericMove(it.nextInt())); // NxR delta 2 gains 2
		assertEquals(new GenericMove("d7b5"), Move.toGenericMove(it.nextInt())); // BxR delta 1 gains 2
		
		// neutral exchanges
		assertEquals(new GenericMove("a6e6"), Move.toGenericMove(it.nextInt())); // QxQ
		assertEquals(new GenericMove("b1b5"), Move.toGenericMove(it.nextInt())); // RxR
		assertEquals(new GenericMove("g2f3"), Move.toGenericMove(it.nextInt())); // PxP		
		
		// losing material
		assertEquals(new GenericMove("g5f3"), Move.toGenericMove(it.nextInt())); // NxP delta -1 loses 2
		assertEquals(new GenericMove("g4f3"), Move.toGenericMove(it.nextInt())); // BxP delta -2 loses 2
		assertEquals(new GenericMove("e3f3"), Move.toGenericMove(it.nextInt())); // RxP delta -3 loses 4
		assertEquals(new GenericMove("a6b5"), Move.toGenericMove(it.nextInt())); // QxR delta -1 loses 4
		
		// add more losing moves???
	}
	
	@Test
	public void test_mvv_lva_order_for_captures_with_check()throws IllegalNotationException {
		// as prior test but adds a king into the mix
		setup("8/N2Bk3/Q3p3/1r3PN1/2P3B1/4Rp2/6P1/1R6 w - - 0 1 ");
		MoveListIterator it =  classUnderTest.createForPly(Move.NULL_MOVE, null, false, false, 0);
		
		// gaining material
		assertEquals(new GenericMove("c4b5"), Move.toGenericMove(it.nextInt())); // PxR delta 3 gains 4
		assertEquals(new GenericMove("a7b5"), Move.toGenericMove(it.nextInt())); // NxR delta 2 gains 2
		assertEquals(new GenericMove("d7b5"), Move.toGenericMove(it.nextInt())); // BxR delta 1 gains 2
		
		// neutral exchanges
		assertEquals(new GenericMove("b1b5"), Move.toGenericMove(it.nextInt())); // RxR
		assertEquals(new GenericMove("f5e6"), Move.toGenericMove(it.nextInt())); // PxP
		assertEquals(new GenericMove("g2f3"), Move.toGenericMove(it.nextInt())); // PxP
		
		// losing material
		assertEquals(new GenericMove("g5f3"), Move.toGenericMove(it.nextInt())); // NxP delta -1 loses 2
		assertEquals(new GenericMove("g5e6"), Move.toGenericMove(it.nextInt())); // NxP delta -1 loses 2
		assertEquals(new GenericMove("d7e6"), Move.toGenericMove(it.nextInt())); // BxP delta -2 loses 2
		assertEquals(new GenericMove("g4f3"), Move.toGenericMove(it.nextInt())); // BxP delta -2 loses 2
		assertEquals(new GenericMove("e3f3"), Move.toGenericMove(it.nextInt())); // RxP delta -3 loses 4 losing material but checks
		assertEquals(new GenericMove("e3e6"), Move.toGenericMove(it.nextInt())); // RxP delta -3 loses 4
		assertEquals(new GenericMove("a6b5"), Move.toGenericMove(it.nextInt())); // QxR delta -1 loses 4
		assertEquals(new GenericMove("a6e6"), Move.toGenericMove(it.nextInt())); // QxP delta -4 loses 8 losing material but checks
		
		// add more losing moves???
	}
	
	@Test
	public void test_move_ordering_when_mix_of_captures_and_checks()throws IllegalNotationException {
		// as prior test but adds a king into the mix
		setup("8/4k3/4p3/5PN1/8/4R1q1/8/8 w - - 0 1");
		MoveListIterator it =  classUnderTest.createForPly(Move.NULL_MOVE, null, false, false, 0);
		
		// gaining material
		assertEquals(new GenericMove("e3g3"), Move.toGenericMove(it.nextInt())); // RxQ delta 1 gains 9
		
		// neutral material
		assertEquals(new GenericMove("f5e6"), Move.toGenericMove(it.nextInt())); // PxP
		
		// losing material
		assertEquals(new GenericMove("g5e6"), Move.toGenericMove(it.nextInt())); // NxP delta -1 loses 2
		assertEquals(new GenericMove("e3e6"), Move.toGenericMove(it.nextInt())); // RxP delta -3 loses 4 losing material (happens to check, but that is ignored)
		
		// regular moves
		assertEquals(new GenericMove("f5f6"), Move.toGenericMove(it.nextInt())); // piece is attacked, regular move
		assertEquals(new GenericMove("g5h7"), Move.toGenericMove(it.nextInt())); // Regular move
		assertEquals(new GenericMove("g5h3"), Move.toGenericMove(it.nextInt())); // Regular move
	}
	
	@Test
	public void test_move_ordering_when_mix_of_promotions_captures_and_checks()throws IllegalNotationException {
		// as prior test but adds a king into the mix
		setup("1n6/P3kP2/8/1Pp2P2/8/8/8/8 w - c6 0 1");
		MoveListIterator it = classUnderTest.createForPly(Move.NULL_MOVE, null, false, false, 0);
		
		// Promotions with capture
		assertEquals(new GenericMove("a7b8q"), Move.toGenericMove(it.nextInt())); // Queen promotion with capture, PxN
		assertEquals(new GenericMove("a7b8r"), Move.toGenericMove(it.nextInt())); // Rook promotion with capture, PxN
		assertEquals(new GenericMove("a7b8b"), Move.toGenericMove(it.nextInt())); // Bishop promotion with capture, PxN
		assertEquals(new GenericMove("a7b8n"), Move.toGenericMove(it.nextInt())); // Knight promotion with capture, PxN
		assertEquals(new GenericMove("a7a8q"), Move.toGenericMove(it.nextInt())); // Queen promotion
		assertEquals(new GenericMove("f7f8q"), Move.toGenericMove(it.nextInt())); // Queen promotion (with check)
		assertEquals(new GenericMove("a7a8r"), Move.toGenericMove(it.nextInt())); // Rook promotion
		assertEquals(new GenericMove("f7f8r"), Move.toGenericMove(it.nextInt())); // Rook promotion
		assertEquals(new GenericMove("a7a8b"), Move.toGenericMove(it.nextInt())); // Bishop promotion
		assertEquals(new GenericMove("f7f8b"), Move.toGenericMove(it.nextInt())); // Bishop promotion (with check)
		assertEquals(new GenericMove("a7a8n"), Move.toGenericMove(it.nextInt())); // Knight promotion
		assertEquals(new GenericMove("f7f8n"), Move.toGenericMove(it.nextInt())); // Knight promotion
		
		// Captures
		assertEquals(new GenericMove("b5c6"), Move.toGenericMove(it.nextInt())); // En Passant capture, PxP
				
		// Regular moves
		assertEquals(new GenericMove("b5b6"), Move.toGenericMove(it.nextInt())); // Regular pawn move
		assertEquals(new GenericMove("f5f6"), Move.toGenericMove(it.nextInt())); // Pawn check
		
		assertFalse(it.hasNext());
	}
	
	@Test
	public void test_best_and_killer_ordering()throws IllegalNotationException {
		PositionManager pm = new PositionManager( "8/3k3B/8/1p6/2P5/8/4K3/8 w - - 0 1 " );
		
		// fake best and killers in this position just to check correct move ordering is used.
		GenericMove best_gen = new GenericMove("c4c5");
		int best = Move.toMove(best_gen, pm.getTheBoard());
		GenericMove killer1_gen = new GenericMove("e2d1");
		int killer1 = Move.toMove(killer1_gen, pm.getTheBoard());
		GenericMove killer2_gen = new GenericMove("h7e4");
		int killer2 = Move.toMove(killer2_gen, pm.getTheBoard());
		int [] killers = new int[3];
		killers[0] = killer1; killers[1] = killer2; killers[2] = Move.NULL_MOVE;
		
		classUnderTest = new MoveList(pm, 1);
		MoveListIterator it = classUnderTest.createForPly(best, killers, false, pm.isKingInCheck(), 0);
		
		// best
		assertEquals(best_gen, Move.toGenericMove(it.nextInt()));
		
		// capture
		assertEquals(new GenericMove("c4b5"), Move.toGenericMove(it.nextInt()));
		
		// killers
		if (KillerList.ENABLE_KILLER_MOVES) {
			assertEquals(killer1_gen, Move.toGenericMove(it.nextInt()));
			assertEquals(killer2_gen, Move.toGenericMove(it.nextInt()));
		}
		
		assertEquals(17, classUnderTest.getList(0).size());
	}
	
	@Test
	public void test_check_extended_search_moves_contain_only_promotions_captures_rook()throws IllegalNotationException {
		setup( "3q1rk1/p4pp1/2p4p/3p4/6Pr/1PNQ4/P1PB1PPb/4RR1K b - - - 2");
		classUnderTest.createForPly(Move.NULL_MOVE, null, true, false, 0);
		MoveListIterator it = classUnderTest.getExtendedIterator();
		
		// Capture
		assertEquals(new GenericMove("h4g4"), Move.toGenericMove(it.nextInt()));		
		assertFalse(it.hasNext());
	}
	
	@Test
	public void test_check_extended_search_moves_contain_only_promotions_captures_knight_queen()throws IllegalNotationException {
		PositionManager pm = new PositionManager("3q1rk1/p4pp1/2p4p/3p4/6Pr/1PNQ4/P1PB1PPb/4RR1K w - - - 2");
		classUnderTest = new MoveList(pm, 1);
		classUnderTest.createForPly(Move.NULL_MOVE, null, true, pm.isKingInCheck(), 0);
		MoveListIterator it = classUnderTest.getExtendedIterator();
		
		// Capture
		assertEquals(new GenericMove("c3d5"), Move.toGenericMove(it.nextInt()));
		assertEquals(new GenericMove("d2h6"), Move.toGenericMove(it.nextInt()));
		assertEquals(new GenericMove("d3d5"), Move.toGenericMove(it.nextInt()));
		assertFalse(it.hasNext());
	}
	
	@Test
	public void test_check_extended_search_moves_contain_only_promotions_captures_king()throws IllegalNotationException {
		PositionManager pm = new PositionManager("3q1rk1/p4pp1/2p4p/3p4/6P1/1PNQ4/P1PB1PPb/4RR1K w - - - 2");
		classUnderTest = new MoveList(pm, 1);
		classUnderTest.createForPly(Move.NULL_MOVE, null, true, pm.isKingInCheck(), 0);
		MoveListIterator it = classUnderTest.getExtendedIterator();
		
		// Capture - removed rook to make the king capture legal!
		assertEquals(new GenericMove("c3d5"), Move.toGenericMove(it.nextInt()));
		assertEquals(new GenericMove("d2h6"), Move.toGenericMove(it.nextInt()));
		assertEquals(new GenericMove("d3d5"), Move.toGenericMove(it.nextInt()));
		assertEquals(new GenericMove("h1h2"), Move.toGenericMove(it.nextInt()));
		assertFalse(it.hasNext());
	}
	
	@Test
	public void test_check_extended_search_moves_contain_only_promotions_and_captures_all()throws IllegalNotationException {
		PositionManager pm = new PositionManager("6k1/PBN5/8/2Kp4/2P5/5Q2/8/3R4 w - - 0 1 ");
		classUnderTest = new MoveList(pm, 1);
		MoveListIterator it = classUnderTest.createForPly(Move.NULL_MOVE, pm.isKingInCheck(), 0);
		
		// Promotion
		assertEquals(new GenericMove("a7a8Q"), Move.toGenericMove(it.nextInt()));
		// Captures
		assertEquals(new GenericMove("c4d5"), Move.toGenericMove(it.nextInt())); // PxP
		assertEquals(new GenericMove("c7d5"), Move.toGenericMove(it.nextInt())); // NxP
		assertEquals(new GenericMove("b7d5"), Move.toGenericMove(it.nextInt())); // BxP
		assertEquals(new GenericMove("d1d5"), Move.toGenericMove(it.nextInt())); // RxP
		assertEquals(new GenericMove("f3d5"), Move.toGenericMove(it.nextInt())); // QxP
		assertEquals(new GenericMove("c5d5"), Move.toGenericMove(it.nextInt())); // KxP
		
		assertFalse(it.hasNext());
		
		// check for identity between normal and extended moves
		int countOfStandardMoves = 0;
		MoveListIterator normal_it = classUnderTest.iterator();
		while (normal_it.hasNext()) {
			System.out.println(Move.toString(normal_it.nextInt()));
			countOfStandardMoves++;
		}
		assertEquals(7, countOfStandardMoves);
	}
	
	@Test
	public void test_mate_in_7_best_move() throws IllegalNotationException {
		PositionManager pm = new PositionManager("5Q2/6K1/8/3k4/8/8/8/8 w - - 1 113");
		int best = Move.valueOf(Position.f8, Piece.WHITE_QUEEN, Position.b4, Piece.NONE);
		classUnderTest = new MoveList(pm, 1);
		classUnderTest.createForPly(best, null, false, pm.isKingInCheck(), 0);
		assertEquals(new GenericMove("f8b4"), Move.toGenericMove(classUnderTest.getBestMove()));
	}
	
	@Test
	public void test_extended_search_iterator_has_next_is_null() {
		setup("8/8/8/8/8/1pp5/ppp5/Kp6 w - - - -"); // is_stalemate
		assertFalse(classUnderTest.getExtendedIterator().hasNext());
	}
	
	@Test
	public void test_attacked_piece_is_ordered_before_other_quiet_moves() throws IllegalNotationException {
		setup("7k/8/8/5n2/8/1PPPP3/8/7K w - - 0 1 ");
		MoveListIterator it =  classUnderTest.createForPly(Move.NULL_MOVE, null, false, false, 0);
		assertEquals(new GenericMove("e3e4"), Move.toGenericMove(it.nextInt())); // Attacked pawn is ordered first
	}
	
	@Test
	public void test_attacked_piece_is_ordered_before_other_quiet_moves_alt() throws IllegalNotationException {
		setup("7k/8/8/5n2/p7/1PPPP3/8/7K w - - 0 1 ");
		MoveListIterator it = classUnderTest.createForPly(Move.NULL_MOVE, null, false, false, 0);
		assertEquals(new GenericMove("b3a4"), Move.toGenericMove(it.nextInt())); // PxP
		assertEquals(new GenericMove("b3b4"), Move.toGenericMove(it.nextInt())); // Pawn attacked by pawn
		assertEquals(new GenericMove("e3e4"), Move.toGenericMove(it.nextInt())); // Attacked pawn is ordered first
	}

	@Test
	public void test_staged_best_move_valid() {
		PositionManager pm = new PositionManager("5Q2/6K1/8/3k4/8/8/8/8 w - - 1 113");
		int best = Move.valueOf(Position.f8, Piece.WHITE_QUEEN, Position.b4, Piece.NONE);
		best = Move.setBest(best);
		classUnderTest = new MoveList(pm, 1);
		classUnderTest.initialise(best, null, pm.isKingInCheck(), false, 0);
		MoveListIterator it = classUnderTest.stagedMoveGen(0);
		
		assertEquals(best, it.nextInt());
	}
	
	@Test
	public void test_staged_best_move_not_valid_no_promos_no_captures() {
		PositionManager pm = new PositionManager("5Q2/6K1/8/3k4/8/8/8/8 w - - 1 113");
		int best = Move.valueOf(Position.a1, Piece.WHITE_QUEEN, Position.a2, Piece.NONE);
		classUnderTest = new MoveList(pm, 1);
		classUnderTest.initialise(best, null, pm.isKingInCheck(), false, 0);
		MoveListIterator it = classUnderTest.stagedMoveGen(0);
		
		assertEquals(Move.valueOf(Position.g7, Piece.WHITE_KING, Position.g8, Piece.NONE), it.nextInt());
	}
	
	@Test
	public void test_staged_cp0_best_move_not_valid_so_returns_promo() {
		PositionManager pm = new PositionManager("5Q2/P5K1/8/3k4/5n2/8/8/8 w - - 1 113");
		int best = Move.valueOf(Position.a1, Piece.WHITE_QUEEN, Position.a2, Piece.NONE);
		classUnderTest = new MoveList(pm, 1);
		classUnderTest.initialise(best, null, pm.isKingInCheck(), false, 0);
		MoveListIterator it = classUnderTest.stagedMoveGen(0);
		
		assertEquals(Move.valueOf(Move.TYPE_PROMOTION_MASK, Position.a7, Piece.WHITE_PAWN, Position.a8, Piece.NONE, Piece.QUEEN), it.nextInt());
	}	
	
	@Test
	public void test_staged_best_move_valid_not_returned_twice() {
		PositionManager pm = new PositionManager("5Q2/P5K1/8/3k4/5n2/8/8/8 w - - 1 113");
		int best = Move.valueOf(Move.TYPE_PROMOTION_MASK, Position.a7, Piece.WHITE_PAWN, Position.a8, Piece.NONE, Piece.QUEEN);
		best = Move.setBest(best);
		classUnderTest = new MoveList(pm, 1);
		classUnderTest.initialise(best, null, pm.isKingInCheck(), false, 0);
		MoveListIterator it = classUnderTest.stagedMoveGen(0);
		
		assertEquals(best, it.nextInt());
		
	    it = classUnderTest.stagedMoveGen(0);
	    assertNotEquals(best, it.nextInt());
	}
	
	@Test
	public void test_compare_extended_search_against_normal_staged_moves_all()throws IllegalNotationException {
		PositionManager pm = new PositionManager("6k1/PBN5/8/2Kp4/2P5/5Q2/8/3R4 w - - 0 1 ");
		classUnderTest = new MoveList(pm, 1);
		MoveListIterator it = classUnderTest.createForPly(Move.NULL_MOVE, pm.isKingInCheck(), 0);
		
		classUnderTest.initialise(Move.NULL_MOVE, null, pm.isKingInCheck(), true, 0);
		MoveListIterator smg_it = classUnderTest.stagedMoveGen(0);
		// Promotion
		assertTrue(Move.areEqualForBestKiller(smg_it.nextInt(), it.nextInt())); // "a7a8Q"
		// Captures
		assertEquals(Move.toString(smg_it.nextInt()), Move.toString(it.nextInt())); // PxP "c4d5"
		assertEquals(Move.toString(smg_it.nextInt()), Move.toString(it.nextInt())); // NxP "c7d5"
		assertEquals(Move.toString(smg_it.nextInt()), Move.toString(it.nextInt())); // BxP "b7d5"
		assertEquals(Move.toString(smg_it.nextInt()), Move.toString(it.nextInt())); // RxP "d1d5"
		assertEquals(Move.toString(smg_it.nextInt()), Move.toString(it.nextInt())); // QxP "f3d5"
		assertEquals(Move.toString(smg_it.nextInt()), Move.toString(it.nextInt())); // KxP "c5d5"
		
		assertFalse(it.hasNext());
		assertFalse(smg_it.hasNext());
	}
}
