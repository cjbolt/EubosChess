package eubos.search.generators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.Piece;
import eubos.board.Piece.Colour;
import eubos.position.Move;
import eubos.position.PositionManager;

import eubos.search.SearchResult;

public class RandomMoveGeneratorTest {
	
	protected LinkedList<Piece> pl;
	protected RandomMoveGenerator classUnderTest;
	protected GenericMove expectedMove;
	
	@Before
	public void setUp() {
		pl = new LinkedList<Piece>();
	}
	
	private PositionManager createPm(String fenString) {
		PositionManager pm = new PositionManager(fenString);
		return pm;
	}
	
	private void performTest( boolean assertSense ) {
		SearchResult res = classUnderTest.findMove((byte)0);
		if ( assertSense )
			assertEquals(expectedMove, Move.toGenericMove(res.pv[0]));
		else
			assertNotEquals(expectedMove, Move.toGenericMove(res.pv[0]));
	}
	
	@Test
	public void test_findBestMove_DoNotMoveIntoCheck()  {
		// 8 K.......
		// 7 ........
		// 6 ..p.....
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ........
		//   abcdefgh
		PositionManager bm = createPm( "k7/8/2P5/8/8/8/8/8 b - - 0 1" );
		classUnderTest = new RandomMoveGenerator( bm, Colour.black );
		expectedMove = new GenericMove( GenericPosition.a8, GenericPosition.b7 );
		performTest(false);
	}
	
	@Test
	@Ignore
	public void test_findBestMove_CaptureToEscapeCheck()  {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 .P......
		// 2 PPP.....
		// 1 kP......
		//   abcdefgh
		// pawn at b2 can be captured to escape check
		PositionManager bm = createPm("8/8/8/8/8/1p6/ppp5/Kp6 w - - 0 1");
		classUnderTest = new RandomMoveGenerator( bm, Colour.white );
		expectedMove = new GenericMove( GenericPosition.a1, GenericPosition.b2 );
		performTest(true);			
	}
	
	@Test
	@Ignore
	public void test_findBestMove_MoveToEscapeCheck()  {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 .PP.....
		// 2 .P......
		// 1 k.......
		//   abcdefgh
		// king can move out of check to b1
		PositionManager bm = createPm("8/8/8/8/8/1pp5/1p6/K7 w - - 0 1 ");
		classUnderTest = new RandomMoveGenerator( bm, Colour.white );
		expectedMove = new GenericMove( GenericPosition.a1, GenericPosition.b1 );
		performTest(true);
	}
	
	@Test
	public void test_findBestMove_NoLegalMove()  {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 .PP.....
		// 2 PPP.....
		// 1 kP......
		//   abcdefgh
		PositionManager bm = createPm("8/8/8/8/8/1pp5/ppp5/Kp6 w - - 0 1");
		classUnderTest = new RandomMoveGenerator( bm, Colour.white );
		assertEquals(Move.NULL_MOVE, classUnderTest.findMove((byte)0).pv[0]);
	}
}
