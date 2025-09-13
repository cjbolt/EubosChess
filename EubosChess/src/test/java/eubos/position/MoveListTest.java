package eubos.position;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.search.KillerList;

public class MoveListTest {

	public static final boolean EXTENDED = true;
	public static final boolean NORMAL = false;
	
	protected MoveList classUnderTest;
	MoveListIterator it;
	
	private void setup(String fen)  {
		PositionManager pm = new PositionManager( fen );
		classUnderTest = new MoveList(pm, 1);
		it = classUnderTest.initialiseAtPly(Move.NULL_MOVE, null, pm.isKingInCheck(), false, 0);
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
		assertTrue(it.hasNext()); // Now we generate all moves, this returns three illegal pawn captures		
	}
	
	@Test
	public void testCreateMoveList_CapturesFirst() throws IllegalNotationException {
		setup("8/3k3B/8/1p6/2P5/8/4K3/8 w - - 0 1 ");
		assertEquals(new GenericMove("c4b5"), Move.toGenericMove(it.nextInt()));
	}
	
	@Test
	public void testCreateMoveList_typePromotionIsSet() throws IllegalNotationException {
		setup("8/4P3/8/8/8/8/8/8 w - - - -");
		assertEquals(new GenericMove("e7e8q"), Move.toGenericMove(it.nextInt()));
		assertEquals(new GenericMove("e7e8n"), Move.toGenericMove(it.nextInt()));
	}
	
	@Test
	public void test_whenNoChecksCapturesOrPromotions() throws IllegalNotationException { 
		setup("8/3p4/8/8/8/5k2/1P6/7K w - - 0 1");
		assertTrue(it.hasNext());
		it = classUnderTest.initialiseAtPly(Move.NULL_MOVE, null, false, true, 0);
		//MoveListIterator extended_iter = classUnderTest.getNextMovesAtPly(0);
		assertFalse(it.hasNext());
		
	}
	
	@Test
	public void test_whenCheckAndCapturePossible() throws IllegalNotationException {
		setup("8/K7/8/8/4B1R1/8/6q1/7k w - - 0 1 ");
		assertEquals(new GenericMove("e4g2"), Move.toGenericMove(it.nextInt())); // capture (happens to have check)
		assertEquals(new GenericMove("g4g2"), Move.toGenericMove(it.nextInt())); // capture
	}
	
	@Test
	public void test_whenPromotionAndPromoteWithCaptureAndCheckPossible()throws IllegalNotationException {
		setup("q1n5/1P6/8/8/8/8/1K6/7k w - - 0 1 ");
		assertEquals(new GenericMove("b7a8q"), Move.toGenericMove(it.nextInt())); // Promotion with check and capture
		assertEquals(new GenericMove("b7c8q"), Move.toGenericMove(it.nextInt())); // Promotion and capture
		assertEquals(new GenericMove("b7a8n"), Move.toGenericMove(it.nextInt())); // Promotion (piece) and capture
		assertEquals(new GenericMove("b7c8n"), Move.toGenericMove(it.nextInt())); // Promotion (piece) and capture
		assertEquals(new GenericMove("b7b8q"), Move.toGenericMove(it.nextInt())); // Promotion
		assertEquals(new GenericMove("b7b8n"), Move.toGenericMove(it.nextInt())); // Promotion (piece)
	}
	
