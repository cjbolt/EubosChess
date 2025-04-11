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
		pm = new PositionManager(fen, sut);
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
	
	@Test
	public void test_WAC009() {
		setupPosition("3q1rk1/p4pp1/2pb3p/3p4/6Pr/1PNQ4/P1PB1PP1/4RRK1 b - - 0 1");
		pm.performMove(Move.valueOf(Position.d6, Piece.BLACK_BISHOP, Position.h2, Piece.NONE));
		assertFalse(pm.isThreefoldRepetitionPossible());
		pm.performMove(Move.valueOf(Position.g1, Piece.WHITE_KING, Position.h1, Piece.NONE));
		assertFalse(pm.isThreefoldRepetitionPossible());
		pm.performMove(Move.valueOf(Position.h2, Piece.BLACK_BISHOP, Position.g3, Piece.NONE));
		assertFalse(pm.isThreefoldRepetitionPossible());
		pm.performMove(Move.valueOf(Position.h1, Piece.WHITE_KING, Position.g1, Piece.NONE));
		assertFalse(pm.isThreefoldRepetitionPossible());
		pm.performMove(Move.valueOf(Position.h4, Piece.BLACK_ROOK, Position.h1, Piece.NONE));
		assertFalse(pm.isThreefoldRepetitionPossible());
		pm.performMove(Move.valueOf(Position.g1, Piece.WHITE_KING, Position.h1, Piece.BLACK_ROOK));
		assertFalse(pm.isThreefoldRepetitionPossible());
		pm.performMove(Move.valueOf(Position.d8, Piece.BLACK_QUEEN, Position.h4, Piece.NONE));
		assertFalse(pm.isThreefoldRepetitionPossible());
		pm.performMove(Move.valueOf(Position.h1, Piece.WHITE_KING, Position.g1, Piece.NONE));
		assertFalse(pm.isThreefoldRepetitionPossible());
		pm.performMove(Move.valueOf(Position.h4, Piece.BLACK_QUEEN, Position.h2, Piece.NONE));
		assertFalse(pm.isThreefoldRepetitionPossible());
	}
}
