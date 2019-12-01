package eubos.board.pieces;

import static org.junit.Assert.*;

import org.junit.Test;

import eubos.board.InvalidPieceException;
import eubos.position.*;

import com.fluxchess.jcpi.models.*;

public class PawnTest_White extends PawnTest {
	@Test
	public void test_InitialMoveOneSquare() {
		pm = new PositionManager("8/8/8/8/8/8/4P3/8 w - - 0 1");
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e4 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_InitialMoveTwoSquares() {
		pm = new PositionManager("8/8/8/8/8/8/4P3/8 w - - 0 1");
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.e3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_InitialBlocked() {
		pm = new PositionManager("8/8/8/8/8/4p3/4P3/8 w - - 0 1");
		ml = pm.generateMoves();
		assertTrue( ml.isEmpty() );
	}

	@Test
	public void test_CaptureEnPassantLeft() throws InvalidPieceException {
		pm = new PositionManager("8/3p4/8/4P3/8/8/8/8 b - - 0 1");
		pm.performMove( new GenericMove( GenericPosition.d7, GenericPosition.d5 ));
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e5, GenericPosition.d6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureEnPassantLeftFalse() throws InvalidPieceException {
		pm = new PositionManager("8/5r2/8/4P3/8/8/8/8 b - - 0 1");
		pm.performMove( new GenericMove( GenericPosition.f7, GenericPosition.f5 ));
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e5, GenericPosition.f6 );
		assertFalse( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureEnPassantRight() throws InvalidPieceException {
		pm = new PositionManager("8/5p2/8/4P3/8/8/8/8 b - - 0 1");
		pm.performMove( new GenericMove( GenericPosition.f7, GenericPosition.f5 ));
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e5, GenericPosition.f6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureEnPassantRightFalse() throws InvalidPieceException {
		pm = new PositionManager("8/5r2/8/4P3/8/8/8/8 b - - 0 1");
		pm.performMove( new GenericMove( GenericPosition.f7, GenericPosition.f5 ));
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e5, GenericPosition.f6 );
		assertFalse( ml.contains( expectedMove ));
	}	
	
	@Test
	public void test_CaptureEnPassantFromAFile() throws InvalidPieceException {
		// white is on a5, black moves b5, then black ml contains capture en passant, axb
		pm = new PositionManager("8/1p6/8/P7/8/8/8/8 b - - 0 1");
		pm.performMove( new GenericMove( GenericPosition.b7, GenericPosition.b5 ));
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.a5, GenericPosition.b6 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureEnPassantFromHFile() throws InvalidPieceException {
		// Black is on h4, white moves g4, then black ml contains capture en passant, hxg
		pm = new PositionManager("8/6p1/8/7P/8/8/8/8 b - - 0 1");
		pm.performMove( new GenericMove( GenericPosition.g7, GenericPosition.g5 ));
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.h5, GenericPosition.g6 );
		assertTrue( ml.contains( expectedMove ));
	}		
	
	@Test
	public void test_MoveOneSquare() throws InvalidPieceException {
		// After initial move, ensure that a pawn can't move 2 any longer
		pm = new PositionManager("8/5p2/8/8/8/8/4P3/8 w - - 0 1");
		pm.performMove( new GenericMove( GenericPosition.e2, GenericPosition.e4 ));
		pm.performMove( new GenericMove( GenericPosition.f7, GenericPosition.f6 ));
		
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e4, GenericPosition.e5 );
		assertTrue( ml.size() == 1 );
		assertTrue( ml.contains( expectedMove ));		
	}	

	@Test
	public void test_CaptureLeft() {
		pm = new PositionManager("8/8/8/8/8/5p2/4P3/8 w - - 0 1");
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.f3 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_CaptureRight() {
		pm = new PositionManager("8/8/8/8/8/3p4/4P3/8 w - - 0 1");
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e2, GenericPosition.d3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_CaptureFork() {
		pm = new PositionManager("8/8/8/8/8/3p1p2/4P3/8 w - - 0 1");
		ml = pm.generateMoves();
		GenericMove captureLeft = new GenericMove( GenericPosition.e2, GenericPosition.d3 );
		GenericMove captureRight = new GenericMove( GenericPosition.e2, GenericPosition.f3 );
		assertTrue( ml.contains( captureLeft ));
		assertTrue( ml.contains( captureRight ));
	}	
	
	@Test
	public void test_CaptureFromAFile() {
		// Can only capture left
		pm = new PositionManager("8/8/8/8/8/1p6/P7/8 w - - 0 1");
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.a2, GenericPosition.b3 );
		assertTrue( ml.contains( expectedMove ));
	}

	@Test
	public void test_CaptureFromHFile() {
		// Can only capture right
		pm = new PositionManager("8/8/8/8/8/6p1/7P/8 w - - 0 1");
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.h2, GenericPosition.g3 );
		assertTrue( ml.contains( expectedMove ));
	}
	
	@Test
	public void test_PromoteQueen() {
		pm = new PositionManager("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e8, GenericChessman.QUEEN );
		assertTrue( ml.contains( expectedMove ));
	}	

	@Test
	public void test_PromoteKnight() {
		pm = new PositionManager("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e8, GenericChessman.KNIGHT );
		assertTrue( ml.contains( expectedMove ));		
	}

	@Test
	public void test_PromoteBishop() {
		pm = new PositionManager("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e8, GenericChessman.BISHOP );
		assertTrue( ml.contains( expectedMove ));			
	}

	@Test
	public void test_PromoteRook() {
		pm = new PositionManager("8/4P3/8/8/8/8/8/8 w - - 0 1 ");
		ml = pm.generateMoves();
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e8, GenericChessman.ROOK );
		assertTrue( ml.contains( expectedMove ));	
	}
}