	@Test
	public void test_mvv_lva_order()throws IllegalNotationException {
		setup("7K/N2B4/Q3q3/1r3PN1/2P3B1/4Rp2/6Pk/1R6 w - - 0 1");
		
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
		setup("7K/N2Bk3/Q3p3/1r3PN1/2P3B1/4Rp2/6P1/1R6 w - - 0 1");
		
		// gaining material
		assertEquals(new GenericMove("c4b5"), Move.toGenericMove(it.nextInt())); // PxR delta 3 gains 4
		assertEquals(new GenericMove("a7b5"), Move.toGenericMove(it.nextInt())); // NxR delta 2 gains 2
		assertEquals(new GenericMove("d7b5"), Move.toGenericMove(it.nextInt())); // BxR delta 1 gains 2
		
		// neutral exchanges
		assertEquals(new GenericMove("b1b5"), Move.toGenericMove(it.nextInt())); // RxR
		assertEquals(new GenericMove("g2f3"), Move.toGenericMove(it.nextInt())); // PxP
		assertEquals(new GenericMove("f5e6"), Move.toGenericMove(it.nextInt())); // PxP
		
		// losing material
		assertEquals(new GenericMove("g5f3"), Move.toGenericMove(it.nextInt())); // NxP delta -1 loses 2
		assertEquals(new GenericMove("g5e6"), Move.toGenericMove(it.nextInt())); // NxP delta -1 loses 2
		assertEquals(new GenericMove("g4f3"), Move.toGenericMove(it.nextInt())); // BxP delta -2 loses 2
		assertEquals(new GenericMove("d7e6"), Move.toGenericMove(it.nextInt())); // BxP delta -2 loses 2
		assertEquals(new GenericMove("e3f3"), Move.toGenericMove(it.nextInt())); // RxP delta -3 loses 4 losing material but checks
		assertEquals(new GenericMove("e3e6"), Move.toGenericMove(it.nextInt())); // RxP delta -3 loses 4
		assertEquals(new GenericMove("a6b5"), Move.toGenericMove(it.nextInt())); // QxR delta -1 loses 4
		assertEquals(new GenericMove("a6e6"), Move.toGenericMove(it.nextInt())); // QxP delta -4 loses 8 losing material but checks
	}
	
	@Test
	public void test_move_ordering_when_mix_of_captures_and_checks()throws IllegalNotationException {
		// as prior test but adds a king into the mix
		setup("7K/4k3/4p3/5PN1/8/4R1q1/8/8 w - - 0 1");
		
		// gaining material
		assertEquals(new GenericMove("e3g3"), Move.toGenericMove(it.nextInt())); // RxQ delta 1 gains 9
		
		// neutral material
		assertEquals(new GenericMove("f5e6"), Move.toGenericMove(it.nextInt())); // PxP
		
		// losing material
		assertEquals(new GenericMove("g5e6"), Move.toGenericMove(it.nextInt())); // NxP delta -1 loses 2
		assertEquals(new GenericMove("e3e6"), Move.toGenericMove(it.nextInt())); // RxP delta -3 loses 4 losing material (happens to check, but that is ignored)

		// regular moves
		assertEquals(new GenericMove("g5h7"), Move.toGenericMove(it.nextInt())); // Regular move
		assertEquals(new GenericMove("g5h3"), Move.toGenericMove(it.nextInt())); // Regular move
		assertEquals(new GenericMove("g5f3"), Move.toGenericMove(it.nextInt())); // Regular move
	}
	
	@Test
	public void test_move_ordering_when_mix_of_promotions_captures_and_checks()throws IllegalNotationException {
		// as prior test but adds a king into the mix
		setup("1n5K/P3kP2/8/1Pp2P2/8/8/8/8 w - c6 0 1");
		
		// Promotions with capture
		assertEquals(new GenericMove("a7b8q"), Move.toGenericMove(it.nextInt())); // Queen promotion with capture, PxN
		assertEquals(new GenericMove("a7b8n"), Move.toGenericMove(it.nextInt())); // Rook promotion with capture, PxN
		assertEquals(new GenericMove("a7a8q"), Move.toGenericMove(it.nextInt())); // Queen promotion
		assertEquals(new GenericMove("f7f8q"), Move.toGenericMove(it.nextInt())); // Queen promotion (with check)
		assertEquals(new GenericMove("a7a8n"), Move.toGenericMove(it.nextInt())); // Rook promotion
		assertEquals(new GenericMove("f7f8n"), Move.toGenericMove(it.nextInt())); // Rook promotion
		
		// Captures
		assertEquals(new GenericMove("b5c6"), Move.toGenericMove(it.nextInt())); // En Passant capture, PxP
		
		// Regular moves
		assertEquals(new GenericMove("b5b6"), Move.toGenericMove(it.nextInt())); // Regular pawn move
		assertEquals(new GenericMove("f5f6"), Move.toGenericMove(it.nextInt())); // Pawn check
		assertEquals(new GenericMove("h8h7"), Move.toGenericMove(it.nextInt()));
		assertEquals(new GenericMove("h8g7"), Move.toGenericMove(it.nextInt()));
		assertEquals(new GenericMove("h8g8"), Move.toGenericMove(it.nextInt()));
		
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
		it = classUnderTest.initialiseAtPly(best, killers, pm.isKingInCheck(), false, 0);
		
		// best
		assertEquals(best_gen, Move.toGenericMove(it.nextInt()));

		// capture
		assertEquals(new GenericMove("c4b5"), Move.toGenericMove(it.nextInt()));

		// killers
		if (KillerList.ENABLE_KILLER_MOVES) {
			assertEquals(killer1_gen, Move.toGenericMove(it.nextInt()));
			assertEquals(killer2_gen, Move.toGenericMove(it.nextInt()));
		}

		// 4 moves already returned, there are 17 possible moves
		assertEquals(13, classUnderTest.getList(it).size());
	}
	
