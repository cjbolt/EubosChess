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
				expectedOutput = "info depth 1 seldepth 6 score cp 7 pv d7e5 f3e5 c7c2 e5f7 hashfull 0 nps 247 time 97 nodes 24"+CMD_TERMINATOR+
	                    "info depth 1 seldepth 6 score cp 169 pv c7c2 d4a7 hashfull 0 nps 441 time 102 nodes 45"+CMD_TERMINATOR+
	                    "info depth 2 seldepth 6 score cp 85 pv c7c2 e1g1 d7e5 hashfull 0 nps 1836 time 116 nodes 225"+CMD_TERMINATOR
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
	public void test_when_has_insufficient_material_to_mate_takes_draw() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new commandPair(POS_FEN_PREFIX+"7K/8/8/8/8/k1N5/p7/N7 w - - 11 1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"2"+CMD_TERMINATOR,BEST_PREFIX+"c3a2"+CMD_TERMINATOR));
		performTest(1000);
	}
	
	@Test
	public void test_capture_clears_draw_checker() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new commandPair(POS_FEN_PREFIX+"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 moves e2e4 e7e5 b1c3 e5e4"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"1"+CMD_TERMINATOR,BEST_PREFIX+"c3e4"+CMD_TERMINATOR));
		performTest(500);
		assertEquals(1, (int)classUnderTest.dc.getNumEntries()); // Capture clears the draw checker, so we just have the position after the capture
	}
	
	@Test
	public void test_pawn_move_clears_draw_checker() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new commandPair(POS_FEN_PREFIX+"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"1"+CMD_TERMINATOR,BEST_PREFIX+"e2e3"+CMD_TERMINATOR));
		commands.add(new commandPair(POS_FEN_PREFIX+"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 moves e2e3 e7e5"+CMD_TERMINATOR, null));
		performTest(500);
		assertEquals(1, (int)classUnderTest.dc.getNumEntries()); // Pawn moves clear DrawChecker history, so we just get the position after the pawn move
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
		/* There are only four positions in this test. */
		assertEquals(4, (int)classUnderTest.dc.getNumEntries());
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
		/* There are only four positions in this test. */
		assertEquals(4, (int)classUnderTest.dc.getNumEntries());
	}
	
	@Test
	public void test_KQk_mate_in_7_NEW() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new commandPair(POS_FEN_PREFIX+"5Q2/6K1/8/3k4/8/8/8/8 w - - 1 113"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_TIME_PREFIX+"30000"+CMD_TERMINATOR, BEST_PREFIX+"f8b4"+CMD_TERMINATOR));
		performTestExpectMate(30000, 7);
		assertEquals(2, (int)classUnderTest.dc.getNumEntries());
	}
	
	@Test
	public void test_KQk_mated_in_6_NEW() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new commandPair(POS_FEN_PREFIX+"8/6K1/8/3k4/1Q6/8/8/8 b - - 1 1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"13"+CMD_TERMINATOR, BEST_PREFIX+"d5c6"+CMD_TERMINATOR));
		performTestExpectMate(30000, -6);
		assertEquals(2, (int)classUnderTest.dc.getNumEntries());
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
		assertEquals(4, (int)classUnderTest.dc.getNumEntries());
	}
	
	@Test
	public void test_KRk_mate_in_11_NEW() throws InterruptedException, IOException {
		int mateDepth = 0;
		setupEngine();
		commands.add(new commandPair(POS_FEN_PREFIX+"8/8/8/3K1k2/8/8/8/7r b - - 5 111"+CMD_TERMINATOR, null));
		if (EubosEngineMain.ENABLE_ASPIRATION_WINDOWS) {
			commands.add(new commandPair(GO_TIME_PREFIX+"23000"+CMD_TERMINATOR, BEST_PREFIX+"h1d1"+CMD_TERMINATOR));
			mateDepth = 20;
		} else {
			commands.add(new commandPair(GO_TIME_PREFIX+"23000"+CMD_TERMINATOR, BEST_PREFIX+"h1h4"+CMD_TERMINATOR));
			mateDepth = 25;
		}
		performTestExpectMate(25000, mateDepth);
		assertEquals(2, (int)classUnderTest.dc.getNumEntries());
	}
	
	 
	@Test
	public void test_mate_in_3_guardian3713() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new commandPair(POS_FEN_PREFIX+"8/2p5/P4p2/Q1N2k1P/2P2P2/3PK2P/5R2/2B2R2 w - - 1 1"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"6"+CMD_TERMINATOR, BEST_PREFIX+"f2d2"+CMD_TERMINATOR));
		performTestExpectMate(4000, 3);
		assertEquals(2, (int)classUnderTest.dc.getNumEntries());
	}
	
	@Test
	public void test_hash_issue_losing_position() throws InterruptedException, IOException {
		setupEngine();
		commands.add(new commandPair(POS_FEN_PREFIX+"3r2k1/5p2/7p/3R2p1/p7/1q1Q1PP1/7P/3R2K1 b - - 1 42"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_DEPTH_PREFIX+"8"+CMD_TERMINATOR, BEST_PREFIX+"d8d5"+CMD_TERMINATOR));

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
				while (!received && timer<400000000) {
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
		assertEquals(Integer.valueOf(1), classUnderTest.dc.getNumEntries());
		classUnderTest.sendBestMoveCommand(new ProtocolBestMoveCommand(new GenericMove("b3b4"), null));
		// White move 63
		applyMoveList = new ArrayList<GenericMove>();
		applyMoveList.add(new GenericMove("b3b4"));
		classUnderTest.createPositionFromAnalyseCommand(new EngineAnalyzeCommand(new GenericBoard("8/8/8/8/8/pk6/8/K7 b - - 5 62"), applyMoveList));
		assertEquals(Integer.valueOf(2), classUnderTest.dc.getNumEntries());
		classUnderTest.sendBestMoveCommand(new ProtocolBestMoveCommand(new GenericMove("a1a2"), null));
		// Black move 63
		applyMoveList = new ArrayList<GenericMove>();
		applyMoveList.add(new GenericMove("b3b4"));
		applyMoveList.add(new GenericMove("a1a2"));
		classUnderTest.createPositionFromAnalyseCommand(new EngineAnalyzeCommand(new GenericBoard("8/8/8/8/8/pk6/8/K7 b - - 5 62"), applyMoveList));
		assertEquals(Integer.valueOf(3), classUnderTest.dc.getNumEntries());
		classUnderTest.sendBestMoveCommand(new ProtocolBestMoveCommand(new GenericMove("b4a4"), null));
		// White move 64
		applyMoveList = new ArrayList<GenericMove>();
		applyMoveList.add(new GenericMove("b3b4"));
		applyMoveList.add(new GenericMove("a1a2"));
		applyMoveList.add(new GenericMove("b4a4"));
		classUnderTest.createPositionFromAnalyseCommand(new EngineAnalyzeCommand(new GenericBoard("8/8/8/8/8/pk6/8/K7 b - - 5 62"), applyMoveList));
		assertEquals(Integer.valueOf(4), classUnderTest.dc.getNumEntries());
		classUnderTest.sendBestMoveCommand(new ProtocolBestMoveCommand(new GenericMove("a2b1"), null));
		//  Black move 64
		applyMoveList = new ArrayList<GenericMove>();
		applyMoveList.add(new GenericMove("b3b4"));
		applyMoveList.add(new GenericMove("a1a2"));
		applyMoveList.add(new GenericMove("b4a4"));
		applyMoveList.add(new GenericMove("a2b1"));
		classUnderTest.createPositionFromAnalyseCommand(new EngineAnalyzeCommand(new GenericBoard("8/8/8/8/8/pk6/8/K7 b - - 5 62"), applyMoveList));
		assertEquals(Integer.valueOf(5), classUnderTest.dc.getNumEntries());
		classUnderTest.sendBestMoveCommand(new ProtocolBestMoveCommand(new GenericMove("a4a5"), null));
		System.err.println(classUnderTest.dc.toString());
		assertEquals(Integer.valueOf(6), classUnderTest.dc.getNumEntries());
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
