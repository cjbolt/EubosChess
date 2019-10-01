package eubos.search;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.commands.ProtocolInformationCommand;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.main.EubosEngineMain;

public class SearchMetricsReporterTest {
	SearchMetricsReporter classUnderTest;
	SearchMetrics sm;
	EubosMock eubos;
	private static final int searchDepth = 4;
	
	private class EubosMock extends EubosEngineMain {
		boolean infoCommandReceived = false;
		ProtocolInformationCommand last_info;
		
		@Override
		public void sendInfoCommand(ProtocolInformationCommand command) {
			infoCommandReceived = true;
			last_info = command;
		}
		
		public boolean getInfoCommandReceived() { return infoCommandReceived; }
	}
	
	@Before
	public void setUp() throws IllegalNotationException {
		eubos = new EubosMock();
		sm = new SearchMetrics(searchDepth);
		// Minimal setup of the Search Metrics object
		LinkedList<GenericMove> pv = new LinkedList<GenericMove>();
		pv.add(new GenericMove("e2e4"));
		sm.setCurrentMove(pv.getFirst());
		sm.setPrincipalVariation(pv);
		classUnderTest = new SearchMetricsReporter(eubos, sm);
	}
	
	@Test
	public void testSearchMetricsReporter() {
		assertTrue(classUnderTest != null);
	}	
	
	@Test
	public void testRun() {
		classUnderTest.start();
		try {
			Thread.sleep(600);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertTrue(eubos.getInfoCommandReceived());
		classUnderTest.end();
	}
	
	@Test
	public void testEnd() {
		classUnderTest.start();
		classUnderTest.end();
		assertFalse(eubos.getInfoCommandReceived());
	}
	
	@Test
	public void testMateScore_gettingMatedIn3() {
		sm.setCpScore((short) (Short.MIN_VALUE+3)); // indicates mate in 3 moves
		classUnderTest.setSendInfo(true);
		classUnderTest.reportPrincipalVariation();
		
		assertEquals(new Integer(-3), eubos.last_info.getMate());
	}
	
	@Test
	public void testMateScore_MateIn3() {
		sm.setCpScore((short) (Short.MAX_VALUE-3)); // indicates mate in 3 moves
		classUnderTest.setSendInfo(true);
		classUnderTest.reportPrincipalVariation();
		
		assertEquals(new Integer(3), eubos.last_info.getMate());
	}
}
