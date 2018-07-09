package eubos.search;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.board.pieces.Bishop;
import eubos.board.pieces.King;
import eubos.board.pieces.Knight;
import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece;
import eubos.board.pieces.Queen;
import eubos.board.pieces.Rook;
import eubos.board.pieces.Piece.Colour;
import eubos.position.Board;
import eubos.position.InvalidPieceException;
import eubos.position.PositionManager;
import eubos.search.NoLegalMoveException;
import eubos.search.RandomMoveGenerator;

public class RandomMoveGeneratorTest {
	
	protected LinkedList<Piece> pl;
	protected RandomMoveGenerator classUnderTest;
	protected GenericMove expectedMove;
	
	@Before
	public void setUp() {
		pl = new LinkedList<Piece>();
	}
	
	private void performTest( boolean assertSense ) {
		try {
			GenericMove selectedMove = classUnderTest.findMove();
			if ( assertSense )
				assertTrue(selectedMove.equals(expectedMove));
			else
				assertFalse(selectedMove.equals(expectedMove));
		} catch ( NoLegalMoveException e ) {
			assert( false );
		} catch (InvalidPieceException e) {
			assert(false);
		}
	}
	
	@Test
	public void test_findBestMove_DoNotMoveIntoCheck() {
		// 8 K.......
		// 7 ........
		// 6 ..p.....
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ........
		//   abcdefgh
		pl.add(new King( Colour.black, GenericPosition.a8 ));
		pl.add(new Pawn( Colour.white, GenericPosition.c6 ));
		PositionManager bm = new PositionManager( new Board( pl ), Colour.black );
		classUnderTest = new RandomMoveGenerator( bm, Colour.black );
		expectedMove = new GenericMove( GenericPosition.a8, GenericPosition.b7 );
		performTest(false);
	}
	
	@Test
	public void test_findBestMove_CaptureToEscapeCheck() throws NoLegalMoveException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 .P......
		// 2 PPP.....
		// 1 kP......
		//   abcdefgh
		pl.add(new King( Colour.white, GenericPosition.a1 ));
		pl.add(new Pawn( Colour.black, GenericPosition.b1 ));
		pl.add(new Pawn( Colour.black, GenericPosition.a2 ));
		pl.add(new Pawn( Colour.black, GenericPosition.b2 ));
		pl.add(new Pawn( Colour.black, GenericPosition.c2 ));
		pl.add(new Pawn( Colour.black, GenericPosition.b3 ));
		// pawn at b2 can be captured to escape check
		PositionManager bm = new PositionManager( new Board( pl ), Colour.white );
		classUnderTest = new RandomMoveGenerator( bm, Colour.white );
		expectedMove = new GenericMove( GenericPosition.a1, GenericPosition.b2 );
		performTest(true);			
	}
	
	@Test
	public void test_findBestMove_MoveToEscapeCheck() throws NoLegalMoveException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 .PP.....
		// 2 .P......
		// 1 k.......
		//   abcdefgh
		pl.add(new King( Colour.white, GenericPosition.a1 ));
		pl.add(new Pawn( Colour.black, GenericPosition.b2 ));
		pl.add(new Pawn( Colour.black, GenericPosition.b3 ));
		pl.add(new Pawn( Colour.black, GenericPosition.c3 ));
		// king can move out of check to b1
		PositionManager bm = new PositionManager( new Board( pl ), Colour.white );
		classUnderTest = new RandomMoveGenerator( bm, Colour.white );
		expectedMove = new GenericMove( GenericPosition.a1, GenericPosition.b1 );
		performTest(true);
	}
	
	@Test(expected=NoLegalMoveException.class)
	public void test_findBestMove_NoLegalMove() throws NoLegalMoveException, InvalidPieceException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 .PP.....
		// 2 PPP.....
		// 1 kP......
		//   abcdefgh
		pl.add(new King( Colour.white, GenericPosition.a1 ));
		pl.add(new Pawn( Colour.black, GenericPosition.b1 ));
		pl.add(new Pawn( Colour.black, GenericPosition.a2 ));
		pl.add(new Pawn( Colour.black, GenericPosition.b2 ));
		pl.add(new Pawn( Colour.black, GenericPosition.c2 ));
		pl.add(new Pawn( Colour.black, GenericPosition.b3 ));
		pl.add(new Pawn( Colour.black, GenericPosition.c3 ));
		PositionManager bm = new PositionManager( new Board( pl ), Colour.white );
		classUnderTest = new RandomMoveGenerator( bm, Colour.white );
		classUnderTest.findMove();
	}
	
	@Test
	public void test_findBestMove_ArenaFailKingMove() throws NoLegalMoveException, InvalidPieceException {
		// 8 ..b.q...
		// 7 ......K.
		// 6 ..q.....
		// 5 p.....b.
		// 4 ....p...
		// 3 .p...npn
		// 2 ........
		// 1 ..kr...r
		//   abcdefgh
		pl.add(new King( Colour.black, GenericPosition.g7 ));
		pl.add(new King( Colour.white, GenericPosition.c1 ));
		pl.add(new Rook( Colour.white, GenericPosition.d1 ));
		pl.add(new Rook( Colour.white, GenericPosition.h1 ));
		pl.add(new Pawn( Colour.white, GenericPosition.b3 ));
		pl.add(new Knight( Colour.white, GenericPosition.f3 ));
		pl.add(new Knight( Colour.white, GenericPosition.h3 ));
		pl.add(new Pawn( Colour.white, GenericPosition.g3 ));
		pl.add(new Pawn( Colour.white, GenericPosition.e4 ));
		pl.add(new Pawn( Colour.white, GenericPosition.a5 ));
		pl.add(new Queen( Colour.white, GenericPosition.c6 ));
		pl.add(new Queen( Colour.white, GenericPosition.e8 ));
		pl.add(new Bishop( Colour.white, GenericPosition.c8 ));
		pl.add(new Bishop( Colour.white, GenericPosition.g5 ));		
		PositionManager bm = new PositionManager( new Board( pl ), Colour.black );
		classUnderTest = new RandomMoveGenerator( bm, Colour.black );
		classUnderTest.findMove();
		expectedMove = new GenericMove( GenericPosition.g7, GenericPosition.h7 );
		performTest(true);
	}	
}
