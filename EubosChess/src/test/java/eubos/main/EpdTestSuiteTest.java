package eubos.main;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.position.Move;
import eubos.position.PositionManager;

public class EpdTestSuiteTest {
	
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

	private void setupEngine() {
		commands.add(new commandPair(UCI_CMD, ID_NAME_CMD+ID_AUTHOR_CMD+OPTION_HASH+OPTION_THREADS+OPTION_MOVE_OVERHEAD+UCI_OK_CMD));
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
	
	private void performTest(int timeout) throws IOException, InterruptedException {
		performTest(timeout, false);
	}

	private void performTest(int timeout, boolean checkInfoMsgs) throws IOException, InterruptedException {
		performTestHelper(timeout, checkInfoMsgs, 0L, 0);
	}
	
	private void performTestExpectMate(int timeout, int mateInX) throws IOException, InterruptedException {
		performTestHelper(timeout, true, 0L, mateInX);
	}
	
	private void pokeHashEntryAndPerformTest(int timeout, long hashEntry) throws IOException, InterruptedException {
		performTestHelper(timeout, false, hashEntry, 0);
	}
	
	private void performTestHelper(int timeout, boolean checkInfoMsgs, long hashEntry, int mateInX) throws IOException, InterruptedException {
		boolean mateDetected = false;
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
				if (inputCmd.startsWith("go") && hashEntry != 0L) {
					Thread.sleep(sleep_50ms);
					// Seed hash table with problematic hash
					long problemHash = classUnderTest.rootPosition.getHash();
					//EubosEngineMain.logger.info(String.format("*************** using hash code %d", problemHash));
					classUnderTest.hashMap.putTransposition(problemHash, hashEntry);
				}
				inputToEngine.write(inputCmd);
				inputToEngine.flush();
				//EubosEngineMain.logger.info(String.format("************* %s", inputCmd));
			}
			// Test expected command was received
			if (expectedOutput != null) {
				boolean received = false;
				int timer = 0;
				boolean accumulate = false;
				String recievedCmd = "";
				// Receive message or wait for timeout to expire.
				while (!received && timer < timeout) {
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
		if (mateInX != 0) {
			assertTrue(mateDetected);
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
	private static final String OPTION_HASH = "option name Hash type spin default 256 min 32 max 4000"+CMD_TERMINATOR;
	private static final String OPTION_MOVE_OVERHEAD = "option name Move Overhead type spin default 10 min 0 max 5000"+CMD_TERMINATOR;
	private static final String OPTION_THREADS = String.format(
			"option name Threads type spin default %s min 1 max %s%s",
			Math.max(1, Runtime.getRuntime().availableProcessors()-2),
			Runtime.getRuntime().availableProcessors(), CMD_TERMINATOR);
	private static final String UCI_OK_CMD = "uciok"+CMD_TERMINATOR;
	private static final String READY_OK_CMD = "readyok"+CMD_TERMINATOR;
	
	private static final int sleep_50ms = 50;
	
	@BeforeEach
	public void setUp() throws IOException {
		// Start engine
		System.setOut(new PrintStream(testOutput));
		inputToEngine = new PipedWriter();
		classUnderTest = new EubosEngineMain(inputToEngine);
		eubosThread = new Thread( classUnderTest );
		eubosThread.start();
	}
	
	@AfterEach
	public void tearDown() throws IOException, InterruptedException {
		// Stop the Engine TODO: could send quit command over stdin
		inputToEngine.write(QUIT_CMD);
		inputToEngine.flush();
		Thread.sleep(10);
		classUnderTest = null;
		eubosThread = null;
	}
	
	public String[] epdTestSuiteList = {""};
	
	public class IndividualTestPosition {
		String fen;
		int bestMove;
		String testName;
		PositionManager pm;
		
		public IndividualTestPosition(String epd) throws IllegalNotationException {
			int bestMoveIndex = epd.indexOf("bm ");
			fen = epd.substring(0, bestMoveIndex);
			pm = new PositionManager(fen+" 0 0");
			String rest = epd.substring(bestMoveIndex+"bm ".length());
			int endOfBestMoveIndex = rest.indexOf(";");
			int x = bestMoveIndex+"bm ".length();
			String bestMoveAsString = epd.substring(x, x+endOfBestMoveIndex);
			bestMove = pm.getNativeMove(bestMoveAsString);
			testName = epd.substring(endOfBestMoveIndex);
		}
	};
	
	public List<IndividualTestPosition> loadTestSuiteFromEpd(String filename) {
		return new ArrayList<IndividualTestPosition>();
	}
	
	public void runTest(IndividualTestPosition test) throws IOException, InterruptedException {
		setupEngine();
		commands.add(new commandPair(POS_FEN_PREFIX+test.fen+CMD_TERMINATOR, null));
		commands.add(new commandPair(GO_TIME_PREFIX+"10000"+CMD_TERMINATOR, BEST_PREFIX+Move.toGenericMove(test.bestMove).toString()+CMD_TERMINATOR));
		performTest(10000);
	}
	
	public void runThroughTestSuite(String filename) {
		List<IndividualTestPosition> testSuite = loadTestSuiteFromEpd(filename);
		for (IndividualTestPosition test : testSuite) {
			// TODO implement reading and running through a whole EPD
		}
	}
	
	String test = "2rr3k/pp3pp1/1nnqbN1p/3pN3/2pP4/2P3Q1/PPB4P/R4RK1 w - - bm Qg6; id \"WAC.001\";";
	
	@Test
	public void test_can_create_position() throws IllegalNotationException, IOException, InterruptedException {
		IndividualTestPosition pos = new IndividualTestPosition(test);
		runTest(pos);		
	}
}
