package eubos.board;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

import eubos.pieces.Pawn;
import eubos.pieces.Piece;

import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericChessman;


public class BoardTest {
	
	protected LinkedList<Piece> pl;
	protected Board classUnderTest;
	
	@Before
	public void setUp() {
		pl = new LinkedList<Piece>();
	}
	
	@Test(expected=java.util.EmptyStackException.class)
	public void test_NoLastMoveToUndo() {
		classUnderTest = new Board();
		classUnderTest.undoLastMove();
	}
	
	@Test
	public void test_UndoPawnMove() {
		pl.add( new Pawn( Piece.PieceColour.white, GenericPosition.e2 ));
		classUnderTest = new Board( pl );
		classUnderTest.performMove( new GenericMove( GenericPosition.e2, GenericPosition.e4 ));
		classUnderTest.undoLastMove();
		Piece expectPawn = classUnderTest.getPieceAtSquare( GenericPosition.e2 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isWhite());		
	}
	
	@Test
	public void test_UndoPawnPromotion() {
		pl.add( new Pawn( Piece.PieceColour.black, GenericPosition.e2 ));
		classUnderTest = new Board( pl );
		classUnderTest.performMove( new GenericMove( GenericPosition.e2, GenericPosition.e1, GenericChessman.QUEEN ));
		classUnderTest.undoLastMove();
		Piece expectPawn = classUnderTest.getPieceAtSquare( GenericPosition.e2 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isBlack());
	}
	
	@Test
	public void test_UndoPawnCapture() {
		pl.add( new Pawn( Piece.PieceColour.black, GenericPosition.d3 ));
		pl.add( new Pawn( Piece.PieceColour.white, GenericPosition.e2 ));
		classUnderTest = new Board( pl );
		classUnderTest.performMove( new GenericMove( GenericPosition.d3, GenericPosition.e2 ));
		classUnderTest.undoLastMove();
		Piece expectPawn = classUnderTest.getPieceAtSquare( GenericPosition.d3 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isBlack());
		expectPawn = classUnderTest.getPieceAtSquare( GenericPosition.e2 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isWhite());
	}
}
