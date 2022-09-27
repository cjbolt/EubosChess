package eubos.board;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import eubos.board.Board.PawnAttackAggregator;
import eubos.position.PositionManager;
import eubos.score.PawnEvalHashTable;
import eubos.search.DrawChecker;

class PawnAttackAggregatorTest {
	
	private PawnAttackAggregator classUnderTest;		
	PositionManager pm;
	
	private boolean attackerIsBlack = false;
	private long[] attacksMask = {0L, 0L};
	
	protected void setUpPosition(String fen) {
		pm = new PositionManager(fen, new DrawChecker(), new PawnEvalHashTable());
		classUnderTest = pm.getTheBoard().paa;
	}

	@Test
	public void test_counted_attacks_one_overlap() {
		setUpPosition("8/8/8/8/8/8/P1P5/8 w - - 0 1");
		classUnderTest.getPawnAttacks(attacksMask, attackerIsBlack);
		assertArrayEquals(new long[] {0xA0000L, 0x20000L}, attacksMask);
	}
	
	@Test
	public void test_counted_attacks_all_attacked_twice_apart_from_rook_pawns() {
		setUpPosition("8/8/8/8/8/8/PPPPPPPP/8 w - - 0 1");
		classUnderTest.getPawnAttacks(attacksMask, attackerIsBlack);
		assertArrayEquals(new long[] {0xFF0000L, 0x7E0000L}, attacksMask);
	}
	
	@Test
	public void test_counted_attacks_one_overlap_black() {
		setUpPosition("8/8/8/8/8/8/p1p5/8 w - - 0 1");
		classUnderTest.getPawnAttacks(attacksMask, attackerIsBlack=true);
		assertArrayEquals(new long[] {0xAL, 0x2L}, attacksMask);
	}
	
	@Test
	public void test_counted_attacks_all_attacked_twice_apart_from_rook_pawns_black() {
		setUpPosition("8/8/8/8/8/8/pppppppp/8 w - - 0 1");
		classUnderTest.getPawnAttacks(attacksMask, attackerIsBlack=true);
		assertArrayEquals(new long[] {0xFFL, 0x7EL}, attacksMask);
	}
}
