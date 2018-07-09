package eubos.board.pieces;

import java.util.List;

import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.pieces.Piece.Colour;

public class KingTest extends PieceTest {

	protected PieceSinglesquareDirectMove classUnderTest;
	
	@Test
	public void test_CornerTopLeft() {
		classUnderTest = new King( Colour.black, GenericPosition.a8 );
		pl.add(classUnderTest);
		List<GenericMove> ml = classUnderTest.generateMoves(new Board( pl ));
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.a7 ));
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.b8 ));
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.b7 ));
		expectedNumMoves = 3;
		checkExpectedMoves(ml);
	}	
}
