package eubos.pieces;

import org.junit.Test;

public abstract class PawnTest {

	@Test
	public abstract void test_InitialMoveOneSquare();
	
	@Test
	public abstract void test_InitialMoveTwoSquares();
	
	@Test
	public abstract void test_CaptureEnPassant();
	
	@Test
	public abstract void test_MoveOneSquare();
	
	@Test
	public abstract void test_CaptureLeft();
	
	@Test
	public abstract void test_CaptureRight();
		
	@Test
	public abstract void test_PromoteQueen();
	
	@Test
	public abstract void test_PromoteKnight();

	@Test
	public abstract void test_PromoteBishop();
	
	@Test
	public abstract void test_PromoteRook();
}
