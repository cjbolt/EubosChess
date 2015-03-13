package eubos.pieces;

import java.util.LinkedList;

import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Board;
import eubos.board.BoardManager;
import eubos.pieces.Piece.Colour;

public class KingTest extends PieceTest {

	protected SinglesquareDirectMovePiece classUnderTest;
	
	@Test
	public void test_CornerTopLeft() {
		classUnderTest = new King( Colour.black, GenericPosition.a8 );
		pl.add(classUnderTest);
		bm = new BoardManager( new Board( pl ));
		LinkedList<GenericMove> ml = classUnderTest.generateMoves( bm );
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.a7 ));
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.b8 ));
		expectedMoves.add( new GenericMove( GenericPosition.a8, GenericPosition.b7 ));
		expectedNumMoves = 3;
		checkExpectedMoves(ml);
	}	
}
