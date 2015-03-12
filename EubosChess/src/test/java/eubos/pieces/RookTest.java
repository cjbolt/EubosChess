package eubos.pieces;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.BoardManager;
import eubos.pieces.Piece.Colour;

public class RookTest {
	protected LinkedList<Piece> pl;
	protected MultisquareDirectMovePiece classUnderTest;
	protected BoardManager bm;
	protected LinkedList<GenericMove> expectedMoves;

	@Test
	public void test_CornerTopLeft() {
		classUnderTest = new Rook( Colour.black, GenericPosition.a8 );
		pl.add(classUnderTest);
		bm = new BoardManager( new Board( pl ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.a7 ));
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.b8 ));
		assertFalse(ml.isEmpty());
		assertTrue(ml.size()==14);
		for ( GenericMove mov : expectedMoves) {
			assertTrue( ml.contains( mov ));
		}
	}
	
	@Before
	public void setUp() {
		pl = new LinkedList<Piece>();
		expectedMoves = new LinkedList<GenericMove>();
	}	
}
