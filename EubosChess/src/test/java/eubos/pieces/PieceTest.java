package eubos.pieces;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;

import org.junit.Before;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.Board;
import eubos.board.BoardManager;
import eubos.pieces.Piece.Colour;

public class PieceTest {

	protected LinkedList<Piece> pl;
	protected BoardManager bm;
	protected GenericMove expectedMove;
	protected LinkedList<GenericMove> expectedMoves;
	protected int expectedNumMoves = 0;
	protected Piece classUnderTest;

	public PieceTest() {
		super();
	}

	@Before
	public void setUp() {
		pl = new LinkedList<Piece>();
		expectedMoves = new LinkedList<GenericMove>();
	}

	protected void checkExpectedMoves(LinkedList<GenericMove> ml) {
		assertFalse(ml.isEmpty());
		assertTrue(ml.size()==expectedNumMoves);
		for ( GenericMove mov : expectedMoves) {
			assertTrue( ml.contains( mov ));
		}
	}

	protected void checkNoMovesGenerated(LinkedList<GenericMove> ml) {
		assertTrue(ml.isEmpty());
	}

	protected LinkedList<GenericMove> completeSetupAndGenerateMoves() {
		pl.add(classUnderTest);
		bm = new BoardManager( new Board( pl ), Colour.white );
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		return ml;
	}

}