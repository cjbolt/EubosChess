package eubos.board;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericChessman;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericPosition;

import eubos.pieces.Bishop;
import eubos.pieces.King;
import eubos.pieces.Knight;
import eubos.pieces.Pawn;
import eubos.pieces.Piece;
import eubos.pieces.Queen;
import eubos.pieces.Rook;
import eubos.pieces.Piece.Colour;

public class MiniMaxMoveGeneratorTest {
	protected LinkedList<Piece> pl;
	protected MiniMaxMoveGenerator classUnderTest;
	protected GenericMove expectedMove;
	
	@Before
	public void setUp() {
		pl = new LinkedList<Piece>();
	}
	
	private void doFindMoveTest( boolean expectMove ) {
		try {
			GenericMove selectedMove = classUnderTest.findMove();
			if ( expectMove )
				assertTrue(selectedMove.equals(expectedMove));
			else
				assertFalse(selectedMove.equals(expectedMove));
		}
		catch ( NoLegalMoveException e ) {
			assert( false );
		}
	}

	@Test
	public void test_findMove_WhitePawnCapture() {
		// 8 ........
		// 7 ........
		// 6 ...P..P.
		// 5 ..p.....
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ........
		//   abcdefgh
		pl.add(new Pawn( Colour.black, GenericPosition.d6 ));
		pl.add(new Pawn( Colour.white, GenericPosition.c5 ));
		pl.add(new Pawn( Colour.black, GenericPosition.g6 ));
		BoardManager bm = new BoardManager( new Board( pl ));
		classUnderTest = new MiniMaxMoveGenerator( bm, Colour.white );
		expectedMove = new GenericMove( GenericPosition.c5, GenericPosition.d6 );
		doFindMoveTest(true);
	}	
	
