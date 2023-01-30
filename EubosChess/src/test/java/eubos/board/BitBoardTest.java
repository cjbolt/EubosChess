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
		long all_pieces = 1L<<BitBoard.h8;
		long empty_mask = BitBoard.upLeftOccludedEmpty(all_pieces, ~all_pieces);
		long expect_inital_sq_set = 1L<<BitBoard.h8;
		assertEquals(expect_inital_sq_set, empty_mask);
	}
	
	@Test
	public void test_up_left_attacks_not_wrapped() {
		long all_pieces = 1L<<Position.h1 |
				1L<<BitBoard.b7 |
				1L<<BitBoard.b1;
		
		long our_pieces = 1L<<BitBoard.h1 |
				1L<<BitBoard.b1;
		long attack_mask = BitBoard.upLeftAttacks(our_pieces, ~all_pieces);
		long expect_nearly_all_diagonal_set =
				// attacks from h1
				1L<<BitBoard.g2 | 
				1L<<BitBoard.f3 |
				1L<<BitBoard.e4 |
				1L<<BitBoard.d5 |
				1L<<BitBoard.c6 |
				1L<<BitBoard.b7 | // Piece is attacked
				// attacks from b1
				1L<<BitBoard.a2;
		assertEquals(expect_nearly_all_diagonal_set, attack_mask);
	}
	
	@Test
	public void test_mapping_between_bits_and_position() {
		for (int square : Position.values) {
			assertEquals(square, BitBoard.bitToPosition_Lut[BitBoard.positionToBit_Lut[square]]);
		}
		assertEquals(0, BitBoard.a1);
		assertEquals(8, BitBoard.a2);
		assertEquals(63, BitBoard.h8);
	}
	
	@Test
	public void test_bitValueOf() {
		assertEquals(BitBoard.h8, BitBoard.bitValueOf(7, 7));
		assertEquals(BitBoard.a1, BitBoard.bitValueOf(0, 0));
	}
	
	@Test
	public void test_mapping_of_bit_rank_to_position_rank() {
		int bit = 0;
		for (int square : Position.values) {
			assertEquals(Position.getRank(square), BitBoard.getRank(bit++));
		}
	}
	
	@Test
	public void test_mapping_of_bit_file_to_position_file() {
		int bit = 0;
		for (int square : Position.values) {
			assertEquals(Position.getFile(square), BitBoard.getFile(bit++));
		}
	}
	
	@Test
	public void test_pawn_capture_target_generation_up_left() {
		assertEquals(0L, BitBoard.generatePawnCaptureTargetBoardUpLeft(BitBoard.positionToBit_Lut[Position.a1]));
	}
	
	@Test
	public void test_pawn_capture_target_generation_up_right() {
		assertEquals((1L << BitBoard.positionToBit_Lut[Position.b2]), BitBoard.generatePawnCaptureTargetBoardUpRight(BitBoard.positionToBit_Lut[Position.a1]));
	}
	
	@Test
	public void test_pawn_capture_target_generation_down_left() {
		assertEquals(0L, BitBoard.generatePawnCaptureTargetBoardDownLeft(BitBoard.positionToBit_Lut[Position.a8]));
	}
	
	@Test
	public void test_pawn_capture_target_generation_down_right() {
		assertEquals((1L << BitBoard.positionToBit_Lut[Position.b7]), BitBoard.generatePawnCaptureTargetBoardDownRight(BitBoard.positionToBit_Lut[Position.a8]));
	}
	
	@Test
	public void test_convert_to_bit_offset() {
		for (int i=0; i<64; i++) {
			assertEquals(Long.numberOfTrailingZeros(1L << i), BitBoard.convertToBitOffset(1L << i));
		}
		assertEquals(Long.numberOfTrailingZeros(0x8000_0000_0000_0001L), BitBoard.convertToBitOffset(0x8000_0000_0000_0001L));
		assertEquals(Long.numberOfTrailingZeros(0x8080_0000_0000_0000L), BitBoard.convertToBitOffset(0x8080_0000_0000_0000L));
		assertEquals(Long.numberOfTrailingZeros(0x8000_0000_0000_1001L), BitBoard.convertToBitOffset(0x8000_0000_0000_1001L));
		assertEquals(Long.numberOfTrailingZeros(0x0000_0000_00C0_0000L), BitBoard.convertToBitOffset(0x0000_0000_00C0_0000L));
	}
	
}