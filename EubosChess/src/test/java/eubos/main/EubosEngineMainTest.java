package eubos.main;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Scanner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EubosEngineMainTest {

	private EubosEngineMain classUnderTest;
	
	private Thread eubosThread;
	private final ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
	private InputStream testInput;
	
	private static final String CMD_TERMINATOR = "\r\n";
	// Engine inputs
	private static final String UCI_CMD = "uci"+CMD_TERMINATOR;
	private static final String ISREADY_CMD = "isready"+CMD_TERMINATOR;
	private static final String NEWGAME_CMD = "ucinewgame"+CMD_TERMINATOR;
	private static final String POS_FEN_PREFIX = "position fen ";
//	private static final String POS_START_PREFIX = "position startpos ";
	private static final String GO_CMD = "go infinite"+CMD_TERMINATOR;
//	private static final String QUIT_CMD = "quit"+CMD_TERMINATOR;
	// Engine outputs
	private static final String ID_NAME_CMD = "id name Eubos"+CMD_TERMINATOR;
	private static final String ID_AUTHOR_CMD = "id author Chris Bolt"+CMD_TERMINATOR;
	private static final String UCI_OK_CMD = "uciok"+CMD_TERMINATOR;
	private static final String READY_OK_CMD = "readyok"+CMD_TERMINATOR;
	private static final String BEST_PREFIX = "bestmove ";
	
	@Before
	public void setUp() {
	}
	
	@Before
	public void setUpStreams() {
	    System.setOut(new PrintStream(testOutput));
	}	
	
	@Test
	public void test_startEngine() throws UnsupportedEncodingException, InterruptedException {
		testInput = new ByteArrayInputStream( UCI_CMD.getBytes("UTF-8") );
		InputStream old = System.in;
		try {
		    System.setIn( testInput );
			// Start the Engine
			classUnderTest = new EubosEngineMain();
			eubosThread = new Thread( classUnderTest );
			eubosThread.start();
			// Give the engine thread some cpu time
			Thread.sleep(50);
		} finally {
		    System.setIn( old );
		}
		assertEquals(ID_NAME_CMD+ID_AUTHOR_CMD+UCI_OK_CMD,testOutput.toString());
	}
	
	@Test
	public void test_mateInOne() throws InterruptedException, IOException {
		// Setup input commands
		ArrayList<String> inputCmds = new ArrayList<String>();
		inputCmds.add(UCI_CMD);
		inputCmds.add(ISREADY_CMD);
		inputCmds.add(NEWGAME_CMD);
		inputCmds.add(ISREADY_CMD);
		inputCmds.add(POS_FEN_PREFIX+"k1K5/b7/R7/1P6/1n6/8/8/8 w - - 0 1"+CMD_TERMINATOR);
		inputCmds.add(GO_CMD);
		// Setup output commands
		ArrayList<String> outputCmds = new ArrayList<String>();
		outputCmds.add(ID_NAME_CMD+ID_AUTHOR_CMD+UCI_OK_CMD);
		outputCmds.add(READY_OK_CMD);
		outputCmds.add(null);
		outputCmds.add(READY_OK_CMD);
		outputCmds.add(null);
		outputCmds.add(BEST_PREFIX+"b5b6"+CMD_TERMINATOR);		
		// Start engine
		PipedWriter inputToEngine = new PipedWriter();
		classUnderTest = new EubosEngineMain(inputToEngine);
		eubosThread = new Thread( classUnderTest );
		eubosThread.start();
		// Apply inputs and check outputs...
		for (String currCmd: inputCmds) {
			// Pass command to engine
			inputToEngine.write(currCmd);
			// Give the engine thread some cpu time
			Thread.sleep(1500);
			// Get the command to check for...
			String expectedOutput = outputCmds.remove(0);
			if (expectedOutput != null) {
				// ignore any line starting with info
				String recievedCmd = testOutput.toString();
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
				assertEquals(expectedOutput,parsedCmd);
				testOutput.reset();
			}
		}
	}
	
	@After
	public void cleanUpStreams() {
	    //System.setOut(null);
	}
	
	@After
	public void tearDown() {
		// Stop the Engine TODO: could send quit command over stdin
		classUnderTest = null;
		eubosThread = null;
	}
}
