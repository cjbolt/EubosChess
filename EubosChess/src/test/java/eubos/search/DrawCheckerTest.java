package eubos.search;

import static org.junit.Assert.*;

import java.util.logging.*;

import org.junit.Before;
import org.junit.Test;

import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.position.Position;
import eubos.position.PositionManager;

public class DrawCheckerTest {
	private DrawChecker sut;
	private PositionManager pm;

	private void setupPosition(String fen) {
		pm = new PositionManager(fen, sut, new eubos.score.PawnEvalHashTable());
	}
	
	@Before
	public void setUp() {
		EubosEngineMain.logger.setLevel(Level.OFF);
		sut = new DrawChecker();
	}
	
	@Test
	public void test_DetectThreeFoldRepetitionDraw() {
		setupPosition("5r1k/pp5p/6p1/1N2q3/2P1P1n1/1P6/P2Q2PP/3R2K1 w - - 0 1");
		// Note no moves have yet been applied, the draw checker is empty
		pm.performMove(Move.valueOf(Position.g1, Piece.WHITE_KING, Position.h1, Piece.NONE));
		pm.performMove(Move.valueOf(Position.h8, Piece.BLACK_KING, Position.g8, Piece.NONE));
		pm.performMove(Move.valueOf(Position.h1, Piece.WHITE_KING, Position.g1, Piece.NONE));
		pm.performMove(Move.valueOf(Position.g8, Piece.BLACK_KING, Position.h8, Piece.NONE));
		// With this next move we have repeated the position after the first move above.
		pm.performMove(Move.valueOf(Position.g1, Piece.WHITE_KING, Position.h1, Piece.NONE));
		assertTrue(pm.isThreefoldRepetitionPossible());
	}
	
	@Test
	public void test_DetectThreeFoldRepetitionDraw_NotADraw() {
		setupPosition("5r1k/pp5p/6p1/1N2q3/2P1P1n1/1P6/P2Q2PP/3R2K1 w - - 0 1");
		Long hashCode = pm.getHash();
		pm.performMove(Move.valueOf(Position.g1, Piece.WHITE_KING, Position.h1, Piece.NONE));
		assertFalse(sut.setPositionReached(hashCode, pm.getPlyNumber()));
	}
}
