package eubos.board.pieces;

import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.position.PositionManager;

public class KnightTest extends PieceTest {
	
	GenericPosition startTestOnSq;
	@Test
	public void test_SquareA8() {
		startTestOnSq = GenericPosition.a8;
		pm = new PositionManager("N7/8/8/8/8/8/8/8 w - - 0 1");
		ml = pm.generateMoves();
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.c7 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.b6 ));
		expectedNumMoves = 2;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_SquareB7() {
		startTestOnSq = GenericPosition.b7;
		pm = new PositionManager("8/1N6/8/8/8/8/8/8 w - - 0 1");
		ml = pm.generateMoves();
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.d8 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.d6 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.a5 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.c5 ));
		expectedNumMoves = 4;
		checkExpectedMoves(ml);
	}
	
	@Test
	public void test_SquareC6() {
		startTestOnSq = GenericPosition.c6;
		pm = new PositionManager("8/8/2N5/8/8/8/8/8 w - - 0 1");
		ml = pm.generateMoves();
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.b8 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.d8 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.e7 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.e5 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.b4 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.d4 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.a7 ));
		expectedMoves.add( new GenericMove( startTestOnSq, GenericPosition.a5 ));
		expectedNumMoves = 8;
		checkExpectedMoves(ml);
	}	
}
