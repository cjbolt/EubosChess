package eubos.board.pieces;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.position.PositionManager;

public class RookTest extends PieceTest {
	
	@Test
	public void test_CornerTopLeft() {
		pm = new PositionManager("R7/8/8/8/8/8/8/8 w - - 0 1 ");
		ml = pm.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.a7 ));
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.b8 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}

	@Test
	public void test_CornerTopRight() {
		pm = new PositionManager("7R/8/8/8/8/8/8/8 w - - 0 1 ");
		ml = pm.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.h8, GenericPosition.h7 ));
		expectedMoves.add( new GenericMove( GenericPosition.h8, GenericPosition.g8 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CornerBottomRight() {
		pm = new PositionManager("8/8/8/8/8/8/8/7R w - - 0 1 ");
		ml = pm.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.h1, GenericPosition.h2 ));
		expectedMoves.add( new GenericMove( GenericPosition.h1, GenericPosition.g1 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CornerBottomLeft() {
		pm = new PositionManager("8/8/8/8/8/8/8/R7 w - - 0 1 ");
		ml = pm.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.a2 ));
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.b1 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CornerBottomLeft_ObstructedOwnPieces() {
		pm = new PositionManager("8/8/8/8/8/8/P7/RK6 w - - 0 1");
		ml = pm.generateMoves();
		expectedNumMoves = 5;
		checkExpectedMoves(ml);
	}

	@Test
	public void test_CornerBottomLeft_PartiallyObstructedOwnPiece() {
		pm = new PositionManager("8/8/8/8/8/8/P7/R7 w - - 0 1");
		ml = pm.generateMoves();
		expectedNumMoves = 9;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CornerBottomLeft_PartiallyObstructedCapturablePiece() {
		pm = new PositionManager("8/8/8/8/8/8/p7/R7 w - - 0 1");
		ml = pm.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.a2 ));
		expectedMoves.add( new GenericMove( GenericPosition.a1, GenericPosition.b1 ));		
		expectedNumMoves = 8;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Middle_ObstructedCapturablePieces() {
		pm = new PositionManager("8/8/8/4P3/3PrP2/4P3/8/8 b - - 0 1");
		ml = pm.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f4 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d4 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e5 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e3 ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Middle_ObstructedMixturePieces() {
		pm = new PositionManager("8/8/8/4P3/3prp2/4P3/8/8 b - - 0 1");
		ml = pm.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e5 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e3 ));
		expectedNumMoves = 6;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_Middle_Unobstructed() {
		pm = new PositionManager("8/8/8/3P1P2/4r3/3P1P2/8/8 b - - 0 1 ");
		ml = pm.generateMoves();
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.f4 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.d4 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e5 ));
		expectedMoves.add( new GenericMove( GenericPosition.e4, GenericPosition.e3 ));
		expectedNumMoves = 14;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_CapturesOnlySinglePiece() {
		pm = new PositionManager("8/8/8/8/8/P7/P7/r7 b - - 0 1 ");
		ml = pm.generateMoves();
		assertFalse(ml.isEmpty());
		assertTrue( ml.contains( new GenericMove( GenericPosition.a1, GenericPosition.a2 )));
		assertFalse(ml.contains( new GenericMove( GenericPosition.a1, GenericPosition.a3 )));
	}
}
