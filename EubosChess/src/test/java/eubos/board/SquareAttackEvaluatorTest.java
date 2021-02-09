package eubos.board;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import eubos.position.Position;
import eubos.position.PositionManager;

public class SquareAttackEvaluatorTest {

	Board bd;
	PositionManager bm;
	
	// In all the following unit tests, d5 is used as the test square. i.e. each
	// test revolves around evaluating whether or not this particular square is 
	// attacked.
	private int testSq = Position.d5;
	
	private void createClassUnderTest(String fenString) {
		bm = new PositionManager(fenString);
	}
	
	private void assertTestSqIsNotAttacked(String fenString) {
		createClassUnderTest(fenString);
		assertFalse(SquareAttackEvaluator.isAttacked(bm.getTheBoard(), testSq, Piece.Colour.getOpposite(bm.getOnMove())));
	}
	
	private void assertTestSqIsAttacked(String fenString) {
		createClassUnderTest(fenString);
		assertTrue(SquareAttackEvaluator.isAttacked(bm.getTheBoard(), testSq, Piece.Colour.getOpposite(bm.getOnMove())));
	}
	
	@Before
	public void setUp() {
		this.testSq = Position.d5;
	}
	
	// Pawn
	@Test
	public void testIsAttacked_AttackedWhitePawnC4() {
		assertTestSqIsAttacked("8/8/8/8/2P5/8/8/8 b - - 0 1"); }
	@Test
	public void testIsAttacked_AttackedWhitePawnE4() {
		assertTestSqIsAttacked("8/8/8/8/2P5/8/8/8 b - - 0 1"); }
	@Test
	public void testIsAttacked_AttackedBlackPawnC6() {
		assertTestSqIsAttacked("8/8/2p5/8/8/8/8/8 w - - 0 1"); }
	@Test
	public void testIsAttacked_AttackedBlackPawnE6() {
		assertTestSqIsAttacked("8/8/4p3/8/8/8/8/8 w - - 0 1"); }
	@Test
	public void testIsAttacked_NotAttackedWhitePawnC6() {
		assertTestSqIsNotAttacked("8/8/2P5/8/8/8/8/8 b - - 0 1"); }
	@Test
	public void testIsAttacked_NotAttackedWhitePawnE6() {
		assertTestSqIsNotAttacked("8/8/5P2/8/8/8/8/8 b - - 0 1"); }
	@Test
	public void testIsAttacked_NotAttackedBlackPawnC4() {
		assertTestSqIsNotAttacked("8/8/8/8/2p5/8/8/8 w - - 0 1"); }
	@Test
	public void testIsAttacked_NotAttackedBlackPawnE4() {
		assertTestSqIsNotAttacked("8/8/8/8/5p2/8/8/8 w - - 0 1"); }
	@Test
	public void testIsAttacked_NotAttackedOwnColourPawnE6() {
		assertTestSqIsNotAttacked("8/8/4p3/8/8/8/8/8 b - - 0 1"); }
	
	// Bishop
	@Test
	public void testIsAttacked_NotAttackedOwnColourPawnE4() {
		assertTestSqIsNotAttacked("8/8/8/3K4/4P3/8/8/8 w - - 0 1"); }
	@Test
	public void testIsAttacked_AttackedWhiteBishopH1() {
		assertTestSqIsAttacked("8/8/8/8/8/8/8/7B b - - 0 1"); }
	@Test
	public void testIsAttacked_AttackedWhiteBishopH1_fen() {
		assertTestSqIsAttacked("8/8/8/3k4/8/8/8/7B b - - 0 1"); }
	@Test
	public void testIsAttacked_NotAttackedWhiteBishopH1Blocked_fen() {
		assertTestSqIsNotAttacked("8/8/8/3k4/8/8/6P1/7B b - - 0 1"); }
	
	// Knight
	@Test
	public void testIsAttacked_AttackedWhiteKnightB4() {
		assertTestSqIsAttacked("8/8/8/8/1N6/8/8/8 b - - 0 1 "); }
	@Test
	public void testIsAttacked_AttackedBlackKnightC3() {
		assertTestSqIsAttacked("8/8/8/8/8/2n5/8/8 w - - 0 1 "); }
	@Test
	public void testIsAttacked_AttackedKnightF6() {
		assertTestSqIsAttacked("8/8/5n2/8/8/8/8/8 w - - 0 1 "); }
	@Test
	public void testIsAttacked_NotAttackedKnightC4() {
		assertTestSqIsNotAttacked("8/8/8/8/2n5/8/8/8 w - - 0 1 "); }
	@Test
	public void testIsAttacked_NotAttackedKnightB3() {
		assertTestSqIsNotAttacked("8/8/8/8/8/1n6/8/8 w - - 0 1 "); }
	@Test
	public void testIsAttacked_NotAttackedKnightC2() {
		assertTestSqIsNotAttacked("8/8/8/8/8/8/2n5/8 w - - 0 1 "); }
	@Test
	public void testIsAttacked_NotAttackedKnightD2() {
		assertTestSqIsNotAttacked("8/8/8/8/8/8/3n4/8 w - - 0 1 "); }
	
