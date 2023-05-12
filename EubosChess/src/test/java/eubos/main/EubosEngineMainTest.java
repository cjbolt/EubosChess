package eubos.main;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.commands.EngineAnalyzeCommand;
import com.fluxchess.jcpi.commands.EngineNewGameCommand;
import com.fluxchess.jcpi.models.GenericBoard;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.BitBoard;
import eubos.board.Piece;
import eubos.position.Move;
import eubos.position.Position;
import eubos.score.PawnEvaluator;
import eubos.search.Score;
import eubos.search.SearchMetrics;
import eubos.search.SearchResult;
import eubos.search.transposition.Transposition;

public class EubosEngineMainTest extends AbstractEubosIntegration {

	@Before
	public void setUp() throws IOException {
		// Start engine
		System.setOut(new PrintStream(testOutput));
		inputToEngine = new PipedWriter();
		classUnderTest = new EubosEngineMain(inputToEngine);
		eubosThread = new Thread( classUnderTest );
		eubosThread.start();
	}
	
	@After
	public void tearDown() throws IOException, InterruptedException {
		// Stop the Engine TODO: could send quit command over stdin
		inputToEngine.write(QUIT_CMD);
		inputToEngine.flush();
		Thread.sleep(10);
		classUnderTest = null;
		eubosThread = null;
	}
	
	@Test
	public void test_startEngine() throws InterruptedException, IOException {
		setupEngine();
		performTest(100);
	}
	
