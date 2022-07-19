package eubos.board;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import eubos.position.Position;

class CountedBitBoardTest {

	long[] cbb;
	
	@Test
	void test_count_is_0_for_empty_cbb() {
		cbb = new long[4];
		assertEquals(0, CountedBitBoard.count(cbb, Position.a1));
	}

	@Test
	void test_count_when_initialised() {
		cbb = new long[] { 1L };
		assertEquals(1, CountedBitBoard.count(cbb, Position.a1));
	}
	
	@Test
	void test_count_when_multiple() {
		cbb = new long[] { 0L, 0L, 0L };
		CountedBitBoard.setBits(cbb, BitBoard.positionToMask_Lut[Position.a1]);
		CountedBitBoard.setBits(cbb, BitBoard.positionToMask_Lut[Position.a1]);
		CountedBitBoard.setBits(cbb, BitBoard.positionToMask_Lut[Position.a1]);
		assertEquals(3, CountedBitBoard.count(cbb, Position.a1));
	}
	
	@Test
	void test_count_when_multiple_alt() {
		cbb = new long[] { 0L, 0L, 0L };
		CountedBitBoard.setBits(cbb, BitBoard.positionToMask_Lut[Position.a1]);
		CountedBitBoard.setBits(cbb, BitBoard.positionToMask_Lut[Position.b1]);
		CountedBitBoard.setBits(cbb, BitBoard.positionToMask_Lut[Position.c1]);
		assertEquals(1, CountedBitBoard.count(cbb, Position.a1));
	}
	
	@Test
	void test_count_when_cleared_is_zero() {
		cbb = new long[] { 0L, 0L, 0L };
		CountedBitBoard.setBits(cbb, BitBoard.positionToMask_Lut[Position.a1]);
		CountedBitBoard.setBits(cbb, BitBoard.positionToMask_Lut[Position.a1]);
		CountedBitBoard.setBits(cbb, BitBoard.positionToMask_Lut[Position.a1]);
		CountedBitBoard.clear(cbb);
		assertEquals(0, CountedBitBoard.count(cbb, Position.a1));
	}
	
	@Test
	void test_count_when_set_bit_array() {
		cbb = new long[] { 0L, 0L, 0L, 0L, 0L};
		long [] bitArray = new long[] { 0L, 0L, 0L, 0L, 0L };
		CountedBitBoard.setBits(cbb, BitBoard.positionToMask_Lut[Position.a1]);
		CountedBitBoard.setBits(cbb, BitBoard.positionToMask_Lut[Position.a1]);
		
		CountedBitBoard.setBits(bitArray, BitBoard.positionToMask_Lut[Position.a1]);
		CountedBitBoard.setBits(bitArray, BitBoard.positionToMask_Lut[Position.a1]);
		
		CountedBitBoard.setBitArrays(cbb, bitArray);
		// 2 + 2 = 4
		assertEquals(4, CountedBitBoard.count(cbb, Position.a1));
	}
}
