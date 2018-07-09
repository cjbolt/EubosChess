package eubos.position;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Direction;

public class DirectionTest {

	GenericPosition resultSq;
	GenericPosition inputSq;
	
	@Before
	public void setUp() throws Exception {
		inputSq = GenericPosition.a1;
	}

	@Test
	public void testGetDirectMoveSq_ConstraintsA1_1() {
		resultSq = Direction.getDirectMoveSq(Direction.up,inputSq);
		assertTrue(resultSq==GenericPosition.a2);
	}
	
	@Test
	public void testGetDirectMoveSq_ConstraintsA1_2() {
		resultSq = Direction.getDirectMoveSq(Direction.upRight,inputSq);
		assertTrue(resultSq==GenericPosition.b2);
	}
	
	@Test
	public void testGetDirectMoveSq_ConstraintsA1_3() {
		resultSq = Direction.getDirectMoveSq(Direction.right,inputSq);
		assertTrue(resultSq==GenericPosition.b1);
	}
	
	@Test
	public void testGetDirectMoveSq_ConstraintsA1_4() {
		resultSq = Direction.getDirectMoveSq(Direction.downRight,inputSq);
		assertTrue(resultSq==null);
	}
	
	@Test
	public void testGetDirectMoveSq_ConstraintsA1_5() {
		resultSq = Direction.getDirectMoveSq(Direction.down,inputSq);
		assertTrue(resultSq==null);
	}
	
	@Test
	public void testGetDirectMoveSq_ConstraintsA1_6() {
		resultSq = Direction.getDirectMoveSq(Direction.downLeft,inputSq);
		assertTrue(resultSq==null);
	}
	
	@Test
	public void testGetDirectMoveSq_ConstraintsA1_7() {
		resultSq = Direction.getDirectMoveSq(Direction.left,inputSq);
		assertTrue(resultSq==null);
	}
	
	@Test
	public void testGetDirectMoveSq_ConstraintsA1_8() {
		resultSq = Direction.getDirectMoveSq(Direction.upLeft,inputSq);
		assertTrue(resultSq==null);
	}

	@Test
	public void testGetIndirectMoveSq_ConstraintsA1_1() {
		resultSq = Direction.getIndirectMoveSq(Direction.upRight,inputSq);
		assertTrue(resultSq==GenericPosition.b3);
	}
	
	@Test
	public void testGetIndirectMoveSq_ConstraintsA1_2() {
		resultSq = Direction.getIndirectMoveSq(Direction.rightUp,inputSq);
		assertTrue(resultSq==GenericPosition.c2);
	}
	
	@Test
	public void testGetIndirectMoveSq_ConstraintsA1_3() {
		resultSq = Direction.getIndirectMoveSq(Direction.rightDown,inputSq);
		assertTrue(resultSq==null);
	}
	
	@Test
	public void testGetIndirectMoveSq_ConstraintsA1_4() {
		resultSq = Direction.getIndirectMoveSq(Direction.downRight,inputSq);
		assertTrue(resultSq==null);
	}
	
	@Test
	public void testGetIndirectMoveSq_ConstraintsA1_5() {
		resultSq = Direction.getIndirectMoveSq(Direction.leftDown,inputSq);
		assertTrue(resultSq==null);
	}
	
	@Test
	public void testGetIndirectMoveSq_ConstraintsA1_6() {
		resultSq = Direction.getIndirectMoveSq(Direction.downLeft,inputSq);
		assertTrue(resultSq==null);
	}
	
	@Test
	public void testGetIndirectMoveSq_ConstraintsA1_7() {
		resultSq = Direction.getIndirectMoveSq(Direction.leftUp,inputSq);
		assertTrue(resultSq==null);
	}
	
	@Test
	public void testGetIndirectMoveSq_ConstraintsA1_8() {
		resultSq = Direction.getIndirectMoveSq(Direction.upLeft,inputSq);
		assertTrue(resultSq==null);
	}

}
