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
import org.junit.Ignore;
import org.junit.Test;

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
	private static final String CMD_TERMINATOR = "\r\n";
	private static final String POS_FEN_PREFIX = "position fen ";
	private static final String GO_DEPTH_PREFIX = "go depth ";
	private static final String GO_WTIME_PREFIX = "go wtime ";
	private static final String GO_BTIME_PREFIX = "go btime ";
	private static final String BEST_PREFIX = "bestmove ";
	
	// Whole Commands
	// Inputs
	private static final String UCI_CMD = "uci"+CMD_TERMINATOR;
	private static final String ISREADY_CMD = "isready"+CMD_TERMINATOR;
	private static final String NEWGAME_CMD = "ucinewgame"+CMD_TERMINATOR;
	//private static final String GO_INF_CMD = "go infinite"+CMD_TERMINATOR;
	private static final String GO_TIME_CMD = "go movetime 1000"+CMD_TERMINATOR;
	private static final String QUIT_CMD = "quit"+CMD_TERMINATOR;
	// Outputs
	private static final String ID_NAME_CMD = "id name Eubos"+CMD_TERMINATOR;
	private static final String ID_AUTHOR_CMD = "id author Chris Bolt"+CMD_TERMINATOR;
	private static final String UCI_OK_CMD = "uciok"+CMD_TERMINATOR;
	private static final String READY_OK_CMD = "readyok"+CMD_TERMINATOR;
	
	private static final int sleep_10ms = 10;
	
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
		Thread.sleep(sleep_10ms);
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
		commands.add(new commandPair(GO_TIME_CMD,BEST_PREFIX+"b5b6"+CMD_TERMINATOR));
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
	
	@Test
	public void test_avoidDraw_lichess_hash_table_terminal_bypasses_drawchecker() throws InterruptedException, IOException {
		setupEngine();
		// Setup Commands specific to this test
		commands.add(new commandPair(POS_FEN_PREFIX+"8/8/2K5/8/7k/8/8/6q1 b - - 1 60"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_BTIME_PREFIX+"2000"+CMD_TERMINATOR,BEST_PREFIX+"g1g2"+CMD_TERMINATOR));
		commands.add(new commandPair(POS_FEN_PREFIX+"8/8/8/2K5/7k/8/6q1/8 b - - 3 61"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_BTIME_PREFIX+"1999"+CMD_TERMINATOR,BEST_PREFIX+"g2g1"+CMD_TERMINATOR));
		commands.add(new commandPair(POS_FEN_PREFIX+"8/8/2K5/8/7k/8/8/6q1 b - - 1 62"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_BTIME_PREFIX+"1998"+CMD_TERMINATOR,BEST_PREFIX+"g1g2"+CMD_TERMINATOR));
		commands.add(new commandPair(POS_FEN_PREFIX+"8/8/8/2K5/7k/8/6q1/8 b - - 3 63"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_BTIME_PREFIX+"1997"+CMD_TERMINATOR,BEST_PREFIX+"g2g1"+CMD_TERMINATOR));
		commands.add(new commandPair(POS_FEN_PREFIX+"8/8/2K5/8/7k/8/8/6q1 b - - 1 64"+CMD_TERMINATOR, null));
		// Varies move as the previous leads to draw by 3-fold repetition of position.
		commands.add(new commandPair(GO_BTIME_PREFIX+"1996"+CMD_TERMINATOR,BEST_PREFIX+"g1g6"+CMD_TERMINATOR));
		performTest(500);
	}
	
	@Test
	@Ignore
	public void test_avoidDraw_lichess_hash_table_terminal_bypasses_drawchecker_1() throws InterruptedException, IOException {
		/*[Event "Rated Blitz game"]
			[Site "https://lichess.org/9loh1Lxx"]
			[Date "2019.11.17"]
			[White "eubos"]
			[Black "turkjs"]
			[Result "1/2-1/2"]
			[UTCDate "2019.11.17"]
			[UTCTime "18:41:20"]			
			1. c4 e5 2. Nc3 Nc6 3. Nf3 { A27 English Opening: King's English Variation, Three Knights System } Nge7 4. e3 d6 5. Bd3 Be6 6. O-O a5 7. Qb3 b6 8. Be4 Bf5 9. d3 g6 10. Bxf5 Nxf5 11. g4 Nh4 12. Nd2 Qg5 13. h3 f5 14. Nd5 Qd8 15. Qa4 Kd7 16. f4 exf4 17. Rxf4 Bh6 18. Rf2 Re8 19. Nf3 fxg4 20. Nxh4 Bxe3 21. Bxe3 Rxe3 22. Nf6+ Ke7 23. Nd5+ Kd7 24. Nf6+ Ke7 25. Nd5+ Kd7 { The game is a draw. } 1/2-1/2
			*/
		setupEngine();
		// Setup Commands specific to this test
		commands.add(new commandPair(POS_FEN_PREFIX+"r2q4/2pk3p/1pnp2p1/p2N4/Q1P3pN/3Pr2P/PP3R2/R5K1 w - - 0 22"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_WTIME_PREFIX+"14800"+CMD_TERMINATOR,BEST_PREFIX+"d5f6"+CMD_TERMINATOR));
		commands.add(new commandPair(POS_FEN_PREFIX+"r2q4/2p1k2p/1pnp1Np1/p7/Q1P3pN/3Pr2P/PP3R2/R5K1 w - - 2 23"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_WTIME_PREFIX+"14000"+CMD_TERMINATOR,BEST_PREFIX+"f6d5"+CMD_TERMINATOR));
		commands.add(new commandPair(POS_FEN_PREFIX+"r2q4/2pk3p/1pnp2p1/p2N4/Q1P3pN/3Pr2P/PP3R2/R5K1 w - - 4 24"+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_WTIME_PREFIX+"13100"+CMD_TERMINATOR,BEST_PREFIX+"d5f6"+CMD_TERMINATOR));
		commands.add(new commandPair(POS_FEN_PREFIX+"r2q4/2p1k2p/1pnp1Np1/p7/Q1P3pN/3Pr2P/PP3R2/R5K1 w - - 6 25"+CMD_TERMINATOR, null));
		// This move allows the opponent to draw. It puts the onus onto the opponent to take a draw by 3-fold repetition...
		commands.add(new commandPair(GO_WTIME_PREFIX+"12300"+CMD_TERMINATOR,BEST_PREFIX+"f6d5"+CMD_TERMINATOR));
		// Opponent selects following and is draw...
		//commands.add(new commandPair(POS_FEN_PREFIX+"r2q4/2pk3p/1pnp2p1/p2N4/Q1P3pN/3Pr2P/PP3R2/R5K1 w - - 8 26"+CMD_TERMINATOR, null));
		performTest(1500);
	}

	private void performTest(int timeout) throws IOException, InterruptedException {
		testOutput.flush();
		inputToEngine.flush();
		int commandNumber = 1;
		for (commandPair currCmdPair: commands) {
			String inputCmd = currCmdPair.getIn();
			String expectedOutput = currCmdPair.getOut();
			String parsedCmd= "";
			// Pass command to engine
			inputToEngine.write(inputCmd);
			inputToEngine.flush();
			// Test expected command was received
			if (expectedOutput != null) {
				boolean received = false;
				int timer = 0;
				// Receive message or wait for timeout to expire.
				while (!received && timer<timeout) {
					// Give the engine thread some CPU time
					Thread.sleep(sleep_10ms);
					timer += sleep_10ms;
					// Ignore any line starting with info
					String recievedCmd = testOutput.toString();
					System.out.println(recievedCmd);
					testOutput.reset();
					parsedCmd = filterInfosOut(recievedCmd);
					if (expectedOutput.equals(parsedCmd))
						received = true;
				}
				if (!received) {
					fail(inputCmd + expectedOutput + "command that failed " + (commandNumber-3));
				}
				commandNumber++;
			}
		}
	}

	private void setupEngine() {
		commands.add(new commandPair(UCI_CMD, ID_NAME_CMD+ID_AUTHOR_CMD+UCI_OK_CMD));
		commands.add(new commandPair(ISREADY_CMD,READY_OK_CMD));
		commands.add(new commandPair(NEWGAME_CMD,null));
		commands.add(new commandPair(ISREADY_CMD,READY_OK_CMD));
	}
	
	private String filterInfosOut(String recievedCmd) {
		String parsedCmd = "";
		String currLine = "";
		Scanner scan = new Scanner(recievedCmd);
		while (scan.hasNextLine()) {
			currLine = scan.nextLine();
			if (!currLine.contains("info")) {
				parsedCmd += (currLine + CMD_TERMINATOR);
			}
		}
		scan.close();
		return parsedCmd;
	}	
}
