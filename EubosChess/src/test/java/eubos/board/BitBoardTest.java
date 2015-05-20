package eubos.board;

import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

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
		classUnderTest.set(0,0);
		assertTrue(classUnderTest.getSquareOccupied() == 0x1L);
	}
	
	@Test
	public void testSet_bit63() {
		classUnderTest.set(7,7);
		assertTrue(classUnderTest.getSquareOccupied() == 0x8000000000000000L);
	}	

	@Test
	public void testClear_bit63() {
		classUnderTest.set(0,0);
		classUnderTest.set(7,7);
		classUnderTest.clear(7,7);
		assertTrue(classUnderTest.getSquareOccupied() == 0x1L);
	}

	@Test
	public void testIsSet_bit63() {
		classUnderTest.set(7,7);
		assertTrue(classUnderTest.isSet(7,7));
	}
	
	@Test
	public void testIterator_bit0() {
		classUnderTest.set(0,0);
		Iterator<Integer> it_setbits = classUnderTest.iterator();
		assertTrue( it_setbits.hasNext() == true );
		assertTrue( it_setbits.next() == 0 );
	}
	
	@Test
	public void testIterator_bit63() {
		classUnderTest.set(7,7);
		Iterator<Integer> it_setbits = classUnderTest.iterator();
		assertTrue( it_setbits.hasNext() == true );
		assertTrue( it_setbits.next() == 63 );
	}
	
	@Test
	public void testIterator_bit62() {
		classUnderTest.set(7,6);
		Iterator<Integer> it_setbits = classUnderTest.iterator();
		assertTrue( it_setbits.hasNext() == true );
		assertTrue( it_setbits.next() == 62 );
	}
	
	@Test
	public void testIterator_bit56() {
		classUnderTest.set(7,0);
		Iterator<Integer> it_setbits = classUnderTest.iterator();
		assertTrue( it_setbits.hasNext() == true );
		assertTrue( it_setbits.next() == 56 );
	}	
	
	@Test
	public void testIterator_bits0and1() {
		classUnderTest.set(0,0);
		classUnderTest.set(0,1);
		Iterator<Integer> it_setbits = classUnderTest.iterator();
		assertTrue( it_setbits.hasNext() == true );
		assertTrue( it_setbits.next() == 0 );
		assertTrue( it_setbits.hasNext() == true );
		assertTrue( it_setbits.next() == 1 );
	}
}
