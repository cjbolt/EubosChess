package eubos.search;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.BoardManager;
import eubos.board.InvalidPieceException;
import eubos.pieces.Piece;
import eubos.search.MiniMaxMoveGenerator;
import eubos.search.NoLegalMoveException;

public class MiniMaxMoveGeneratorTest {
	
	private static final int SEARCH_DEPTH_IN_PLY = 4;
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
		} catch ( NoLegalMoveException e ) {
			fail();
		} catch ( InvalidPieceException e ) {
			fail();
		}
	}

	@Test
	public void test_findMove_WhitePawnCapture() throws IllegalNotationException {
		// 8 ........
		// 7 ........
		// 6 ...P..P.
		// 5 ..p.....
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ........
		//   abcdefgh
		BoardManager bm = new BoardManager( "8/8/3p2p1/2P5/8/8/8/8 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( bm, SEARCH_DEPTH_IN_PLY );
		expectedMove = new GenericMove("c5d6");
		doFindMoveTest(true);
	}	
	
	@Test
	public void test_findMove_BlackPawnCapture() throws IllegalNotationException {
		// 8 ........
		// 7 ...P....
		// 6 ..p.....
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 .....p..
		// 1 ........
		//   abcdefgh
		BoardManager bm = new BoardManager( "8/3p4/2P5/8/8/8/5P2/8 b - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( bm,SEARCH_DEPTH_IN_PLY );
		expectedMove = new GenericMove("d7c6");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_CaptureToEscapeCheck() throws NoLegalMoveException, IllegalNotationException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 .P......
		// 2 PPP.....
		// 1 kP......
		//   abcdefgh
		BoardManager bm = new BoardManager( "8/8/8/8/8/1p6/ppp5/Kp6 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( bm,SEARCH_DEPTH_IN_PLY );
		expectedMove = new GenericMove("a1b2");
		doFindMoveTest(true);			
	}
	
	@Test
	public void test_findMove_MoveToEscapeCheck() throws NoLegalMoveException, IllegalNotationException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 .PP.....
		// 2 .P......
		// 1 k.......
		//   abcdefgh
		BoardManager bm = new BoardManager( "8/8/8/8/8/1pp5/1p6/K7 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( bm,SEARCH_DEPTH_IN_PLY );
		expectedMove = new GenericMove("a1b1");
		doFindMoveTest(true);
	}
	
	@Test(expected=NoLegalMoveException.class)
	public void test_findMove_NoLegalMove() throws NoLegalMoveException, InvalidPieceException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 .PP.....
		// 2 PPP.....
		// 1 kP......
		//   abcdefgh
		BoardManager bm = new BoardManager( "8/8/8/8/8/1pp5/ppp5/Kp6 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( bm,SEARCH_DEPTH_IN_PLY );
		classUnderTest.findMove();
	}
	
	@Test
	public void test_findMove_ArenaFailKingMove() throws NoLegalMoveException, IllegalNotationException {
		// 8 ..b.q...
		// 7 ......K.
		// 6 ..q.....
		// 5 p.....b.
		// 4 ....p...
		// 3 .p...npn
		// 2 ........
		// 1 ..kr...r
		//   abcdefgh
		BoardManager bm = new BoardManager( "2B1Q3/6k1/2Q5/P5B1/4P3/1P3NPN/8/2KR3R b - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( bm,SEARCH_DEPTH_IN_PLY );
		expectedMove = new GenericMove("g7h7");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_ArenaFailKingInCheck() throws NoLegalMoveException, IllegalNotationException {
		// 8 ...NKBNR
		// 7 ...P.PPP
		// 6 ........
		// 5 .b.P....
		// 4 r..n....
		// 3 ........
		// 2 ......pp
		// 1 ....r.k.
		//   abcdefgh
		BoardManager bm = new BoardManager( "3nkbnr/3p1ppp/8/1B1p4/R2N4/8/6PP/4R1K1 b - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( bm,SEARCH_DEPTH_IN_PLY );
		expectedMove = new GenericMove("g8e7");
		doFindMoveTest(true);
	}	
	
	@Test
	public void test_findMove_ChooseHighestValueCapture() throws NoLegalMoveException, IllegalNotationException {
		// 8 ........
		// 7 .....Q..
		// 6 ...Pp...
		// 5 ..p.....
		// 4 .B......
		// 3 p.......
		// 2 ........
		// 1 ........
		//   abcdefgh
		BoardManager bm = new BoardManager( "8/5q2/3pP3/2P5/1b6/P7/8/8 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( bm,SEARCH_DEPTH_IN_PLY );
		expectedMove = new GenericMove("e6f7");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_ChooseHighestValueCaptureAndPromotion() throws NoLegalMoveException, IllegalNotationException {
		// 8 .....Q..
		// 7 ....p...
		// 6 ...P....
		// 5 ..p.....
		// 4 .B......
		// 3 p.......
		// 2 ........
		// 1 ........
		//   abcdefgh
		BoardManager bm = new BoardManager( "5q2/4P3/3p4/2P5/1b6/P7/8/8 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( bm,SEARCH_DEPTH_IN_PLY );
		expectedMove = new GenericMove("e7f8Q");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_pawnPromotion()  throws NoLegalMoveException, IllegalNotationException {
		// 8 ........
		// 7 ....p...
		// 6 ...P....
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ........
		//   abcdefgh
		BoardManager bm = new BoardManager( "8/4P3/3p4/8/8/8/8/8 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( bm,SEARCH_DEPTH_IN_PLY );
		expectedMove = new GenericMove("e7e8Q");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_pinnedPawn1()  throws NoLegalMoveException, IllegalNotationException {
		// 8 ....K...
		// 7 ........
		// 6 ....P...
		// 5 .....b..
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ....r...
		//   abcdefgh
		BoardManager bm = new BoardManager( "4k3/8/4p3/5b2/8/8/8/4R3 b - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( bm,SEARCH_DEPTH_IN_PLY );
		expectedMove = new GenericMove("e6f5");
		doFindMoveTest(false);
	}

	@Test
	public void test_findMove_pinnedPawn2()  throws NoLegalMoveException, IllegalNotationException {
		// Observed to produce an illegal move exception in Arena testing, 29th March 2015.
		// 8 .NBQK..R
		// 7 ...P...P
		// 6 R.PBPN..
		// 5 .P...b..
		// 4 ...p....
		// 3 pqp..n..
		// 2 .....ppp
		// 1 r...r.k.
		//   abcdefgh
		BoardManager bm = new BoardManager( "1nbqk2r/3p3p/r1pbpn2/1p3B2/3P4/PQP2N2/5PPP/R3R1K1 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator(bm,2);
		expectedMove = new GenericMove("e6f5");
		doFindMoveTest(false);
	}

	@Test
	public void test_findMove_mateInOne1()  throws NoLegalMoveException, IllegalNotationException {
		// chess.com Problem ID: 0160818
		BoardManager bm = new BoardManager( "5r1k/p2R4/1pp2p1p/8/5q2/3Q1bN1/PP3P2/6K1 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator(bm,2);
		expectedMove = new GenericMove("d3h7");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_mateInOne2()  throws NoLegalMoveException, IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		BoardManager bm = new BoardManager( "2N5/4R3/2k3KQ/R7/1PB5/5N2/8/6B1 w - - 0 1" );
		classUnderTest = new MiniMaxMoveGenerator(bm,2);
		// various possible mates
		//expectedMove = new GenericMove("a5a6");
		expectedMove = new GenericMove("f3e5");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_mateInOne3()  throws NoLegalMoveException, IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		BoardManager bm = new BoardManager( "4N3/5P1P/5N1k/Q5p1/5PKP/B7/8/1B6 w - - 0 1" );
		classUnderTest = new MiniMaxMoveGenerator(bm,2);
		// various possible mates
		//expectedMove = new GenericMove("h4g5");
		expectedMove = new GenericMove("a5g5");
		//expectedMove = new GenericMove("a3f8");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_mateInOne4()  throws NoLegalMoveException, IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		BoardManager bm = new BoardManager( "8/4N3/7Q/4k3/8/4KP2/3P4/8 w - - 0 1" );
		classUnderTest = new MiniMaxMoveGenerator(bm,2);
		// Two possible pawn mates
		expectedMove = new GenericMove("d2d4");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_mateInOne5()  throws NoLegalMoveException, IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		BoardManager bm = new BoardManager( "8/8/K7/p7/k2N3R/p7/P7/8 w - - 0 1" );
		classUnderTest = new MiniMaxMoveGenerator(bm,2);
		expectedMove = new GenericMove("d4e6");
		doFindMoveTest(true);
	}

	@Test
	public void test_findMove_mateInOne6()  throws NoLegalMoveException, IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		BoardManager bm = new BoardManager( "1rk2N2/1p6/8/B1Pp4/B6Q/K7/8/2R5 w - d6 0 1" );
		classUnderTest = new MiniMaxMoveGenerator(bm,2);
		expectedMove = new GenericMove("c5d6"); //en passant move causes discovered checkmate
		doFindMoveTest(true);
	}

	@Test
	public void test_findMove_mateInOne7()  throws NoLegalMoveException, IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		BoardManager bm = new BoardManager( "8/7B/8/3N4/8/1Q2B3/PPP5/rk2K2R w K - 0 1" );
		classUnderTest = new MiniMaxMoveGenerator(bm,2);
		//expectedMove = new GenericMove("c2c3");
		expectedMove = new GenericMove("e1e2");
		doFindMoveTest(true);
	}

	@Test
	public void test_findMove_mateInTwo1()  throws NoLegalMoveException, IllegalNotationException {
		// chess.com Problem ID: 0022190
		BoardManager bm = new BoardManager( "k1K5/b7/R7/1P6/1n6/8/8/8 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( bm,SEARCH_DEPTH_IN_PLY );
		expectedMove = new GenericMove("b5b6");
		doFindMoveTest(true);
	}

	@Test
	public void test_findMove_mateInTwo2()  throws NoLegalMoveException, IllegalNotationException {
		// chess.com Problem ID: 0102832
		BoardManager bm = new BoardManager( "r1r3k1/pb1p1p2/1p2p1p1/2pPP1B1/1nP4Q/1Pq2NP1/P4PBP/b2R2K1 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( bm,SEARCH_DEPTH_IN_PLY );
		expectedMove = new GenericMove("g5f6");
		doFindMoveTest(true);
	}

	@Test
	public void test_findMove_mateInTwo3()  throws NoLegalMoveException, IllegalNotationException {
		// chess.com Problem ID: 0551140
		BoardManager bm = new BoardManager( "rnbq1rk1/p4ppN/4p2n/1pbp4/8/2PQP2P/PPB2PP1/RNB1K2R w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( bm,SEARCH_DEPTH_IN_PLY );
		expectedMove = new GenericMove("h7f6");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_ArenaFailIllegalMove() throws InvalidPieceException, IllegalNotationException {
		// Observed in arena, black tries to moves as white: 6th April 2015.
		// N.b. this phenomenon was caused by a combination of the castle move
		// "secondary rook move" missing implementation bug and the fact that an
		// invalid piece exception was not previously implemented.
		BoardManager bm = new BoardManager( "2b1k1nr/2p2ppp/2p5/p3q3/P3Q3/P4P2/2P1B1PP/1r3R1K w k - 2 23" );
		bm.performMove(new GenericMove("f1b1"));
		classUnderTest = new MiniMaxMoveGenerator(bm,2);
		expectedMove = new GenericMove("e5e4");
		//expectedMove = new GenericMove("e5e6");
		doFindMoveTest(true);
		
	}
	
	@Test
	// Currently fails because the status of the initial enPassant Target square is not backed up by
	// the move generator (because the move was never executed as far as Eubos is concerned).
	public void test_findMove_enPassantCapture() throws InvalidPieceException, IllegalNotationException {
		BoardManager bm = new BoardManager( "8/8/8/8/1pPP4/8/8/8 b - c3 0 1");
		classUnderTest = new MiniMaxMoveGenerator(bm,2);
		expectedMove = new GenericMove("b4c3");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_mateInOne8() throws InvalidPieceException, IllegalNotationException {
		BoardManager bm = new BoardManager( "1k6/ppR5/8/8/8/8/PP6/K1Qq2r1 w - - - -");
		classUnderTest = new MiniMaxMoveGenerator(bm,2);
		expectedMove = new GenericMove("c7c8");
		doFindMoveTest(true);
	}	
}