	@Test
	public void test_mateInTwo() throws InterruptedException, IOException {
		setupEngine();
		// Setup Commands specific to this test
		commands.add(new CommandPair(POS_FEN_PREFIX+"k1K5/b7/R7/1P6/1n6/8/8/8 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"4"+CMD_TERMINATOR,BEST_PREFIX+"b5b6"+CMD_TERMINATOR));
		performTest(1000);
	}
	
	@Test
	public void test_mateInTwo_onTime() throws InterruptedException, IOException {
		setupEngine();
		// Setup Commands specific to this test
		commands.add(new CommandPair(POS_FEN_PREFIX+"k1K5/b7/R7/1P6/1n6/8/8/8 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_TIME_PREFIX+"1000"+CMD_TERMINATOR,BEST_PREFIX+"b5b6"+CMD_TERMINATOR));
		performTest(1000);
	}
	
	@Test
	public void test_mateInTwo_fromBlack() throws InterruptedException, IOException {
		setupEngine();
		// Setup Commands specific to this test
		commands.add(new CommandPair(POS_FEN_PREFIX+"k1K5/b7/R7/1P6/1n6/8/8/8 w - - 0 1 moves b5b6"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"4"+CMD_TERMINATOR,BEST_PREFIX+"b4a6"+CMD_TERMINATOR));
		performTest(1000);
	}	
	
	@Test
	public void test_mateInOne() throws InterruptedException, IOException {
		setupEngine();
		// Setup Commands specific to this test
		commands.add(new CommandPair(POS_FEN_PREFIX+"5r1k/p2R4/1pp2p1p/8/5q2/3Q1bN1/PP3P2/6K1 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"2"+CMD_TERMINATOR,BEST_PREFIX+"d3h7"+CMD_TERMINATOR));
		performTest(1000);
	}
	
	@SuppressWarnings("unused")
	@Test
	public void test_infoMessageSending_clearsPreviousPvMoves() throws InterruptedException, IOException {
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING && !SearchMetrics.ENABLE_SINGLE_MOVE_PV) {
			String expectedOutput = "info depth 1 seldepth 6 score cp -118 pv d7e5 f3e5 c7c2 hashfull 0 nps 0 time 0 nodes 24"+CMD_TERMINATOR+
					"info depth 1 seldepth 5 score cp 376 pv c7c2 d4a7 hashfull 0 nps 0 time 0 nodes 43"+CMD_TERMINATOR+
                    "info depth 2 seldepth 7 score cp 106 pv c7c2 e1g1 d7e5 hashfull 0 nps 0 time 0 nodes 187"+CMD_TERMINATOR
                    +BEST_PREFIX+"c7c2";
			if (PawnEvaluator.ENABLE_PP_IMBALANCE_EVALUATION) {
				expectedOutput = "info depth 1 seldepth 5 score cp -55 pv d7e5 f3e5 c7c2 hashfull 0 nps 0 time 0 nodes 21"+CMD_TERMINATOR+
							     "info depth 1 seldepth 5 score cp 471 pv c7c2 d4a7 hashfull 0 nps 0 time 0 nodes 34"+CMD_TERMINATOR+
							     "info depth 2 seldepth 6 score cp 128 pv c7c2 e1g1 d7e5 hashfull 0 nps 0 time 0 nodes 185"+CMD_TERMINATOR
								 +BEST_PREFIX+"c7c2";
			}
			setupEngine();
			// Setup Commands specific to this test
			commands.add(new CommandPair(POS_FEN_PREFIX+"r1b1kb1r/ppqnpppp/8/3pP3/3Q4/5N2/PPP2PPP/RNB1K2R b KQkq - 2 8"+CMD_TERMINATOR, null));
			commands.add(new CommandPair(GO_DEPTH_PREFIX+"2"+CMD_TERMINATOR, removeTimeFieldsFromUciInfoMessage(expectedOutput)+CMD_TERMINATOR));
			
			/* Historically, this position and search caused a bad UCI info message to be generated. 
			 * The second info contains c7e5, which has not been cleared from the first PV of the ext search...
			info depth 1 seldepth 4 score cp -149 pv d7e5 f3e5 c7e5 nps 200 time 35 nodes 7
			info depth 1 seldepth 4 score cp 135 pv c7c2 e1g1 nps 73 time 122 nodes 9
			info depth 2 seldepth 0 score cp 24 pv c7c2 d4d5 e8d8 nps 562 time 151 nodes 85 */
			performTest(2000, true); // check infos
		}
	}
	
	@Test
	public void test_game_position_detect_draw() throws InterruptedException, IOException {
		setupEngine();
		// Setup Commands specific to this test, checking to avoid draw by repetition..
		commands.add(new CommandPair(POS_FEN_PREFIX+"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 moves "
				+ "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6 f2f3 e7e5 d4b3 c8e6 f1d3 b8c6 e1g1"
				+ " e6b3 a2b3 d6d5 e4d5 f8c5 g1h1 f6d5 c3e4 c5e3 c2c4 d5b4 c1e3 b4d3 d1e2 f7f5 e4g3 f5f4"
				+ " f1d1 f4g3 d1d3 d8h4 h2h3 a6a5 e3c5 a8d8 d3d8 e8d8 b3b4 h4f6 b4a5 d8c7 a5a6 h8a8 e2e1"
				+ " a8a6 a1a6 b7a6 e1g3 c6a5 g3f2 a5c4 f2c2 c4d6 c5d6 c7d6 c2h7 d6c5 h7g8 c5d4 g8b3 f6c6"
				+ " b3d1 d4e3 b2b4 e3f4 d1d3 f4g5 d3e3 g5f5 h3h4 f5e6 e3g5 c6c4 g5g6 e6d5 g6f7 d5d4 f7g7"
				+ " c4c1 h1h2 c1f4 h2h3 d4c4 g7b7 f4f5 g2g4 f5f6 h3g2 f6d6 h4h5 d6d2 g2h3 d2d6 g4g5 d6e6"
				+ " h3g3 c4c3 g5g6 e6f5 g6g7 f5f4 g3g2 f4g5 g2f1 g5c1 f1e2 c1b2 e2e3 b2d2 e3e4 d2f4 e4d5"
				+ " f4f3 d5e5 f3b7 g7g8q b7f3 g8g5 f3e2 e5d6 c3b4 g5f4 b4b3 h5h6 e2d3 d6e6 d3h7 f4e3 b3c4"
				+ " e3e2 c4b4 e2d2 b4b3 d2d5 b3b2 d5d4 b2c2 d4c5 c2d2 c5g5 d2d1 e6f6 h7h8 g5g7 h8d8 f6f5"
				+ " d8d5 f5f4 d5d6 f4e4 d6c6 e4d3 c6d5 g7d4 d5b3 d3e4 d1e2 e4e5 b3b8 d4d6 b8b2 e5f4 b2c1"
				+ " f4f5 c1c2 f5f6 c2a4 d6h2 e2d3 h2h3 d3d2 h3h2 d2d3 h2h3 d3d2"+CMD_TERMINATOR, null));
		// Need to insert a trans in the hash table for the root position with best score that is a draw at this new position!
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"2"+CMD_TERMINATOR,BEST_PREFIX+"h3g2"+CMD_TERMINATOR)); // i.e not h2h3
		performTest(5000);
	}
	
	@Test
	public void test_game_position_takes_draw() throws InterruptedException, IOException {
		setupEngine();
		// Setup Commands specific to this test, checking to achieve draw by repetition..
		commands.add(new CommandPair(POS_FEN_PREFIX+"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 moves "
				+ "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6 c1e3 e7e6 f2f3 b7b5 d1d3 b5b4 c3e2 e6e5 d4b3 "
				+ "f8e7 d3c4 d6d5 e4d5 f6d5 e1c1 c8e6 c4e4 b8c6 c2c4 f7f5 e4b1 d5e3 d1d8 a8d8 b3d2 e6c4 e2g3 e7g5 "
				+ "f1c4 e3c4 b1f5 g5d2 c1b1 c6d4 f5d3 d8c8 g3f5 d4f5 d3f5 e8e7 a2a3 g7g6 f5e4 b4a3 b2b3 c4e3 e4e5 "
				+ "e7f7 e5f4 f7g8 f4d6 c8c2 d6e6 g8g7 e6e7 g7g8 e7d8 g8f7 d8d7 f7f8 d7d6 f8e8 d6e6 e8d8 e6d6 d8c8 "
				+ "d6a6 c8d7 a6a3 h8c8 a3a4 d7e6 a4h4 c8c3 h4a4 e6e7 g2g3 e7f8 f3f4 h7h6 a4b5 d2c1 b5b8 f8f7 b1a1 "
				+ "c1b2 a1b1 c3c7 b8c7 c2c7 b1b2 c7c2 b2a3 e3g4 h2h3 g4f6 g3g4 c2f2 g4g5 h6g5 f4g5 f6e4 h3h4 f2f3 "
				+ "a3a2 e4d2 b3b4 d2c4 a2b1 f3f2 b1c1 f2b2 h1f1 f7g7 f1e1 b2b4 e1e4 g7f7 c1c2 c4a3 c2d3 b4b3 d3d4 "
				+ "a3c2 d4c5 b3c3 c5d5 c3d3 d5e5 d3d7 e4f4 f7g7 e5e6 d7a7 f4e4 a7a1 e4c4 a1e1 e6d6 e1d1 d6e5 c2e3 "
				+ "c4c7 g7g8 c7c8 g8f7 c8c7 f7f8 e5e6 e3d5 c7c8 f8g7 c8c4 d1d2 c4c6 d5f4 e6e5 f4d3 e5e4 d3b4 c6c7 "
				+ "g7f8 e4e5 d2e2 e5d6 b4d3 c7c4 f8f7 " 
				/* The repetition starts from here, when rook goes to c7 and gives check. */
				+ "c4c7 f7g8 c7c8 g8f7 c8c7 f7g8 c7c8 g8f7"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"1"+CMD_TERMINATOR,BEST_PREFIX+"c8c7"+CMD_TERMINATOR));
		performTest(5000);
	}
	
	@Test
	public void test_achieves_draw_black_repeated_check() throws InterruptedException, IOException {
		setupEngine();
		// Setup Commands specific to this test
		commands.add(new CommandPair(POS_FEN_PREFIX+"7q/1P6/8/8/8/8/2k3PQ/7K b - - 0 42"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"h8a1"+CMD_TERMINATOR));
		commands.add(new CommandPair(POS_FEN_PREFIX+"7q/1P6/8/8/8/8/2k3PQ/7K b - - 0 42 moves h8a1 h2g1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"a1h8"+CMD_TERMINATOR));
		commands.add(new CommandPair(POS_FEN_PREFIX+"7q/1P6/8/8/8/8/2k3PQ/7K b - - 0 42 moves h8a1 h2g1 a1h8 g1h2"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"h8a1"+CMD_TERMINATOR));
		commands.add(new CommandPair(POS_FEN_PREFIX+"7q/1P6/8/8/8/8/2k3PQ/7K b - - 0 42 moves h8a1 h2g1 a1h8 g1h2 h8a1 h2g1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"a1h8"+CMD_TERMINATOR));
		commands.add(new CommandPair(POS_FEN_PREFIX+"7q/1P6/8/8/8/8/2k3PQ/7K b - - 0 42 moves h8a1 h2g1 a1h8 g1h2 h8a1 h2g1 a1h8 g1h2"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"h8a1"+CMD_TERMINATOR));
		performTest(500);
	}
	
	@Test
	public void test_achieves_draw_white_repeated_check() throws InterruptedException, IOException {
		setupEngine();
		// Setup Commands specific to this test
		commands.add(new CommandPair(POS_FEN_PREFIX+"7k/2K3pq/8/8/8/8/1p6/7Q w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"h1a8"+CMD_TERMINATOR));
		commands.add(new CommandPair(POS_FEN_PREFIX+"7k/2K3pq/8/8/8/8/1p6/7Q w - - 0 1 moves h1a8 h7g8"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"a8h1"+CMD_TERMINATOR));
		commands.add(new CommandPair(POS_FEN_PREFIX+"7k/2K3pq/8/8/8/8/1p6/7Q w - - 0 1 moves h1a8 h7g8 a8h1 g8h7"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"h1a8"+CMD_TERMINATOR));
		commands.add(new CommandPair(POS_FEN_PREFIX+"7k/2K3pq/8/8/8/8/1p6/7Q w - - 0 1 moves h1a8 h7g8 a8h1 g8h7 h1a8 h7g8"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"a8h1"+CMD_TERMINATOR));
		commands.add(new CommandPair(POS_FEN_PREFIX+"7k/2K3pq/8/8/8/8/1p6/7Q w - - 0 1 moves h1a8 h7g8 a8h1 g8h7 h1a8 h7g8 a8h1 g8h7"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"h1a8"+CMD_TERMINATOR));
		performTest(500);
	}
	
	@Test
	public void test_KQk_mate_in_7_NEW() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"5Q2/6K1/8/3k4/8/8/8/8 w - - 1 113"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_TIME_PREFIX+"30000"+CMD_TERMINATOR, BEST_PREFIX+"f8b4"+CMD_TERMINATOR));
		performTestExpectMate(15000, 7);
	}
	
	@Test
	public void test_KQk_mated_in_6_NEW() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/6K1/8/3k4/1Q6/8/8/8 b - - 1 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"12"+CMD_TERMINATOR, BEST_PREFIX+"d5c6"+CMD_TERMINATOR));
		performTestExpectMate(5000, -6);
	} 
	
	@Test
	public void test_WAC009() throws InterruptedException, IOException {
		setupEngine();
		// 1
		commands.add(new CommandPair(POS_FEN_PREFIX+"3q1rk1/p4pp1/2pb3p/3p4/6Pr/1PNQ4/P1PB1PP1/4RRK1 b - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"9"+CMD_TERMINATOR,BEST_PREFIX+"d6h2"+CMD_TERMINATOR));
		// 2
		commands.add(new CommandPair(POS_FEN_PREFIX+"3q1rk1/p4pp1/2pb3p/3p4/6Pr/1PNQ4/P1PB1PP1/4RRK1 b - - 0 1 moves d6h2 g1h1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"7"+CMD_TERMINATOR,BEST_PREFIX+"h2g3"+CMD_TERMINATOR));
		// 3
		commands.add(new CommandPair(POS_FEN_PREFIX+"3q1rk1/p4pp1/2pb3p/3p4/6Pr/1PNQ4/P1PB1PP1/4RRK1 b - - 0 1 moves d6h2 g1h1 h2g3 h1g1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"h4h1"+CMD_TERMINATOR));
		// 4
		commands.add(new CommandPair(POS_FEN_PREFIX+"3q1rk1/p4pp1/2pb3p/3p4/6Pr/1PNQ4/P1PB1PP1/4RRK1 b - - 0 1 moves d6h2 g1h1 h2g3 h1g1 h4h1 g1h1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"d8h4"+CMD_TERMINATOR));
		// 5
		commands.add(new CommandPair(POS_FEN_PREFIX+"3q1rk1/p4pp1/2pb3p/3p4/6Pr/1PNQ4/P1PB1PP1/4RRK1 b - - 0 1 moves d6h2 g1h1 h2g3 h1g1 h4h1 g1h1 d8h4 h1g1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"h4h2"+CMD_TERMINATOR));

		performTest(15000);
	}
	
	@Test
	public void test_KRk_mate_in_11_NEW() throws InterruptedException, IOException {
		int mateDepth = 0;
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/8/8/3K1k2/8/8/8/7r b - - 5 111"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_TIME_PREFIX+"14000"+CMD_TERMINATOR, BEST_PREFIX+"h1h4"+CMD_TERMINATOR));
		mateDepth = 12;
		performTestExpectMate(14000, mateDepth);
	}
	
	 
	@Test
	public void test_mate_in_3_guardian3713() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/2p5/P4p2/Q1N2k1P/2P2P2/3PK2P/5R2/2B2R2 w - - 1 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"8"+CMD_TERMINATOR, BEST_PREFIX+"f2d2"+CMD_TERMINATOR));
		performTestExpectMate(4000, 4);
	}
	
	@Test
	@Ignore
	public void test_won_position_need_to_push_pp() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/6r1/8/R5p1/3K4/1P4Pk/7P/8 w - - 3 49"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"10"+CMD_TERMINATOR, BEST_PREFIX+"b3b4"+CMD_TERMINATOR));
		performTest(5000);
	}
	
	@Test
	public void test_defect_en_passant_treated_as_playable_move_regardless_of_board_state() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"r3qrk1/pbpp1ppp/np1b1n2/8/2PPp3/P1N1P1PP/1P2NPB1/R1BQK2R w KQ - 1 10"+CMD_TERMINATOR, null));
		//commands.add(new CommandPair(GO_DEPTH_PREFIX+"8"+CMD_TERMINATOR, BEST_PREFIX+"d1b3"+CMD_TERMINATOR));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"8"+CMD_TERMINATOR, BEST_PREFIX+"e1g1"+CMD_TERMINATOR));
		//commands.add(new CommandPair(GO_DEPTH_PREFIX+"8"+CMD_TERMINATOR, BEST_PREFIX+"e2f4"+CMD_TERMINATOR));
		performTest(5000);
	}
	
	@Test
	public void test_tricky_endgame_position() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/8/4kp1p/3pb1p1/P5P1/3KN1PP/8/8 b - - 5 57"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"13"+CMD_TERMINATOR, BEST_PREFIX+"e5g3"+CMD_TERMINATOR));
		performTest(15000);
	}
	
	@SuppressWarnings("unused")
	@Test
	public void test_hash_issue_losing_position() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"3r2k1/5p2/7p/3R2p1/p7/1q1Q1PP1/7P/3R2K1 b - - 1 42"+CMD_TERMINATOR, null));

		commands.add(new CommandPair(GO_DEPTH_PREFIX+"8"+CMD_TERMINATOR, BEST_PREFIX+"d8d5"+CMD_TERMINATOR));
		//commands.add(new CommandPair(GO_DEPTH_PREFIX+"8"+CMD_TERMINATOR, BEST_PREFIX+"b3b6"+CMD_TERMINATOR));

		int hashMove = Move.valueOf(Position.b3, Piece.BLACK_QUEEN, Position.d1, Piece.WHITE_ROOK);
		long hashEntry = Transposition.valueOf((byte)6, (short)0, Score.exact, hashMove, 42 >> 2);
		pokeHashEntryAndPerformTest(12000, hashEntry);
	}
	
	@Test
	public void test_hash_issue_threw_away_draw() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 moves "
				+ "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6 c1e3 e7e6 g2g4 e6e5 d4f5 g7g6 "
				+ "g4g5 g6f5 g5f6 f5f4 e3d2 b8d7 f1g2 d7f6 d1e2 h8g8 g2f3 f8h6 e1c1 c8e6 c3d5 f6d5 "
				+ "e4d5 e6f5 f3e4 f5g6 h2h3 e8f8 c1b1 g6e4 e2e4 g8g6 h1e1 d8b6 e1e2 f8g8 a2a4 h6g5 "
				+ "d1e1 g5h4 a4a5 b6b5 c2c4 b5b3 c4c5 d6c5 d2c3 f7f6 e2d2 a8d8 d5d6 g6g7 e4f3 b3e6 "
				+ "e1d1 h7h6 d2d5 e6c8 d5d3 g8h8 f3h5 c5c4 h5h4 c8f5 c3e5 f6e5 h4d8 h8h7 d6d7 c4d3 "
				+ "d8h8 h7h8 d7d8q g7g8 d8d3 f5d3 d1d3 e5e4 d3d6 g8g1 b1a2 g1g5 d6h6 h8g8 h6e6 g5a5 "
				+ "a2b1 e4e3 f2e3 g8f7 e6e4 f4e3 b1c2 a5c5 c2d3 c5b5 b2b4 b5h5 h3h4 e3e2 d3e2 h5h6 "
				+ "e2f3 b7b5 f3g4 h6g6 g4f5 g6g2 h4h5 g2h2 f5g5 h2d2 e4f4 f7g8 h5h6 d2d5 g5g6 d5d8 "
				+ "f4f6 d8a8 f6e6 g8h8 h6h7 a8b8 e6d6 b8a8 d6c6 a8d8 c6b6 d8c8 g6h6 c8a8 b6f6 a8b8 "
				+ "h6g6 b8a8 f6d6 a8c8 g6h6 c8b8 d6c6 b8d8 c6e6 d8c8 e6g6 c8b8 g6f6 b8c8 f6e6 c8d8 "
				+ "e6c6 d8b8 c6d6 b8e8 d6d7 e8e6 h6g5 e6e5 g5g6 e5e6 g6f5 e6e2 d7a7 e2b2 f5g6 b2g2 "
				+ "g6h5 g2e2 a7c7 e2d2 c7e7 d2d5 h5h6 d5d6 h6g5 d6d5 g5f6 d5d4 f6g6 d4d6 g6f5 d6d5 "
				+ "f5e4 d5d2 e7a7 d2d6 e4e5 d6c6 e5f5 c6b6 f5g5 b6c6 a7b7 c6c4 g5h5 c4b4 h5g6 b4g4 "
				+ "g6h6 g4h4 h6g6 h4g4 g6h6 g4h4 h6g6"+CMD_TERMINATOR, null));
		//commands.add(new CommandPair(GO_DEPTH_PREFIX+"20"+CMD_TERMINATOR, BEST_PREFIX+"a6a5"+CMD_TERMINATOR));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR, BEST_PREFIX+"h4g4"+CMD_TERMINATOR));
		
