package eubos.search.generators;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.logging.Level;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.After;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.position.PositionManager;
import eubos.search.transposition.FixedSizeTranspositionTable;
import eubos.search.transposition.Transposition;
import eubos.search.SearchMetricsReporter;
import eubos.search.SearchResult;

public class MiniMaxMoveGeneratorTest {
	
	private static final byte SEARCH_DEPTH_IN_PLY = 4;
	protected LinkedList<Piece> pl;
	protected MiniMaxMoveGenerator classUnderTest;
	protected GenericMove expectedMove;
	protected FixedSizeTranspositionTable hashMap;
	PositionManager pm;
	SearchMetricsReporter sr_stub;
	
	@Before
	public void setUp() {
		sr_stub = new SearchMetricsReporter();
		EubosEngineMain.logger.setLevel(Level.OFF);
		pl = new LinkedList<Piece>();
		hashMap = new FixedSizeTranspositionTable();
		pm = null;
	}
	
	@After
	public void tearDown() {
		if (classUnderTest != null)
			classUnderTest.sda.close();
	}
	
	private void doFindMoveTest( boolean expectMove ) {
		doFindMoveTest( SEARCH_DEPTH_IN_PLY, expectMove );
	}
	
	private void doFindMoveTest( byte searchDepth, boolean expectMove ) {
		SearchResult res = classUnderTest.findMove(searchDepth);
		if ( expectMove )
			assertEquals(expectedMove, Move.toGenericMove(res.pv[0]));
		else
			assertFalse(Move.toGenericMove(res.pv[0]).equals(expectedMove));
	}

	protected void setupPosition(String fen) {
		pm = new PositionManager( fen );
		classUnderTest = new MiniMaxMoveGenerator( hashMap, pm, pm);
	}	
	
