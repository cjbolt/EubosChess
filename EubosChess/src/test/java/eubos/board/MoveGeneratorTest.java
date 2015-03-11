package eubos.board;

import static org.junit.Assert.assertFalse;

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
}