		int hashMove = Move.valueOf(Position.h4, Piece.BLACK_ROOK, Position.g4, Piece.NONE);
		long hashEntry = Transposition.valueOf((byte)3, (short)0, Score.upperBound, hashMove, 107 >> 2);
		pokeHashEntryAndPerformTest(10000, hashEntry);
	}
	
	@Test
	public void test_hash_issue_best_move_changed_unexpectedly() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"2k5/1p3Rb1/p2pN3/P2P4/1P1P4/1K6/8/2r5 b - - 0 66"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"9"+CMD_TERMINATOR, BEST_PREFIX+"g7h6"+CMD_TERMINATOR));
		
		int hashMove = Move.valueOf(Position.g7, Piece.BLACK_BISHOP, Position.h6, Piece.NONE);
		long hashEntry = Transposition.valueOf((byte)8, (short)-55, Score.lowerBound, hashMove, 66 >> 2);
		pokeHashEntryAndPerformTest(10000, hashEntry);
	}
	
    @Test
    public void test_createPositionFromAnalyseCommand_enPassantMovesAreIdentifedCorrectly() throws IllegalNotationException {
    	classUnderTest.receive(new EngineNewGameCommand());
    	// To catch a defect where En Passant moves were not properly identified when applied through the UCI received from lichess/Arena
    	ArrayList<GenericMove> applyMoveList = new ArrayList<GenericMove>();
    	applyMoveList.add(new GenericMove("c2c4"));
    	applyMoveList.add(new GenericMove("b4c3")); // en passant capture!
    	// In the defect case the captured pawn was not removed from the board.
		classUnderTest.createPositionFromAnalyseCommand(new EngineAnalyzeCommand(new GenericBoard("r4b2/1b2k1p1/1B1pPp1q/p2P1r2/1p6/1P1N4/P1P3QP/R5RK w - - 4 32"), applyMoveList));
		assertEquals("r4b2/1b2k1p1/1B1pPp1q/p2P1r2/8/1PpN4/P5QP/R5RK w - - - 33", classUnderTest.lastFen);
    }
    
	@Test
	public void test_createPositionFromAnalyseCommand() throws IllegalNotationException {
		classUnderTest.receive(new EngineNewGameCommand());
		// Black move 62
		ArrayList<GenericMove> applyMoveList = new ArrayList<GenericMove>();
		classUnderTest.createPositionFromAnalyseCommand(new EngineAnalyzeCommand(new GenericBoard("8/8/8/8/8/pk6/8/K7 b - - 5 62"), applyMoveList));
		classUnderTest.sendBestMoveCommand(new SearchResult(Move.valueOfBit(BitBoard.b3, Piece.BLACK_KING, BitBoard.b4, Piece.NONE)));
		// White move 63
		applyMoveList = new ArrayList<GenericMove>();
		applyMoveList.add(new GenericMove("b3b4"));
		classUnderTest.createPositionFromAnalyseCommand(new EngineAnalyzeCommand(new GenericBoard("8/8/8/8/8/pk6/8/K7 b - - 5 62"), applyMoveList));
		classUnderTest.sendBestMoveCommand(new SearchResult(Move.valueOfBit(BitBoard.a1, Piece.WHITE_KING, BitBoard.a2, Piece.NONE)));
		// Black move 63
		applyMoveList = new ArrayList<GenericMove>();
		applyMoveList.add(new GenericMove("b3b4"));
		applyMoveList.add(new GenericMove("a1a2"));
		classUnderTest.createPositionFromAnalyseCommand(new EngineAnalyzeCommand(new GenericBoard("8/8/8/8/8/pk6/8/K7 b - - 5 62"), applyMoveList));
		classUnderTest.sendBestMoveCommand(new SearchResult(Move.valueOfBit(BitBoard.b4, Piece.BLACK_KING, BitBoard.a4, Piece.NONE)));
		// White move 64
		applyMoveList = new ArrayList<GenericMove>();
		applyMoveList.add(new GenericMove("b3b4"));
		applyMoveList.add(new GenericMove("a1a2"));
		applyMoveList.add(new GenericMove("b4a4"));
		classUnderTest.createPositionFromAnalyseCommand(new EngineAnalyzeCommand(new GenericBoard("8/8/8/8/8/pk6/8/K7 b - - 5 62"), applyMoveList));
		classUnderTest.sendBestMoveCommand(new SearchResult(Move.valueOfBit(BitBoard.a2, Piece.WHITE_KING, BitBoard.b1, Piece.NONE)));
		//  Black move 64
		applyMoveList = new ArrayList<GenericMove>();
		applyMoveList.add(new GenericMove("b3b4"));
		applyMoveList.add(new GenericMove("a1a2"));
		applyMoveList.add(new GenericMove("b4a4"));
		applyMoveList.add(new GenericMove("a2b1"));
		classUnderTest.createPositionFromAnalyseCommand(new EngineAnalyzeCommand(new GenericBoard("8/8/8/8/8/pk6/8/K7 b - - 5 62"), applyMoveList));
		classUnderTest.sendBestMoveCommand(new SearchResult(Move.valueOfBit(BitBoard.a4, Piece.BLACK_KING, BitBoard.a5, Piece.NONE)));
		/* The positions are getting double incremented in test_avoidDraw_lichess_hash_table_draw_kpK_rook_pawn_alt
		 * because Eubos is calculating moves for both black and white. Therefore we double count, once when the 
		 * bestmove is sent, the again on the next ply when the analyse is received!
		 */
	}
	
	@Test
	public void test_WAC_086_position() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/p7/1ppk1n2/5ppp/P1PP4/2P1K1P1/5N1P/8 b - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"11"+CMD_TERMINATOR, BEST_PREFIX+"f6g4"+CMD_TERMINATOR));
		performTest(8000);
	}
	
	@Test
	public void test_WAC_100_position() throws InterruptedException, IOException {
		if (EubosEngineMain.ENABLE_TEST_SUITES) {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/k1b5/P4p2/1Pp2p1p/K1P2P1P/8/3B4/8 w - - 0 1"+CMD_TERMINATOR, null));
		String [] acceptable_best_move_commands = {
				BEST_PREFIX+"b5b6"+CMD_TERMINATOR,
				BEST_PREFIX+"d2e3"+CMD_TERMINATOR};
		commands.add(new MultipleAcceptableCommandPair(GO_DEPTH_PREFIX+"12"+CMD_TERMINATOR, acceptable_best_move_commands));
		performTest(15000);
		// 21st March 2023, 2mins+ for 22 ply to find b6+, which is winning move
		}
	}
	
	@Test
	public void test_WAC_230_position() throws InterruptedException, IOException {
		if (EubosEngineMain.ENABLE_TEST_SUITES) {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"2b5/1r6/2kBp1p1/p2pP1P1/2pP4/1pP3K1/1R3P2/8 b - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"25"+CMD_TERMINATOR, BEST_PREFIX+"b7b4"+CMD_TERMINATOR));
		performTest(15000);
		// 21st March 2023, takes SF 25 plies to see Rb4, 3 to 4 seconds. Eubos is going over 19 minutes just to get to 19 ply - no idea
		}
	}
	
	@Test
	public void test_WAC_243_position() throws InterruptedException, IOException {
		if (EubosEngineMain.ENABLE_TEST_SUITES) {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"1r3r1k/3p4/1p1Nn1R1/4Pp1q/pP3P1p/P7/5Q1P/6RK w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"12"+CMD_TERMINATOR, BEST_PREFIX+"f2e2"+CMD_TERMINATOR));
		performTest(15000);
		}
	}
	
	@Test
	public void test_WAC_252_position() throws InterruptedException, IOException {
		if (EubosEngineMain.ENABLE_TEST_SUITES) {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"1rb1r1k1/p1p2ppp/5n2/2pP4/5P2/2QB4/qNP3PP/2KRB2R b - - 0 1"+CMD_TERMINATOR, null));
		String [] acceptable_best_move_commands = {
				BEST_PREFIX+"e8e2"+CMD_TERMINATOR,
				BEST_PREFIX+"c8g4"+CMD_TERMINATOR};
		commands.add(new MultipleAcceptableCommandPair(GO_DEPTH_PREFIX+"12"+CMD_TERMINATOR, acceptable_best_move_commands));
		performTest(15000);
		}
	}
	
	@Test
	public void test_WAC_264_position() throws InterruptedException, IOException {
		if (EubosEngineMain.ENABLE_TEST_SUITES) {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"r2r2k1/1R2qp2/p5pp/2P5/b1PN1b2/P7/1Q3PPP/1B1R2K1 b - - 0 1"+CMD_TERMINATOR, null));
		String [] acceptable_best_move_commands = {
				BEST_PREFIX+"a8b8"+CMD_TERMINATOR,
				BEST_PREFIX+"e7e5"+CMD_TERMINATOR};
		commands.add(new MultipleAcceptableCommandPair(GO_DEPTH_PREFIX+"12"+CMD_TERMINATOR, acceptable_best_move_commands));
		performTest(15000);
		}
	}
	
	@Test
	public void test_WAC_283_position() throws InterruptedException, IOException {
		if (EubosEngineMain.ENABLE_TEST_SUITES) {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"3q1rk1/4bp1p/1n2P2Q/3p1p2/6r1/Pp2R2N/1B4PP/7K w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"12"+CMD_TERMINATOR, BEST_PREFIX+"h3g5"+CMD_TERMINATOR));
		performTest(15000);
		}
	}
	
	@Test
	public void test_aspiration_failure_processing() throws IOException, InterruptedException {
		if (EubosEngineMain.ENABLE_TEST_SUITES) {
		setupEngine();
		commands.add(new CommandPair(POS_START_PREFIX+"moves e2e4 c7c6 d2d4 d7d5 b1d2 d5e4 d2e4"+
		" b8d7 g1f3 g8f6 e4g3 g7g6 f1c4 f8g7 c1g5 d7b6 c4b3 h7h6 g5d2 a7a5 a2a4 h6h5 f3e5 b6d5 c2c4 d5c7 g3e2"+
	    " c6c5 d4c5 f6e4 d2f4 d8d1 a1d1 c7e6 e5d3 e4c5 d3c5 e6c5 b3c2 g7b2 f4e3 b2a3 e2c3 a3b4 e1g1 b4c3 e3c5"+
		" c8d7 c5d4 c3d4 d1d4 d7c6 f1d1 e8g8 c2b3 e7e6 d4d6 f8c8 f2f4 c6e8 g1f2 c8c6 g2g3 c6d6 d1d6 e8c6 c4c5"+
	    " a8b8 f2e3 b8e8 h2h4 e8a8 e3d4 a8b8 d4e3 b8e8 e3d3 e8f8 d3e3 f8b8 e3d4 g8h7 b3c2 b8c8 c2b3 h7h8 d4e5"+
		" h8g7 e5d4 g7h7 d4c3 c8e8 c3d3 e8a8 d3d4 h7g8 b3c2 a8c8 c2b3 g8g7 d4e3 g7h7 e3d4 h7h8 d4e5 c6d5 b3d5"+
	    " c8c5 e5d4 c5d5 d6d5 e6d5 d4d5 h8g7 d5c5 g7f6 c5b6 f6e6 b6b7 e6e7 b7b6 e7d6 b6b5"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"25"+CMD_TERMINATOR, BEST_PREFIX+"d6d5"+CMD_TERMINATOR));
		performTest(1000000000);
		}
	}
	
	@Test
	public void test_lichess_pi_crash() throws IOException, InterruptedException {
		setupEngine();
		commands.add(new CommandPair(POS_START_PREFIX+"moves g2g3 e7e5 c2c4 f8c5 e2e3 b8c6 b1c3 d7d6 a2a3 g8f6"+
		" b2b4 c5b6 c3d5 f6d5 c4d5 c6e7 d1f3 c7c6 d5c6 b7c6 c1b2 c8e6 a3a4 e7d5 b4b5 d5b4 a1c1 e6b3 b5c6 e8g8 f1e2"+
		" d8c7 f3f5 b3a4 e2f3 g7g6 f5e4 a7a5 e4c4 a8c8 b2a3 a4c6 f3c6 b4c6 c4e4 f8d8 g1f3 d6d5 e4a4 d8e8 a4b5 e5e4"+
		" a3b4 a5b4 f3g5 d5d4 h2h4 h7h6 g5h3 e8e5 b5a4 g6g5 h4g5 h6g5 a4d1 c7d7 d1h5 f7f6 h3g1 d7g7 h5g4 f6f5 g4d1"+
		" g7f7 d1h5 f7g7 h5d1 g7f7 d1h5 f7h5 h1h5 g5g4 g1e2 d4e3 d2e3 b4b3 e1f1 b6c5 f1g1 c8d8 h5h6 c6e7 c1b1 d8d2"+
		" e2f4 b3b2 h6a6 d2c2 a6a8 g8f7 a8d8 e7d5 b1b2 c2b2 d8d5 e5d5 f4d5 f7e6 d5f4 e6d6 g1f1 c5b4 f4e2 d6e5 e2f4"+
		" b4c3 f4e2 c3a5 e2d4 b2b1 f1g2 a5e1 d4e2 b1b2 g2f1 e1b4 e2f4 b2a2 f4e2 a2a1 f1g2 a1a2 g2f1 a2a1 f1g2 b4e1"+
		" g2g1 e1d2 g1g2 a1a2 g2f1 d2b4 e2d4 b4c3 d4e2 c3b2 f1g1 e5d5 e2f4 d5e5 f4e2 e5d5 e2f4 d5d6 g1g2 b2c3 g2f1"+
		" a2b2 f4e2 c3e5 e2f4 e5c3 f4e2 c3f6 e2f4 d6e5 f1g2 e5d6 g2f1 b2a2 f1g2 f6e5 f4g6 e5c3 g2f1 a2b2 g6f4 b2a2"+
		" f4g6 c3b4 g6h4 d6e5 h4g6 e5d5 g6f4 d5d6 f4e2 d6e6 e2d4 e6f6 d4e2 b4d2 e2f4 f6e5 f4e2 a2a1 f1g2 d2e1 g2g1"+
		" e5d5 g1f1 e1c3 f1g2 c3e5 e2f4 e5f4 g3f4 d5d6 g2g3 a1a2 g3g2 a2b2 g2g3 d6d5 g3g2 b2a2 g2g3 a2b2 g3g2 b2a2"+
		" g2g3 a2c2 g3g2 d5d6 g2g3 d6d5 g3g2 c2b2 g2g3 d5d6 g3g2 d6d5 g2g3 d5e6 g3g2 e6d7 g2g1 d7d6 g1g2 d6e6 g2g3"+
		" b2a2 g3g2 e6d6 g2g1 d6d5 g1g2 a2e2 g2f1 e2a2 f1g2 d5d6"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"3"+CMD_TERMINATOR, BEST_PREFIX+"g2g1"+CMD_TERMINATOR));
		performTest(500);
	}
	 
	@Test
	@Ignore // Takes a 17 ply search with LMR on to find the correct move? Check evaluation
	public void test_try_to_draw_KBB_kr_EG() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"7r/8/7B/5p1P/1pB5/6P1/2k4K/8 w - - 3 88"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"17"+CMD_TERMINATOR, BEST_PREFIX+"h6g7"+CMD_TERMINATOR));
		performTest(18000);
	}
	
	@Test
	public void test_endgame_e() throws IllegalNotationException, IOException, InterruptedException {
		// Fine: problem 25
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"6k1/7p/5P1K/8/8/8/7P/8 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"19"+CMD_TERMINATOR, BEST_PREFIX+"h6g5"+CMD_TERMINATOR));
		performTest(3000);	
	}
	
	@Test
	public void test_endgame_k() throws IllegalNotationException, IOException, InterruptedException {
		// Fine: problem 26
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/2k5/p1P5/P1K5/8/8/8/8 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"22"+CMD_TERMINATOR, BEST_PREFIX+"c5d5"+CMD_TERMINATOR));
		performTest(1000);
	}
	
	@Test
	public void test_endgame_o() throws IllegalNotationException, IOException, InterruptedException {
		// Fine: problem 29
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"4k3/4Pp2/5P2/4K3/8/8/8/8 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"17"+CMD_TERMINATOR, BEST_PREFIX+"e5f5"+CMD_TERMINATOR));
		performTest(1000);
	}
	
	@Test
	public void test_endgame_a() throws IllegalNotationException, IOException, InterruptedException {
		// Fine: problem 51
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/8/2pp3k/8/1P1P3K/8/8/8 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"17"+CMD_TERMINATOR, BEST_PREFIX+"d4d5"+CMD_TERMINATOR));
		performTest(4000);
	}
	
	@Test
	public void test_endgame_b() throws IllegalNotationException, IOException, InterruptedException {
		// Fine: problem 61
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/ppp5/8/PPP5/6kp/8/6KP/8 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"11"+CMD_TERMINATOR, BEST_PREFIX+"b5b6"+CMD_TERMINATOR));
		performTest(2000);		
	}
	
	@Test
	public void test_endgame_i() throws IllegalNotationException, IOException, InterruptedException {
		// Fine: problem 70
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/k7/3p4/p2P1p2/P2P1P2/8/8/K7 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"25"+CMD_TERMINATOR, BEST_PREFIX+"a1b1"+CMD_TERMINATOR));
		performTest(10000);
	}
	
	@Test
	public void test_endgame_Fine_80() throws IllegalNotationException, IOException, InterruptedException {
		// Fine: problem 80
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/8/1ppk4/p4pp1/P1PP2p1/2P1K1P1/7P/8 b - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"14"+CMD_TERMINATOR, BEST_PREFIX+"b6b5"+CMD_TERMINATOR));
		performTest(4000);
	}
	
	@Test
	public void test_endgame_Fine_53() throws IllegalNotationException, IOException, InterruptedException {
		// Fine: problem 53
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/8/3pkp2/8/8/3PK3/5P2/8 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"20"+CMD_TERMINATOR, BEST_PREFIX+"e3e4"+CMD_TERMINATOR));
		performTest(3000);	
	}
	
	@Test
	public void test_endgame_Fine_58() throws IllegalNotationException, IOException, InterruptedException {
		// Fine: problem 58
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/8/2ppk3/8/2PPK3/2P5/8/8 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"15"+CMD_TERMINATOR, BEST_PREFIX+"d4d5"+CMD_TERMINATOR));
		performTest(1000);	
	}
	
	@Test
	public void test_endgame_Fine_67() throws IllegalNotationException, IOException, InterruptedException {
		// Fine: problem 67
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/2p5/3k4/1p1p1K2/8/1P1P4/2P5/8 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"16"+CMD_TERMINATOR, BEST_PREFIX+"b3b4"+CMD_TERMINATOR));
		performTest(2000);		
	}
	
	@Test
	public void test_endgame_Fine_76() throws IllegalNotationException, IOException, InterruptedException {
		// Fine: problem 76
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/8/p6p/1p3kp1/1P6/P4PKP/5P2/8 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"7"+CMD_TERMINATOR, BEST_PREFIX+"f3f4"+CMD_TERMINATOR));
		performTest(1000);	
	}
	
	@Test
	public void test_endgame_Fine_42() throws IllegalNotationException, IOException, InterruptedException {
		// Fine: problem 42
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/5p2/8/4K1P1/5Pk1/8/8/8 w - - 2 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"16"+CMD_TERMINATOR, BEST_PREFIX+"e5e4"+CMD_TERMINATOR));
		performTest(1000);		
	}
	
	@Test
	public void test_endgame_Fine_90() throws IllegalNotationException, IOException, InterruptedException {
		// Fine: problem 90
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/7p/2k1Pp2/pp1p2p1/3P2P1/4P3/P3K2P/8 w - - 2 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"13"+CMD_TERMINATOR, BEST_PREFIX+"e3e4"+CMD_TERMINATOR));
		performTest(2000);		
	}
	
	@Test
	public void test_endgame_Fine_100A() throws IllegalNotationException, IOException, InterruptedException {
		// Fine: problem 100A
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/6p1/3k1p2/2p2Pp1/2P1p1P1/1P4P1/4K3/8 w - - 2 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"21"+CMD_TERMINATOR, BEST_PREFIX+"e2f2"+CMD_TERMINATOR));
		performTest(4000);		
	}
	
	@Test
	public void test_endgame_Fine_66() throws IllegalNotationException, IOException, InterruptedException {
		// Fine: problem 66
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/1k3ppp/8/3K4/7P/5PP1/8/8 w - - 2 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"14"+CMD_TERMINATOR, BEST_PREFIX+"d5d6"+CMD_TERMINATOR));
		performTest(2000);		
	}
	 
	@Test
	public void test_endgame_d() throws IllegalNotationException, IOException, InterruptedException {
		// Fine: problem 82
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"8/pp5p/8/PP2k3/2P2pp1/3K4/6PP/8 w - - 1 10"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"12"+CMD_TERMINATOR, BEST_PREFIX+"c4c5"+CMD_TERMINATOR));
		performTest(3000);		
	}
	
	@Test
	public void test_mateInFour() throws IllegalNotationException, IOException, InterruptedException {
		// chess.com Problem ID: 0102832
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"r1r3k1/pb1p1p2/1p2p1p1/2pPP1B1/1nP4Q/1Pq2NP1/P4PBP/b2R2K1 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"7"+CMD_TERMINATOR, BEST_PREFIX+"g5f6"+CMD_TERMINATOR));
		performTest(1000);
	}
	
	@Test
	public void test_findMove_mateInTwo() throws IllegalNotationException, IOException, InterruptedException {
		// chess.com Problem ID: 0551140
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"rnbq1rk1/p4ppN/4p2n/1pbp4/8/2PQP2P/PPB2PP1/RNB1K2R w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"5"+CMD_TERMINATOR, BEST_PREFIX+"h7f6"+CMD_TERMINATOR));
		performTest(1000);
	}
	
	@Test
	public void test_findMove_mateInThree() throws IllegalNotationException, IOException, InterruptedException {
		setupEngine();
		commands.add(new CommandPair(POS_FEN_PREFIX+"2kr3r/ppp2ppp/8/8/1P5P/1K1b1P1N/P3P1P1/4qB1R b - - 3 24"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR, BEST_PREFIX+"e1b1"+CMD_TERMINATOR));
		performTest(1000);
	}
}