	@Test
	public void test_findMove_BlackPawnCapture() throws IllegalNotationException {
		// 8 .......K
		// 7 ...P....
		// 6 ..p.....
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 .....p..
		// 1 .......k
		//   abcdefgh
		setupPosition( "7K/3p4/2P5/8/8/8/5P2/7k b - - - 1" );
		expectedMove = new GenericMove("d7c6");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_CaptureToEscapeCheck() throws IllegalNotationException {
		// 8 .......k
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 .P......
		// 2 PPP.....
		// 1 kB......
		//   abcdefgh
		setupPosition( "7k/8/8/8/8/1p6/ppp5/Kb6 w - - - 1" );
		expectedMove = new GenericMove("a1b2");
		doFindMoveTest(true);			
	}
	
	@Test
	public void test_findMove_MoveToEscapeCheck() throws IllegalNotationException {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 .PP.....
		// 2 .P......
		// 1 k.......
		//   abcdefgh
		setupPosition( "7k/8/8/8/8/1pp5/1p6/K7 w - - - 1" );
		expectedMove = new GenericMove("a1b1");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_NoLegalMove()  {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 .PP.....
		// 2 PPP.....
		// 1 kP......
		//   abcdefgh
		setupPosition( "8/8/8/8/8/1pp5/ppp5/Kp6 w - - - 1" );
		assertEquals(Move.NULL_MOVE, classUnderTest.findMove(SEARCH_DEPTH_IN_PLY).pv[0]);
	}
	
	@Test
	public void test_findMove_ArenaFailKingMove() throws IllegalNotationException {
		// 8 ..b.q...
		// 7 ......K.
		// 6 ..q.....
		// 5 p.....b.
		// 4 ....p...
		// 3 .p...npn
		// 2 ........
		// 1 ..kr...r
		//   abcdefgh
		setupPosition("2B1Q3/6k1/2Q5/P5B1/4P3/1P3NPN/8/2KR3R b - - - 1" );
		expectedMove = new GenericMove("g7h7");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_ArenaFailKingInCheck()throws IllegalNotationException {
		// 8 ...NKBNR
		// 7 ...P.PPP
		// 6 ........
		// 5 .b.P....
		// 4 r..n....
		// 3 ........
		// 2 ......pp
		// 1 ....r.k.
		//   abcdefgh
		setupPosition( "3nkbnr/3p1ppp/8/1B1p4/R2N4/8/6PP/4R1K1 b - - - 1" );
		SearchResult res = classUnderTest.findMove(SEARCH_DEPTH_IN_PLY);
		pm.performMove(res.pv[0]);
		assertFalse(pm.isKingInCheck());
	}	
	
	@Test
	public void test_findMove_ChooseHighestValueCapture()throws IllegalNotationException {
		// 8 K.......
		// 7 .....Q..
		// 6 ...Pp...
		// 5 ..p.....
		// 4 .B......
		// 3 p.......
		// 2 ........
		// 1 .......k
		//   abcdefgh
		setupPosition( "K7/5q2/3pP3/2P5/1b6/P7/8/7k w - - - 1" );
		expectedMove = new GenericMove("e6f7");
		doFindMoveTest((byte)2, true);
	}
	
	@Test
	public void test_findMove_ChooseHighestValueCaptureAndPromotion()throws IllegalNotationException {
		// 8 K....Q..
		// 7 ....p...
		// 6 ...P....
		// 5 ..p.....
		// 4 .B......
		// 3 p.......
		// 2 ........
		// 1 .......k
		//   abcdefgh
		setupPosition( "K4q2/4P3/3p4/2P5/1b6/P7/8/7k w - - - 1" );
		expectedMove = new GenericMove("e7f8Q");
		doFindMoveTest((byte)2, true);
	}
	
	@Test
	public void test_findMove_pawnPromotion() throws IllegalNotationException {
		// 8 K......k
		// 7 ....P...
		// 6 ...p....
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ........
		//   abcdefgh
		setupPosition( "K6k/4P3/3p4/8/8/8/8/8 w - - - 1" );
		expectedMove = new GenericMove("e7e8Q");
		doFindMoveTest((byte)2, true);
	}
	
	@Test
	public void test_findMove_pinnedPawn1() throws IllegalNotationException {
		// 8 ....K...
		// 7 ........
		// 6 ....P...
		// 5 .....b..
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 k...r...
		//   abcdefgh
		setupPosition( "4k3/8/4p3/5b2/8/8/8/K3R3 b - - - 1" );
		expectedMove = new GenericMove("e6f5");
		doFindMoveTest((byte)2, false);
	}

	@Test
	public void test_findMove_pinnedPawn2() throws IllegalNotationException {
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
		setupPosition( "1nbqk2r/3p3p/r1pbpn2/1p3B2/3P4/PQP2N2/5PPP/R3R1K1 w - - - 1" );
		expectedMove = new GenericMove("e6f5");
		doFindMoveTest((byte)2, false);
	}

	@Test
	public void test_findMove_mateInOne1() throws IllegalNotationException {
		// chess.com Problem ID: 0160818
		setupPosition( "5r1k/p2R4/1pp2p1p/8/5q2/3Q1bN1/PP3P2/6K1 w - - - 1" );
		expectedMove = new GenericMove("d3h7");
		doFindMoveTest((byte)2, true);
	}
	
	@Test
	public void test_findMove_mateInOne2() throws IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		setupPosition( "2N5/4R3/2k3KQ/R7/1PB5/5N2/8/6B1 w - - 0 1" );
		// various possible mates
		expectedMove = new GenericMove("c4d5");
		doFindMoveTest((byte)2, true);
	}
	
	@Test
	public void test_findMove_mateInOne3() throws IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		setupPosition( "4N3/5P1P/5N1k/Q5p1/5PKP/B7/8/1B6 w - - 0 1" );
		// various possible mates
		expectedMove = new GenericMove("f7f8q");
		doFindMoveTest((byte)1, true);
	}
	
	@Test
	public void test_findMove_mateInOne4() throws IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		setupPosition("8/4N3/7Q/4k3/8/4KP2/3P4/8 w - - 0 1" );
		// Two possible pawn mates
		//expectedMove = new GenericMove("f3f4");
		expectedMove = new GenericMove("d2d4");
		doFindMoveTest((byte)2, true);
	}
	
