package eubos.pieces;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.BoardManager;

public abstract class PawnTest {

	protected LinkedList<Piece> pl;
	protected SinglesquareDirectMovePiece classUnderTest;
	protected BoardManager bm;
	protected GenericMove expectedMove;

	@Test
	public abstract void test_InitialMoveOneSquare();
	
	@Test
	public abstract void test_InitialMoveTwoSquares();
	
	@Test
	public abstract void test_CaptureEnPassantLeft();

	@Test
	public abstract void test_CaptureEnPassantRight();	
	
	@Test
	public abstract void test_MoveOneSquare();
	
	@Test
	public abstract void test_CaptureLeft();
	
	@Test
	public abstract void test_CaptureRight();
		
	@Test
	public abstract void test_PromoteQueen();
	
	@Test
	public abstract void test_PromoteKnight();

	@Test
	public abstract void test_PromoteBishop();
	
	@Test
	public abstract void test_PromoteRook();

	@Before
	public void setUp() {
		pl = new LinkedList<Piece>();
	}

	protected SinglesquareDirectMovePiece addBlackPawn(GenericPosition square) {
		SinglesquareDirectMovePiece newPawn = new Pawn( Piece.Colour.black, square );
		pl.add( newPawn );
		return newPawn;
	}

	protected SinglesquareDirectMovePiece addWhitePawn(GenericPosition square) {
		SinglesquareDirectMovePiece newPawn = new Pawn( Piece.Colour.white, square );
		pl.add( newPawn );
		return newPawn;
	}
}