	@Test
	public void test_check_extended_search_moves_contain_only_promotions_captures_rook()throws IllegalNotationException {
		setup( "3q1rk1/p4pp1/2p4p/3p4/6Pr/1PNQ4/P1PB1PPb/4RR1K b - - - 2");
		it = classUnderTest.initialiseAtPly(Move.NULL_MOVE, null, false, true, 0);
		
		// Capture
		assertEquals(new GenericMove("h4g4"), Move.toGenericMove(it.nextInt()));		
		assertFalse(it.hasNext());
	}
	
	@Test
	public void test_check_extended_search_moves_contain_only_promotions_captures_knight_queen()throws IllegalNotationException {
		PositionManager pm = new PositionManager("3q1rk1/p4pp1/2p4p/3p4/6Pr/1PNQ4/P1PB1PPb/4RR1K w - - - 2");
		classUnderTest = new MoveList(pm, 1);
		it = classUnderTest.initialiseAtPly(Move.NULL_MOVE, null, pm.isKingInCheck(), true, 0);
		
		// Capture
		assertEquals(new GenericMove("c3d5"), Move.toGenericMove(it.nextInt()));
		assertEquals(new GenericMove("d2h6"), Move.toGenericMove(it.nextInt()));
		assertEquals(new GenericMove("d3d5"), Move.toGenericMove(it.nextInt()));
		assertEquals(new GenericMove("h1h2"), Move.toGenericMove(it.nextInt())); // The move is illegal, but it is caught when applied
		assertFalse(it.hasNext());
	}
	
	@Test
	public void test_check_extended_search_moves_contain_only_promotions_captures_king()throws IllegalNotationException {
		PositionManager pm = new PositionManager("3q1rk1/p4pp1/2p4p/3p4/6P1/1PNQ4/P1PB1PPb/4RR1K w - - - 2");
		classUnderTest = new MoveList(pm, 1);
		it = classUnderTest.initialiseAtPly(Move.NULL_MOVE, null, pm.isKingInCheck(), true, 0);
		
		// Capture - removed rook to make the king capture legal!
		assertEquals(new GenericMove("c3d5"), Move.toGenericMove(it.nextInt()));
		assertEquals(new GenericMove("d2h6"), Move.toGenericMove(it.nextInt()));
		assertEquals(new GenericMove("d3d5"), Move.toGenericMove(it.nextInt()));
		assertEquals(new GenericMove("h1h2"), Move.toGenericMove(it.nextInt()));
		assertFalse(it.hasNext());
	}
	
