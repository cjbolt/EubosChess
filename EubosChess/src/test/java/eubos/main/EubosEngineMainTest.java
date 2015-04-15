package eubos.main;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

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
//	private static final String ISREADY_CMD = "isready"+CMD_TERMINATOR;
//	private static final String QUIT_CMD = "quit"+CMD_TERMINATOR;
	// Engine outputs
	private static final String ID_NAME_CMD = "id name Eubos"+CMD_TERMINATOR;
	private static final String ID_AUTHOR_CMD = "id author Chris Bolt"+CMD_TERMINATOR;
	private static final String UCI_OK_CMD = "uciok"+CMD_TERMINATOR;
//	private static final String READY_OK_CMD = "readyok"+CMD_TERMINATOR;
	
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
