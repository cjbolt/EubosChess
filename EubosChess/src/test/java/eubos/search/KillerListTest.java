package eubos.search;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import eubos.board.Piece;
import eubos.position.Move;
import eubos.position.Position;

public class KillerListTest {

	private KillerList sut;
	
	private int moveKingPawn  = Move.valueOf(Position.e2, Piece.WHITE_PAWN, Position.e4, Piece.NONE);
	private int moveQueenPawn = Move.valueOf(Position.d2, Piece.WHITE_PAWN, Position.d4, Piece.NONE);
	
	@Before
	public void setUp() throws Exception {
		sut = new KillerList(35);
	}

	@Test
	public void test_canAddAndGetOneMove() {
		if (KillerList.ENABLE_KILLER_MOVES) {
			int ply = 0;
			sut.addMove(ply, moveKingPawn);
			assertEquals(moveKingPawn, sut.getMove(ply));
		}
	}
	
	@Test
	public void test_canAddOneAndGetTwoMoves() {
		if (KillerList.ENABLE_KILLER_MOVES) {
			int ply = 0;
			sut.addMove(ply, moveKingPawn);
			int [] moves = sut.getMoves(ply);
			assertEquals(moveKingPawn, moves[0]);
			assertEquals(Move.NULL_MOVE, moves[1]);
		}
	}
	
	@Test
	public void test_canAddTwoAndGetTwoMoves() {
		if (KillerList.ENABLE_KILLER_MOVES) {
			int ply = 0;
			sut.addMove(ply, moveKingPawn);
			sut.addMove(ply, moveQueenPawn);
			int [] moves = sut.getMoves(ply);
			assertEquals(moveKingPawn, moves[0]);
			assertEquals(moveQueenPawn, moves[1]);
		}
	}

}