	@Test
	public void test_amateur_104th_defect() throws IllegalNotationException {
		setup("2kr3r/pbppb3/4p3/n1P5/5qPp/P1P5/2QNBB1P/R4RK1 b - - 0 21");
				
		// Captures
		assertEquals(new GenericMove("e7c5"), Move.toGenericMove(it.nextInt())); /* Losing bishop captures pawn */
		assertEquals(new GenericMove("f4f2"), Move.toGenericMove(it.nextInt())); /* Losing queen captures bishop */
		assertEquals(new GenericMove("f4d2"), Move.toGenericMove(it.nextInt())); /* Losing queen captures knight */
		assertEquals(new GenericMove("f4h2"), Move.toGenericMove(it.nextInt())); /* Losing queen captures pawn */
		assertEquals(new GenericMove("f4g4"), Move.toGenericMove(it.nextInt())); /* Losing queen captures pawn */
		
		// Quiet
		assertEquals(new GenericMove("b7a6"), Move.toGenericMove(it.nextInt())); /* puts bishop en prise, not good */
		assertEquals(new GenericMove("b7a8"), Move.toGenericMove(it.nextInt()));
	}
	
	@Test
	public void test_check_extended_search_moves_contain_only_promotions_and_captures_all()throws IllegalNotationException {
		PositionManager pm = new PositionManager("6k1/PBN5/8/2Kp4/2P5/5Q2/8/3R4 w - - 0 1 ");
		classUnderTest = new MoveList(pm, 1);
		it = classUnderTest.initialiseAtPly(Move.NULL_MOVE, null, pm.isKingInCheck(), true, 1);
		
		// Promotion
		assertEquals(new GenericMove("a7a8Q"), Move.toGenericMove(it.nextInt()));
		if (EubosEngineMain.ENABLE_PERFT) {
			assertEquals(new GenericMove("a7a8R"), Move.toGenericMove(it.nextInt()));
			assertEquals(new GenericMove("a7a8B"), Move.toGenericMove(it.nextInt()));
			assertEquals(new GenericMove("a7a8N"), Move.toGenericMove(it.nextInt()));
		}

		// Captures
		assertEquals(new GenericMove("c4d5"), Move.toGenericMove(it.nextInt())); // PxP
		assertEquals(new GenericMove("c7d5"), Move.toGenericMove(it.nextInt())); // NxP
		assertEquals(new GenericMove("b7d5"), Move.toGenericMove(it.nextInt())); // BxP
		assertEquals(new GenericMove("d1d5"), Move.toGenericMove(it.nextInt())); // RxP
		assertEquals(new GenericMove("f3d5"), Move.toGenericMove(it.nextInt())); // QxP
		assertEquals(new GenericMove("c5d5"), Move.toGenericMove(it.nextInt())); // KxP
		
		// No more extended search moves
		assertFalse(it.hasNext());
		
		// Check expected normal moves number
		int countOfStandardMoves = 0;
		it = classUnderTest.initialiseAtPly(Move.NULL_MOVE, null, pm.isKingInCheck(), false, 1);
		while (it.hasNext()) {
			System.out.println(Move.toString(it.nextInt()));
			countOfStandardMoves++;
		}
		assertEquals(EubosEngineMain.ENABLE_PERFT ? 55 : 52, countOfStandardMoves); // Don't generate under promotions at ply 1, only at ply 0.
	}
	
	@Test
	public void test_crash_too_many_moves() throws IllegalNotationException {
		PositionManager pm = new PositionManager("5r1k/ppp4p/2n5/8/1P6/P4b1K/2P1q3/1R6 b - - 1 35");
		classUnderTest = new MoveList(pm, 1);
		it = classUnderTest.initialiseAtPly(Move.NULL_MOVE, null, pm.isKingInCheck(), true, 1);
		while (it.hasNext()) {
			System.out.println(Move.toString(it.nextInt()));
		}
		
		// Check expected normal moves number
		int countOfStandardMoves = 0;
		it = classUnderTest.initialiseAtPly(Move.NULL_MOVE, null, pm.isKingInCheck(), false, 1);
		while (it.hasNext()) {
			System.out.println(Move.toString(it.nextInt()));
			countOfStandardMoves++;
		}
		assertEquals(49, countOfStandardMoves); // Don't generate under promotions at ply 1, only at ply 0.
	}
	
