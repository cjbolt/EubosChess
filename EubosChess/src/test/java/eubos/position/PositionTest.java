package eubos.position;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PositionTest {

	@Test
	void testDistance() {
		assertEquals(0, Position.distance(Position.a1, Position.a1));
		
		assertEquals(1, Position.distance(Position.a1, Position.b1));
		assertEquals(2, Position.distance(Position.a1, Position.c1));
		assertEquals(3, Position.distance(Position.a1, Position.d1));
		assertEquals(4, Position.distance(Position.a1, Position.e1));
		assertEquals(5, Position.distance(Position.a1, Position.f1));
		assertEquals(6, Position.distance(Position.a1, Position.g1));
		assertEquals(7, Position.distance(Position.a1, Position.h1));
		
		assertEquals(1, Position.distance(Position.a1, Position.a2));
		assertEquals(2, Position.distance(Position.a1, Position.a3));
		assertEquals(3, Position.distance(Position.a1, Position.a4));
		assertEquals(4, Position.distance(Position.a1, Position.a5));
		assertEquals(5, Position.distance(Position.a1, Position.a6));
		assertEquals(6, Position.distance(Position.a1, Position.a7));
		assertEquals(7, Position.distance(Position.a1, Position.a8));
		
		assertEquals(1, Position.distance(Position.a1, Position.b2));
		assertEquals(2, Position.distance(Position.a1, Position.c3));
		assertEquals(3, Position.distance(Position.a1, Position.d4));
		assertEquals(4, Position.distance(Position.a1, Position.e5));
		assertEquals(5, Position.distance(Position.a1, Position.f6));
		assertEquals(6, Position.distance(Position.a1, Position.g7));
		assertEquals(7, Position.distance(Position.a1, Position.h8));
		
		assertEquals(4, Position.distance(Position.f8, Position.c4));
	}
}
