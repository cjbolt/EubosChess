package eubos.search;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.commands.ProtocolInformationCommand;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.main.EubosEngineMain;
import eubos.position.PositionManager;

public class IterativeMoveSearcherTest {
	
	protected IterativeMoveSearcher sut;
	protected GenericMove expectedMove;
	protected FixedSizeTranspositionTable hashMap;
	PositionManager pm;
	
	private class EubosMock extends EubosEngineMain {
		boolean bestMoveCommandReceived = false;
		ProtocolBestMoveCommand last_bestMove;
		
		@Override
		public void sendInfoCommand(ProtocolInformationCommand command) {
		}
		
		@Override
		public void sendBestMoveCommand(ProtocolBestMoveCommand command) {
			bestMoveCommandReceived = true;
			last_bestMove = command;
		}
	}
	private EubosMock eubos;
	
	protected void setupPosition(String fen, long time) {
		pm = new PositionManager( fen );
		sut = new IterativeMoveSearcher(eubos, hashMap, pm, pm, time, pm.getPositionEvaluator());
	}
	
	@Before
	public void setUp() throws Exception {
		eubos = new EubosMock();
		SearchDebugAgent.open();
		hashMap = new FixedSizeTranspositionTable();
	}
	
	@After
	public void tearDown() {
		SearchDebugAgent.close();
	}

