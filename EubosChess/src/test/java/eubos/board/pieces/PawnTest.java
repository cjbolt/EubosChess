package eubos.board.pieces;

import org.junit.Test;

import com.fluxchess.jcpi.models.GenericPosition;

import eubos.position.InvalidPieceException;

public abstract class PawnTest extends PieceTest {

	protected PieceSinglesquareDirectMove classUnderTest;
	
	@Test
	public abstract void test_InitialMoveOneSquare();
	
	@Test
	public abstract void test_InitialMoveTwoSquares();
	
	@Test
	public abstract void test_CaptureEnPassantLeft() throws InvalidPieceException;

	@Test
	public abstract void test_CaptureEnPassantRight() throws InvalidPieceException;	
	
	@Test
	public abstract void test_MoveOneSquare() throws InvalidPieceException;
	
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

	protected PieceSinglesquareDirectMove addBlackPawn(GenericPosition square) {
		PieceSinglesquareDirectMove newPawn = new Pawn( Piece.Colour.black, square );
		pl.add( newPawn );
		return newPawn;
	}

	protected PieceSinglesquareDirectMove addWhitePawn(GenericPosition square) {
		PieceSinglesquareDirectMove newPawn = new Pawn( Piece.Colour.white, square );
		pl.add( newPawn );
		return newPawn;
	}
}
