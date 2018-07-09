package eubos.position;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;









//import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.pieces.Bishop;
import eubos.board.pieces.Knight;
import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Queen;
import eubos.board.pieces.Piece.Colour;
import eubos.position.Board;
import eubos.position.PositionManager;
import eubos.position.SquareAttackEvaluator;

public class SquareAttackEvaluatorTest {

	private SquareAttackEvaluator classUnderTest = null;
	// In all the following unit tests, d5 is used as the test square. i.e. each
	// test revolves around evaluating whether or not this particular square is 
	// attacked.
	private GenericPosition testSq = GenericPosition.d5;
	private List<Piece> pieceList = null;
	
	private void createClassUnderTest(Piece attacker) {
		pieceList = new LinkedList<Piece>();
		pieceList.add(attacker);
		Board bd = new Board(pieceList);
		classUnderTest = new SquareAttackEvaluator(bd,testSq,Colour.getOpposite(attacker.getColour()));
	}
	
	private void createClassUnderTest(String fenString) {
		PositionManager bm = new PositionManager(fenString);
		classUnderTest = new SquareAttackEvaluator(bm.getTheBoard(),testSq,bm.getOnMove());
	}
	
	private void assertTestSqIsNotAttacked(String fenString) {
		createClassUnderTest(fenString);
		assertFalse(classUnderTest.isAttacked());
	}
	
	private void assertTestSqIsAttacked(String fenString) {
		createClassUnderTest(fenString);
		assertTrue(classUnderTest.isAttacked());
	}
	
	private void assertTestSqIsNotAttacked(Piece attacker) {
		createClassUnderTest(attacker);
		assertFalse(classUnderTest.isAttacked());
	}
	
	private void assertTestSqIsAttacked(Piece attacker) {
		createClassUnderTest(attacker);
		assertTrue(classUnderTest.isAttacked());
	}

	// Pawn
	@Test
	public void testIsAttacked_AttackedWhitePawnC4() {
		assertTestSqIsAttacked(new Pawn(Colour.white,GenericPosition.c4)); }
	@Test
	public void testIsAttacked_AttackedWhitePawnE4() {
		assertTestSqIsAttacked(new Pawn(Colour.white,GenericPosition.e4)); }
	@Test
	public void testIsAttacked_AttackedBlackPawnC6() {
		assertTestSqIsAttacked(new Pawn(Colour.black,GenericPosition.c6)); }
	@Test
	public void testIsAttacked_AttackedBlackPawnE6() {
		assertTestSqIsAttacked(new Pawn(Colour.black,GenericPosition.e6)); }
	@Test
	public void testIsAttacked_NotAttackedWhitePawnC6() {
		assertTestSqIsNotAttacked(new Pawn(Colour.white,GenericPosition.c6)); }
	@Test
	public void testIsAttacked_NotAttackedWhitePawnE6() {
		assertTestSqIsNotAttacked(new Pawn(Colour.white,GenericPosition.e6)); }
	@Test
	public void testIsAttacked_NotAttackedBlackPawnC4() {
		assertTestSqIsNotAttacked(new Pawn(Colour.black,GenericPosition.c4)); }
	@Test
	public void testIsAttacked_NotAttackedBlackPawnE4() {
		assertTestSqIsNotAttacked(new Pawn(Colour.black,GenericPosition.e4)); }
	@Test
	public void testIsAttacked_NotAttackedOwnColourPawnE6() {
		assertTestSqIsNotAttacked("8/8/4p3/3k4/8/8/8/8 b - - 0 1"); }
	
	// Bishop
	@Test
	public void testIsAttacked_NotAttackedOwnColourPawnE4() {
		assertTestSqIsNotAttacked("8/8/8/3K4/4P3/8/8/8 w - - 0 1"); }
	@Test
	public void testIsAttacked_AttackedWhiteBishopH1() {
		assertTestSqIsAttacked(new Bishop(Colour.white,GenericPosition.h1)); }
	@Test
	public void testIsAttacked_AttackedWhiteBishopH1_fen() {
		assertTestSqIsAttacked("8/8/8/3k4/8/8/8/7B b - - 0 1"); }
	@Test
	public void testIsAttacked_NotAttackedWhiteBishopH1Blocked_fen() {
		assertTestSqIsNotAttacked("8/8/8/3k4/8/8/6P1/7B b - - 0 1"); }
	
	// Knight
	@Test
	public void testIsAttacked_AttackedWhiteKnightB4() {
		assertTestSqIsAttacked(new Knight(Colour.white,GenericPosition.b4)); }
	@Test
	public void testIsAttacked_AttackedBlackKnightC3() {
		assertTestSqIsAttacked(new Knight(Colour.black,GenericPosition.c3)); }
	@Test
	public void testIsAttacked_AttackedKnightF6() {
		assertTestSqIsAttacked(new Knight(Colour.black,GenericPosition.f6)); }
	@Test
	public void testIsAttacked_NotAttackedKnightC4() {
		assertTestSqIsNotAttacked(new Knight(Colour.white,GenericPosition.c4)); }
	@Test
	public void testIsAttacked_NotAttackedKnightB3() {
		assertTestSqIsNotAttacked(new Knight(Colour.white,GenericPosition.b3)); }
	@Test
	public void testIsAttacked_NotAttackedKnightC2() {
		assertTestSqIsNotAttacked(new Knight(Colour.black,GenericPosition.c2)); }
	@Test
	public void testIsAttacked_NotAttackedKnightD2() {
		assertTestSqIsNotAttacked(new Knight(Colour.black,GenericPosition.d2)); }
	
	// Queen
	@Test
	public void testIsAttacked_AttackedQueenG8() {
		assertTestSqIsAttacked(new Queen(Colour.white,GenericPosition.g8)); }
	@Test
	public void testIsAttacked_AttackedQueenB4() {
		assertTestSqIsAttacked(new Queen(Colour.white,GenericPosition.d8)); }
	@Test
	public void testIsAttacked_AttackedQueenA5() {
		assertTestSqIsAttacked(new Queen(Colour.black,GenericPosition.a5)); }
	@Test
	public void testIsAttacked_AttackedQueenH1() {
		assertTestSqIsAttacked(new Queen(Colour.black,GenericPosition.h1)); }
	@Test
	public void testIsAttacked_NotAttackedQueenC3() {
		assertTestSqIsNotAttacked(new Queen(Colour.white,GenericPosition.c3)); }
	@Test
	public void testIsAttacked_NotAttackedQueenB4() {
		assertTestSqIsNotAttacked(new Queen(Colour.white,GenericPosition.b4)); }
	@Test
	public void testIsAttacked_NotAttackedQueenE3() {
		assertTestSqIsNotAttacked(new Queen(Colour.black,GenericPosition.e3)); }
	@Test
	public void testIsAttacked_NotAttackedQueenE7() {
		assertTestSqIsNotAttacked(new Queen(Colour.black,GenericPosition.e7)); }

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
}
