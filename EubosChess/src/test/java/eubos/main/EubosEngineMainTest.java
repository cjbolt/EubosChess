package eubos.main;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.commands.EngineAnalyzeCommand;
import com.fluxchess.jcpi.commands.EngineNewGameCommand;
import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.models.GenericBoard;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.Board;
import eubos.board.Piece;
import eubos.position.Move;
import eubos.position.Position;
import eubos.score.PositionEvaluator;
import eubos.search.transposition.Transposition;

public class EubosEngineMainTest {

	private EubosEngineMain classUnderTest;
	private Thread eubosThread;
	
	// Command Lists emptied in main test loop.
	public class commandPair {
		private String in;
		private String out;
		public commandPair(String input, String output) {
			in = input;
			out = output;
		}
		public String getIn() {
			return in;
		}
		public String getOut() {
			return out;
		}
	}
	
	private ArrayList<commandPair> commands = new ArrayList<commandPair>();
	
	// Test infrastructure to allow pushing commands into Eubos and sniffing them out.
	private PipedWriter inputToEngine;
	private final ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
	
	// Command building blocks
	private static final String CMD_TERMINATOR = System.lineSeparator();
	private static final String POS_FEN_PREFIX = "position fen ";
	private static final String GO_DEPTH_PREFIX = "go depth ";
	//private static final String GO_WTIME_PREFIX = "go wtime ";
	//private static final String GO_BTIME_PREFIX = "go btime ";
	private static final String GO_TIME_PREFIX = "go movetime ";
	private static final String BEST_PREFIX = "bestmove ";
	
	// Whole Commands
	// Inputs
	private static final String UCI_CMD = "uci"+CMD_TERMINATOR;
	private static final String ISREADY_CMD = "isready"+CMD_TERMINATOR;
	private static final String NEWGAME_CMD = "ucinewgame"+CMD_TERMINATOR;
	//private static final String GO_INF_CMD = "go infinite"+CMD_TERMINATOR;
	private static final String QUIT_CMD = "quit"+CMD_TERMINATOR;
	// Outputs
	private static final String ID_NAME_CMD = String.format("id name Eubos %d.%d%s", 
			EubosEngineMain.EUBOS_MAJOR_VERSION, EubosEngineMain.EUBOS_MINOR_VERSION, CMD_TERMINATOR);
	private static final String ID_AUTHOR_CMD = "id author Chris Bolt"+CMD_TERMINATOR;
	private static final String OPTION_HASH = "option name Hash type spin default 1310 min 32 max 4000"+CMD_TERMINATOR;
	private static final String OPTION_THREADS = String.format(
			"option name Threads type spin default %s min 1 max %s%s",
			Math.max(1, Runtime.getRuntime().availableProcessors()-2),
			Runtime.getRuntime().availableProcessors(), CMD_TERMINATOR);
	private static final String UCI_OK_CMD = "uciok"+CMD_TERMINATOR;
	private static final String READY_OK_CMD = "readyok"+CMD_TERMINATOR;
	
