package eubos.main;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedWriter;
import java.util.ArrayList;
import java.util.Scanner;

public abstract class AbstractEubosIntegration {
	protected EubosEngineMain classUnderTest;
	protected Thread eubosThread;
	
	protected ArrayList<CommandPair> commands = new ArrayList<CommandPair>();
	
	// Test infrastructure to allow pushing commands into Eubos and sniffing them out.
	protected PipedWriter inputToEngine;
	protected final ByteArrayOutputStream testOutput = new ByteArrayOutputStream();
	
	// Command building blocks
	protected static final String CMD_TERMINATOR = System.lineSeparator();
	protected static final String POS_FEN_PREFIX = "position fen ";
	protected static final String POS_START_PREFIX = "position startpos ";
	protected static final String GO_DEPTH_PREFIX = "go depth ";
	protected static final String GO_WTIME_PREFIX = "go wtime ";
	protected static final String GO_BTIME_PREFIX = "go btime ";
	protected static final String GO_TIME_PREFIX = "go movetime ";
	protected static final String BEST_PREFIX = "bestmove ";
	
	// Whole Commands
	// Inputs
	protected static final String UCI_CMD = "uci"+CMD_TERMINATOR;
	protected static final String ISREADY_CMD = "isready"+CMD_TERMINATOR;
	protected static final String NEWGAME_CMD = "ucinewgame"+CMD_TERMINATOR;
	protected static final String GO_INF_CMD = "go infinite"+CMD_TERMINATOR;
	protected static final String QUIT_CMD = "quit"+CMD_TERMINATOR;
	// Outputs
	protected static final String ID_NAME_CMD = String.format("id name Eubos %d.%d%s", 
			EubosEngineMain.EUBOS_MAJOR_VERSION, EubosEngineMain.EUBOS_MINOR_VERSION, CMD_TERMINATOR);
	protected static final String ID_AUTHOR_CMD = "id author Chris Bolt"+CMD_TERMINATOR;
	protected static final String OPTION_HASH = "option name Hash type spin default 256 min 32 max 4000"+CMD_TERMINATOR;
	protected static final String OPTION_MOVE_OVERHEAD = "option name Move Overhead type spin default 10 min 0 max 5000"+CMD_TERMINATOR;
	protected static final String OPTION_LAZY_THRESHOLD = "option name Lazy Threshold type spin default 450 min 0 max 1000"+CMD_TERMINATOR;
	protected static final String OPTION_THREADS = String.format(
			"option name Threads type spin default %s min 1 max %s%s",
			Math.max(1, Runtime.getRuntime().availableProcessors()-2),
			Runtime.getRuntime().availableProcessors(), CMD_TERMINATOR);
	protected static final String UCI_OK_CMD = "uciok"+CMD_TERMINATOR;
	protected static final String READY_OK_CMD = "readyok"+CMD_TERMINATOR;
	
	protected static final int sleep_50ms = 50;

	protected void setupEngine() {
		commands.add(new CommandPair(UCI_CMD, ID_NAME_CMD+ID_AUTHOR_CMD+OPTION_HASH+OPTION_THREADS+OPTION_MOVE_OVERHEAD+OPTION_LAZY_THRESHOLD+UCI_OK_CMD));
		commands.add(new CommandPair("setoption name NumberOfWorkerThreads value 1"+CMD_TERMINATOR, null));
		commands.add(new CommandPair("setoption name Hash value 256"+CMD_TERMINATOR, null));
		commands.add(new CommandPair(ISREADY_CMD,READY_OK_CMD));
		commands.add(new CommandPair(NEWGAME_CMD,null));
		commands.add(new CommandPair(ISREADY_CMD,READY_OK_CMD));
	}
	
	protected String parseReceivedCommandString(String recievedCmd, boolean checkInfoMessages) {
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
	
	protected String removeTimeFieldsFromUciInfoMessage(String info) {
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
	
	protected void performTest(int timeout) throws IOException, InterruptedException {
		performTest(timeout, false);
	}

	protected void performTest(int timeout, boolean checkInfoMsgs) throws IOException, InterruptedException {
		performTestHelper(timeout, checkInfoMsgs, 0L, 0);
	}
	
	protected void performTestExpectMate(int timeout, int mateInX) throws IOException, InterruptedException {
		performTestHelper(timeout, true, 0L, mateInX);
	}
	
	protected void pokeHashEntryAndPerformTest(int timeout, long hashEntry) throws IOException, InterruptedException {
		performTestHelper(timeout, false, hashEntry, 0);
	}
	
	protected void performTestHelper(int timeout, boolean checkInfoMsgs, long hashEntry, int mateInX) throws IOException, InterruptedException {
		boolean mateDetected = false;
		String mateExpectation = String.format("mate %d", mateInX);
		testOutput.flush();
		inputToEngine.flush();
		int commandNumber = 1;
		for (CommandPair currCmdPair: commands) {
			String inputCmd = currCmdPair.getIn();
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
			if (currCmdPair.expectOutput()) {
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
							if (currCmdPair.isExpectedOutput(parsedCmd)) {
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
					fail(inputCmd + currCmdPair.getOut() + "command that failed " + (commandNumber-3));
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
}
