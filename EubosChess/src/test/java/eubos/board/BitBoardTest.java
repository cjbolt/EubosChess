package eubos.board;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import eubos.board.BitBoard;
import eubos.position.Position;

public class BitBoardTest {
	
	@Before
	public void setUp() {
	}
	
	@Test
	public void test_fill_down_all_empty() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.a8];
		long empty_mask = BitBoard.downOccludedEmpty(all_pieces, ~all_pieces);
		long expect_all_a_file_set = BitBoard.positionToMask_Lut[Position.a8] |
				BitBoard.positionToMask_Lut[Position.a7] | 
				BitBoard.positionToMask_Lut[Position.a6] |
				BitBoard.positionToMask_Lut[Position.a5] |
				BitBoard.positionToMask_Lut[Position.a4] |
				BitBoard.positionToMask_Lut[Position.a3] |
				BitBoard.positionToMask_Lut[Position.a2] |
				BitBoard.positionToMask_Lut[Position.a1];
		assertEquals(expect_all_a_file_set, empty_mask);
	}
	
	@Test
	public void test_fill_down_blocked_immediately() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.a8] |
				BitBoard.positionToMask_Lut[Position.a7];
		long our_piece = BitBoard.positionToMask_Lut[Position.a8];
		long empty_mask = BitBoard.downOccludedEmpty(our_piece, ~all_pieces);
		long expect_none_set = BitBoard.positionToMask_Lut[Position.a8];
		assertEquals(expect_none_set, empty_mask);
	}
	
	@Test
	public void test_fill_down_blocked_on_a2() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.a8] |
				BitBoard.positionToMask_Lut[Position.a2];
		long our_piece = BitBoard.positionToMask_Lut[Position.a8];
		long empty_mask = BitBoard.downOccludedEmpty(our_piece, ~all_pieces);
		long expect_almost_all_a_file_set = BitBoard.positionToMask_Lut[Position.a8] |
				BitBoard.positionToMask_Lut[Position.a7] | 
				BitBoard.positionToMask_Lut[Position.a6] |
				BitBoard.positionToMask_Lut[Position.a5] |
				BitBoard.positionToMask_Lut[Position.a4] |
				BitBoard.positionToMask_Lut[Position.a3];
		assertEquals(expect_almost_all_a_file_set, empty_mask);
	}
	
	@Test
	public void test_fill_down_none_possible() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.a1];
		long empty_mask = BitBoard.downOccludedEmpty(all_pieces, ~all_pieces);
		long expect_only_original_sq_set = BitBoard.positionToMask_Lut[Position.a1];
		assertEquals(expect_only_original_sq_set, empty_mask);
	}
	
	@Test
	public void test_fill_right_all() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.a1];
		long empty_mask = BitBoard.rightOccludedEmpty(all_pieces, ~all_pieces);
		long expect_all_first_rank_set = BitBoard.positionToMask_Lut[Position.a1] |
				BitBoard.positionToMask_Lut[Position.b1] | 
				BitBoard.positionToMask_Lut[Position.c1] |
				BitBoard.positionToMask_Lut[Position.d1] |
				BitBoard.positionToMask_Lut[Position.e1] |
				BitBoard.positionToMask_Lut[Position.f1] |
				BitBoard.positionToMask_Lut[Position.g1] |
				BitBoard.positionToMask_Lut[Position.h1];
		assertEquals(expect_all_first_rank_set, empty_mask);
	}
	
	@Test
	public void test_fill_right_none() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.h1];
		long empty_mask = BitBoard.rightOccludedEmpty(all_pieces, ~all_pieces);
		long expect_none_set = BitBoard.positionToMask_Lut[Position.h1];
		assertEquals(expect_none_set, empty_mask);
	}
	
	@Test
	public void test_fill_right_almost_all() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.a1] |
				BitBoard.positionToMask_Lut[Position.h1];
		long our_piece = BitBoard.positionToMask_Lut[Position.a1];
		long empty_mask = BitBoard.rightOccludedEmpty(our_piece, ~all_pieces);
		long expect_all_first_rank_set = BitBoard.positionToMask_Lut[Position.a1] |
				BitBoard.positionToMask_Lut[Position.b1] | 
				BitBoard.positionToMask_Lut[Position.c1] |
				BitBoard.positionToMask_Lut[Position.d1] |
				BitBoard.positionToMask_Lut[Position.e1] |
				BitBoard.positionToMask_Lut[Position.f1] |
				BitBoard.positionToMask_Lut[Position.g1];
		assertEquals(expect_all_first_rank_set, empty_mask);
	}
}