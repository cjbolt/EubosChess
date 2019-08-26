package eubos.search;

import static org.junit.Assert.*;
import org.junit.Ignore;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.board.pieces.Piece;
import eubos.position.PositionManager;
import eubos.search.MiniMaxMoveGenerator;
import eubos.search.NoLegalMoveException;
import eubos.search.PlySearcher;

public class MiniMaxMoveGeneratorTest {
	
	private static final byte SEARCH_DEPTH_IN_PLY = 4;
	protected LinkedList<Piece> pl;
	protected MiniMaxMoveGenerator classUnderTest;
	protected GenericMove expectedMove;
	protected FixedSizeTranspositionTable hashMap;
	
	@Before
	public void setUp() {
		SearchDebugAgent.open();
		pl = new LinkedList<Piece>();
		hashMap = new FixedSizeTranspositionTable();
	}
	
	@After
	public void tearDown() {
		SearchDebugAgent.close();
	}
	
	private void doFindMoveTest( boolean expectMove ) {
		doFindMoveTest( SEARCH_DEPTH_IN_PLY, expectMove );
	}
	
	private void doFindMoveTest( byte searchDepth, boolean expectMove ) {
		try {
			SearchResult res = classUnderTest.findMove(searchDepth);
			if ( expectMove )
				assertEquals(expectedMove, res.bestMove);
			else
				assertFalse(res.bestMove.equals(expectedMove));
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
		PositionManager pm = new PositionManager( "8/8/3p2p1/2P5/8/8/8/8 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( hashMap, pm,pm,pm );
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
		PositionManager pm = new PositionManager( "8/3p4/2P5/8/8/8/5P2/8 b - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( hashMap, pm,pm,pm );
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
		PositionManager pm = new PositionManager( "8/8/8/8/8/1p6/ppp5/Kp6 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( hashMap, pm,pm,pm );
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
		PositionManager pm = new PositionManager( "8/8/8/8/8/1pp5/1p6/K7 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( hashMap, pm,pm,pm );
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
		PositionManager pm = new PositionManager( "8/8/8/8/8/1pp5/ppp5/Kp6 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( hashMap, pm,pm,pm );
		classUnderTest.findMove(SEARCH_DEPTH_IN_PLY);
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
		PositionManager pm = new PositionManager( "2B1Q3/6k1/2Q5/P5B1/4P3/1P3NPN/8/2KR3R b - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( hashMap, pm,pm,pm );
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
		PositionManager pm = new PositionManager( "3nkbnr/3p1ppp/8/1B1p4/R2N4/8/6PP/4R1K1 b - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( hashMap, pm,pm,pm );
		if (PlySearcher.ENABLE_SEARCH_EXTENSION_FOR_RECAPTURES)
			expectedMove = new GenericMove("f8e7");
		else
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
		PositionManager pm = new PositionManager( "8/5q2/3pP3/2P5/1b6/P7/8/8 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( hashMap, pm,pm,pm );
		expectedMove = new GenericMove("e6f7");
		doFindMoveTest((byte)2, true);
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
		PositionManager pm = new PositionManager( "5q2/4P3/3p4/2P5/1b6/P7/8/8 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( hashMap, pm,pm,pm );
		expectedMove = new GenericMove("e7f8Q");
		doFindMoveTest((byte)2, true);
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
		PositionManager pm = new PositionManager( "8/4P3/3p4/8/8/8/8/8 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( hashMap, pm,pm,pm );
		expectedMove = new GenericMove("e7e8Q");
		doFindMoveTest((byte)2, true);
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
		PositionManager pm = new PositionManager( "4k3/8/4p3/5b2/8/8/8/4R3 b - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( hashMap, pm,pm,pm );
		expectedMove = new GenericMove("e6f5");
		doFindMoveTest((byte)2, false);
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
		PositionManager pm = new PositionManager( "1nbqk2r/3p3p/r1pbpn2/1p3B2/3P4/PQP2N2/5PPP/R3R1K1 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("e6f5");
		doFindMoveTest((byte)2, false);
	}

	@Test
	public void test_findMove_mateInOne1()  throws NoLegalMoveException, IllegalNotationException {
		// chess.com Problem ID: 0160818
		PositionManager pm = new PositionManager( "5r1k/p2R4/1pp2p1p/8/5q2/3Q1bN1/PP3P2/6K1 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("d3h7");
		doFindMoveTest((byte)2, true);
	}
	
	@Test
	public void test_findMove_mateInOne2()  throws NoLegalMoveException, IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		PositionManager pm = new PositionManager( "2N5/4R3/2k3KQ/R7/1PB5/5N2/8/6B1 w - - 0 1" );
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		// various possible mates
		//expectedMove = new GenericMove("a5a6");
		//expectedMove = new GenericMove("f3e5");
		expectedMove = new GenericMove("g6f7");
		doFindMoveTest((byte)2, true);
	}
	
	@Test
	public void test_findMove_mateInOne3()  throws NoLegalMoveException, IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		PositionManager pm = new PositionManager( "4N3/5P1P/5N1k/Q5p1/5PKP/B7/8/1B6 w - - 0 1" );
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		// various possible mates
		//expectedMove = new GenericMove("h4g5");
		expectedMove = new GenericMove("a5g5");
		//expectedMove = new GenericMove("a3f8");
		doFindMoveTest((byte)2, true);
	}
	
	@Test
	public void test_findMove_mateInOne4()  throws NoLegalMoveException, IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		PositionManager pm = new PositionManager( "8/4N3/7Q/4k3/8/4KP2/3P4/8 w - - 0 1" );
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		// Two possible pawn mates
		//expectedMove = new GenericMove("d2d4");
		expectedMove = new GenericMove("f3f4");
		doFindMoveTest((byte)2, true);
	}
	
	@Test
	public void test_findMove_mateInOne5()  throws NoLegalMoveException, IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		PositionManager pm = new PositionManager( "8/8/K7/p7/k2N3R/p7/P7/8 w - - 0 1" );
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("d4b3");
		doFindMoveTest((byte)2, true);
	}

	@Test
	public void test_findMove_mateInOne6()  throws NoLegalMoveException, IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		PositionManager pm = new PositionManager( "1rk2N2/1p6/8/B1Pp4/B6Q/K7/8/2R5 w - d6 0 1" );
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("c5d6"); //en passant move causes discovered checkmate
		doFindMoveTest((byte)2, true);
	}

	@Test
	public void test_findMove_mateInOne7()  throws NoLegalMoveException, IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		PositionManager pm = new PositionManager( "8/7B/8/3N4/8/1Q2B3/PPP5/rk2K2R w K - 0 1" );
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("e1g1");
		doFindMoveTest((byte)2, true);
	}

	@Test
	public void test_findMove_mateInTwo1()  throws NoLegalMoveException, IllegalNotationException {
		// chess.com Problem ID: 0022190
		PositionManager pm = new PositionManager( "k1K5/b7/R7/1P6/1n6/8/8/8 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( hashMap, pm,pm,pm );
		expectedMove = new GenericMove("b5b6");
		doFindMoveTest(true);
	}

	@Test
	public void test_findMove_mateInThree2()  throws NoLegalMoveException, IllegalNotationException, InvalidPieceException {
		// chess.com Problem ID: 0102832
		// Actually it is mate in 3, but can win queen at 4ply search.
		PositionManager pm = new PositionManager( "r1r3k1/pb1p1p2/1p2p1p1/2pPP1B1/1nP4Q/1Pq2NP1/P4PBP/b2R2K1 w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator( hashMap, pm,pm,pm );
		expectedMove = new GenericMove("g5f6");
		doFindMoveTest(true);
	}

	@Test
	public void test_findMove_mateInTwo3()  throws NoLegalMoveException, IllegalNotationException {
		// chess.com Problem ID: 0551140
		PositionManager pm = new PositionManager( "rnbq1rk1/p4ppN/4p2n/1pbp4/8/2PQP2P/PPB2PP1/RNB1K2R w - - - -" );
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm );
		expectedMove = new GenericMove("h7f6");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_ArenaFailIllegalMove() throws InvalidPieceException, IllegalNotationException {
		// Observed in arena, black tries to moves as white: 6th April 2015.
		// N.b. this phenomenon was caused by a combination of the castle move
		// "secondary rook move" missing implementation bug and the fact that an
		// invalid piece exception was not previously implemented.
		PositionManager pm = new PositionManager( "2b1k1nr/2p2ppp/2p5/p3q3/P3Q3/P4P2/2P1B1PP/1r3R1K w k - 2 23" );
		pm.performMove(new GenericMove("f1b1"));
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("e5e4");
		//expectedMove = new GenericMove("e5e6");
		doFindMoveTest(true);
		
	}
	
	@Test
	public void test_findMove_enPassantCapture() throws InvalidPieceException, IllegalNotationException {
		PositionManager pm = new PositionManager( "8/8/8/8/1pPP4/8/8/8 b - c3 0 1");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("b4c3");
		doFindMoveTest((byte)2, true);
	}
	
	@Test
	public void test_findMove_mateInOne8() throws InvalidPieceException, IllegalNotationException {
		PositionManager pm = new PositionManager( "1k6/ppR5/8/8/8/8/PP6/K1Qq2r1 w - - - -");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("c7c8");
		doFindMoveTest((byte)2, true);
	}
	
	@Test
	public void test_findMove_mateInOne9() throws InvalidPieceException, IllegalNotationException {
		// From a game against Eubos, 26th feb 2016 - pc reported by eubos was
		// FEN: 5r1k/ppp4p/2n5/1BNb2q1/1P6/P7/2PP4/1R1Q3K w - - 6 32
	    // EubosRunnableTest:
		// 6	00:06	 2,917k	486k	-12.00	Qf3 Bxf3+ Kh2 Qe5+ Kh3 Qxc5
		// 6	00:09	 3,664k	385k	-3.00	Kh2 Ba2 Rc1 Qe5+ Kh3 Qxc5
		PositionManager pm = new PositionManager( "5r1k/ppp4p/2n5/1BNb2q1/1P6/P7/2PP3K/1R1Q4 b - - 7 32");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("g5g2");
		doFindMoveTest((byte)2, true);
	}
	
	@Test
	public void test_findMove_mateInThree_WhiteMated() throws InvalidPieceException, IllegalNotationException {
		// From a game against Eubos, 26th feb 2016 - pc reported by eubos was rubbish!
		PositionManager pm = new PositionManager( "5r1k/ppp4p/2n5/1B1b2q1/1P6/P7/2PP4/1R1Q3K w - - 6 32");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("d1f3");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_mateInThree_BlackMated() throws InvalidPieceException, IllegalNotationException {
		// An exact negation of the position in test_findMove_mateInThree_WhiteMated().
		PositionManager pm = new PositionManager( "1r1q3k/2pp4/p7/1p6/1b1B2Q1/2N5/PPP4P/5R1K b - - 6 1");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("d8f6");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_crashLichess_CloudFunction() throws InvalidPieceException, IllegalNotationException {
		/*  [Site "https://lichess.org/fBSEDCCo"]
			[Date "2018.06.29"]
			[Round "-"]
			[White "eubos"]
			[Black "CloudFunction"]
			[Result "0-1"]
			[UTCDate "2018.06.29"]
			[UTCTime "19:28:24"] */
		PositionManager pm = new PositionManager( "4r1k1/p2b1ppp/1q6/2Kp4/8/R2BP3/3n3P/3N2NR w - - 6 36");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("c5d5");
		doFindMoveTest(true);
	}
		
	@Test
	public void test_findMove_bugPromotingPawn_Arena_4ply() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		PositionManager pm = new PositionManager( "7K/7P/8/6Q1/3k4/8/8/8 w - - 1 69");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("h8g7");
		
		SearchResult res = classUnderTest.findMove((byte)4);
		
		assertEquals(expectedMove, res.bestMove);
	}
		
	@Test
	public void test_findMove_bugPromotingPawn_Arena_5ply() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		// In this test, Eubos originally couldn't find the move to promote the 2nd pawn and just checked indefinitely with queen.
		// It can do it with depth = 3, but not 5>=depth<10 (Because move order is not considered in depth first mini max algorithm).
		// The solution is to do an iterative search, deepening and seeding each time.
		PositionManager pm = new PositionManager( "7K/7P/8/6Q1/3k4/8/8/8 w - - 1 69");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("h8g7");
		
		classUnderTest.findMove((byte)4);
		List<GenericMove >lastPc = classUnderTest.pc.toPvList();
		SearchResult res = classUnderTest.findMove((byte)5, lastPc);
		
		assertEquals(expectedMove, res.bestMove);
	}
	
	@Test
	public void test_findMove_bugPromotingPawn_Arena_6ply() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		// N.b. as per test_findMove_bugPromotingPawn_Arena_5ply
		PositionManager pm = new PositionManager( "7K/7P/8/6Q1/3k4/8/8/8 w - - 1 69");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("h8g7");
		
		classUnderTest.findMove((byte)4);
		List<GenericMove> lastPc = classUnderTest.pc.toPvList();
		classUnderTest.findMove((byte)5,lastPc);
		lastPc = classUnderTest.pc.toPvList();
		SearchResult res = classUnderTest.findMove((byte)6,lastPc);
		
	    assertEquals(expectedMove, res.bestMove);
	}
	
	@Test
	@Ignore
	public void test_findMove_bugPromotingPawn_Arena_10ply() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		// with depth=10 Eubos can find the forced mate.
		PositionManager pm = new PositionManager( "7K/7P/8/6Q1/3k4/8/8/8 w - - 1 69");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("h8g7");
		
		SearchResult res = classUnderTest.findMove((byte)10);
		
		assertEquals(expectedMove, res.bestMove);
	}
	
	@Test
	@Ignore
	public void test_findMove_bugBlunderVsFidelityCC10_7ply() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		PositionManager pm = new PositionManager( "4k3/2pqbppr/8/p2p3p/3B4/3P3P/4QPP1/5K1R w - - 2 30");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		// Blunder move!
		expectedMove = new GenericMove("d4g7");
		
		SearchResult res = classUnderTest.findMove((byte)7);
		
		assertEquals(expectedMove, res.bestMove);
	}
	
	@Test
	@Ignore
	public void test_findMove_bugBlunderVsFidelityCC10_5ply() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		PositionManager pm = new PositionManager( "4k3/2pqbppr/8/p2p3p/3B4/3P3P/4QPP1/5K1R w - - 2 30");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		// Blunder move!
		expectedMove = new GenericMove("d4g7");
		
		SearchResult res = classUnderTest.findMove((byte)5);
		
		assertEquals(expectedMove, res.bestMove);
	}
	
	@Test
	public void test_findMove_NeedToCastle_FromLichess() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		PositionManager pm = new PositionManager( "4k2r/2Q2ppp/8/3r4/1P5P/P1p5/4PP2/R3K1N1 b Qk - - -");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("e8g8");
		
		SearchResult res = classUnderTest.findMove((byte)3);
		
		assertEquals(expectedMove, res.bestMove);
	}
	
	@Test
	public void test_findMove_NeedToCastle_FromLichess1() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		PositionManager pm = new PositionManager( "rnb1kbnr/p4p1p/1qp1p1p1/3p4/8/1B2PN2/PPPP1PPP/RNBQK2R w KQkq - - -");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("e1g1");
		
		SearchResult res = classUnderTest.findMove((byte)3);
		
		assertEquals(expectedMove, res.bestMove);
	}
	
	@Test
	@Ignore
	public void test_PieceIsntARookDefect_WhilstSearching_Move() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		PositionManager pm = new PositionManager("r1bqkbnr/nppppppp/8/p3P3/3P4/2N5/PPP2PPP/R1BQKBNR b KQkq - 4 6");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		pm.performMove(new GenericMove("d7d6"));
		pm.performMove(new GenericMove("g1e2"));
		pm.performMove(new GenericMove("d6e5"));
		pm.performMove(new GenericMove("d4e5"));
		pm.performMove(new GenericMove("d8d1"));
		pm.performMove(new GenericMove("e1d1"));
		pm.performMove(new GenericMove("a7c6"));
		pm.performMove(new GenericMove("c3d5"));
		classUnderTest.findMove((byte)5);
	}
	
	@Test
	@Ignore
	public void test_PieceIsntARookDefect_WhilstSearching_Move1() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		PositionManager pm = new PositionManager("rnbqkbnr/p1p1pppp/8/1p6/2pPP3/8/PP3PPP/RNBQKBNR w KQkq - 0 4");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		pm.performMove(new GenericMove("a2a4"));
		pm.performMove(new GenericMove("e7e5"));
		pm.performMove(new GenericMove("a4b5"));
		pm.performMove(new GenericMove("f8b4"));
		pm.performMove(new GenericMove("b1c3"));
		pm.performMove(new GenericMove("d8d4"));
		pm.performMove(new GenericMove("d1c2"));
		pm.performMove(new GenericMove("d4c5"));
		classUnderTest.findMove((byte)5);
	}
	
	@Test
	@Ignore
	public void test_badMoveSelection() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		PositionManager pm = new PositionManager("r3k2r/pp2npp1/2p5/q3P2p/1N2PB1b/P5P1/1PP1NQ1P/R3K2R b KQkq - 0 16");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		pm.performMove(new GenericMove("c6c5"));
		pm.performMove(new GenericMove("c2c3"));
		pm.performMove(new GenericMove("c5b4"));
		pm.performMove(new GenericMove("c3b4"));
		pm.performMove(new GenericMove("a5b6"));
		pm.performMove(new GenericMove("f4e3"));
		pm.performMove(new GenericMove("b6c7"));
		pm.performMove(new GenericMove("e3f4"));
		// Eubos generates a wtf queen for pawn sacrifice. When run here it is ok.
		expectedMove = new GenericMove("e7c6");
		
		classUnderTest.findMove((byte)1);
		List<GenericMove> lastPc = classUnderTest.pc.toPvList();
		classUnderTest.findMove((byte)2,lastPc);
		lastPc = classUnderTest.pc.toPvList();
		classUnderTest.findMove((byte)3,lastPc);
		lastPc = classUnderTest.pc.toPvList();
		SearchResult res = classUnderTest.findMove((byte)4,lastPc);
		
		assertEquals(expectedMove, res.bestMove);
	}
	
	@Test
	@Ignore
	public void test_badMoveSelectionBishop() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		PositionManager pm = new PositionManager("r1bqk1nr/1pp2ppp/p3p1n1/4P3/1b1B4/P1N2N2/1PP2PPP/R2QKB1R b KQkq - 0 10");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("b4c3");
		
		classUnderTest.findMove((byte)1);
		List<GenericMove> lastPc = classUnderTest.pc.toPvList();
		classUnderTest.findMove((byte)2,lastPc);
		lastPc = classUnderTest.pc.toPvList();
		classUnderTest.findMove((byte)3,lastPc);
		lastPc = classUnderTest.pc.toPvList();
		classUnderTest.findMove((byte)4,lastPc);
		lastPc = classUnderTest.pc.toPvList();
		SearchResult res = classUnderTest.findMove((byte)5,lastPc);
		
		assertEquals(expectedMove, res.bestMove);
	}
	
	@Test
	@Ignore
	public void test_throwAwayBishop5Ply() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		PositionManager pm = new PositionManager("r1bqkb1r/p4ppp/2pp1n2/4p3/2P1PB2/2N5/PP3PPP/R2QKB1R w KQkq - 0 9");
		classUnderTest = new MiniMaxMoveGenerator(hashMap, pm,pm,pm);
		expectedMove = new GenericMove("g4f5");
		
		classUnderTest.findMove((byte)1);
		List<GenericMove> lastPc = classUnderTest.pc.toPvList();
		classUnderTest.findMove((byte)2,lastPc);
		lastPc = classUnderTest.pc.toPvList();
		classUnderTest.findMove((byte)3,lastPc);
		lastPc = classUnderTest.pc.toPvList();
		classUnderTest.findMove((byte)4,lastPc);
		lastPc = classUnderTest.pc.toPvList();
		SearchResult res = classUnderTest.findMove((byte)5,lastPc);
		
		assertEquals(expectedMove, res.bestMove);
	}
	 
}
