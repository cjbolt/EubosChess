package eubos.board.pieces;

import static org.junit.Assert.*;

import org.junit.Test;

import eubos.board.InvalidPieceException;
import eubos.position.PositionManager;

import com.fluxchess.jcpi.models.*;

public class PawnTest_Black extends PawnTest {

	@Test
	public void test_InitialMoveOneSquare() {
		theBoard = new PositionManager("8/4p3/8/8/8/8/8/8 b - - 0 1").getTheBoard();
		classUnderTest = (Pawn)theBoard.getPieceAtSquare( GenericPosition.e7 );
		ml = classUnderTest.generateMoves(theBoard);
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e6 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_InitialMoveTwoSquares() {
		theBoard = new PositionManager("8/4p3/8/8/8/8/8/8 b - - 0 1").getTheBoard();
		classUnderTest = (Pawn)theBoard.getPieceAtSquare( GenericPosition.e7 );
		ml = classUnderTest.generateMoves(theBoard);
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e5 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_InitialBlocked() {
		theBoard = new PositionManager("8/4p3/4P3/8/8/8/8/8 b - - 0 1 ").getTheBoard();
		classUnderTest = (Pawn)theBoard.getPieceAtSquare( GenericPosition.e7 );
		ml = classUnderTest.generateMoves(theBoard);
		assertTrue( ml.isEmpty() );
	}

	@Test
	public void test_CaptureEnPassantLeft() throws InvalidPieceException {
		// Black is on e4, white moves f4, then black ml contains capture en passant, exf
		pm = new PositionManager("8/8/8/8/4p3/8/5P2/8 w - - 0 1");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.e4);
		pm.performMove( new GenericMove( GenericPosition.f2, GenericPosition.f4 ));
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		expectedMove = new GenericMove( GenericPosition.e4, GenericPosition.f3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureEnPassantLeftFalse() throws InvalidPieceException {
		// Black is on e4, white moves a knight to f4, check black ml doesn't contain a capture en passant, exf
		pm = new PositionManager("8/8/8/8/4p3/8/5N2/8 w - - 0 1 ");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.e4);
		pm.performMove( new GenericMove( GenericPosition.f2, GenericPosition.f4 ));
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		expectedMove = new GenericMove( GenericPosition.e4, GenericPosition.f3 );
		assertFalse( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureEnPassantRight() throws InvalidPieceException {
		// Black is on e4, white moves d4, then black ml contains capture en passant, exd
		pm = new PositionManager("8/8/8/8/4p3/8/3P4/8 w - - 0 1 ");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.e4);
		pm.performMove( new GenericMove( GenericPosition.d2, GenericPosition.d4 ));
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		expectedMove = new GenericMove( GenericPosition.e4, GenericPosition.d3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureEnPassantRightFalse() throws InvalidPieceException {
		// Black is on e4, white moves a knight to d4, check black ml doesn't contain a capture en passant, exd
		pm = new PositionManager("8/8/8/8/4p3/8/3N4/8 w - - 0 1 ");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.e4);
		pm.performMove( new GenericMove( GenericPosition.d2, GenericPosition.d4 ));
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		expectedMove = new GenericMove( GenericPosition.e4, GenericPosition.d3 );
		assertFalse( ml.contains( expectedMove ));
	}	
	
	@Test
	public void test_CaptureEnPassantFromAFile() throws InvalidPieceException {
		// Black is on a4, white moves b4, then black ml contains capture en passant, axb
		pm = new PositionManager("8/8/8/8/p7/8/1P6/8 w - - 0 1 ");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.a4);
		pm.performMove( new GenericMove( GenericPosition.b2, GenericPosition.b4 ));
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		expectedMove = new GenericMove( GenericPosition.a4, GenericPosition.b3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureEnPassantFromAFile_1() throws InvalidPieceException {
		// Black is on a4, white moves b4, then black ml contains capture en passant, axb
		pm = new PositionManager("8/8/8/8/pP6/8/8/8 w - - 0 1 ");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.a4);
		pm.performMove( new GenericMove( GenericPosition.b4, GenericPosition.b5 ));
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		expectedMove = new GenericMove( GenericPosition.a4, GenericPosition.b3 );
		assertFalse( ml.contains( expectedMove ));
	}	
	
	@Test
	public void test_CaptureEnPassantFromHFile() throws InvalidPieceException {
		// Black is on h4, white moves g4, then black ml contains capture en passant, hxg
		pm = new PositionManager("8/8/8/8/7p/8/6P1/8 w - - 0 1 ");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.h4);
		pm.performMove( new GenericMove( GenericPosition.g2, GenericPosition.g4 ));
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		expectedMove = new GenericMove( GenericPosition.h4, GenericPosition.g3 );
		assertTrue( ml.contains( expectedMove ));
	}		
	
	@Test
	public void test_MoveOneSquare() throws InvalidPieceException {
		// After initial move, ensure that a pawn can't move 2 any longer
		pm = new PositionManager("8/4p3/8/8/8/8/4P3/8 w - - 0 1 ");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.e7);
		pm.performMove( new GenericMove( GenericPosition.e7, GenericPosition.e6 ));
		pm.performMove( new GenericMove( GenericPosition.e2, GenericPosition.e4 ));
		
		classUnderTest = (Pawn) pm.getTheBoard().getPieceAtSquare(GenericPosition.e6);
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		expectedMove = new GenericMove( GenericPosition.e6, GenericPosition.e5 );
		assertTrue( ml.size() == 1 );
		assertTrue( ml.contains( expectedMove ));		
	}	

	@Test
	public void test_CaptureLeft() {
		pm = new PositionManager("8/4p3/5P2/8/8/8/8/8 w - - 0 1 ");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.e7);
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.f6 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_CaptureRight() {
		pm = new PositionManager("8/4p3/3P4/8/8/8/8/8 w - - 0 1 ");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.e7);
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.d6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureFork() {
		pm = new PositionManager("8/4p3/3P1P2/8/8/8/8/8 w - - 0 1 ");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.e7);
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		GenericMove captureLeft = new GenericMove( GenericPosition.e7, GenericPosition.d6 );
		GenericMove captureRight = new GenericMove( GenericPosition.e7, GenericPosition.f6 );
		assertTrue( ml.contains( captureLeft ));
		assertTrue( ml.contains( captureRight ));
	}	
	
	@Test
	public void test_CaptureFromAFile() {
		// Can only capture left
		pm = new PositionManager("8/p7/1P6/8/8/8/8/8 w - - 0 1 ");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.a7);
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		expectedMove = new GenericMove( GenericPosition.a7, GenericPosition.b6 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_CaptureFromHFile() {
		// Can only capture right
		pm = new PositionManager("8/7p/6P1/8/8/8/8/8 w - - 0 1 ");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.h7);
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		expectedMove = new GenericMove( GenericPosition.h7, GenericPosition.g6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_PromoteQueen() {
		pm = new PositionManager("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.e2);
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e1, GenericChessman.QUEEN );
		assertTrue( ml.contains( expectedMove ));
	}	

	@Test
	public void test_PromoteKnight() {
		pm = new PositionManager("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.e2);
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e1, GenericChessman.KNIGHT );
		assertTrue( ml.contains( expectedMove ));		
	}

	@Test
	public void test_PromoteBishop() {
		pm = new PositionManager("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.e2);
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e1, GenericChessman.BISHOP );
		assertTrue( ml.contains( expectedMove ));			
	}

	@Test
	public void test_PromoteRook() {
		pm = new PositionManager("8/8/8/8/8/8/4p3/8 b - - 0 1 ");
		classUnderTest = (Pawn)pm.getTheBoard().getPieceAtSquare(GenericPosition.e2);
		ml = classUnderTest.generateMoves( pm.getTheBoard() );
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e1, GenericChessman.ROOK );
		assertTrue( ml.contains( expectedMove ));	
	}
}