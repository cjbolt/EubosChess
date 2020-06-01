package eubos.board;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import eubos.position.Position;

public class DirectionTest {

	int resultSq;
	int inputSq;
	
	@Before
	public void setUp() throws Exception {
		inputSq = Position.a1;
	}

	@Test
	public void testGetDirectMoveSq_ConstraintsA1_1() {
		resultSq = Direction.getDirectMoveSq(Direction.up,inputSq);
		assertTrue(resultSq==Position.a2);
	}
	
	@Test
	public void testGetDirectMoveSq_ConstraintsA1_2() {
		resultSq = Direction.getDirectMoveSq(Direction.upRight,inputSq);
		assertTrue(resultSq==Position.b2);
	}
	
	@Test
	public void testGetDirectMoveSq_ConstraintsA1_3() {
		resultSq = Direction.getDirectMoveSq(Direction.right,inputSq);
		assertTrue(resultSq==Position.b1);
	}
	
	@Test
	public void testGetDirectMoveSq_ConstraintsA1_4() {
		resultSq = Direction.getDirectMoveSq(Direction.downRight,inputSq);
		assertTrue(resultSq==Position.NOPOSITION);
	}
	
	@Test
	public void testGetDirectMoveSq_ConstraintsA1_5() {
		resultSq = Direction.getDirectMoveSq(Direction.down,inputSq);
		assertTrue(resultSq==Position.NOPOSITION);
	}
	
	@Test
	public void testGetDirectMoveSq_ConstraintsA1_6() {
		resultSq = Direction.getDirectMoveSq(Direction.downLeft,inputSq);
		assertTrue(resultSq==Position.NOPOSITION);
	}
	
	@Test
	public void testGetDirectMoveSq_ConstraintsA1_7() {
		resultSq = Direction.getDirectMoveSq(Direction.left,inputSq);
		assertTrue(resultSq==Position.NOPOSITION);
	}
	
	@Test
	public void testGetDirectMoveSq_ConstraintsA1_8() {
		resultSq = Direction.getDirectMoveSq(Direction.upLeft,inputSq);
		assertTrue(resultSq==Position.NOPOSITION);
	}

	@Test
	public void testGetIndirectMoveSq_ConstraintsA1_1() {
		resultSq = Direction.getIndirectMoveSq(Direction.upRight,inputSq);
		assertTrue(resultSq==Position.b3);
	}
	
	@Test
	public void testGetIndirectMoveSq_ConstraintsA1_2() {
		resultSq = Direction.getIndirectMoveSq(Direction.rightUp,inputSq);
		assertTrue(resultSq==Position.c2);
	}
	
	@Test
	public void testGetIndirectMoveSq_ConstraintsA1_3() {
		resultSq = Direction.getIndirectMoveSq(Direction.rightDown,inputSq);
		assertTrue(resultSq==Position.NOPOSITION);
	}
	
	@Test
	public void testGetIndirectMoveSq_ConstraintsA1_4() {
		resultSq = Direction.getIndirectMoveSq(Direction.downRight,inputSq);
		assertTrue(resultSq==Position.NOPOSITION);
	}
	
	@Test
	public void testGetIndirectMoveSq_ConstraintsA1_5() {
		resultSq = Direction.getIndirectMoveSq(Direction.leftDown,inputSq);
		assertTrue(resultSq==Position.NOPOSITION);
	}
	
	@Test
	public void testGetIndirectMoveSq_ConstraintsA1_6() {
		resultSq = Direction.getIndirectMoveSq(Direction.downLeft,inputSq);
		assertTrue(resultSq==Position.NOPOSITION);
	}
	
	@Test
	public void testGetIndirectMoveSq_ConstraintsA1_7() {
		resultSq = Direction.getIndirectMoveSq(Direction.leftUp,inputSq);
		assertTrue(resultSq==Position.NOPOSITION);
	}
	
	@Test
	public void testGetIndirectMoveSq_ConstraintsA1_8() {
		resultSq = Direction.getIndirectMoveSq(Direction.upLeft,inputSq);
		assertTrue(resultSq==Position.NOPOSITION);
	}

}