	@Test
	public void test_findMove_BlackPawnCapture() {
		// 8 ........
		// 7 ...P....
		// 6 ..p.....
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 .....p..
		// 1 ........
		//   abcdefgh
		pl.add(new Pawn( Colour.black, GenericPosition.d7 ));
		pl.add(new Pawn( Colour.white, GenericPosition.c6 ));
		pl.add(new Pawn( Colour.white, GenericPosition.f2 ));
		BoardManager bm = new BoardManager( new Board( pl ));
		classUnderTest = new MiniMaxMoveGenerator( bm, Colour.black );
		expectedMove = new GenericMove( GenericPosition.d7, GenericPosition.c6 );
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_CaptureToEscapeCheck() throws NoLegalMoveException {
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
		BoardManager bm = new BoardManager( new Board( pl ));
		classUnderTest = new MiniMaxMoveGenerator( bm, Colour.white );
		expectedMove = new GenericMove( GenericPosition.a1, GenericPosition.b2 );
		doFindMoveTest(true);			
	}
	
	@Test
	public void test_findMove_MoveToEscapeCheck() throws NoLegalMoveException {
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
		BoardManager bm = new BoardManager( new Board( pl ));
		classUnderTest = new MiniMaxMoveGenerator( bm, Colour.white );
		expectedMove = new GenericMove( GenericPosition.a1, GenericPosition.b1 );
		doFindMoveTest(true);
	}
	
	@Test(expected=NoLegalMoveException.class)
	public void test_findMove_NoLegalMove() throws NoLegalMoveException {
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
		BoardManager bm = new BoardManager( new Board( pl ));
		classUnderTest = new MiniMaxMoveGenerator( bm, Colour.white );
		classUnderTest.findMove();
	}
	
	@Test
	public void test_findMove_ArenaFailKingMove() throws NoLegalMoveException {
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
		BoardManager bm = new BoardManager( new Board( pl ));
		classUnderTest = new MiniMaxMoveGenerator( bm, Colour.black );
		classUnderTest.findMove();
		expectedMove = new GenericMove( GenericPosition.g7, GenericPosition.h7 );
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_ArenaFailKingInCheck() throws NoLegalMoveException {
		// 8 ...NKBNR
		// 7 ...P.PPP
		// 6 ........
		// 5 .b.P....
		// 4 r..n....
		// 3 ........
		// 2 ......pp
		// 1 ....r.k.
		//   abcdefgh
		pl.add(new King( Colour.black, GenericPosition.e8 ));
		pl.add(new Bishop( Colour.black, GenericPosition.f8 ));
		pl.add(new Knight( Colour.black, GenericPosition.g8 ));
		pl.add(new Knight( Colour.black, GenericPosition.d8 ));
		pl.add(new Rook( Colour.black, GenericPosition.h8 ));
		pl.add(new Pawn( Colour.black, GenericPosition.d7 ));
		pl.add(new Pawn( Colour.black, GenericPosition.f7 ));
		pl.add(new Pawn( Colour.black, GenericPosition.g7 ));
		pl.add(new Pawn( Colour.black, GenericPosition.h7 ));
		pl.add(new Pawn( Colour.black, GenericPosition.d5 ));
		pl.add(new King( Colour.white, GenericPosition.g1 ));
		pl.add(new Bishop( Colour.white, GenericPosition.b5 ));
		pl.add(new Knight( Colour.white, GenericPosition.d4 ));
		pl.add(new Rook( Colour.white, GenericPosition.e1 ));
		pl.add(new Rook( Colour.white, GenericPosition.a4 ));
		pl.add(new Pawn( Colour.white, GenericPosition.h2 ));
		pl.add(new Pawn( Colour.white, GenericPosition.g2 ));
		BoardManager bm = new BoardManager( new Board( pl ));
		classUnderTest = new MiniMaxMoveGenerator( bm, Colour.black );
		classUnderTest.findMove();
		expectedMove = new GenericMove( GenericPosition.f8, GenericPosition.e7 );
		doFindMoveTest(true);
	}	
	
	@Test
	public void test_findMove_ChooseHighestValueCapture() throws NoLegalMoveException {
		// 8 ........
		// 7 .....Q..
		// 6 ...Pp...
		// 5 ..p.....
		// 4 .B......
		// 3 p.......
		// 2 ........
		// 1 ........
		//   abcdefgh
		pl.add(new Pawn( Colour.white, GenericPosition.a3 ));
		pl.add(new Pawn( Colour.white, GenericPosition.c5 ));
		pl.add(new Pawn( Colour.white, GenericPosition.e6 ));
		pl.add(new Bishop( Colour.black, GenericPosition.b4 ));
		pl.add(new Pawn( Colour.black, GenericPosition.d6 ));
		pl.add(new Queen( Colour.black, GenericPosition.f7 ));
		BoardManager bm = new BoardManager( new Board( pl ));
		classUnderTest = new MiniMaxMoveGenerator( bm, Colour.white );
		expectedMove = new GenericMove( GenericPosition.e6, GenericPosition.f7 );
		doFindMoveTest(true);
	}
	
	@Test
	//@Ignore
	public void test_findMove_ChooseHighestValueCaptureAndPromotion() throws NoLegalMoveException {
		// 8 .....Q..
		// 7 ....p...
		// 6 ...P....
		// 5 ..p.....
		// 4 .B......
		// 3 p.......
		// 2 ........
		// 1 ........
		//   abcdefgh
		pl.add(new Pawn( Colour.white, GenericPosition.a3 ));
		pl.add(new Pawn( Colour.white, GenericPosition.c5 ));
		pl.add(new Pawn( Colour.white, GenericPosition.e7 ));
		pl.add(new Bishop( Colour.black, GenericPosition.b4 ));
		pl.add(new Pawn( Colour.black, GenericPosition.d6 ));
		pl.add(new Queen( Colour.black, GenericPosition.f8 ));
		BoardManager bm = new BoardManager( new Board( pl ));
		classUnderTest = new MiniMaxMoveGenerator( bm, Colour.white );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.f8, GenericChessman.QUEEN );
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_pawnPromotion()  throws NoLegalMoveException {
		// 8 ........
		// 7 ....p...
		// 6 ...P....
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ........
		//   abcdefgh
		pl.add(new Pawn( Colour.white, GenericPosition.e7 ));
		pl.add(new Pawn( Colour.black, GenericPosition.d6 ));
		BoardManager bm = new BoardManager( new Board( pl ));
		classUnderTest = new MiniMaxMoveGenerator( bm, Colour.white );
		expectedMove = new GenericMove( GenericPosition.e7, GenericPosition.e8, GenericChessman.QUEEN );
		doFindMoveTest(true);
	}
}
