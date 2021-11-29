package eubos.board;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import eubos.position.Position;

public class BitBoardTest {
	
	@Before
	public void setUp() {
	}
	
	@Test
	public void test_fill_up_all_empty() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.a1];
		long empty_mask = BitBoard.upOccludedEmpty(all_pieces, ~all_pieces);
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
	public void test_fill_up_blocked_immediately() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.a1] |
				BitBoard.positionToMask_Lut[Position.a2];
		long our_piece = BitBoard.positionToMask_Lut[Position.a1];
		long empty_mask = BitBoard.downOccludedEmpty(our_piece, ~all_pieces);
		long just_our_sq_set = BitBoard.positionToMask_Lut[Position.a1];
		assertEquals(just_our_sq_set, empty_mask);
	}
	
	@Test
	public void test_fill_up_blocked_on_a7() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.a2] |
				BitBoard.positionToMask_Lut[Position.a7];
		long our_piece = BitBoard.positionToMask_Lut[Position.a2];
		long empty_mask = BitBoard.upOccludedEmpty(our_piece, ~all_pieces);
		long expect_almost_all_a_file_set = BitBoard.positionToMask_Lut[Position.a6] |
				BitBoard.positionToMask_Lut[Position.a5] |
				BitBoard.positionToMask_Lut[Position.a4] |
				BitBoard.positionToMask_Lut[Position.a3] |
				BitBoard.positionToMask_Lut[Position.a2];
		assertEquals(expect_almost_all_a_file_set, empty_mask);
	}
	
	@Test
	public void test_fill_up_none_possible() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.a8];
		long empty_mask = BitBoard.upOccludedEmpty(all_pieces, ~all_pieces);
		long expect_only_original_sq_set = BitBoard.positionToMask_Lut[Position.a8];
		assertEquals(expect_only_original_sq_set, empty_mask);
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
	
	@Test
	public void test_fill_down_right_all() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.a8];
		long empty_mask = BitBoard.downRightOccludedEmpty(all_pieces, ~all_pieces);
		long expect_all_first_rank_set = BitBoard.positionToMask_Lut[Position.a8] |
				BitBoard.positionToMask_Lut[Position.b7] | 
				BitBoard.positionToMask_Lut[Position.c6] |
				BitBoard.positionToMask_Lut[Position.d5] |
				BitBoard.positionToMask_Lut[Position.e4] |
				BitBoard.positionToMask_Lut[Position.f3] |
				BitBoard.positionToMask_Lut[Position.g2] |
				BitBoard.positionToMask_Lut[Position.h1];
		assertEquals(expect_all_first_rank_set, empty_mask);
	}
	
	@Test
	public void test_fill_down_right_almost_all() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.a8] |
				BitBoard.positionToMask_Lut[Position.g2];
		long our_piece = BitBoard.positionToMask_Lut[Position.a8];
		long empty_mask = BitBoard.downRightOccludedEmpty(our_piece, ~all_pieces);
		long expect_all_first_rank_set = BitBoard.positionToMask_Lut[Position.a8] |
				BitBoard.positionToMask_Lut[Position.b7] | 
				BitBoard.positionToMask_Lut[Position.c6] |
				BitBoard.positionToMask_Lut[Position.d5] |
				BitBoard.positionToMask_Lut[Position.e4] |
				BitBoard.positionToMask_Lut[Position.f3];
		assertEquals(expect_all_first_rank_set, empty_mask);
	}
	
	@Test
	public void test_fill_down_right_none() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.h1];
		long empty_mask = BitBoard.downRightOccludedEmpty(all_pieces, ~all_pieces);
		long expect_inital_sq_set = BitBoard.positionToMask_Lut[Position.h1];
		assertEquals(expect_inital_sq_set, empty_mask);
	}
	
	@Test
	public void test_fill_up_right_all() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.a1];
		long empty_mask = BitBoard.upRightOccludedEmpty(all_pieces, ~all_pieces);
		long expect_all_diagonal_set = BitBoard.positionToMask_Lut[Position.a1] |
				BitBoard.positionToMask_Lut[Position.b2] | 
				BitBoard.positionToMask_Lut[Position.c3] |
				BitBoard.positionToMask_Lut[Position.d4] |
				BitBoard.positionToMask_Lut[Position.e5] |
				BitBoard.positionToMask_Lut[Position.f6] |
				BitBoard.positionToMask_Lut[Position.g7] |
				BitBoard.positionToMask_Lut[Position.h8];
		assertEquals(expect_all_diagonal_set, empty_mask);
	}
	
	@Test
	public void test_fill_up_right_almost_all() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.a1] |
				BitBoard.positionToMask_Lut[Position.g7];
		long our_piece = BitBoard.positionToMask_Lut[Position.a1];
		long empty_mask = BitBoard.upRightOccludedEmpty(our_piece, ~all_pieces);
		long expect_nearly_all_diagonal_set = BitBoard.positionToMask_Lut[Position.a1] |
				BitBoard.positionToMask_Lut[Position.b2] | 
				BitBoard.positionToMask_Lut[Position.c3] |
				BitBoard.positionToMask_Lut[Position.d4] |
				BitBoard.positionToMask_Lut[Position.e5] |
				BitBoard.positionToMask_Lut[Position.f6];
		assertEquals(expect_nearly_all_diagonal_set, empty_mask);
	}
	
	@Test
	public void test_fill_up_right_none() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.a8];
		long empty_mask = BitBoard.upRightOccludedEmpty(all_pieces, ~all_pieces);
		long expect_inital_sq_set = BitBoard.positionToMask_Lut[Position.a8];
		assertEquals(expect_inital_sq_set, empty_mask);
	}
	
	@Test
	public void test_fill_left_all() {
		// Check eighth rank as this exposes sign issues (sign bit is h8 bit)
		long all_pieces = BitBoard.positionToMask_Lut[Position.h8];
		long empty_mask = BitBoard.leftOccludedEmpty(all_pieces, ~all_pieces);
		long expect_all_eighth_rank_set = BitBoard.positionToMask_Lut[Position.a8] |
				BitBoard.positionToMask_Lut[Position.b8] | 
				BitBoard.positionToMask_Lut[Position.c8] |
				BitBoard.positionToMask_Lut[Position.d8] |
				BitBoard.positionToMask_Lut[Position.e8] |
				BitBoard.positionToMask_Lut[Position.f8] |
				BitBoard.positionToMask_Lut[Position.g8] |
				BitBoard.positionToMask_Lut[Position.h8];
		assertEquals(expect_all_eighth_rank_set, empty_mask);
	}
	
	@Test
	public void test_fill_left_none() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.a8];
		long empty_mask = BitBoard.leftOccludedEmpty(all_pieces, ~all_pieces);
		long expect_none_set = BitBoard.positionToMask_Lut[Position.a8];
		assertEquals(expect_none_set, empty_mask);
	}
	
	@Test
	public void test_fill_left_almost_all() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.h8] |
				BitBoard.positionToMask_Lut[Position.b8];
		long our_piece = BitBoard.positionToMask_Lut[Position.h8];
		long empty_mask = BitBoard.leftOccludedEmpty(our_piece, ~all_pieces);
		long expect_almost_all_eighth_rank_set = BitBoard.positionToMask_Lut[Position.h8] |
				BitBoard.positionToMask_Lut[Position.g8] | 
				BitBoard.positionToMask_Lut[Position.f8] |
				BitBoard.positionToMask_Lut[Position.e8] |
				BitBoard.positionToMask_Lut[Position.d8] |
				BitBoard.positionToMask_Lut[Position.c8];
		assertEquals(expect_almost_all_eighth_rank_set, empty_mask);
	}
	
	@Test
	public void test_fill_down_left_all() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.h8];
		long empty_mask = BitBoard.downLeftOccludedEmpty(all_pieces, ~all_pieces);
		long expect_all_first_rank_set = BitBoard.positionToMask_Lut[Position.h8] |
				BitBoard.positionToMask_Lut[Position.g7] | 
				BitBoard.positionToMask_Lut[Position.f6] |
				BitBoard.positionToMask_Lut[Position.e5] |
				BitBoard.positionToMask_Lut[Position.d4] |
				BitBoard.positionToMask_Lut[Position.c3] |
				BitBoard.positionToMask_Lut[Position.b2] |
				BitBoard.positionToMask_Lut[Position.a1];
		assertEquals(expect_all_first_rank_set, empty_mask);
	}
	
	@Test
	public void test_fill_down_left_almost_all() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.h8] |
				BitBoard.positionToMask_Lut[Position.b2];
		long our_piece = BitBoard.positionToMask_Lut[Position.h8];
		long empty_mask = BitBoard.downLeftOccludedEmpty(our_piece, ~all_pieces);
		long expect_all_first_rank_set = BitBoard.positionToMask_Lut[Position.h8] |
				BitBoard.positionToMask_Lut[Position.g7] | 
				BitBoard.positionToMask_Lut[Position.f6] |
				BitBoard.positionToMask_Lut[Position.e5] |
				BitBoard.positionToMask_Lut[Position.d4] |
				BitBoard.positionToMask_Lut[Position.c3];
		assertEquals(expect_all_first_rank_set, empty_mask);
	}
	
	@Test
	public void test_fill_down_left_none() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.h1];
		long empty_mask = BitBoard.downLeftOccludedEmpty(all_pieces, ~all_pieces);
		long expect_inital_sq_set = BitBoard.positionToMask_Lut[Position.h1];
		assertEquals(expect_inital_sq_set, empty_mask);
	}
	
	@Test
	public void test_fill_up_left_all() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.h1];
		long empty_mask = BitBoard.upLeftOccludedEmpty(all_pieces, ~all_pieces);
		long expect_all_diagonal_set = BitBoard.positionToMask_Lut[Position.a8] |
				BitBoard.positionToMask_Lut[Position.b7] | 
				BitBoard.positionToMask_Lut[Position.c6] |
				BitBoard.positionToMask_Lut[Position.d5] |
				BitBoard.positionToMask_Lut[Position.e4] |
				BitBoard.positionToMask_Lut[Position.f3] |
				BitBoard.positionToMask_Lut[Position.g2] |
				BitBoard.positionToMask_Lut[Position.h1];
		assertEquals(expect_all_diagonal_set, empty_mask);
	}
	
	@Test
	public void test_fill_up_left_almost_all() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.h1] |
				BitBoard.positionToMask_Lut[Position.b7];
		long our_piece = BitBoard.positionToMask_Lut[Position.h1];
		long empty_mask = BitBoard.upLeftOccludedEmpty(our_piece, ~all_pieces);
		long expect_nearly_all_diagonal_set = BitBoard.positionToMask_Lut[Position.h1] |
				BitBoard.positionToMask_Lut[Position.g2] | 
				BitBoard.positionToMask_Lut[Position.f3] |
				BitBoard.positionToMask_Lut[Position.e4] |
				BitBoard.positionToMask_Lut[Position.d5] |
				BitBoard.positionToMask_Lut[Position.c6];
		assertEquals(expect_nearly_all_diagonal_set, empty_mask);
	}
	
	@Test
	public void test_fill_up_left_none() {
		long all_pieces = BitBoard.positionToMask_Lut[Position.h8];
		long empty_mask = BitBoard.upLeftOccludedEmpty(all_pieces, ~all_pieces);
		long expect_inital_sq_set = BitBoard.positionToMask_Lut[Position.h8];
		assertEquals(expect_inital_sq_set, empty_mask);
	}
}