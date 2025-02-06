package eubos.position;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eubos.score.PawnEvalHashTable;
import eubos.search.DrawChecker;

public class MoveTest {
	MoveList ml;
	
	protected void setUpPosition(String fen) {
		PositionManager pm = new PositionManager(fen, new DrawChecker(), new PawnEvalHashTable());
		ml = new MoveList(pm, 1);
	}
	
	@Before
	public void setUp() {
	}
	
	@After
	public void tearDown() {
	}
	
	@Test
	public void test_template() throws InterruptedException {
		setUpPosition("k7/8/8/8/8/8/8/6RK w - - 0 25 ");
		MoveListIterator normal_it = ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0);
		while (normal_it.hasNext()) {
			normal_it.nextInt();
		}
	}
	
	@Test
	public void test_optimised_unload() throws InterruptedException {
		setUpPosition("r1bq1rk1/pppnbppp/3p1n2/4p3/1PP5/2N1PN2/PB1PBPPP/R2QK2R b KQ - 6 7");
		MoveListIterator normal_it = ml.initialiseAtPly(Move.NULL_MOVE, null, false, false, 0);
		while (normal_it.hasNext()) {
			int move = normal_it.nextInt();
			// unload move
			int temp = move;
			int targetBitOffset = temp & 0x3F;
			temp >>>= 6;
			int originBitOffset = temp & 0x3F;
			temp >>>= 6;
			int promotedPiece = temp & 0x7;
			temp >>>= 4; // Skip enPassant flag as well
			int targetPiece = temp & 0xF;
			temp >>>= 4;
			int pieceToMove = temp & 0xF;
			temp >>>= 4;
			assertEquals(Move.getTargetPosition(move), targetBitOffset);
			assertEquals(Move.getOriginPosition(move), originBitOffset);
			assertEquals(Move.getPromotion(move), promotedPiece);
			assertEquals(Move.getTargetPiece(move), targetPiece);
			assertEquals(Move.getOriginPiece(move), pieceToMove);
		}
	}
}
