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

	private EubosEngineMain eubos;
	private Thread eubosThread;
	
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	
	@Before
	public void setUp() {
//		// start the Engine
//		eubos = new EubosEngineMain();
//		eubosThread = new Thread( eubos );
//		eubosThread.start();
	}
	
	@Before
	public void setUpStreams() {
	    System.setOut(new PrintStream(outContent));
	}	
	
	@Test
	public void test_startEngine() throws UnsupportedEncodingException {
		String data = "uci\n";
		InputStream testInput = new ByteArrayInputStream( data.getBytes("UTF-8") );
		InputStream old = System.in;
		try {
		    System.setIn( testInput );
			// start the Engine
			eubos = new EubosEngineMain();
			eubosThread = new Thread( eubos );
			eubosThread.start();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} finally {
		    System.setIn( old );
		}
		assertEquals("id name Eubos\nid author Chris Bolt\nuciok\n\n", outContent.toString());
	}
	
	@After
	public void cleanUpStreams() {
	    System.setOut(null);
	}
	
	@After
	public void tearDown() {
		// Stop the Engine
		// TODO: could send quit command over stdin
		eubos = null;
		eubosThread = null;
	}
}
