package eubos.position;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.board.Piece;

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
	public void test_setBestMove() throws IllegalNotationException {
		int expected = Move.valueOf(Position.g3, Piece.WHITE_KING, Position.f2, Piece.NONE); 
		setup("8/8/4n1p1/1R3p1p/3k3P/2rB2K1/2P3P1/8 w - - 15 51");
		Iterator<Integer> it = classUnderTest.iterator();
		assertFalse(Move.areEqual(expected, it.next()));
		classUnderTest.reorderWithNewBestMove(expected);
		it = classUnderTest.iterator();
		assertTrue(Move.areEqual(expected, it.next()));
	}
	
	@Test
	public void test_bestMove_ChangedAgain() throws IllegalNotationException {
		GenericMove expected = new GenericMove("b7b6"); 
		setup("r1bqkb1r/ppp1pppp/2n5/3p4/3PN3/4PN2/PPP2PPP/R1BQKB1R b KQkq - - 5");
		
		Iterator<Integer> it = classUnderTest.iterator();
		assertNotEquals(expected, Move.toGenericMove(it.next()));
		
		classUnderTest.reorderWithNewBestMove(Move.toMove(new GenericMove("a7a6")));
		it = classUnderTest.iterator();
		assertEquals(new GenericMove("a7a6"), Move.toGenericMove(it.next()));
		
		classUnderTest.reorderWithNewBestMove(Move.toMove(expected));
		it = classUnderTest.iterator();
		assertEquals(expected, Move.toGenericMove(it.next()));
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
	public void test_whenChangedBestCapture_BothIteratorsAreUpdated() throws IllegalNotationException {
		setup("8/1B6/8/3q1r2/4P3/8/8/8 w - - 0 1");
		Iterator<Integer> it = classUnderTest.getStandardIterator(NORMAL);
		GenericMove first = Move.toGenericMove(it.next());
		
		int newBestCapture = Move.valueOf(Position.e4, Piece.NONE, Position.f5, Piece.NONE);
		assertNotEquals(first, newBestCapture);
		
		classUnderTest.reorderWithNewBestMove(newBestCapture);
		
		it = classUnderTest.getStandardIterator(NORMAL);
		assertTrue(it.hasNext());
		assertTrue(Move.areEqual(newBestCapture, it.next()));
		
		it = classUnderTest.getStandardIterator(EXTENDED);
		assertTrue(it.hasNext());
		assertTrue(Move.areEqual(newBestCapture, it.next()));
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
	public void test_ChangingBestMove() {
		setup("8/8/5P2/4P3/3P4/2P5/8/8 w - - 0 1");
		Iterator<Integer> it = classUnderTest.iterator();
		int [] original_moves = new int[4];
		for (int i=0; i<original_moves.length; i++)
			original_moves[i] = it.next();
		
		// Set new best move
		classUnderTest.reorderWithNewBestMove(original_moves[3]);
		it = classUnderTest.iterator();
		int [] reordered_moves = new int[4];
		for (int i=0; i<reordered_moves.length; i++)
			reordered_moves[i] = it.next();
		assertEquals(original_moves[0], reordered_moves[3]);
		assertEquals(original_moves[3], reordered_moves[0]);
		
		// Revert change
		classUnderTest.reorderWithNewBestMove(original_moves[0]);
		it = classUnderTest.iterator();
		reordered_moves = new int[4];
		for (int i=0; i<reordered_moves.length; i++)
			reordered_moves[i] = it.next();
		assertEquals(original_moves[0], reordered_moves[0]);
		assertEquals(original_moves[3], reordered_moves[3]);
		
		// Set second move as best
		classUnderTest.reorderWithNewBestMove(original_moves[1]);
		it = classUnderTest.iterator();
		reordered_moves = new int[4];
		for (int i=0; i<reordered_moves.length; i++)
			reordered_moves[i] = it.next();
		assertEquals(original_moves[0], reordered_moves[1]);
		assertEquals(original_moves[1], reordered_moves[0]);
		
		// Revert change
		classUnderTest.reorderWithNewBestMove(original_moves[0]);
		it = classUnderTest.iterator();
		reordered_moves = new int[4];
		for (int i=0; i<reordered_moves.length; i++)
			reordered_moves[i] = it.next();
		assertEquals(original_moves[0], reordered_moves[0]);
		assertEquals(original_moves[1], reordered_moves[1]);
		assertEquals(original_moves[2], reordered_moves[2]);
		assertEquals(original_moves[3], reordered_moves[3]);
		
		// Set third move as best
		classUnderTest.reorderWithNewBestMove(original_moves[2]);
		it = classUnderTest.iterator();
		reordered_moves = new int[4];
		for (int i=0; i<reordered_moves.length; i++)
			reordered_moves[i] = it.next();
		assertEquals(original_moves[0], reordered_moves[2]);
		assertEquals(original_moves[1], reordered_moves[1]);
		assertEquals(original_moves[2], reordered_moves[0]);
		assertEquals(original_moves[3], reordered_moves[3]);
		
		// Set fourth move as best
		classUnderTest.reorderWithNewBestMove(original_moves[3]);
		it = classUnderTest.iterator();
		reordered_moves = new int[4];
		for (int i=0; i<reordered_moves.length; i++)
			reordered_moves[i] = it.next();
		assertEquals(original_moves[0], reordered_moves[3]);
		assertEquals(original_moves[1], reordered_moves[1]);
		assertEquals(original_moves[2], reordered_moves[2]);
		assertEquals(original_moves[3], reordered_moves[0]);
		
		// Set fourth move as best
		classUnderTest.reorderWithNewBestMove(reordered_moves[3]);
		it = classUnderTest.iterator();
		reordered_moves = new int[4];
		for (int i=0; i<reordered_moves.length; i++)
			reordered_moves[i] = it.next();
		assertEquals(original_moves[0], reordered_moves[0]);
		assertEquals(original_moves[1], reordered_moves[1]);
		assertEquals(original_moves[2], reordered_moves[2]);
		assertEquals(original_moves[3], reordered_moves[3]);
		
		// Set fourth move as best
		classUnderTest.reorderWithNewBestMove(original_moves[2]);
		it = classUnderTest.iterator();
		reordered_moves = new int[4];
		for (int i=0; i<reordered_moves.length; i++)
			reordered_moves[i] = it.next();
		assertEquals(original_moves[0], reordered_moves[2]);
		assertEquals(original_moves[1], reordered_moves[1]);
		assertEquals(original_moves[2], reordered_moves[0]);
		assertEquals(original_moves[3], reordered_moves[3]);	
	}
	
	@Test
	public void test_ChangingBestMove_WhenSeeded() throws IllegalNotationException {
		PositionManager pm = new PositionManager( "8/8/5P2/4P3/3P4/2P5/8/8 w - - 0 1" );
		classUnderTest = new MoveList(pm, Move.valueOf(Position.f6, Piece.WHITE_PAWN, Position.f7, Piece.NONE)); // seed with last move as best
		Iterator<Integer> it = classUnderTest.iterator();
		int [] original_moves = new int[4];
		for (int i=0; i<original_moves.length; i++)
			original_moves[i] = it.next();
		System.out.println("original - seeded last as best");
		System.out.println(Arrays.toString(original_moves));
		
		// Set new best move as second move
		classUnderTest.reorderWithNewBestMove(original_moves[2]);
		it = classUnderTest.iterator();
		int [] reordered_moves = new int[4];
		for (int i=0; i<reordered_moves.length; i++)
			reordered_moves[i] = it.next();
		
		System.out.println("3rd as best");
		System.out.println(Arrays.toString(reordered_moves));
		assertEquals(original_moves[2], reordered_moves[0]);
		assertEquals(original_moves[0], reordered_moves[2]);
		
		// Revert change
		System.out.println("revert");
		classUnderTest.reorderWithNewBestMove(reordered_moves[2]);
		it = classUnderTest.iterator();
		reordered_moves = new int[4];
		for (int i=0; i<reordered_moves.length; i++)
			reordered_moves[i] = it.next();
		System.out.println(Arrays.toString(reordered_moves));
		assertEquals(reordered_moves[0], original_moves[0]);
		assertEquals(reordered_moves[2], original_moves[2]);
		assertEquals(reordered_moves[3], original_moves[3]);
	}
	
	@Test
	public void test_whenMoveIsAlreadyBest_doNothing() throws IllegalNotationException {
		setup("8/8/5P2/4P3/3P4/2P5/8/8 w - - 0 1");
		Iterator<Integer> it = classUnderTest.iterator();
		int [] original_moves = new int[4];
		for (int i=0; i<original_moves.length; i++)
			original_moves[i] = it.next();
		
		// Set last as best move
		classUnderTest.reorderWithNewBestMove(original_moves[3]);
		it = classUnderTest.iterator();
		int [] reordered_moves = new int[4];
		for (int i=0; i<reordered_moves.length; i++)
			reordered_moves[i] = it.next();
		assertEquals(original_moves[0], reordered_moves[3]);
		assertEquals(original_moves[1], reordered_moves[1]);
		assertEquals(original_moves[2], reordered_moves[2]);
		assertEquals(original_moves[3], reordered_moves[0]);
		
		// Re-set last as best
		classUnderTest.reorderWithNewBestMove(original_moves[3]);
		it = classUnderTest.iterator();
		reordered_moves = new int[4];
		for (int i=0; i<reordered_moves.length; i++)
			reordered_moves[i] = it.next();
		assertEquals(original_moves[0], reordered_moves[3]);
		assertEquals(original_moves[1], reordered_moves[1]);
		assertEquals(original_moves[2], reordered_moves[2]);
		assertEquals(original_moves[3], reordered_moves[0]);		
	}
	
	@Test
	public void test_from_lichess_game_after_int_move_change() {
		setup("6r1/7p/4Pk1P/1p3p2/p1pN4/P1P2KPR/2P5/6r1 b - - 12 44");
		Iterator<Integer> it = classUnderTest.getStandardIterator(NORMAL);
		int [] original_moves = new int[27];
		int i=0;
		String ml = "";
		while(it.hasNext()) {
			original_moves[i] = it.next();
			ml += Move.toGenericMove(original_moves[i]);
			ml += " ";
			i+=1;
		}
		System.out.println(ml);
		// ply 1
		classUnderTest.reorderWithNewBestMove(original_moves[1]); //g8g3
		classUnderTest.reorderWithNewBestMove(original_moves[1]); //g8g3
		classUnderTest.reorderWithNewBestMove(original_moves[2]); //g1f1
		classUnderTest.reorderWithNewBestMove(original_moves[4]); //g1e1
		// ply 2
		classUnderTest.reorderWithNewBestMove(original_moves[2]); //g1f1
		classUnderTest.reorderWithNewBestMove(original_moves[14]); //f6e5
		classUnderTest.reorderWithNewBestMove(original_moves[22]); //g8d8
		// ply 3
		// ply 4
		// ply 5
		// ply 6
		// ply 7
		// ply 8
		classUnderTest.reorderWithNewBestMove(original_moves[2]); //??? pv was null
		classUnderTest.reorderWithNewBestMove(original_moves[6]); //f3f2
		
		it = classUnderTest.getStandardIterator(NORMAL);
		ml = "";
		while(it.hasNext()) {
			ml += Move.toGenericMove(it.next());
			ml += " ";
		}
		System.out.println(ml);
	}
}
