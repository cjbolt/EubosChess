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
		pm = new PositionManager(fen, sut, null);
	}
	
	@Before
	public void setUp() {
		EubosEngineMain.logger.setLevel(Level.OFF);
		sut = new DrawChecker();
	}
	
	@Test
	public void test_CanIncrementCount() {
		setupPosition("5r1k/pp5p/6p1/1N2q3/2P1P1n1/1P6/P2Q2PP/3R2K1 w - - 0 1");
		Long hashCode = pm.getHash();
		assertEquals(0, (byte)sut.getPositionReachedCount(hashCode));
		sut.incrementPositionReachedCount(hashCode);
		assertEquals(1, (byte)sut.getPositionReachedCount(hashCode));
		sut.incrementPositionReachedCount(hashCode);
		assertEquals(2, (byte)sut.getPositionReachedCount(hashCode));
	}
	
	@Test
	public void test_DetectThreeFoldRepetitionDraw() {
		setupPosition("5r1k/pp5p/6p1/1N2q3/2P1P1n1/1P6/P2Q2PP/3R2K1 w - - 0 1");
		Long hashCode = pm.getHash();
		sut.incrementPositionReachedCount(hashCode);
		sut.incrementPositionReachedCount(hashCode);
		sut.incrementPositionReachedCount(hashCode);
		assertTrue(sut.isPositionOpponentCouldClaimDraw(hashCode));
	}
	
	@Test
	public void test_DetectThreeFoldRepetitionDraw_NotADraw() {
		setupPosition("5r1k/pp5p/6p1/1N2q3/2P1P1n1/1P6/P2Q2PP/3R2K1 w - - 0 1");
		Long hashCode = pm.getHash();
		sut.incrementPositionReachedCount(hashCode);
		assertFalse(sut.isPositionOpponentCouldClaimDraw(hashCode));
	}
	
	@Test
	public void test_TestSize_InitialAnd3Moves()  {
		setupPosition("r1bq1rk1/ppp2ppp/2np1n2/1B2p1B1/4P3/2PP1N2/P1P2PPP/R2Q1RK1 b - - 0 8");
		Long hashCode = pm.getHash();
		sut.incrementPositionReachedCount(hashCode);
		pm.performMove(Move.valueOf(Position.a7, Piece.BLACK_PAWN, Position.a6, Piece.NONE));
		pm.performMove(Move.valueOf(Position.b5, Piece.WHITE_BISHOP, Position.a4, Piece.NONE));;
		pm.performMove(Move.valueOf(Position.b7, Piece.BLACK_PAWN, Position.b5, Piece.NONE));
		//pm.performMove(Move.valueOf(Position.b5, Piece.WHITE_BISHOP, Position.a4, Piece.NONE));
		assertEquals(4, (int)sut.getNumEntries());
	}
}