	@Test
	public void test_findMove_mateInOne5() throws IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		setupPosition( "8/8/K7/p7/k2N3R/p7/P7/8 w - - 0 1" );
		expectedMove = new GenericMove("d4e6");
		doFindMoveTest((byte)2, true);
	}

	@Test
	public void test_findMove_mateInOne6() throws IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		setupPosition( "1rk2N2/1p6/8/B1Pp4/B6Q/K7/8/2R5 w - d6 0 1" );
		expectedMove = new GenericMove("c5d6"); //en passant move causes discovered checkmate
		doFindMoveTest((byte)1, true);
	}

	@Test
	public void test_findMove_mateInOne7() throws IllegalNotationException {
		// http://open-chess.org/viewtopic.php?f=7&t=997
		//setupPosition( "8/7B/8/3N4/8/1Q2B3/PPP5/rk2K2R w K - 0 1" ); // multiple possible mates! Doesn't ensure castle is mate
		setupPosition("q7/8/8/8/8/1Q2B3/PPP5/rk2K2R w K - 0 1");
		expectedMove = new GenericMove("e1g1");
		//expectedMove = new GenericMove("e1e2");
		doFindMoveTest((byte)2, true);
	}

	@Test
	public void test_findMove_mateInTwo1() throws IllegalNotationException {
		// chess.com Problem ID: 0022190
		setupPosition("k1K5/b7/R7/1P6/1n6/8/8/8 w - - - 1" );
		expectedMove = new GenericMove("b5b6");
		//doFindMoveTest(true);
		doFindMoveTest((byte)6, true);
	}

	@Test
	@Ignore // as we need to update the material due to move applied
	public void test_findMove_ArenaFailIllegalMove()throws IllegalNotationException {
		// Observed in arena, black tries to moves as white: 6th April 2015.
		// N.b. this phenomenon was caused by a combination of the castle move
		// "secondary rook move" missing implementation bug and the fact that an
		// invalid piece exception was not previously implemented.
		setupPosition( "2b1k1nr/2p2ppp/2p5/p3q3/P3Q3/P4P2/2P1B1PP/1r3R1K w k - 2 23" );
		pm.performMove(Move.toMove(new GenericMove("f1b1"), pm.getTheBoard()));
		expectedMove = new GenericMove("e5e4");
		//expectedMove = new GenericMove("e5e6");
		doFindMoveTest(true);
		
	}
	
	@Test
	public void test_findMove_enPassantCapture()throws IllegalNotationException {
		setupPosition( "k7/8/8/8/1pPP4/8/8/7K b - c3 0 1");
		expectedMove = new GenericMove("b4c3");
		doFindMoveTest((byte)1, true);
	}
	
	@Test
	public void test_findMove_mateInOne8()throws IllegalNotationException {
		setupPosition("1k6/ppR5/8/8/8/8/PP6/K1Qq2r1 w - - - 1");
		expectedMove = new GenericMove("c7c8");
		doFindMoveTest((byte)2, true);
	}
	
	@Test
	public void test_findMove_mateInOne9()throws IllegalNotationException {
		// From a game against Eubos, 26th feb 2016 - pc reported by eubos was
		// FEN: 5r1k/ppp4p/2n5/1BNb2q1/1P6/P7/2PP4/1R1Q3K w - - 6 32
	    // EubosRunnableTest:
		// 6	00:06	 2,917k	486k	-12.00	Qf3 Bxf3+ Kh2 Qe5+ Kh3 Qxc5
		// 6	00:09	 3,664k	385k	-3.00	Kh2 Ba2 Rc1 Qe5+ Kh3 Qxc5
		setupPosition( "5r1k/ppp4p/2n5/1BNb2q1/1P6/P7/2PP3K/1R1Q4 b - - 7 32");
		expectedMove = new GenericMove("g5g2");
		doFindMoveTest((byte)2, true);
	}
	
	@Test
	public void test_findMove_mateInThree_WhiteMated()throws IllegalNotationException {
		// From a game against Eubos, 26th feb 2016 - pc reported by eubos was rubbish!
		setupPosition("5r1k/ppp4p/2n5/1B1b2q1/1P6/P7/2PP4/1R1Q3K w - - 6 32");
		expectedMove = new GenericMove("d1f3");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_mateInThree_BlackMated()throws IllegalNotationException {
		// An exact negation of the position in test_findMove_mateInThree_WhiteMated().
		setupPosition( "1r1q3k/2pp4/p7/1p6/1b1B2Q1/2N5/PPP4P/5R1K b - - 6 1");
		expectedMove = new GenericMove("d8f6");
		doFindMoveTest(true);
	}
	
	@Test
	public void test_findMove_crashLichess_CloudFunction()throws IllegalNotationException {
		/*  [Site "https://lichess.org/fBSEDCCo"]
			[Date "2018.06.29"]
			[Round "-"]
			[White "eubos"]
			[Black "CloudFunction"]
			[Result "0-1"]
			[UTCDate "2018.06.29"]
			[UTCTime "19:28:24"] */
		setupPosition( "4r1k1/p2b1ppp/1q6/2Kp4/8/R2BP3/3n3P/3N2NR w - - 6 36");
		expectedMove = new GenericMove("c5d5");
		doFindMoveTest(true);
	}
	
	@Test
	@Ignore // Not decisive enough case to castle. Not massively clear why should castle.
	public void test_findMove_NeedToCastle_FromLichess()throws IllegalNotationException  {
		setupPosition("4k2r/2Q2ppp/8/3r4/1P5P/P1p5/4PP2/R3K1N1 b Qk - - -");
		expectedMove = new GenericMove("e8g8");
		
		SearchResult res = classUnderTest.findMove((byte)2);
		
		assertEquals(expectedMove, res.pv[0]);
	}
	
	@Test
	public void test_mate_in_2()throws IllegalNotationException  {
		setupPosition("5bkr/5ppp/5P2/8/8/8/6Q1/R4KR1 w - - 0 38 ");
		expectedMove = new GenericMove("g2g7"); // queen sac leads to mate in 1
		SearchResult res = classUnderTest.findMove((byte)5);
		
		assertEquals(expectedMove, Move.toGenericMove(res.pv[0]));
	}
	
	@Test
	@Ignore // Test seems very odd. I think this should be changed - it appears ill-conceived
	public void test_extendedSearch_recaptureQueenLeadsToLossOfMaterial()throws IllegalNotationException  {
		setupPosition("8/6q1/5p2/8/8/2Q5/8/8 w - - 0 38 ");
		expectedMove = new GenericMove("c3f6");
		
		SearchResult res = classUnderTest.findMove((byte)1);
		// Leads to stalemate
		assertEquals(expectedMove, Move.toGenericMove(res.pv[0]));
	}
	
	@Test
	@Ignore // confusing as the position is unreal - e.g. stalemate! Perhaps add Kings???
	public void test_extendedSearch_recaptureBishopLeadsToLossOfMaterial() throws IllegalNotationException {
		setupPosition("8/6q1/5p2/8/8/2B5/8/8 w - - 0 38 ");
		expectedMove = new GenericMove("c3f6");
		
		SearchResult res = classUnderTest.findMove((byte)1);
		
		assertNotEquals(expectedMove, res.pv[0]);
	}
	
	@Test
	@Ignore // Not a good test - nowadays it isn't clear eubos should castle.
	public void test_findMove_NeedToCastle_FromLichess1() throws IllegalNotationException {
		setupPosition( "rnb1kbnr/p4p1p/1qp1p1p1/3p4/8/1B2PN2/PPPP1PPP/RNBQK2R w KQkq - - -");
		expectedMove = new GenericMove("e1g1");
		
		SearchResult res = classUnderTest.findMove((byte)3);
		
		assertEquals(expectedMove, res.pv[0]);
	}
	
	@Test
	@Ignore // Don't usually run, takes ~12 seconds from an empty hashTable!
	public void test_assymetric_eval_due_to_zugzwang() throws IllegalNotationException {
		setupPosition("8/8/8/3K2k1/5b2/5p2/5B2/8 w - - - 61");
		SearchResult res = classUnderTest.findMove((byte)11);
		
		// Check the move and score are as expected
		expectedMove = new GenericMove("d5e4");
		assertEquals(expectedMove, Move.toGenericMove(res.pv[0]));
		assertEquals(-139, classUnderTest.getScore());
		
		// Check the hash score is updated properly, negated for white POV
		pm.performMove(res.pv[0]);
		assertEquals(139,Transposition.getScore(hashMap.getTransposition(pm.getHash())));
	}
	
	@Test
	@Ignore // Don't usually run, takes ~7 seconds from an empty hashTable!
	public void test_assymetric_eval_due_to_zugzwang_from_black() throws IllegalNotationException {
		setupPosition("8/8/8/6k1/4Kb2/5p2/5B2/8 b - - 1 1");
		SearchResult res = classUnderTest.findMove((byte)11);
		
		// Check the move and score are as expected
		expectedMove = new GenericMove("g5g4");
		assertEquals(expectedMove, Move.toGenericMove(res.pv[0]));
		assertEquals(157, classUnderTest.getScore());
	}
}
