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
	private int moveRookPawn = Move.valueOf(Position.h2, Piece.WHITE_PAWN, Position.h4, Piece.NONE);
	private int moveKnightPawn = Move.valueOf(Position.g2, Piece.WHITE_PAWN, Position.g4, Piece.NONE);
	
	@Before
	public void setUp() throws Exception {
		sut = new KillerList();
	}
	
	@Test
	public void test_canAddOneAndGetTwoMoves() {
		if (KillerList.ENABLE_KILLER_MOVES) {
			int ply = 0;
			sut.addMove(ply, moveKingPawn);
			int [] moves = sut.getMoves(ply);
			assertTrue(Move.areEqualForBestKiller(moveKingPawn, moves[0]));
			assertTrue(Move.areEqualForBestKiller(Move.NULL_MOVE, moves[1]));
			assertTrue(Move.areEqualForBestKiller(Move.NULL_MOVE, moves[2]));
		}
	}
	
	@Test
	public void test_canAddTwoAndGetTwoMoves() {
		if (KillerList.ENABLE_KILLER_MOVES) {
			int ply = 0;
			sut.addMove(ply, moveKingPawn);
			sut.addMove(ply, moveQueenPawn);
			int [] moves = sut.getMoves(ply);
			assertTrue(Move.areEqualForBestKiller(moveKingPawn, moves[0]));
			assertTrue(Move.areEqualForBestKiller(moveQueenPawn, moves[1]));
		}
	}
	
	@Test
	public void test_canAddThreeMovesReplacingOldest() {
		if (KillerList.ENABLE_KILLER_MOVES) {
			int ply = 0;
			sut.addMove(ply, moveKingPawn);
			sut.addMove(ply, moveQueenPawn);
			sut.addMove(ply, moveRookPawn);
			// Rook pawn move should replace oldest move
			int [] moves = sut.getMoves(ply);
			assertTrue(Move.areEqualForBestKiller(moveKingPawn, moves[0]));
			assertTrue(Move.areEqualForBestKiller(moveQueenPawn, moves[1]));
			assertTrue(Move.areEqualForBestKiller(moveRookPawn, moves[2]));
		}
	}
	
	@Test
	public void test_canAddFourMovesReplacingOldest() {
		if (KillerList.ENABLE_KILLER_MOVES) {
			int ply = 0;
			sut.addMove(ply, moveKingPawn);
			sut.addMove(ply, moveQueenPawn);
			sut.addMove(ply, moveRookPawn);
			sut.addMove(ply, moveKnightPawn);
			// Rook pawn move should replace oldest move
			int [] moves = sut.getMoves(ply);
			assertTrue(Move.areEqualForBestKiller(moveKnightPawn, moves[0]));
			assertTrue(Move.areEqualForBestKiller(moveQueenPawn, moves[1]));
		}
	}

	@Test
	public void test_whenMoveIsTheSameDontReplace() {
		if (KillerList.ENABLE_KILLER_MOVES) {
			int ply = 0;
			sut.addMove(ply, moveKingPawn);
			sut.addMove(ply, moveKingPawn);
			sut.addMove(ply, moveKingPawn);
			sut.addMove(ply, moveKingPawn);
			// Rook pawn move should replace oldest move
			
			int [] moves = sut.getMoves(ply);
			assertTrue(Move.areEqualForBestKiller(moveKingPawn, moves[0]));
			assertTrue(Move.areEqualForBestKiller(Move.NULL_MOVE, moves[1]));
		}
	}
	
	@Test
	public void test_whenMoveIsTheSameDontReplace_alt() {
		if (KillerList.ENABLE_KILLER_MOVES) {
			int ply = 0;
			sut.addMove(ply, moveKingPawn);
			sut.addMove(ply, moveQueenPawn);
			sut.addMove(ply, moveQueenPawn);
			sut.addMove(ply, moveKingPawn);
			
			int [] moves = sut.getMoves(ply);
			assertTrue(Move.areEqualForBestKiller(moveKingPawn, moves[0]));
			assertTrue(Move.areEqualForBestKiller(moveQueenPawn, moves[1]));
		}
	}
	
	@Test
	public void test_killerListShouldBeLastTwoMovesAdded() {
		if (KillerList.ENABLE_KILLER_MOVES) {
			int ply = 0;
			sut.addMove(ply, moveKingPawn);
			sut.addMove(ply, moveQueenPawn);
			sut.addMove(ply, moveQueenPawn);
			sut.addMove(ply, moveKingPawn);
			sut.addMove(ply, moveRookPawn);
			
			int [] moves = sut.getMoves(ply);
			assertTrue(Move.areEqualForBestKiller(moveKingPawn, moves[0]));
			assertTrue(Move.areEqualForBestKiller(moveQueenPawn, moves[1]));
			assertTrue(Move.areEqualForBestKiller(moveRookPawn, moves[2]));
		}
	}
	
	@Test
	public void test_killerListShouldBeLastTwoMovesAdded_alt() {
		if (KillerList.ENABLE_KILLER_MOVES) {
			int ply = 0;
			sut.addMove(ply, moveKingPawn);
			sut.addMove(ply, moveQueenPawn);
			sut.addMove(ply, moveQueenPawn);
			sut.addMove(ply, moveKingPawn);
			sut.addMove(ply, moveRookPawn);
			sut.addMove(ply, moveKnightPawn);

			int [] moves = sut.getMoves(ply);
			assertTrue(Move.areEqualForBestKiller(moveKnightPawn, moves[0]));
			assertTrue(Move.areEqualForBestKiller(moveQueenPawn, moves[1]));
		}
	}
}
