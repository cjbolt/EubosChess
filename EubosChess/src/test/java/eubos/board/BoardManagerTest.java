package eubos.board;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import static org.junit.Assert.assertTrue;
import eubos.pieces.Pawn;
import eubos.pieces.Piece;

import com.fluxchess.jcpi.models.GenericPosition;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericChessman;


public class BoardManagerTest {
	
	protected LinkedList<Piece> pl;
	protected BoardManager classUnderTest;
	
	@Before
	public void setUp() {
		pl = new LinkedList<Piece>();
	}
	
	@Test(expected=java.util.EmptyStackException.class)
	@Ignore
	public void test_NoLastMoveToUndo() {
		classUnderTest = new BoardManager();
		classUnderTest.undoPreviousMove();
	}
	
	@Test
	public void test_UndoPawnMove() {
		pl.add( new Pawn( Piece.Colour.white, GenericPosition.e2 ));
		classUnderTest = new BoardManager( new Board( pl ));
		classUnderTest.performMove( new GenericMove( GenericPosition.e2, GenericPosition.e4 ));
		classUnderTest.undoPreviousMove();
		Piece expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.e2 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isWhite());		
	}
	
	@Test
	public void test_UndoPawnPromotion() {
		pl.add( new Pawn( Piece.Colour.black, GenericPosition.e2 ));
		classUnderTest = new BoardManager( new Board( pl ));
		classUnderTest.performMove( new GenericMove( GenericPosition.e2, GenericPosition.e1, GenericChessman.QUEEN ));
		classUnderTest.undoPreviousMove();
		Piece expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.e2 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isBlack());
	}
	
	@Test
	public void test_UndoPawnCapture() {
		pl.add( new Pawn( Piece.Colour.black, GenericPosition.d3 ));
		pl.add( new Pawn( Piece.Colour.white, GenericPosition.e2 ));
		classUnderTest = new BoardManager( new Board( pl ));
		classUnderTest.performMove( new GenericMove( GenericPosition.d3, GenericPosition.e2 ));
		classUnderTest.undoPreviousMove();
		Piece expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.d3 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isBlack());
		expectPawn = classUnderTest.getTheBoard().getPieceAtSquare( GenericPosition.e2 );
		assertTrue( expectPawn instanceof Pawn );
		assertTrue( expectPawn.isWhite());
	}
}
