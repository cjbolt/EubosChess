package eubos.search;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import eubos.board.pieces.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IEvaluate;
import eubos.position.IGenerateMoveList;
import eubos.position.IPositionAccessors;

import static org.mockito.Mockito.*;

public class PlySearcherTest {
	
	private PlySearcher classUnderTest;
	private static final byte searchDepth = 2;
	
	private IChangePosition mock_pm;
	private IGenerateMoveList mock_mlgen;
	private IPositionAccessors mock_pos;
	private IEvaluate mock_pe;
	
	PrincipalContinuation pc;
	private SearchMetrics sm;
	private SearchMetricsReporter sr;
	private EubosEngineMain mockEubos;
	
	LinkedList<GenericMove> lastPc;
	
	private FixedSizeTranspositionTable mock_hashMap;
	
	@Before
	public void setUp() throws Exception {
		SearchDebugAgent.open();
		
		pc = new PrincipalContinuation(searchDepth);
		sm = new SearchMetrics(searchDepth);
		sm.setPrincipalVariation(pc.toPvList());
		mockEubos = new EubosEngineMain();
		sr = new SearchMetricsReporter(mockEubos,sm);
		mock_pm = mock(IChangePosition.class);
		mock_mlgen = mock(IGenerateMoveList.class);
		mock_pos = mock(IPositionAccessors.class);
		mock_hashMap = mock(FixedSizeTranspositionTable.class);
		lastPc = null;
		mock_pe = mock(IEvaluate.class);
		
		when(mock_pos.getOnMove()).thenReturn(Colour.white);
		
		classUnderTest = new PlySearcher(
				mock_hashMap,
			    pc,
				sm,
				sr,
				searchDepth,
				mock_pm,
				mock_mlgen,
				mock_pos,
				lastPc,
				mock_pe);
	}
	
	@After
	public void tearDown() {
		SearchDebugAgent.close();
	}
	
	@Test
	public void testPlySearcher() {
		assertTrue(classUnderTest != null);
	}
}
