package eubos.search;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.commands.ProtocolInformationCommand;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.position.Position;
import eubos.position.PositionManager;
import eubos.search.transposition.FixedSizeTranspositionTable;

public class SearchMetricsReporterTest {
	SearchMetricsReporter classUnderTest;
	SearchMetrics sm;
	EubosMock eubos;
	protected FixedSizeTranspositionTable hashMap;
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
		PositionManager pm = new PositionManager();
		hashMap = new FixedSizeTranspositionTable();
		sm = new SearchMetrics(searchDepth, pm);
		// Minimal setup of the Search Metrics object
		int [] pv = { Move.valueOf(Position.e2, Piece.WHITE_PAWN, Position.e4, Piece.NONE) };
		sm.setPrincipalVariation(pv, 1);
		classUnderTest = new SearchMetricsReporter(eubos, hashMap, new ReferenceScore(hashMap)
				);
		classUnderTest.register(sm);
	}
	
	@After
	public void tearDown() {
	}
	
	@Test
	public void testSearchMetricsReporter() {
		assertTrue(classUnderTest != null);
	}	
	
	@Test
	@Ignore // not really needed and takes too long
	public void testRun() {
		classUnderTest.start();
		try {
			Thread.sleep(550);
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
		sm.setCpScore((short) (Score.PROVISIONAL_ALPHA+6)); // indicates mate in 3 moves
		classUnderTest.setSendInfo(true);
		classUnderTest.reportPrincipalVariation(sm, true, false);
		
		assertEquals(Integer.valueOf(-3), eubos.last_info.getMate());
	}
	
	@Test
	public void testMateScore_MateIn3() {
		sm.setCpScore((short) (Short.MAX_VALUE-6)); // indicates mate in 3 moves
		classUnderTest.setSendInfo(true);
		classUnderTest.reportPrincipalVariation(sm, true, false);
		
		assertEquals(Integer.valueOf(3), eubos.last_info.getMate());
	}
}
