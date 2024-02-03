package eubos.search;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.jupiter.api.Test;

import eubos.board.Piece;
import eubos.position.Move;
import eubos.position.Position;
import it.unimi.dsi.fastutil.ints.IntArrays;

class HistoryTest {

	private History sut = new History();
	
	private int moveKingPawn  = Move.valueOf(Position.e2, Piece.WHITE_PAWN, Position.e4, Piece.NONE);
	private int moveQueenPawn = Move.valueOf(Position.d2, Piece.WHITE_PAWN, Position.d4, Piece.NONE);
	private int moveRookPawn = Move.valueOf(Position.h2, Piece.WHITE_PAWN, Position.h4, Piece.NONE);
	private int moveKnightPawn = Move.valueOf(Position.g2, Piece.WHITE_PAWN, Position.g4, Piece.NONE);
	
	int [] moves = new int []{moveKingPawn, moveQueenPawn, moveRookPawn, moveKnightPawn};
	
	@Before
	public void setUp() throws Exception {
		sut = new History();
		moves = new int []{moveKingPawn, moveQueenPawn, moveRookPawn, moveKnightPawn};
	}
	
	@Test
	public void test_canAddOneAndGetTwoMoves() {
		sut.updateMove(1, moveKingPawn);
		sut.updateMove(3, moveRookPawn);
		sut.updateMove(4, moveKnightPawn);
		
		IntArrays.quickSort(moves, 0, 4, sut.moveHistoryComparator);
		assertTrue(Move.areEqual(moveKnightPawn, moves[0]));
		assertTrue(Move.areEqual(moveRookPawn, moves[1]));
		assertTrue(Move.areEqual(moveKingPawn, moves[2]));
		assertTrue(Move.areEqual(moveQueenPawn, moves[3]));
	}
}