	@Test
	public void test_mate_in_7_best_move() throws IllegalNotationException {
		PositionManager pm = new PositionManager("5Q2/6K1/8/3k4/8/8/8/8 w - - 1 113");
		int best = Move.valueOf(Position.f8, Piece.WHITE_QUEEN, Position.b4, Piece.NONE);
		classUnderTest = new MoveList(pm, 1);
		it = classUnderTest.initialiseAtPly(best, null, pm.isKingInCheck(), false, 0);
		assertEquals(new GenericMove("f8b4"), Move.toGenericMove(it.nextInt()));
	}
	
	@Test
	public void test_extended_search_iterator_has_next_is_null() {
		setup("k7/8/8/8/8/1pp5/ppp5/Kp6 w - - - -"); // is_stalemate
		it = classUnderTest.initialiseAtPly(Move.NULL_MOVE, null, false, true, 0);
		assertTrue(it.hasNext()); // Now has pseudo legal capture
	}
	
	@Test
	public void test_staged_best_move_valid() {
		PositionManager pm = new PositionManager("5Q2/6K1/8/3k4/8/8/8/8 w - - 1 113");
		int best = Move.valueOf(Position.f8, Piece.WHITE_QUEEN, Position.b4, Piece.NONE);
		best = Move.setBest(best);
		classUnderTest = new MoveList(pm, 1);
		it = classUnderTest.initialiseAtPly(best, null, pm.isKingInCheck(), false, 0);
		
		assertEquals(best, it.nextInt());
	}
	
	@Test
	public void test_staged_best_move_not_valid_no_promos_no_captures() {
		PositionManager pm = new PositionManager("5Q2/6K1/8/3k4/8/8/8/8 w - - 1 113");
		int best = Move.valueOf(Position.a1, Piece.WHITE_QUEEN, Position.a2, Piece.NONE);
		classUnderTest = new MoveList(pm, 1);
		it = classUnderTest.initialiseAtPly(best, null, pm.isKingInCheck(), false, 0);
		
		assertEquals(Move.toString(Move.valueOf(Position.f8, Piece.WHITE_QUEEN, Position.e7, Piece.NONE)), Move.toString(it.nextInt()));
	}
	
	@Test
	public void test_staged_cp0_best_move_not_valid_so_returns_promo() {
		PositionManager pm = new PositionManager("5Q2/P5K1/8/3k4/5n2/8/8/8 w - - 1 113");
		int best = Move.valueOf(Position.a1, Piece.WHITE_QUEEN, Position.a2, Piece.NONE);
		classUnderTest = new MoveList(pm, 1);
		it = classUnderTest.initialiseAtPly(best, null, pm.isKingInCheck(), false, 0);
		
		assertEquals(Move.valueOf(Move.TYPE_PROMOTION_MASK, Position.a7, Piece.WHITE_PAWN, Position.a8, Piece.NONE, Piece.QUEEN), it.nextInt());
	}	
	
	@Test
	public void test_staged_best_move_valid_not_returned_twice() {
		PositionManager pm = new PositionManager("5Q2/P5K1/8/3k4/5n2/8/8/8 w - - 1 113");
		int best = Move.valueOf(Move.TYPE_PROMOTION_MASK, Position.a7, Piece.WHITE_PAWN, Position.a8, Piece.NONE, Piece.QUEEN);
		best = Move.setBest(best);
		classUnderTest = new MoveList(pm, 1);
		it = classUnderTest.initialiseAtPly(best, null, pm.isKingInCheck(), false, 0);
		
		assertEquals(best, it.nextInt());
	    assertNotEquals(best, it.nextInt());
	}
	
	@Test
	public void test_moved_into_knight_check() {
		PositionManager pm = new PositionManager("1r1qkb1r/5ppp/pN1pbn2/8/1p1QP3/4B3/PPP1NPPP/R3K2R b KQk - 4 15");
		int illegal = Move.valueOf(Position.e8, Piece.BLACK_KING, Position.d7, Piece.NONE);
		classUnderTest = new MoveList(pm, 1);
		int move = MoveList.getRandomMove(pm);
	    assertNotEquals(illegal, move);		
	}
}
