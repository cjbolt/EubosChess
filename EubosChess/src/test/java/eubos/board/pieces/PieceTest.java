package eubos.board.pieces;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.Board;
import eubos.position.PositionManager;

public class PieceTest {

	protected List<Piece> pl;
	protected PositionManager pm;
	protected GenericMove expectedMove;
	protected List<GenericMove> expectedMoves;
	protected int expectedNumMoves = 0;
	protected Piece classUnderTest;
	protected List<GenericMove> ml;

	public PieceTest() {
		super();
	}

	@Before
	public void setUp() {
		pl = new LinkedList<Piece>();
		expectedMoves = new LinkedList<GenericMove>();
	}

	protected void checkExpectedMoves(List<GenericMove> ml) {
		assertFalse(ml.isEmpty());
		assertTrue(ml.size()==expectedNumMoves);
		for ( GenericMove mov : expectedMoves) {
			assertTrue( ml.contains( mov ));
		}
	}

	protected void checkNoMovesGenerated(List<GenericMove> ml) {
		assertTrue(ml.isEmpty());
	}

	protected List<GenericMove> completeSetupAndGenerateMoves() {
		pl.add(classUnderTest);
		List<GenericMove> ml = classUnderTest.generateMoves(new Board( pl ));
		return ml;
	}

}