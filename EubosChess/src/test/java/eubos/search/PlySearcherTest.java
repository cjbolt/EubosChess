package eubos.search;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
import eubos.search.Transposition.ScoreType;
import eubos.search.TranspositionEvaluation.TranspositionTableStatus;
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
	
	private void initialisePositionAndSearch(String fen, byte depth) {
		pm = new PositionManager(fen);
		PositionEvaluator pe = new PositionEvaluator(pm);
		classUnderTest = new PlySearcher(
				mock_hashMap,
				st,
			    pc,
				sm,
				sr,
				depth,
				pm,
				pm,
				pm,
				lastPc,
				pe);		
	}
	
	@Test
	public void test_depthSearchedUpdates() throws InvalidPieceException, IllegalNotationException {
		initialisePositionAndSearch("7K/7P/8/6Q1/3k4/8/8/8 w - - 1 69", (byte)4);
		doReturn(new TranspositionEvaluation()).when(mock_hashMap).getTransposition(anyByte(), anyInt());

		assertEquals(2*Queen.MATERIAL_VALUE, classUnderTest.normalSearchPly());		
	}
	
	@Test
	public void test_singleLineOfPlay_depthSearchedUpdates() throws InvalidPieceException, IllegalNotationException {
		initialisePositionAndSearch("8/8/1P6/8/5p2/8/8/8 w - - 0 1", (byte)4);
		
		doReturn(new TranspositionEvaluation()).when(mock_hashMap).getTransposition(anyByte(), anyInt());
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
	
	@Test
	public void test_singleLineOfPlay_exactHashHit() throws InvalidPieceException, IllegalNotationException {
		initialisePositionAndSearch("8/8/1P6/8/5p2/8/8/8 w - - 0 1", (byte)1);
		
		TranspositionEvaluation eval = new TranspositionEvaluation();
		eval.status = TranspositionTableStatus.sufficientTerminalNode;
		eval.trans = new Transposition((byte)1, (short)50, ScoreType.exact, null, new GenericMove("b6b7"));
		
		when(mock_hashMap.getTransposition((byte)0, 1)).thenReturn(eval);
		
		assertEquals(50, classUnderTest.normalSearchPly());
	}
	
	@Test
	public void test_refutation() throws InvalidPieceException, IllegalNotationException {
		initialisePositionAndSearch("6k1/5pb1/6p1/r2R4/8/2q5/1B3PP1/5RK1 w - - 0 1", (byte)2);
		
		TranspositionEvaluation eval0 = new TranspositionEvaluation();
		eval0.status = TranspositionTableStatus.sufficientSeedMoveList;
		ArrayList<GenericMove> ml_ply0 = new ArrayList<GenericMove>();
		ml_ply0.add(new GenericMove("b2c3"));
		ml_ply0.add(new GenericMove("d5a5"));
		eval0.trans = new Transposition((byte)1, (short)-5, ScoreType.lowerBound, ml_ply0, new GenericMove("b2c3"));
		
		TranspositionEvaluation eval1_0 = new TranspositionEvaluation();
		eval1_0.status = TranspositionTableStatus.sufficientTerminalNode;
		ArrayList<GenericMove> ml_ply1_0 = new ArrayList<GenericMove>();
		ml_ply1_0.add(new GenericMove("a5d5"));
		ml_ply1_0.add(new GenericMove("g7c3"));
		eval1_0.trans = new Transposition((byte)1, (short)0, ScoreType.exact, ml_ply1_0, new GenericMove("a5d5"));

		TranspositionEvaluation eval1_1 = new TranspositionEvaluation();
		eval1_1.status = TranspositionTableStatus.sufficientSeedMoveList;
		ArrayList<GenericMove> ml_ply1_1 = new ArrayList<GenericMove>();
		ml_ply1_1.add(new GenericMove("c3a5"));
		ml_ply1_1.add(new GenericMove("g6g5"));
		eval1_1.trans = new Transposition((byte)1, (short)-400, ScoreType.exact, ml_ply1_1, new GenericMove("c3a5"));
		
		when(mock_hashMap.getTransposition((byte)0, 2)).thenReturn(eval0);
		when(mock_hashMap.getTransposition((byte)1, 1)).thenReturn(eval1_0).thenReturn(eval1_1);
		
		assertEquals(0, classUnderTest.normalSearchPly());
	}
	
	@Test
	public void test_when_aborted_doesnt_update_transposition_table() throws InvalidPieceException, IllegalNotationException {
		initialisePositionAndSearch("6k1/5pb1/6p1/r2R4/8/2q5/1B3PP1/5RK1 w - - 0 1", (byte)2);
		
	    setupBackUpToRootNodeTerminatesTest();
		doReturn(new TranspositionEvaluation()).when(mock_hashMap).getTransposition(anyByte(), anyInt());
		verify(mock_hashMap, never()).setTransposition(any(SearchMetrics.class), anyByte(), (Transposition)isNull(), any(Transposition.class));
		classUnderTest.normalSearchPly();
	}

	private void setupBackUpToRootNodeTerminatesTest() throws InvalidPieceException {
		doAnswer(new Answer<Void>(){
            public Void answer(InvocationOnMock invocation) throws Throwable {
            	classUnderTest.terminateFindMove();
                return null;
            }}).when(mock_hashMap).createPrincipalContinuation(any(PrincipalContinuation.class), anyByte(), any(PositionManager.class));
	}
}