	private void runSearcherAndTestBestMoveReturned() {
		eubos.bestMoveCommandReceived = false;
		eubos.last_bestMove = null;
		sut.start(); // need to wait for result
		while (!eubos.bestMoveCommandReceived) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}
		assertEquals(expectedMove, eubos.last_bestMove.bestMove);
	}
	
	@Test
	@Ignore // The problem with this test is that the best move is rejected after ply 10.
	public void test_endgame_a() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		setupPosition("8/8/2pp3k/8/1P1P3K/8/8/8 w - - 0 1", 1000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("d4d5"); //Levy
		runSearcherAndTestBestMoveReturned();
	}
	
	@Test
	public void test_endgame_b() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		setupPosition("8/ppp5/8/PPP5/6kp/8/6KP/8 w - - 0 1", 6000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("b5b6"); // Levy
		runSearcherAndTestBestMoveReturned();		
	}
	
	@Test
	@Ignore // Eubos doesn't get close :(
	// According to Stockfish this position is a dead draw, so I guess Levy is wrong.
	public void test_endgame_c() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		setupPosition("8/p7/1p1k1p2/1P2pp1p/1P1P3P/4KPP1/8/8 w - - 1 10", 6000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("g3g4"); // Levy
		runSearcherAndTestBestMoveReturned();		
	}
	 
	@Test
	@Ignore // Eubos needs 2mins to reliably find the correct move
	public void test_endgame_d() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		setupPosition("8/pp5p/8/PP2k3/2P2pp1/3K4/6PP/8 w - - 1 10", 6000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("c4c5"); // Levy
		runSearcherAndTestBestMoveReturned();		
	}
	
	@Test
	//@Ignore // needs to search to 20 odd plies to see a win (when mate should be seen in 19 - this is a bug!)
	public void test_endgame_e() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		setupPosition("6k1/7p/5P1K/8/8/8/7P/8 w - - 0 1", 1000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("h6g5"); // Stockfish
		runSearcherAndTestBestMoveReturned();		
	}
	
	@Test
	@Ignore //Eubos doesn't have a clue, even at ply==24; probably indicating a bug.
	public void test_endgame_i() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		setupPosition("8/k7/3p4/p2P1p2/P2P1P2/8/8/K7 w - - 0 1", 100);
		expectedMove = new GenericMove("c5d5");
		runSearcherAndTestBestMoveReturned();
	}
	
	@Test
	public void test_endgame_k() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		setupPosition("8/2k5/p1P5/P1K5/8/8/8/8 w - - 0 1", 100);
		expectedMove = new GenericMove("c5d5");
		runSearcherAndTestBestMoveReturned();
	}
	
	@Test
	public void test_endgame_o() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		setupPosition("4k3/4Pp2/5P2/4K3/8/8/8/8 w - - 0 1", 100);
		expectedMove = new GenericMove("e5f5"); // Stockfish
		runSearcherAndTestBestMoveReturned();
	}
	
	@Test
	public void test_mateInFour() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		// chess.com Problem ID: 0102832
		setupPosition( "r1r3k1/pb1p1p2/1p2p1p1/2pPP1B1/1nP4Q/1Pq2NP1/P4PBP/b2R2K1 w - - - -", 1000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("g5f6");
		runSearcherAndTestBestMoveReturned();
	}
	
	@Test
	public void test_findMove_mateInTwo()  throws NoLegalMoveException, IllegalNotationException {
		// chess.com Problem ID: 0551140
		setupPosition("rnbq1rk1/p4ppN/4p2n/1pbp4/8/2PQP2P/PPB2PP1/RNB1K2R w - - - -", 1000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("h7f6");
		runSearcherAndTestBestMoveReturned();
	}
	
	@Test
	@Ignore
	public void test_lichess_blunder() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		/* [Event "Rated Bullet game"]
		   [Site "https://lichess.org/eLdAvxeq"]
				[Date "2019.10.15"]
				[Round "-"]
				[White "eubos"]
				[Black "Elmichess"]
				[Result "1/2-1/2"]
				[UTCDate "2019.10.15"]
				[UTCTime "07:33:51"]
				[WhiteElo "1730"]
				[BlackElo "1638"]
				[WhiteRatingDiff "-2"]
				[BlackRatingDiff "+2"]
				[WhiteTitle "BOT"]
				[BlackTitle "BOT"]
				[Variant "Standard"]
				[TimeControl "60+1"]
				[ECO "A00"]
				[Opening "Hungarian Opening: Myers Defense"]
				[Termination "Normal"]
				[Annotator "lichess.org"]

				1. g3 g5 { A00 Hungarian Opening: Myers Defense } 2. Nf3 e6 3. Bg2 Nc6 4. b3 g4 5. Nh4 Nf6 6. Bxc6 dxc6 7. O-O Ne4 8. Bb2 Rg8 9. c4 Bc5 10. d4 Bb4 11. f3 gxf3 12. Nxf3 f5 13. e3 Bd7 14. Ne5 Qg5 15. Nxd7 Qxe3+ 16. Kg2 Kxd7 17. Rf3 Qh6 18. Nc3 Bxc3 19. Bxc3 Ng5 20. Rf2 Qh3+ 21. Kg1 Ne4 22. Rf3 f4 23. Qc2 Qf5 24. Qd3 Ng5 25. Raf1 Nxf3+ 26. Qxf3 Qc2 27. Qxf4 Qxc3 28. Qf7+ Kd6 29. c5+ Kd5 30. Qd7+ Ke4 31. Rf4+ Ke3 32. Qxe6+ Kd2 33. Rf2+ Kd3 34. Rf3+ Kc2 35. Qe4+ Kd2 36. Rxc3 Kxc3 37. Qxh7 Kxd4 38. Qxc7 Kxc5 39. Qxb7 a5 40. Qe7+ Kd5 41. Qd7+ Kc5 42. Qe7+ Kd5 43. Qd7+ Ke4 44. Qxc6+ Kd4 45. a4 Rgf8 46. h4 Rac8 47. Qb6+ Kc3 48. h5 Rfe8 49. Qxa5+ Kxb3 50. h6 Kc4 51. h7 Rc5 52. Qa6+ Kd5 53. Qb7+ Kd6 54. Qb4 Kc6 55. Qb2 Rh5 56. Qb5+ Rxb5 57. axb5+ Kxb5 58. g4 Rh8 59. Kg2 Kc4 60. g5 Rxh7 61. Kg3 Kb5 62. Kg4 Ka6 63. g6 Rd7 64. Kg5 Ka7 65. Kh6 Rd2 66. g7 Rd8 67. Kh7 Rd7 68. Kh8 Rxg7 69. Kxg7 { The game is a draw. } 1/2-1/2
				*/
		/* Try to build up hash table by running previous moves. */
		setupPosition("2r1r3/8/8/Q6P/P7/1k4P1/8/6K1 w - - 0 50", 12200); 
		expectedMove = new GenericMove("h5h6");
		runSearcherAndTestBestMoveReturned();
		// Black Kb3
		setupPosition("2r1r3/8/7P/Q7/P1k5/6P1/8/6K1 w - - 1 51", 11600); 
		expectedMove = new GenericMove("h6h7");
		runSearcherAndTestBestMoveReturned();
		// Black Rc5
		setupPosition("4r3/7P/8/Q1r5/P1k5/6P1/8/6K1 w - - 1 52", 11400); 
		expectedMove = new GenericMove("a5a6");		
		runSearcherAndTestBestMoveReturned();
		// black Kd5
		setupPosition("4r3/7P/Q7/2rk4/P7/6P1/8/6K1 w - - 3 53", 11300);
		expectedMove = new GenericMove("a6b7");
		runSearcherAndTestBestMoveReturned();
		// black Kd6
		setupPosition("4r3/1Q5P/3k4/2r5/P7/6P1/8/6K1 w - - 5 54", 11100);
		expectedMove = new GenericMove("b7b4");
		runSearcherAndTestBestMoveReturned();
		// black Kc6
		setupPosition("4r3/7P/2k5/2r5/PQ6/6P1/8/6K1 w - - 7 55", 10900);
		expectedMove = new GenericMove("b4b2");
		runSearcherAndTestBestMoveReturned();
		// black Rh5
		setupPosition("4r3/7P/2k5/7r/P7/6P1/1Q6/6K1 w - - 9 56", 10900);
		SearchDebugAgent.isDebugOn = true;
		expectedMove = new GenericMove("b2b5");
		runSearcherAndTestBestMoveReturned();
	}
	
	
}
