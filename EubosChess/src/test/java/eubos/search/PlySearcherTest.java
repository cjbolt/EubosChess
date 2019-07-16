package eubos.search;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
import eubos.position.ZobristHashCode;
import static org.mockito.Mockito.*;

public class PlySearcherTest {
	
	private PlySearcher classUnderTest;
	private static final int searchDepth = 2;
	
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
	public void whenMoveListEmpty_ScoreMate() throws IllegalNotationException, InvalidPieceException {
		when(mock_pos.getOnMove()).thenReturn(Colour.white);
		when(mock_mlgen.getMoveList()).thenReturn(input_ml);
		
		score = classUnderTest.searchPly();
		
		verify(mock_sg).scoreMate(0, true, Colour.white);
	}

	@Test
	@Ignore
	public void whenMoveListNotEmpty_DontScoreMate() throws IllegalNotationException, InvalidPieceException {
		input_ml.add(new GenericMove("e2e4"));
		when(mock_pos.getOnMove()).thenReturn(Colour.white);
		when(mock_mlgen.getMoveList()).thenReturn(input_ml);
		
		score = classUnderTest.searchPly();
		
		verify(mock_sg, never()).scoreMate(0, true, Colour.white);
	}	
	
	@Test
	public void whenMoveListNotEmptyAndSearchTerminated_dontPerformMove() throws IllegalNotationException, InvalidPieceException {
		input_ml.add(new GenericMove("e2e4"));
		when(mock_pos.getOnMove()).thenReturn(Colour.white);
		when(mock_mlgen.getMoveList()).thenReturn(input_ml);
		
		classUnderTest.terminateFindMove();
		score = classUnderTest.searchPly();
		
		verify(mock_pm, never()).performMove(null, input_ml.get(0));
	}

	@Test
	public void whenMoveListNotEmpty_PerformMove() throws IllegalNotationException, InvalidPieceException {
		LinkedList<GenericMove> empty_ml = new LinkedList<GenericMove>();
		input_ml.add(new GenericMove("e2e4"));
		
		when(mock_pos.getOnMove()).thenReturn(Colour.white).thenReturn(Colour.black);
		when(mock_mlgen.getMoveList()).thenReturn(input_ml).thenReturn(empty_ml);
		
		score = classUnderTest.searchPly();
		
		verify(mock_pm).performMove(isA(ZobristHashCode.class), eq(input_ml.get(0)));
	}
	
	@Test
	public void whenNotTerminalNode_DontEvaluatePosition() throws IllegalNotationException, InvalidPieceException {
		LinkedList<GenericMove> empty_ml = new LinkedList<GenericMove>();
		input_ml.add(new GenericMove("e2e4"));
		
		when(mock_pos.getOnMove()).thenReturn(Colour.white).thenReturn(Colour.black);
		when(mock_mlgen.getMoveList()).thenReturn(input_ml).thenReturn(empty_ml);
		
		score = classUnderTest.searchPly();
		
		verify(mock_pe, never()).evaluatePosition(mock_pos);
	}	
	
	@Test
	public void whenTerminalNode_EvaluatePosition() throws IllegalNotationException, InvalidPieceException {
		LinkedList<GenericMove> black_ml = new LinkedList<GenericMove>();
		input_ml.add(new GenericMove("e2e4"));
		black_ml.add(new GenericMove("e7e5"));
		
		when(mock_pos.getOnMove()).thenReturn(Colour.white).thenReturn(Colour.black);
		when(mock_mlgen.getMoveList()).thenReturn(input_ml).thenReturn(black_ml);
		
		score = classUnderTest.searchPly();
		
		verify(mock_pe).evaluatePosition(mock_pos);
	}

	
	@Test
	public void whenWhiteNoMoves_ScoreIsMateInOneWhite() throws IllegalNotationException, InvalidPieceException {
		when(mock_pos.getOnMove()).thenReturn(Colour.white);
		when(mock_mlgen.getMoveList()).thenReturn(input_ml);
		when(mock_sg.scoreMate(0, true, Colour.white)).thenReturn(King.MATERIAL_VALUE);
		
		score = classUnderTest.searchPly();
		
		assertEquals(King.MATERIAL_VALUE, score);
	}
	
	@Test
	public void whenBlackNoMoves_ScoreIsMateInOneBlack() throws IllegalNotationException, InvalidPieceException {
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
		
		score = classUnderTest.searchPly();
		
		assertEquals(-King.MATERIAL_VALUE, score);
	}	
}
