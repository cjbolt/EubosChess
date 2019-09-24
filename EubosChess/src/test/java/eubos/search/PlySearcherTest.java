package eubos.search;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.board.pieces.Piece.Colour;
import eubos.board.pieces.Queen;
import eubos.main.EubosEngineMain;
import eubos.position.IPositionAccessors;
import eubos.position.PositionEvaluator;
import eubos.position.PositionManager;
import eubos.search.ITranspositionAccessor;
import static org.mockito.Mockito.*;

public class PlySearcherTest {
	
	private PlySearcher classUnderTest;
	private static final byte searchDepth = 2;
	
	private PositionManager pm;
	private IPositionAccessors mock_pos;
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
		mock_pos = mock(IPositionAccessors.class);
		mock_hashMap = mock(ITranspositionAccessor.class);
		st = new ScoreTracker(searchDepth*3, true);
		lastPc = null;
		
		when(mock_pos.getOnMove()).thenReturn(Colour.white);
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
				(byte)4,
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
				(byte) 4,
				pm,
				pm,
				pm,
				lastPc,
				pe);
		
		doReturn(new TranspositionEvaluation()).when(mock_hashMap).getTransposition(anyByte(), anyInt());
		doReturn(new TranspositionEvaluation()).when(mock_hashMap).getTransposition(anyInt());
		doReturn(new Transposition((byte)1, (short)0, null, null, null)).when(mock_hashMap).setTransposition(any(SearchMetrics.class), anyByte(), (Transposition)isNull(), any(Transposition.class));
		
		assertEquals(650, classUnderTest.normalSearchPly());
		
		//verify(mock_hashMap, times(8)).setTransposition(any(SearchMetrics.class), anyByte(), (Transposition)isNull(), any(Transposition.class));
		
		ArgumentCaptor<Transposition> captorNew = ArgumentCaptor.forClass(Transposition.class);
		ArgumentCaptor<Transposition> captorOld = ArgumentCaptor.forClass(Transposition.class);
		ArgumentCaptor<Byte> captorPly = ArgumentCaptor.forClass(Byte.class);
		verify(mock_hashMap, times(8)).setTransposition(any(SearchMetrics.class), captorPly.capture(), captorOld.capture(), captorNew.capture());
		
		List<Transposition> new_trans_args = captorNew.getAllValues();
		List<Transposition> trans_args = captorOld.getAllValues();
		List<Byte> plies = captorPly.getAllValues();
		assertEquals(new GenericMove("f3f2"),new_trans_args.get(0).getBestMove());
		assertNull(trans_args.get(0));
		assertEquals(3, plies.get(0).byteValue());
		
		assertEquals(new GenericMove("b7b8r"),new_trans_args.get(1).getBestMove());
		assertNull(trans_args.get(1));
		assertEquals(2, plies.get(1).byteValue());
		
		assertEquals(new GenericMove("f3f2"),new_trans_args.get(2).getBestMove());
		assertNull(trans_args.get(2));
		assertEquals(3, plies.get(2).byteValue());
		
		assertEquals(new GenericMove("f3f2"),new_trans_args.get(3).getBestMove());
		assertNull(trans_args.get(3));
		assertEquals(3, plies.get(3).byteValue());
		
		assertEquals(new GenericMove("f3f2"),new_trans_args.get(4).getBestMove());
		assertNull(trans_args.get(4));
		assertEquals(3, plies.get(4).byteValue());
		
		assertEquals(new GenericMove("b7b8q"),new_trans_args.get(5).getBestMove());
		assertNotNull(trans_args.get(5));
		assertEquals(2, plies.get(5).byteValue());
		
		assertEquals(new GenericMove("f4f3"),new_trans_args.get(6).getBestMove());
		assertNull(trans_args.get(6));
		assertEquals(1, plies.get(6).byteValue());
		
		assertEquals(new GenericMove("b6b7"),new_trans_args.get(7).getBestMove());
		assertNull(trans_args.get(7));
		assertEquals(0, plies.get(7).byteValue());
	}	 
}
