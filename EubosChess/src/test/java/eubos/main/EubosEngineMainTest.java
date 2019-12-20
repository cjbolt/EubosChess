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
	//private static final String GO_WTIME_PREFIX = "go wtime ";
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
		commands.add(new commandPair(GO_BTIME_PREFIX+"1997"+CMD_TERMINATOR,BEST_PREFIX+"g2g5"+CMD_TERMINATOR));
		// results in new position and avoids the draw by 3-fold!
		// white could move Kc6, which would result in this again: "8/8/2K5/8/7k/8/8/6q1 b - - 9 64" 
		performTest(500);
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
