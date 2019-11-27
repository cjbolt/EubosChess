package eubos.board.pieces;

import java.util.List;

import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.position.PositionManager;

public class KingTest extends PieceTest {

	protected PieceSinglesquareDirectMove classUnderTest;
	
	@Test
	public void test_CornerTopLeft() {
		Board theBoard = new PositionManager("k7/8/8/8/8/8/8/8 b - - 0 1").getTheBoard();
		classUnderTest = (King)theBoard.getPieceAtSquare(GenericPosition.a8);
		List<GenericMove> ml = classUnderTest.generateMoves(theBoard);
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.a7 ));
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.b8 ));
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.b7 ));
		expectedNumMoves = 3;
		checkExpectedMoves(ml);
	}	
}
