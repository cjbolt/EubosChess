package eubos.position;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import eubos.board.BitBoard;

public class BitBoardTest {
	private BitBoard classUnderTest;
	
	@Before
	public void setUp() {
		classUnderTest = new BitBoard();
	}
	
	@Test
	public void testConstruct() {
		assertTrue(classUnderTest != null);
	}
	
	@Test
	public void testSet_bit0() {
		classUnderTest.set(0);
		assertTrue(classUnderTest.getSquareOccupied() == 0x1L);
	}
	
	@Test
	public void testSet_bit63() {
		classUnderTest.set(63);
		assertTrue(classUnderTest.getSquareOccupied() == 0x8000000000000000L);
	}	

	@Test
	public void testClear_bit63() {
		classUnderTest.set(0);
		classUnderTest.set(63);
		classUnderTest.clear(63);
		assertTrue(classUnderTest.getSquareOccupied() == 0x1L);
	}

	@Test
	public void testIsSet_bit63() {
		classUnderTest.set(63);
		assertTrue(classUnderTest.isSet(63));
	}
	
	@Test
	public void testIterator_bit0() {
		classUnderTest.set(0);
		Iterator<Integer> it_setbits = classUnderTest.iterator();
		assertTrue( it_setbits.hasNext() == true );
		assertTrue( it_setbits.next() == 0 );
	}
	
	@Test
	public void testIterator_bit63() {
		classUnderTest.set(63);
		Iterator<Integer> it_setbits = classUnderTest.iterator();
		assertTrue( it_setbits.hasNext() == true );
		assertTrue( it_setbits.next() == 63 );
	}
	
	@Test
	public void testIterator_bit62() {
		classUnderTest.set(62);
		Iterator<Integer> it_setbits = classUnderTest.iterator();
		assertTrue( it_setbits.hasNext() == true );
		assertTrue( it_setbits.next() == 62 );
	}
	
	@Test
	public void testIterator_bit56() {
		classUnderTest.set(56);
		Iterator<Integer> it_setbits = classUnderTest.iterator();
		assertTrue( it_setbits.hasNext() == true );
		assertTrue( it_setbits.next() == 56 );
	}	
	
	@Test
	public void testIterator_bits0and1() {
		classUnderTest.set(0);
		classUnderTest.set(1);
		Iterator<Integer> it_setbits = classUnderTest.iterator();
		assertTrue( it_setbits.hasNext() == true );
		assertTrue( it_setbits.next() == 0 );
		assertTrue( it_setbits.hasNext() == true );
		assertTrue( it_setbits.next() == 1 );
	}
	
	@Test
	public void testIterator_allOddBits() {
		for (int i=0; i<64; i++) {
			if ((i%2) == 1) {
				classUnderTest.set(i);
			}
		}
		Iterator<Integer> it_setbits = classUnderTest.iterator();
		int oddBitCount = 0;
		while(it_setbits.hasNext()) {
			int curr = it_setbits.next();
			if (curr%2 == 0) {
				fail();
			} else {
				oddBitCount++;
			}
		}
		assertTrue(oddBitCount == 32);
	}
	
	@Test
	public void testIterator_allEvenBits() {
		for (int i=0; i<64; i++) {
			if ((i%2) == 0) {
				classUnderTest.set(i);
			}
		}
		Iterator<Integer> it_setbits = classUnderTest.iterator();
		int evenBitCount = 0;
		while(it_setbits.hasNext()) {
			int curr = it_setbits.next();
			if (curr%2 == 1) {
				fail();
			} else {
				evenBitCount++;
			}
		}
		assertTrue(evenBitCount == 32);
	}
}
