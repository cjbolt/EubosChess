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
	private ArrayList<String> inputCmds = new ArrayList<String>();
	private ArrayList<String> outputCmds = new ArrayList<String>();
	
	// Test infrastructure to allow pushing commands into Eubos and sniffing them out.
	private PipedWriter inputToEngine;
	private final ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
	
	// Command building blocks
	private static final String CMD_TERMINATOR = "\r\n";
	private static final String POS_FEN_PREFIX = "position fen ";
//	private static final String POS_START_PREFIX = "position startpos ";
	private static final String BEST_PREFIX = "bestmove ";
	
	// Whole Commands
	// Inputs
	private static final String UCI_CMD = "uci"+CMD_TERMINATOR;
	private static final String ISREADY_CMD = "isready"+CMD_TERMINATOR;
	private static final String NEWGAME_CMD = "ucinewgame"+CMD_TERMINATOR;
	private static final String GO_CMD = "go infinite"+CMD_TERMINATOR;
//	private static final String QUIT_CMD = "quit"+CMD_TERMINATOR;
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
	public void tearDown() {
		// Stop the Engine TODO: could send quit command over stdin
		classUnderTest = null;
		eubosThread = null;
	}
	
	@Test
	public void test_startEngine() throws InterruptedException, IOException {
		// Setup input/output commands
		setupEngine();
		performTest(1000);
	}
	
	@Test
	public void test_mateInOne() throws InterruptedException, IOException {
		setupEngine();
		// Setup Commands specific to this test
		inputCmds.add(POS_FEN_PREFIX+"k1K5/b7/R7/1P6/1n6/8/8/8 w - - 0 1"+CMD_TERMINATOR);
		inputCmds.add(GO_CMD);
		outputCmds.add(null);
		outputCmds.add(BEST_PREFIX+"b5b6"+CMD_TERMINATOR);		
		performTest(2000);
	}

	private void performTest(int timeout) throws IOException, InterruptedException {
		// Apply inputs and check outputs...
		for (String currCmd: inputCmds) {
			// Pass command to engine
			inputToEngine.write(currCmd);
			inputToEngine.flush();
			// Get the command to check for...
			String expectedOutput = outputCmds.remove(0);
			if (expectedOutput != null) {
				boolean received = false;
				int timer = 0;
				// Recieve message or wait for timeout to expire.
				while (!received && timer<timeout) {
					// Give the engine thread some cpu time
					Thread.sleep(sleep_10ms);
					timer += sleep_10ms;
					// ignore any line starting with info
					String recievedCmd = testOutput.toString();
					String parsedCmd = filterInfosOut(recievedCmd);
					if (expectedOutput.equals(parsedCmd))
						received = true;
					testOutput.reset();
				}
				if (!received) {
					fail();
				}
			}
		}
	}

	private void setupEngine() {
		// Input
		inputCmds.add(UCI_CMD);
		inputCmds.add(ISREADY_CMD);
		inputCmds.add(NEWGAME_CMD);
		inputCmds.add(ISREADY_CMD);
		// Output
		outputCmds.add(ID_NAME_CMD+ID_AUTHOR_CMD+UCI_OK_CMD);
		outputCmds.add(READY_OK_CMD);
		outputCmds.add(null);
		outputCmds.add(READY_OK_CMD);
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
