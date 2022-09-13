package eubos.search.transposition;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FixedSizeTranspositionTableTest {

	FixedSizeTranspositionTable SUT;
	
	@BeforeEach
	public void setUp() {
		SUT = new FixedSizeTranspositionTable();
	}
	
	@Test
	void test_get_empty() {
		assertEquals(0L, SUT.getTransposition(0));
	}
	
	private void fullyPopulateIndexRegionSameAge() {
		// Cause a lot of index clashes such that the region is filled up
		for (int i=0; i < FixedSizeTranspositionTable.RANGE_TO_SEARCH; i++) {
			SUT.putTransposition((SUT.maxTableSize*i)+200L, i);
		}
	}
	
	private void fullyPopulateIndexRegionOneOld() {
		// Cause a lot of index clashes such that the region is filled up
		for (int i=0; i < FixedSizeTranspositionTable.RANGE_TO_SEARCH; i++) {
			long trans = i;
			if (i != 10) {
				trans = Transposition.setAge(trans, 10);
			}
			SUT.putTransposition((SUT.maxTableSize*i)+200L, trans);
		}
	}

	@Test
	void test_put_and_get_back() {
		SUT.putTransposition(0L, 12L);
		assertEquals(12L, SUT.getTransposition(0));
	}

	@Test
	void test_index_clash_low() {
		SUT.putTransposition(0L, 12L);
		SUT.putTransposition(SUT.maxTableSize, 14L); // Causes an indexing clash
		assertEquals(0L, SUT.getTransposition(0L)); // First transposition was overwritten
		assertEquals(14L, SUT.getTransposition(SUT.maxTableSize));
	}
	
	@Test
	void test_index_clash() {
		SUT.putTransposition(200L, 12L);
		SUT.putTransposition(SUT.maxTableSize+200L, 14L);
		// In this example the indexing clash is managed by having free slots below the hash value
		assertEquals(12L, SUT.getTransposition(200L));
		assertEquals(14L, SUT.getTransposition(SUT.maxTableSize+200L));
	}
	
	@Test
	void test_index_clash_when_region_full_and_same_age_overwrite_first() {
		fullyPopulateIndexRegionSameAge();
		SUT.putTransposition(200L, 0xFFFFL);
		// expect overwrite
		assertEquals(0xFFFFL, SUT.getTransposition(200L));
	}
	
	@Test
	void test_index_clash_when_region_full_and_overwrite_oldest() {
		fullyPopulateIndexRegionOneOld();
		long test_trans = Transposition.setAge(0xFFFF, 10);
		SUT.putTransposition(200L, test_trans);
		// expect overwrite
		assertEquals(test_trans, SUT.getTransposition(200L));
	}
}
