package eubos.search;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.board.pieces.Pawn;
import eubos.board.pieces.Piece.Colour;
import eubos.board.pieces.Queen;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IEvaluate;
import eubos.position.IGenerateMoveList;
import eubos.position.IPositionAccessors;
import eubos.position.PositionEvaluator;
import eubos.position.PositionManager;
import eubos.search.ITranspositionAccessor;
import static org.mockito.Mockito.*;

public class PlySearcherTest {
	
	private PlySearcher classUnderTest;
	private static final byte searchDepth = 4;
	
	private PositionManager pm;
	private IChangePosition mock_pm;
	private IGenerateMoveList mock_mlgen;
	private IPositionAccessors mock_pos;
	private IEvaluate mock_pe;
	
	PrincipalContinuation pc;
	private SearchMetrics sm;
	private SearchMetricsReporter sr;
	private EubosEngineMain mockEubos;
	
	LinkedList<GenericMove> lastPc;
	
	private ITranspositionAccessor mock_hashMap;
	private ScoreTracker st;
	
	@Before
	public void setUp() throws Exception {
		SearchDebugAgent.open();
		
		pc = new PrincipalContinuation(searchDepth*3);
		sm = new SearchMetrics(searchDepth*3);
		sm.setPrincipalVariation(pc.toPvList());
		mockEubos = new EubosEngineMain();
		sr = new SearchMetricsReporter(mockEubos,sm);
		mock_pm = mock(IChangePosition.class);
		mock_mlgen = mock(IGenerateMoveList.class);
		mock_pos = mock(IPositionAccessors.class);
		mock_hashMap = mock(ITranspositionAccessor.class);
		st = new ScoreTracker(searchDepth*3, true);
		lastPc = null;
		mock_pe = mock(IEvaluate.class);
		
		when(mock_pos.getOnMove()).thenReturn(Colour.white);
		
		classUnderTest = new PlySearcher(
				mock_hashMap,
				st,
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
	public void test_depthSearchedUpdates() throws InvalidPieceException, IllegalNotationException {
		pm = new PositionManager("7K/7P/8/6Q1/3k4/8/8/8 w - - 1 69");
		PositionEvaluator pe = new PositionEvaluator(pm);
		classUnderTest = new PlySearcher(
				mock_hashMap,
				st,
			    pc,
				sm,
				sr,
				searchDepth,
				pm,
				pm,
				pm,
				lastPc,
				pe);
		doReturn(new TranspositionEvaluation()).when(mock_hashMap).getTransposition(anyByte(), anyInt());
		doReturn(new TranspositionEvaluation()).when(mock_hashMap).getTransposition(anyInt());
		assertEquals(2*Queen.MATERIAL_VALUE, classUnderTest.normalSearchPly());		
	}
	
	@Test
	public void test_singleLineOfPlay_depthSearchedUpdates() throws InvalidPieceException, IllegalNotationException {
		pm = new PositionManager("8/8/1P6/8/5p2/8/8/8 w - - 0 1");
		PositionEvaluator pe = new PositionEvaluator(pm);
		classUnderTest = new PlySearcher(
				mock_hashMap,
				st,
			    pc,
				sm,
				sr,
				searchDepth,
				pm,
				pm,
				pm,
				lastPc,
				pe);
		GenericMove[] bestMoves = new GenericMove[]{
				new GenericMove("b6b7"), new GenericMove("f4f3"),
				new GenericMove("b7b8Q"), new GenericMove("f3f2")};
		doReturn(new TranspositionEvaluation()).when(mock_hashMap).getTransposition(anyByte(), anyInt());
		doReturn(new TranspositionEvaluation()).when(mock_hashMap).getTransposition(anyInt());
		//verify(mock_hashMap.setTransposition( , currPly, trans, new_trans))
		assertEquals(Queen.MATERIAL_VALUE-(Pawn.MATERIAL_VALUE+100+50), classUnderTest.normalSearchPly());		
	}
	 
}