	// Queen
	@Test
	public void testIsAttacked_AttackedQueenG8() {
		assertTestSqIsAttacked("6q1/8/8/8/8/8/8/8 w - - 0 1 "); }
	@Test
	public void testIsAttacked_AttackedQueenB4() {
		assertTestSqIsNotAttacked("8/8/8/8/1q6/8/8/8 w - - 0 1 "); }
	@Test
	public void testIsAttacked_AttackedQueenA5() {
		assertTestSqIsAttacked("8/8/8/q7/8/8/8/8 w - - 0 1 "); }
	@Test
	public void testIsAttacked_AttackedQueenH1() {
		assertTestSqIsAttacked("8/8/8/8/8/8/8/7q w - - 0 1 "); }
	@Test
	public void testIsAttacked_NotAttackedQueenC3() {
		assertTestSqIsNotAttacked("8/8/8/8/8/4q3/8/8 w - - 0 1 "); }
	@Test
	public void testIsAttacked_NotAttackedQueenB4() {
		assertTestSqIsNotAttacked("8/8/8/8/8/4q3/8/8 w - - 0 1 "); }
	@Test
	public void testIsAttacked_NotAttackedQueenE3() {
		assertTestSqIsNotAttacked("8/8/8/8/8/4q3/8/8 w - - 0 1 "); }
	@Test
	public void testIsAttacked_NotAttackedQueenE7() {
		assertTestSqIsNotAttacked("8/4q3/8/8/8/8/8/8 w - - 0 1 "); }

	// Blocked attacks
	@Test
	public void testIsAttacked_NotAttacked_BlockedQueenG8() {
		assertTestSqIsNotAttacked("6Q1/8/4p3/3k4/8/8/8/8 b - - 0 1"); }
	@Test
	public void testIsAttacked_NotAttacked_BlockedQueenD8() {
		assertTestSqIsNotAttacked("3Q4/3p4/8/3k4/8/8/8/8 b - - 0 1"); }
	@Test
	public void testIsAttacked_NotAttacked_BlockedQueenA5() {
		assertTestSqIsNotAttacked("8/3p4/8/Qppk4/8/8/8/8 b - - 0 1"); }
	
	// Compound Tests
	@Test
	public void testIsAttacked_NotAttacked_Complex1() {
		assertTestSqIsNotAttacked("B1N5/3p1N2/2P3R1/Qp1k4/1PpP4/8/6q1/Q2R3B b - - 0 1"); }
	@Test
	public void testIsAttacked_Attacked_Complex1_QueenA5() {
		assertTestSqIsAttacked("B1N5/3p1N2/2P3R1/Q2k4/1PpP4/8/6q1/Q2R3B b - - 0 1"); }
	@Test
	public void testIsAttacked_Attacked_Illegal1_KingC5() {
		assertTestSqIsAttacked("B1N5/3p1N2/2P3R1/Q1Kk4/1PpP4/8/6q1/Q2R3B b - - 0 1"); }
	@Test
	public void testIsAttacked_MateInOne() {
		PositionManager pm = new PositionManager("5r1k/p2R3Q/1pp2p1p/8/5q2/5bN1/PP3P2/6K1 b - - - 0");
		assertTrue(SquareAttackEvaluator.isAttacked(pm.getTheBoard(), Position.g8, Piece.Colour.getOpposite(pm.getOnMove())));
	}
	
	@Test
	public void test_findDirectionToTarget_upRight() {
		assertEquals(Direction.upRight, SquareAttackEvaluator.findDirectionToTarget(Position.a1, Position.b2, SquareAttackEvaluator.diagonals));
	}
	
	@Test
	public void test_findDirectionToTarget_fail() {
		assertNull(SquareAttackEvaluator.findDirectionToTarget(Position.a1, Position.b8, SquareAttackEvaluator.allDirect));
	}
}
