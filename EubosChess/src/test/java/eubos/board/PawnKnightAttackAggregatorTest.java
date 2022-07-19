package eubos.board;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import eubos.board.Board.PawnKnightAttackAggregator;
import eubos.position.Position;
import eubos.position.PositionManager;
import eubos.search.DrawChecker;

class PawnKnightAttackAggregatorTest {
	
	private PawnKnightAttackAggregator classUnderTest;		
	PositionManager pm;
	
	private boolean attackerIsBlack = false;
	private long[] attacksMask;
	
	protected void setUpPosition(String fen) {
		pm = new PositionManager(fen, new DrawChecker());
		classUnderTest = pm.getTheBoard().pkaa;
	}

	@Test
	public void test_counted_attacks_one_overlap() {
		setUpPosition("8/8/8/8/8/8/P1P5/8 w - - 0 1");
		attacksMask = classUnderTest.getAttacks(attackerIsBlack);
		assertArrayEquals(new long[] {0xA0000L, 0x20000L, 0L, 0L, 0L}, attacksMask);
	}
	
	@Test
	public void test_counted_attacks_all_attacked_twice_apart_from_rook_pawns() {
		setUpPosition("8/8/8/8/8/8/PPPPPPPP/8 w - - 0 1");
		attacksMask = classUnderTest.getAttacks(attackerIsBlack);
		assertArrayEquals(new long[] {0xFF0000L, 0x7E0000L, 0L, 0L, 0L}, attacksMask);
	}
	
	@Test
	public void test_counted_attacks_one_overlap_black() {
		setUpPosition("8/8/8/8/8/8/p1p5/8 w - - 0 1");
		attacksMask = classUnderTest.getAttacks(attackerIsBlack=true);
		assertArrayEquals(new long[] {0xAL, 0x2L, 0L, 0L, 0L}, attacksMask);
	}
	
	@Test
	public void test_counted_attacks_all_attacked_twice_apart_from_rook_pawns_black() {
		setUpPosition("8/8/8/8/8/8/pppppppp/8 w - - 0 1");
		attacksMask = classUnderTest.getAttacks(attackerIsBlack=true);
		assertArrayEquals(new long[] {0xFFL, 0x7EL, 0L, 0L, 0L}, attacksMask);
	}
	
	@Test
	public void test_counted_attacks_pawns_and_knight() {
		setUpPosition("8/8/8/8/1N6/8/P1P5/8 w - - 0 1");
		attacksMask = classUnderTest.getAttacks(attackerIsBlack);
		assertArrayEquals(new long[] {5531918533888L, 0xA0000L, 0L, 0L, 0L}, attacksMask);
	}
	
	@Test
	public void test_counted_attacks_e5_attacked_5_times() {
		setUpPosition("8/3N1N2/8/8/3P1P2/5N2/8/8 b - - 0 1");
		attacksMask = classUnderTest.getAttacks(attackerIsBlack);
		assertArrayEquals(new long[] {-6196765807226091440L, 360777252864L, 343597383680L, 68719476736L, 68719476736L}, attacksMask);
	}
	
	@Test
	public void test_counted_attacks_e5_attacked_6_times() {
		setUpPosition("8/3N1N2/8/8/3P1P2/3N1N2/8/8 b - - 0 1");
		attacksMask = classUnderTest.getAttacks(attackerIsBlack);
		assertArrayEquals(new long[] {
				BitBoard.valueOf(new int[] {Position.c1, Position.e1, Position.g1,
						Position.b2, Position.d2, Position.f2, Position.h2,
						Position.b4, Position.d4, Position.f4, Position.h4,
						Position.c5, Position.e5, Position.g5,
						Position.b6, Position.d6, Position.f6, Position.h6,
						Position.b8, Position.d8, Position.f8, Position.h8}),
				BitBoard.valueOf(new int[] {Position.e1,
						Position.c5, Position.e5, Position.g5}), 
				BitBoard.valueOf(new int[] {
						Position.c5, Position.e5, Position.g5}),
				BitBoard.valueOf(new int[] { Position.e5}),
				BitBoard.valueOf(new int[] { Position.e5})},
				attacksMask);
	} 
}

