package eubos.pieces;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import eubos.board.*;

import com.fluxchess.jcpi.models.*;

public class PawnTest_Black extends PawnTest {

	@Test
	public void testInitial_MoveOneSquare() {
		LinkedList<Piece> setup = new LinkedList<Piece>();
		Pawn pieceUnderTest = new Pawn( Piece.PieceColour.black, GenericPosition.e7 );
		setup.add( pieceUnderTest );
		Board testPosition = new Board( setup );
		LinkedList<GenericMove> ml = pieceUnderTest.generateMoveList( testPosition );
		assertTrue( ml.contains( new GenericMove( GenericPosition.e7, GenericPosition.e6 ) ));
	}

	@Test
	public void testInitial_MoveTwoSquares() {
		LinkedList<Piece> setup = new LinkedList<Piece>();
		Pawn pieceUnderTest = new Pawn( Piece.PieceColour.black, GenericPosition.e7 );
		setup.add( pieceUnderTest );
		Board testPosition = new Board( setup );
		LinkedList<GenericMove> ml = pieceUnderTest.generateMoveList( testPosition );
		assertTrue( ml.contains( new GenericMove( GenericPosition.e7, GenericPosition.e5 ) ));
	}
	
	@Test
	public void testInitial_Blocked() {
		LinkedList<Piece> setup = new LinkedList<Piece>();
		Pawn pieceUnderTest = new Pawn( Piece.PieceColour.black, GenericPosition.e7 );
		setup.add( pieceUnderTest );
		setup.add( new Pawn( Piece.PieceColour.black, GenericPosition.e6 ));
		Board testPosition = new Board( setup );
		LinkedList<GenericMove> ml = pieceUnderTest.generateMoveList( testPosition );
		assertFalse( ml.contains( new GenericMove( GenericPosition.e7, GenericPosition.e6 ) ));
		assertFalse( ml.contains( new GenericMove( GenericPosition.e7, GenericPosition.e5 ) ));	
	}

	@Test
	@Ignore
	public void testInitial_EnPassant() {
	}

	@Test
	@Ignore
	public void test_MoveOneSquare() {
	}

	@Test
	@Ignore
	public void test_CaptureLeft() {
	}

	@Test
	@Ignore
	public void test_CaptureRight() {
	}

	@Test
	@Ignore
	public void test_PromoteQueen() {
	}	

	@Test
	@Ignore
	public void test_PromoteKnight() {
	}

	@Test
	@Ignore
	public void test_PromoteBishop() {
	}

	@Test
	@Ignore
	public void test_PromoteRook() {
	}
}
