package eubos.search;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.board.pieces.King;
import eubos.board.pieces.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IEvaluate;
import eubos.position.IGenerateMoveList;
import eubos.position.IPositionAccessors;
import eubos.position.IScoreMate;
import static org.mockito.Mockito.*;

public class PlySearcherTest {
	
	private PlySearcher classUnderTest;
	private static final int searchDepth = 4;
	
	private IChangePosition mock_pm;
	private IGenerateMoveList mock_mlgen;
	private IPositionAccessors mock_pos;
	private IEvaluate mock_pe;
	private IScoreMate mock_sg;
	
	PrincipalContinuation pc;
	private SearchMetrics sm;
	private SearchMetricsReporter sr;
	private EubosEngineMain mockEubos;
	
	private int score;
	private LinkedList<GenericMove> input_ml;
	LinkedList<GenericMove> lastPc;
	
	@Before
	public void setUp() throws Exception {
		SearchDebugAgent.open();
		
		score = 0;
		input_ml = new LinkedList<GenericMove>();
		
		mock_pe = mock(IEvaluate.class);
		mock_sg = mock(IScoreMate.class);
		pc = new PrincipalContinuation(searchDepth);
		sm = new SearchMetrics(searchDepth);
		sm.setPrincipalVariation(pc.toPvList());
		mockEubos = new EubosEngineMain();
		sr = new SearchMetricsReporter(mockEubos,sm);
		mock_pm = mock(IChangePosition.class);
		mock_mlgen = mock(IGenerateMoveList.class);
		mock_pos = mock(IPositionAccessors.class);
		lastPc = null;
		
		when(mock_pos.getOnMove()).thenReturn(Colour.white);
		
		classUnderTest = new PlySearcher(
				mock_pe,
				mock_sg,
			    pc,
				sm,
				sr,
				searchDepth,
				mock_pm,
				mock_mlgen,
				mock_pos,
				lastPc);
	}
	
	@After
	public void tearDown() {
		SearchDebugAgent.close();
	}
	
	@Test
	public void testPlySearcher() {
		assertTrue(classUnderTest != null);
	}
	
	@Test
	public void testSearchPly_Mate() throws IllegalNotationException, InvalidPieceException {
		when(mock_pos.getOnMove()).thenReturn(Colour.white);
		when(mock_mlgen.getMoveList()).thenReturn(input_ml);
		
		score = classUnderTest.searchPly(0);
		
		verify(mock_sg).scoreMate(0, true, Colour.white);
	}
	
	@Test
	public void test_scoreMateWhite() throws IllegalNotationException, InvalidPieceException {
		when(mock_pos.getOnMove()).thenReturn(Colour.white);
		when(mock_mlgen.getMoveList()).thenReturn(input_ml);
		when(mock_sg.scoreMate(0, true, Colour.white)).thenReturn(King.MATERIAL_VALUE);
		
		score = classUnderTest.searchPly(0);
		
		assertEquals(King.MATERIAL_VALUE, score);
	}
	
	@Test
	public void test_scoreMateBlack() throws IllegalNotationException, InvalidPieceException {
		when(mock_pos.getOnMove()).thenReturn(Colour.black);
		
		classUnderTest = new PlySearcher(
				mock_pe,
				mock_sg,
			    pc,
				sm,
				sr,
				searchDepth,
				mock_pm,
				mock_mlgen,
				mock_pos,
				lastPc);
		
		when(mock_pos.getOnMove()).thenReturn(Colour.black);
		when(mock_mlgen.getMoveList()).thenReturn(input_ml);
		when(mock_sg.scoreMate(0, false, Colour.black)).thenReturn(-King.MATERIAL_VALUE);
		
		score = classUnderTest.searchPly(0);
		
		assertEquals(-King.MATERIAL_VALUE, score);
	}	
}
