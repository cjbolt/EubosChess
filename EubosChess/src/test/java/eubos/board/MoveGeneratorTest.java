package eubos.board;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.pieces.King;
import eubos.pieces.Pawn;
import eubos.pieces.Piece;
import eubos.pieces.Piece.Colour;

public class MoveGeneratorTest {
	
	protected LinkedList<Piece> pl;
	protected MoveGenerator classUnderTest;
	
	@Before
	public void setUp() {
		pl = new LinkedList<Piece>();
	}
	
	@Test
	public void test_doNotMoveIntoCheck() {
		pl.add(new King( Colour.black, GenericPosition.a8 ));
		pl.add(new Pawn( Colour.white, GenericPosition.c6 ));
		BoardManager bm = new BoardManager( new Board( pl ));
		classUnderTest = new MoveGenerator( bm, Colour.black );
		try {
			GenericMove selectedMove = classUnderTest.findBestMove();
			assertFalse(selectedMove.equals(new GenericMove( GenericPosition.a8, GenericPosition.b7 )));
		}
		catch ( NoLegalMoveException e ) {
			assert( false );
		}	
	}
	
	@Test(expected=NoLegalMoveException.class)
	public void test_Checkmate_1() throws NoLegalMoveException {
		pl.add(new King( Colour.white, GenericPosition.a1 ));
		pl.add(new Pawn( Colour.black, GenericPosition.b1 ));
		pl.add(new Pawn( Colour.black, GenericPosition.a2 ));
		pl.add(new Pawn( Colour.black, GenericPosition.b2 ));
		pl.add(new Pawn( Colour.black, GenericPosition.c2 ));
		pl.add(new Pawn( Colour.black, GenericPosition.b3 ));
		pl.add(new Pawn( Colour.black, GenericPosition.c3 ));
		BoardManager bm = new BoardManager( new Board( pl ));
		classUnderTest = new MoveGenerator( bm, Colour.white );
		classUnderTest.findBestMove();
	}
	
	@Test
	public void test_CaptureToEscapeCheck() throws NoLegalMoveException {
		pl.add(new King( Colour.white, GenericPosition.a1 ));
		pl.add(new Pawn( Colour.black, GenericPosition.b1 ));
		pl.add(new Pawn( Colour.black, GenericPosition.a2 ));
		pl.add(new Pawn( Colour.black, GenericPosition.b2 ));
		pl.add(new Pawn( Colour.black, GenericPosition.c2 ));
		pl.add(new Pawn( Colour.black, GenericPosition.b3 ));
		// pawn at b2 can be captured to escape check
		BoardManager bm = new BoardManager( new Board( pl ));
		classUnderTest = new MoveGenerator( bm, Colour.white );
		classUnderTest.findBestMove();
		try {
			GenericMove selectedMove = classUnderTest.findBestMove();
			assertTrue(selectedMove.equals(new GenericMove( GenericPosition.a1, GenericPosition.b2 )));
		}
		catch ( NoLegalMoveException e ) {
			assert( false );
		}			
	}
	
	@Test
	public void test_MoveToEscapeCheck() throws NoLegalMoveException {
		pl.add(new King( Colour.white, GenericPosition.a1 ));
		pl.add(new Pawn( Colour.black, GenericPosition.b2 ));
		pl.add(new Pawn( Colour.black, GenericPosition.b3 ));
		pl.add(new Pawn( Colour.black, GenericPosition.c3 ));
		// king can move out of check to b1
		BoardManager bm = new BoardManager( new Board( pl ));
		classUnderTest = new MoveGenerator( bm, Colour.white );
		classUnderTest.findBestMove();
		try {
			GenericMove selectedMove = classUnderTest.findBestMove();
			assertTrue(selectedMove.equals(new GenericMove( GenericPosition.a1, GenericPosition.b1 )));
		}
		catch ( NoLegalMoveException e ) {
			assert( false );
		}			
	}	
}