	private static final int sleep_50ms = 50;
	
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
		commands.add(new commandPair(POS_FEN_PREFIX+"k1K5/b7/R7/1P6/1n6/8/8/8 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"4"+CMD_TERMINATOR,BEST_PREFIX+"b5b6"+CMD_TERMINATOR));
		performTest(1000);
	}
	
	@Test
	public void test_mateInTwo_onTime() throws InterruptedException, IOException {
		setupEngine();
		// Setup Commands specific to this test
		commands.add(new commandPair(POS_FEN_PREFIX+"k1K5/b7/R7/1P6/1n6/8/8/8 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_TIME_PREFIX+"1000"+CMD_TERMINATOR,BEST_PREFIX+"b5b6"+CMD_TERMINATOR));
		performTest(1000);
	}
	
	@Test
	public void test_mateInTwo_fromBlack() throws InterruptedException, IOException {
		setupEngine();
		// Setup Commands specific to this test
		commands.add(new commandPair(POS_FEN_PREFIX+"k1K5/b7/R7/1P6/1n6/8/8/8 w - - 0 1 moves b5b6"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"4"+CMD_TERMINATOR,BEST_PREFIX+"b4a6"+CMD_TERMINATOR));
		performTest(1000);
	}	
	
	@Test
	public void test_mateInOne() throws InterruptedException, IOException {
		setupEngine();
		// Setup Commands specific to this test
		commands.add(new commandPair(POS_FEN_PREFIX+"5r1k/p2R4/1pp2p1p/8/5q2/3Q1bN1/PP3P2/6K1 w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"2"+CMD_TERMINATOR,BEST_PREFIX+"d3h7"+CMD_TERMINATOR));
		performTest(1000);
	}
	
	@SuppressWarnings("unused")
	@Test
	public void test_infoMessageSending_clearsPreviousPvMoves() throws InterruptedException, IOException {
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) {
			String expectedOutput;
			if (Board.ENABLE_PIECE_LISTS && PositionEvaluator.ENABLE_DYNAMIC_POSITIONAL_EVALUATION && PositionEvaluator.ENABLE_KING_SAFETY_EVALUATION && EubosEngineMain.ENABLE_QUIESCENCE_CHECK) {
				expectedOutput = "info depth 1 seldepth 6 score cp -17 pv d7e5 f3e5 c7c2 e5f7 hashfull 0 nps 0 time 0 nodes 24"+CMD_TERMINATOR+
						"info depth 1 seldepth 6 score cp 145 pv c7c2 d4a7 hashfull 0 nps 441 time 102 nodes 38"+CMD_TERMINATOR+
	                    "info depth 2 seldepth 6 score cp 67 pv c7c2 e1g1 d7e5 hashfull 0 nps 1836 time 116 nodes 224"+CMD_TERMINATOR
	                    +BEST_PREFIX+"c7c2";
			} else if (Board.ENABLE_PIECE_LISTS && PositionEvaluator.ENABLE_KING_SAFETY_EVALUATION && !PositionEvaluator.ENABLE_DYNAMIC_POSITIONAL_EVALUATION) {
				expectedOutput = "info depth 1 seldepth 4 score cp -141 pv d7e5 f3e5 c7e5 hashfull 0 nps 500 time 14 nodes 7"+CMD_TERMINATOR+
                        "info depth 1 seldepth 4 score cp 178 pv c7c2 hashfull 0 nps 122 time 98 nodes 12"+CMD_TERMINATOR+
                        "info depth 2 seldepth 0 score cp 38 pv c7c2 d4d5 hashfull 0 nps 803 time 102 nodes 82"+CMD_TERMINATOR
                        +BEST_PREFIX+"c7c2";
			} else if (Board.ENABLE_PIECE_LISTS && !PositionEvaluator.ENABLE_KING_SAFETY_EVALUATION &&!PositionEvaluator.ENABLE_DYNAMIC_POSITIONAL_EVALUATION) {
				expectedOutput = "info depth 1 seldepth 4 score cp -160 pv d7e5 f3e5 c7e5 hashfull 0 nps 538 time 13 nodes 7"+CMD_TERMINATOR+
                        "info depth 1 seldepth 4 score cp 155 pv c7c2 hashfull 0 nps 126 time 95 nodes 12"+CMD_TERMINATOR+
                        "info depth 2 seldepth 0 score cp 15 pv c7c2 d4d5 hashfull 0 nps 820 time 100 nodes 82"+CMD_TERMINATOR
                        +BEST_PREFIX+"c7c2";
			} else if (!EubosEngineMain.ENABLE_QUIESCENCE_CHECK) {
				expectedOutput = "info depth 1 seldepth 0 score cp 171 pv d7e5 hashfull 0 nps 83 time 12 nodes 1"+CMD_TERMINATOR+
                        "info depth 2 seldepth 0 score cp -149 pv d7e5 f3e5 hashfull 0 nps 757 time 103 nodes 78"+CMD_TERMINATOR+
                        "info depth 2 seldepth 0 score cp 30 pv c7c2 d4d5 hashfull 0 nps 1161 time 105 nodes 122"+CMD_TERMINATOR
                        +BEST_PREFIX+"c7c2";
			} else {
				expectedOutput = "info depth 1 seldepth 4 score cp -149 pv d7e5 f3e5 c7e5 nps 0 time 0 nodes 7"+CMD_TERMINATOR+
                        "info depth 1 seldepth 4 score cp 155 pv c7c2 hashfull 0 nps 130 time 92 nodes 12"+CMD_TERMINATOR+
                        "info depth 2 seldepth 0 score cp 36 pv c7c2 d4d5 h7h5 nps 0 time 0 nodes 85"+CMD_TERMINATOR
                        +BEST_PREFIX+"c7c2";
			}
			setupEngine();
			// Setup Commands specific to this test
			commands.add(new commandPair(POS_FEN_PREFIX+"r1b1kb1r/ppqnpppp/8/3pP3/3Q4/5N2/PPP2PPP/RNB1K2R b KQkq - 2 8"+CMD_TERMINATOR, null));
			commands.add(new commandPair(GO_DEPTH_PREFIX+"2"+CMD_TERMINATOR, removeTimeFieldsFromUciInfoMessage(expectedOutput)+CMD_TERMINATOR));
			
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
		commands.add(new commandPair(POS_FEN_PREFIX+"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 moves "
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
		commands.add(new commandPair(GO_DEPTH_PREFIX+"2"+CMD_TERMINATOR,BEST_PREFIX+"f6e5"+CMD_TERMINATOR)); // i.e not h2h3
		performTest(5000);
	}
	
	@Test
	public void test_game_position_takes_draw() throws InterruptedException, IOException {
		setupEngine();
		// Setup Commands specific to this test, checking to achieve draw by repetition..
		commands.add(new commandPair(POS_FEN_PREFIX+"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 moves "
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
		commands.add(new commandPair(GO_DEPTH_PREFIX+"1"+CMD_TERMINATOR,BEST_PREFIX+"c8c7"+CMD_TERMINATOR));
		performTest(5000);
	}
	
	@Test
	public void test_achieves_draw_black_repeated_check() throws InterruptedException, IOException {
		setupEngine();
		// Setup Commands specific to this test
		commands.add(new commandPair(POS_FEN_PREFIX+"7q/1P6/8/8/8/8/2k3PQ/7K b - - 0 42"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"h8a1"+CMD_TERMINATOR));
		commands.add(new commandPair(POS_FEN_PREFIX+"7q/1P6/8/8/8/8/2k3PQ/7K b - - 0 42 moves h8a1 h2g1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"a1h8"+CMD_TERMINATOR));
		commands.add(new commandPair(POS_FEN_PREFIX+"7q/1P6/8/8/8/8/2k3PQ/7K b - - 0 42 moves h8a1 h2g1 a1h8 g1h2"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"h8a1"+CMD_TERMINATOR));
		commands.add(new commandPair(POS_FEN_PREFIX+"7q/1P6/8/8/8/8/2k3PQ/7K b - - 0 42 moves h8a1 h2g1 a1h8 g1h2 h8a1 h2g1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"a1h8"+CMD_TERMINATOR));
		commands.add(new commandPair(POS_FEN_PREFIX+"7q/1P6/8/8/8/8/2k3PQ/7K b - - 0 42 moves h8a1 h2g1 a1h8 g1h2 h8a1 h2g1 a1h8 g1h2"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"h8a1"+CMD_TERMINATOR));
		performTest(500);
	}
	
	@Test
	public void test_achieves_draw_white_repeated_check() throws InterruptedException, IOException {
		setupEngine();
		// Setup Commands specific to this test
		commands.add(new commandPair(POS_FEN_PREFIX+"7k/2K3pq/8/8/8/8/1p6/7Q w - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"h1a8"+CMD_TERMINATOR));
		commands.add(new commandPair(POS_FEN_PREFIX+"7k/2K3pq/8/8/8/8/1p6/7Q w - - 0 1 moves h1a8 h7g8"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"a8h1"+CMD_TERMINATOR));
		commands.add(new commandPair(POS_FEN_PREFIX+"7k/2K3pq/8/8/8/8/1p6/7Q w - - 0 1 moves h1a8 h7g8 a8h1 g8h7"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"h1a8"+CMD_TERMINATOR));
		commands.add(new commandPair(POS_FEN_PREFIX+"7k/2K3pq/8/8/8/8/1p6/7Q w - - 0 1 moves h1a8 h7g8 a8h1 g8h7 h1a8 h7g8"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"a8h1"+CMD_TERMINATOR));
		commands.add(new commandPair(POS_FEN_PREFIX+"7k/2K3pq/8/8/8/8/1p6/7Q w - - 0 1 moves h1a8 h7g8 a8h1 g8h7 h1a8 h7g8 a8h1 g8h7"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"h1a8"+CMD_TERMINATOR));
		performTest(500);
	}
	
	@Test
	public void test_KQk_mate_in_7_NEW() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new commandPair(POS_FEN_PREFIX+"5Q2/6K1/8/3k4/8/8/8/8 w - - 1 113"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_TIME_PREFIX+"30000"+CMD_TERMINATOR, BEST_PREFIX+"f8b4"+CMD_TERMINATOR));
		performTestExpectMate(30000, 7);
	}
	
	@Test
	public void test_KQk_mated_in_6_NEW() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new commandPair(POS_FEN_PREFIX+"8/6K1/8/3k4/1Q6/8/8/8 b - - 1 1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"12"+CMD_TERMINATOR, BEST_PREFIX+"d5e5"+CMD_TERMINATOR));
		performTestExpectMate(15000, -6);
	} 
	
	@Test
	public void test_WAC009() throws InterruptedException, IOException {
		setupEngine();
		// 1
		commands.add(new commandPair(POS_FEN_PREFIX+"3q1rk1/p4pp1/2pb3p/3p4/6Pr/1PNQ4/P1PB1PP1/4RRK1 b - - 0 1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"d6h2"+CMD_TERMINATOR));
		// 2
		commands.add(new commandPair(POS_FEN_PREFIX+"3q1rk1/p4pp1/2pb3p/3p4/6Pr/1PNQ4/P1PB1PP1/4RRK1 b - - 0 1 moves d6h2 g1h1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"7"+CMD_TERMINATOR,BEST_PREFIX+"h2g3"+CMD_TERMINATOR));
		// 3
		commands.add(new commandPair(POS_FEN_PREFIX+"3q1rk1/p4pp1/2pb3p/3p4/6Pr/1PNQ4/P1PB1PP1/4RRK1 b - - 0 1 moves d6h2 g1h1 h2g3 h1g1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"h4h1"+CMD_TERMINATOR));
		// 4
		commands.add(new commandPair(POS_FEN_PREFIX+"3q1rk1/p4pp1/2pb3p/3p4/6Pr/1PNQ4/P1PB1PP1/4RRK1 b - - 0 1 moves d6h2 g1h1 h2g3 h1g1 h4h1 g1h1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"d8h4"+CMD_TERMINATOR));
		// 5
		commands.add(new commandPair(POS_FEN_PREFIX+"3q1rk1/p4pp1/2pb3p/3p4/6Pr/1PNQ4/P1PB1PP1/4RRK1 b - - 0 1 moves d6h2 g1h1 h2g3 h1g1 h4h1 g1h1 d8h4 h1g1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR,BEST_PREFIX+"h4h2"+CMD_TERMINATOR));

		performTest(15000);
	}
	
	@Test
	public void test_KRk_mate_in_11_NEW() throws InterruptedException, IOException {
		int mateDepth = 0;
		setupEngine();
		commands.add(new commandPair(POS_FEN_PREFIX+"8/8/8/3K1k2/8/8/8/7r b - - 5 111"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_TIME_PREFIX+"14000"+CMD_TERMINATOR, BEST_PREFIX+"h1d1"+CMD_TERMINATOR));
		mateDepth = 13;
		performTestExpectMate(14000, mateDepth);
	}
	
	 
	@Test
	public void test_mate_in_3_guardian3713() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new commandPair(POS_FEN_PREFIX+"8/2p5/P4p2/Q1N2k1P/2P2P2/3PK2P/5R2/2B2R2 w - - 1 1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR, BEST_PREFIX+"f2d2"+CMD_TERMINATOR));
		performTestExpectMate(4000, 3);
	}
	
	@Test
	public void test_tricky_endgame_position() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new commandPair(POS_FEN_PREFIX+"8/8/4kp1p/3pb1p1/P5P1/3KN1PP/8/8 b - - 5 57"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"12"+CMD_TERMINATOR, BEST_PREFIX+"h6h5"+CMD_TERMINATOR));
		// h6h5 loses, it is a terrible move, but that is what Eubos selects. We should go with Bxg3 according to stockfish
		/*
		 * FEN: 8/8/4kp1p/3pb1p1/P5P1/3KN1PP/8/8 b - - 5 57
		 * 
			Eubos Dev Version:
			 1/3	00:00	 3	166	+1.19	Bxg3 Nxd5
			 2/3	00:00	 34	2k	+1.05	Bxg3 a5
			 3/6	00:00	 267	9k	+0.51	Bxg3 Nf5 Bf2 Nxh6
			 3/9	00:00	 656	18k	+0.91	h5 gxh5 Bxg3 Nxd5
			 4/9	00:00	 1k	24k	+0.77	h5 gxh5 Bxg3 a5
			 5/11	00:00	 4k	60k	+0.87	h5 gxh5 Bxg3 a5 Ke5 Nxd5
			 6/11	00:00	 10k	124k	+0.73	h5 gxh5 Bxg3 a5 Ke5 a6
			 7/14	00:00	 30k	235k	+0.73	h5 gxh5 Bxg3 h6 Be5 h7 f5 h8Q
			 8/13	00:00	 83k	340k	+0.59	h5 gxh5 Bxg3 h6 Be5 h7 f5 a5
			 9/15	00:00	 246k	440k	+0.72	h5 gxh5 Bxg3 a5 f5 a6 Bb8 Kd4 Ba7+ Kd3 Bxe3
			 10/17	00:01	 655k	604k	+0.80	h5 gxh5 Bxg3 a5 f5 a6 Bb8 h6 f4 h7 fxe3
			 11/19	00:01	 1,772k	941k	+0.58	h5 gxh5 Bxg3 Nc2 Be5 a5 Bc7 Nd4+ Ke5 h6 f5 a6
			 12/21	00:03	 4,727k	1,316k	+0.53	h5 gxh5 Bxg3 Nc2 f5 Nd4+ Kf6 h6 Kg6 Nc6 Kxh6 Kd4 f4 Kxd5
			 13/22	00:07	 11,217k	1,559k	+0.03	h5 gxh5
			 13/23	00:16	 26,542k	1,622k	+0.34	Bxg3 a5 Bb8 Nf5 h5 Nd4+ Kf7 gxh5 Ba7
			 14/24	00:28	 47,868k	1,693k	+0.25	Bxg3 a5 Bb8 Nf5 Ba7 a6 Bb6 Nxh6 Ke5 Nf7+
			 15/27	01:54	 184,310k	1,609k	+0.25	Bxg3 a5 Bb8 Nf5 Ba7 a6 Bb6 Nxh6 Ke5 Nf5 d4 Ng3 Ke6 Ke4 Kd6
			 16/29	04:56	 475,978k	1,604k	 0.00	Bxg3 a5 Bb8 Nf5 Ke5 a6 h5 Nd4 hxg4 hxg4 Kd6 Nb5+ Ke5 Nd4
			 17/30	18:13	 1,728,005k	1,581k	 0.00	Bxg3 a5 Bb8 Nf5 Kd7 Nxh6 Ba7 Nf5 Bc5 Nd4 Kc7 Nb5+ Kc6 Nd4+ Kc7
			*/
		performTest(15000);
	}
	
	@Test
	public void test_hash_issue_losing_position() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new commandPair(POS_FEN_PREFIX+"3r2k1/5p2/7p/3R2p1/p7/1q1Q1PP1/7P/3R2K1 b - - 1 42"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"8"+CMD_TERMINATOR, BEST_PREFIX+"b3b6"+CMD_TERMINATOR));

		testOutput.flush();
		inputToEngine.flush();
		int commandNumber = 1;
		for (commandPair currCmdPair: commands) {
			String inputCmd = currCmdPair.getIn();
			String expectedOutput = currCmdPair.getOut();
			String parsedCmd= "";
			// Pass command to engine
			if (inputCmd != null) {
				if (inputCmd.startsWith("go")) {
					Thread.sleep(sleep_50ms);
					// Seed hash table with problematic hash
					long problemHash = classUnderTest.rootPosition.getHash();
					EubosEngineMain.logger.info(String.format("*************** using hash code %d", problemHash));
					classUnderTest.hashMap.putTransposition(
							problemHash,
							new Transposition((byte)6, (short)0, (byte)1, Move.valueOf(Position.b3, Piece.BLACK_QUEEN, Position.d1, Piece.WHITE_ROOK), null));
				}
				inputToEngine.write(inputCmd);
				inputToEngine.flush();
				EubosEngineMain.logger.info(String.format("************* %s", inputCmd));
			}
			// Test expected command was received
			if (expectedOutput != null) {
				boolean received = false;
				int timer = 0;
				boolean accumulate = false;
				String recievedCmd = "";
				// Receive message or wait for timeout to expire.
				while (!received && timer<20000) {
					// Give the engine thread some CPU time
					Thread.sleep(sleep_50ms);
					timer += sleep_50ms;
					testOutput.flush();
					if (accumulate) {
						recievedCmd += testOutput.toString();
					} else {
						recievedCmd = testOutput.toString();
					}
					if (recievedCmd != null && !recievedCmd.isEmpty()) {
						System.err.println(recievedCmd);
						testOutput.reset();
						// Ignore any line starting with info, if not checking infos
					    parsedCmd = parseReceivedCommandString(recievedCmd, false);
					    if (!parsedCmd.isEmpty()) { // want to use isBlank(), but that is Java 11 only.
							if (parsedCmd.equals(expectedOutput)) {
								received = true;
								accumulate = false;
							} else if (expectedOutput.startsWith(parsedCmd)){
								accumulate = true;
							} else {
								EubosEngineMain.logger.info(String.format("parsed '%s' != '%s'", parsedCmd, expectedOutput));
								accumulate = false;
							}
					    }
					}
				}
				if (!received) {
					fail(inputCmd + expectedOutput + "command that failed " + (commandNumber-3));
				}
				commandNumber++;
			} else {
				Thread.sleep(sleep_50ms);
			}
		}
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
		classUnderTest.sendBestMoveCommand(new ProtocolBestMoveCommand(new GenericMove("b3b4"), null));
		// White move 63
		applyMoveList = new ArrayList<GenericMove>();
		applyMoveList.add(new GenericMove("b3b4"));
		classUnderTest.createPositionFromAnalyseCommand(new EngineAnalyzeCommand(new GenericBoard("8/8/8/8/8/pk6/8/K7 b - - 5 62"), applyMoveList));
		classUnderTest.sendBestMoveCommand(new ProtocolBestMoveCommand(new GenericMove("a1a2"), null));
		// Black move 63
		applyMoveList = new ArrayList<GenericMove>();
		applyMoveList.add(new GenericMove("b3b4"));
		applyMoveList.add(new GenericMove("a1a2"));
		classUnderTest.createPositionFromAnalyseCommand(new EngineAnalyzeCommand(new GenericBoard("8/8/8/8/8/pk6/8/K7 b - - 5 62"), applyMoveList));
		classUnderTest.sendBestMoveCommand(new ProtocolBestMoveCommand(new GenericMove("b4a4"), null));
		// White move 64
		applyMoveList = new ArrayList<GenericMove>();
		applyMoveList.add(new GenericMove("b3b4"));
		applyMoveList.add(new GenericMove("a1a2"));
		applyMoveList.add(new GenericMove("b4a4"));
		classUnderTest.createPositionFromAnalyseCommand(new EngineAnalyzeCommand(new GenericBoard("8/8/8/8/8/pk6/8/K7 b - - 5 62"), applyMoveList));
		classUnderTest.sendBestMoveCommand(new ProtocolBestMoveCommand(new GenericMove("a2b1"), null));
		//  Black move 64
		applyMoveList = new ArrayList<GenericMove>();
		applyMoveList.add(new GenericMove("b3b4"));
		applyMoveList.add(new GenericMove("a1a2"));
		applyMoveList.add(new GenericMove("b4a4"));
		applyMoveList.add(new GenericMove("a2b1"));
		classUnderTest.createPositionFromAnalyseCommand(new EngineAnalyzeCommand(new GenericBoard("8/8/8/8/8/pk6/8/K7 b - - 5 62"), applyMoveList));
		classUnderTest.sendBestMoveCommand(new ProtocolBestMoveCommand(new GenericMove("a4a5"), null));
		/* The positions are getting double incremented in test_avoidDraw_lichess_hash_table_draw_kpK_rook_pawn_alt
		 * because Eubos is calculating moves for both black and white. Therefore we double count, once when the 
		 * bestmove is sent, the again on the next ply when the analyse is received!
		 */
	}
	
	private void performTest(int timeout) throws IOException, InterruptedException {
		performTest(timeout, false);
	}

	private void performTest(int timeout, boolean checkInfoMsgs) throws IOException, InterruptedException {
		testOutput.flush();
		inputToEngine.flush();
		int commandNumber = 1;
		for (commandPair currCmdPair: commands) {
			String inputCmd = currCmdPair.getIn();
			String expectedOutput = currCmdPair.getOut();
			String parsedCmd= "";
			// Pass command to engine
			if (inputCmd != null) {
				inputToEngine.write(inputCmd);
				inputToEngine.flush();
			}
			// Test expected command was received
			if (expectedOutput != null) {
				boolean received = false;
				int timer = 0;
				boolean accumulate = false;
				String recievedCmd = "";
				// Receive message or wait for timeout to expire.
				while (!received && timer<timeout) {
					// Give the engine thread some CPU time
					Thread.sleep(sleep_50ms);
					timer += sleep_50ms;
					testOutput.flush();
					if (accumulate) {
						recievedCmd += testOutput.toString();
					} else {
						recievedCmd = testOutput.toString();
					}
					if (recievedCmd != null && !recievedCmd.isEmpty()) {
						System.err.println(recievedCmd);
						testOutput.reset();
						// Ignore any line starting with info, if not checking infos
					    parsedCmd = parseReceivedCommandString(recievedCmd, checkInfoMsgs);
					    if (!parsedCmd.isEmpty()) { // want to use isBlank(), but that is Java 11 only.
							if (parsedCmd.equals(expectedOutput)) {
								received = true;
								accumulate = false;
							} else if (expectedOutput.startsWith(parsedCmd)){
								accumulate = true;
							} else {
								EubosEngineMain.logger.info(String.format("parsed '%s' != '%s'", parsedCmd, expectedOutput));
								accumulate = false;
							}
					    }
					}
				}
				if (!received) {
					fail(inputCmd + expectedOutput + "command that failed " + (commandNumber-3));
				}
				commandNumber++;
			} else {
				Thread.sleep(sleep_50ms);
			}
		}
	}
	
	private void performTestExpectMate(int timeout, int mateInX) throws IOException, InterruptedException {
		boolean mateDetected = false;
		boolean checkInfoMsgs = true;
		String mateExpectation = String.format("mate %d", mateInX);
		testOutput.flush();
		inputToEngine.flush();
		int commandNumber = 1;
		for (commandPair currCmdPair: commands) {
			String inputCmd = currCmdPair.getIn();
			String expectedOutput = currCmdPair.getOut();
			String parsedCmd= "";
			// Pass command to engine
			if (inputCmd != null) {
				inputToEngine.write(inputCmd);
				inputToEngine.flush();
			}
			// Test expected command was received
			if (expectedOutput != null) {
				boolean received = false;
				int timer = 0;
				boolean accumulate = false;
				String recievedCmd = "";
				// Receive message or wait for timeout to expire.
				while (!received && timer<timeout) {
					// Give the engine thread some CPU time
					Thread.sleep(sleep_50ms);
					timer += sleep_50ms;
					testOutput.flush();
					if (accumulate) {
						recievedCmd += testOutput.toString();
					} else {
						recievedCmd = testOutput.toString();
					}
					if (recievedCmd != null && !recievedCmd.isEmpty()) {
						if (!accumulate)
							System.err.println(recievedCmd);
						testOutput.reset();
						// Ignore any line starting with info, if not checking infos
					    parsedCmd = parseReceivedCommandString(recievedCmd, checkInfoMsgs);
					    if (!parsedCmd.isEmpty()) { // want to use isBlank(), but that is Java 11 only.
							if (parsedCmd.endsWith(expectedOutput)) {
								received = true;
								accumulate = false;
								if (parsedCmd.contains(mateExpectation)) {
									mateDetected = true;
								}
							} else if (parsedCmd.contains(mateExpectation)) {
								mateDetected = true;
								accumulate = true;
							} else {
								//EubosEngineMain.logger.info(String.format("parsed '%s' != '%s'", parsedCmd, expectedOutput));
								accumulate = false;
							}
					    }
					}
				}
				if (!received) {
					fail(inputCmd + expectedOutput + "command that failed " + (commandNumber-3));
				}
				commandNumber++;
			} else {
				Thread.sleep(sleep_50ms);
			}
		}
		assertTrue(mateDetected);
	}

	private void setupEngine() {
		commands.add(new commandPair(UCI_CMD, ID_NAME_CMD+ID_AUTHOR_CMD+OPTION_HASH+OPTION_THREADS+UCI_OK_CMD));
		commands.add(new commandPair("setoption name NumberOfWorkerThreads value 1"+CMD_TERMINATOR, null));
		commands.add(new commandPair("setoption name Hash value 256"+CMD_TERMINATOR, null));
		commands.add(new commandPair(ISREADY_CMD,READY_OK_CMD));
		commands.add(new commandPair(NEWGAME_CMD,null));
		commands.add(new commandPair(ISREADY_CMD,READY_OK_CMD));
	}
	
	private String parseReceivedCommandString(String recievedCmd, boolean checkInfoMessages) {
		String parsedCmd = "";
		String currLine = "";
		Scanner scan = new Scanner(recievedCmd);
		while (scan.hasNextLine()) {
			currLine = scan.nextLine();
			if (currLine.startsWith("#")) {
				/* Silently consume JOL warnings like:
				 * # WARNING: Unable to get Instrumentation. Dynamic Attach failed. You may add this JAR as -javaagent manually, or supply -Djdk.attach.allowAttachSelf
				 * # WARNING: Unable to attach Serviceability Agent. Unable to attach even with module exceptions: [org.openjdk.jol.vm.sa.SASupportException: Sense failed., org.openjdk.jol.vm.sa.SASupportException: Sense failed., org.openjdk.jol.vm.sa.SASupportException: Sense failed.]
				 */
			} else if (!currLine.contains("info")) {
				//EubosEngineMain.logger.info(String.format("raw text received was '%s'", currLine));
				parsedCmd += (currLine + CMD_TERMINATOR);
			} else if (checkInfoMessages) {
				// parse to remove time from info messages
				parsedCmd += (removeTimeFieldsFromUciInfoMessage(currLine) + CMD_TERMINATOR);
			} else {
				// omit line
			}
		}
		scan.close();
		return parsedCmd;
	}
	
	private String removeTimeFieldsFromUciInfoMessage(String info) {
		String [] array = info.split(" ");
		String output = "";
		boolean delete_next_token = false;
		for (String token : array) {
			if (delete_next_token) {
				Integer.parseInt(token);
				// skip this token
				delete_next_token = false;
			} else {
				// reconstruct
				output = String.join(" ", output, token);
			}
			if (token.equals("nps") || token.equals("time")) {
				delete_next_token = true;
			}
		}
		output = output.trim();
		//EubosEngineMain.logger.info(String.format("parsed UCI Info was '%s'", output));
		return output;
	}
}
