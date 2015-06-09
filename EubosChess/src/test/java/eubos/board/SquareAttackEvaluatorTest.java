package eubos.board;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;





//import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.pieces.Bishop;
import eubos.pieces.Pawn;
import eubos.pieces.Piece;
import eubos.pieces.Piece.Colour;

public class SquareAttackEvaluatorTest {

	private SquareAttackEvaluator classUnderTest = null;
	private GenericPosition testSq = GenericPosition.d5;
	private List<Piece> pieceList = null;
	
	private void createClassUnderTest(Piece attacker) {
		pieceList = new LinkedList<Piece>();
		pieceList.add(attacker);
		Board bd = new Board(pieceList);
		classUnderTest = new SquareAttackEvaluator(bd,testSq,Colour.getOpposite(attacker.getColour()));
	}
	
	private void createClassUnderTest(String fenString) {
		BoardManager bm = new BoardManager(fenString);
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
}
