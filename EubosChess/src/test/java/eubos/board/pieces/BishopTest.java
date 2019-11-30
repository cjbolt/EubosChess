package eubos.board.pieces;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.position.PositionManager;

public class BishopTest extends PieceTest {
	
	@Test
	public void test_CornerTopLeft() {
		pm = new PositionManager("b7/8/8/8/8/8/8/8 b - - 0 1");
		classUnderTest = (Bishop)pm.getTheBoard().getPieceAtSquare( GenericPosition.a8 );
		ml = classUnderTest.generateMoves(pm.getTheBoard());
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.b7 ));
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.h1 ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml);
	}

	@Test
	public void test_CornerTopRight() {
		pm = new PositionManager("7b/8/8/8/8/8/8/8 b - - 0 1");
		classUnderTest = (Bishop)pm.getTheBoard().getPieceAtSquare( GenericPosition.h8 );
		ml = classUnderTest.generateMoves(pm.getTheBoard());
		expectedMoves.add( new GenericMove( GenericPosition.h8, GenericPosition.g7 ));
		expectedMoves.add( new GenericMove( GenericPosition.h8, GenericPosition.a1 ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CornerBottomRight() {
		pm = new PositionManager("8/8/8/8/8/8/8/7b b - - 0 1");
		classUnderTest = (Bishop)pm.getTheBoard().getPieceAtSquare( GenericPosition.h1 );
		ml = classUnderTest.generateMoves(pm.getTheBoard());
		expectedMoves.add( new GenericMove( GenericPosition.h1, GenericPosition.g2 ));
		expectedMoves.add( new GenericMove( GenericPosition.h1, GenericPosition.a8 ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CornerBottomLeft() {
		pm = new PositionManager("8/8/8/8/8/8/8/b7 b - - 0 1");
		classUnderTest = (Bishop)pm.getTheBoard().getPieceAtSquare( GenericPosition.a1 );
		ml = classUnderTest.generateMoves(pm.getTheBoard());
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.b2 ));
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.h8 ));
		expectedNumMoves = 7;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CornerBottomLeft_ObstructedOwnPieces() {
		pm = new PositionManager("8/8/8/8/8/8/1p6/b7 b - - 0 1");
		classUnderTest = (Bishop)pm.getTheBoard().getPieceAtSquare( GenericPosition.a1 );
		ml = classUnderTest.generateMoves(pm.getTheBoard());
		checkNoMovesGenerated(ml);
	}

	@Test
	public void test_LeftEdge_PartiallyObstructedOwnPiece() {
		pm = new PositionManager("8/8/8/8/b7/1p6/8/8 b - - 0 1");
		classUnderTest = (Bishop)pm.getTheBoard().getPieceAtSquare( GenericPosition.a4 );
		ml = classUnderTest.generateMoves(pm.getTheBoard());
		expectedMoves.add( new GenericMove( GenericPosition.a4, GenericPosition.b5 ));
		expectedMoves.add( new GenericMove( GenericPosition.a4, GenericPosition.e8 ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml);		
	}
	
	@Test
	public void test_LeftEdge_PartiallyObstructedCapturablePiece() {
		pm = new PositionManager("8/8/8/8/b7/1P6/8/8 b - - 0 1");
		classUnderTest = (Bishop)pm.getTheBoard().getPieceAtSquare( GenericPosition.a4 );
		ml = classUnderTest.generateMoves(pm.getTheBoard());
		expectedMoves.add( new GenericMove( GenericPosition.a4, GenericPosition.b5 ));
		expectedMoves.add( new GenericMove( GenericPosition.a4, GenericPosition.e8 ));
		expectedMoves.add( new GenericMove( GenericPosition.a4, GenericPosition.b3 ));
		expectedNumMoves = 5;
		checkExpectedMoves(ml);	
	}
	
	@Test
	public void test_Middle_ObstructedCapturablePieces() {
		pm = new PositionManager("8/8/8/3P1P2/4b3/3P1P2/8/8 b - - 0 1");
		classUnderTest = (Bishop)pm.getTheBoard().getPieceAtSquare( GenericPosition.e4 );
		ml = classUnderTest.generateMoves(pm.getTheBoard());
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d3 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d5 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f3 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f5 ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Middle_ObstructedMixturePieces() {
		pm = new PositionManager("8/8/8/3P1p2/4b3/3P1p2/8/8 b - - 0 1");
		classUnderTest = (Bishop)pm.getTheBoard().getPieceAtSquare( GenericPosition.e4 );
		ml = classUnderTest.generateMoves(pm.getTheBoard());
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d3 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d5 ));
		expectedNumMoves = 2;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Middle_Unobstructed() {
		pm = new PositionManager("8/8/8/4P3/3PbP2/4P3/8/8 b - - 0 1");
		classUnderTest = (Bishop)pm.getTheBoard().getPieceAtSquare( GenericPosition.e4 );
		ml = classUnderTest.generateMoves(pm.getTheBoard());
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d3 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d5 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f3 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f5 ));
		expectedNumMoves = 13;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CapturesOnlySinglePiece() {
		pm = new PositionManager("8/8/8/8/8/2P5/1P6/b7 b - - 0 1");
		classUnderTest = (Bishop)pm.getTheBoard().getPieceAtSquare( GenericPosition.a1 );
		ml = classUnderTest.generateMoves(pm.getTheBoard());
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.b2 ));
		expectedNumMoves = 1;
		checkExpectedMoves(ml);
		assertFalse(ml.contains( new GenericMove( GenericPosition.a1, GenericPosition.c3 )));
	}
}
