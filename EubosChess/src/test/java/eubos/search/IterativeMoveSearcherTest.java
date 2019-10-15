package eubos.search;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
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
		//boolean infoCommandReceived = false;
		boolean bestMoveCommandReceived = false;
		//ProtocolInformationCommand last_info;
		ProtocolBestMoveCommand last_bestMove;
		
		@Override
		public void sendInfoCommand(ProtocolInformationCommand command) {
			//infoCommandReceived = true;
			//last_info = command;
		}
		
		@Override
		public void sendBestMoveCommand(ProtocolBestMoveCommand command) {
			bestMoveCommandReceived = true;
			last_bestMove = command;
		}
		
		//public boolean getInfoCommandReceived() { return infoCommandReceived; }
	}
	private EubosMock eubos;
	
	protected void setupPosition(String fen, long time) {
		pm = new PositionManager( fen );
		sut = new IterativeMoveSearcher(eubos, hashMap, pm, pm, pm, time, pm.getPositionEvaluator());
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		assertEquals(expectedMove, eubos.last_bestMove.bestMove);
	}
	
	@Test
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
		expectedMove = new GenericMove("b2b5");
		runSearcherAndTestBestMoveReturned();
	}
}
