package eubos.search;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.position.PositionManager;
import eubos.score.MaterialEvaluator;
import eubos.score.PositionEvaluator;
import eubos.search.transposition.ITranspositionAccessor;
import eubos.search.transposition.Transposition;
import eubos.search.transposition.TranspositionEvaluation;
import eubos.search.transposition.TranspositionEvaluation.TranspositionTableStatus;
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
	List<Integer> lastPc;
	private ITranspositionAccessor mock_hashMap;
	private ScoreTracker st;
	
	@Before
	public void setUp() throws Exception {
		SearchDebugAgent.open(0, true);
		
		pc = new PrincipalContinuation(searchDepth*3);
		sm = new SearchMetrics(searchDepth*3, new PositionManager());
		sm.setPrincipalVariation(pc.toPvList(0));
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
		pm = new PositionManager(fen, new DrawChecker());
		PositionEvaluator pe = new PositionEvaluator(pm, new DrawChecker());
		classUnderTest = new PlySearcher(
				mock_hashMap,
				st,
			    pc,
				sm,
				sr,
				depth,
				pm,
				pm,
				lastPc,
				pe);		
	}
	
	@Test
	@Ignore // Till resolve fact that pawn is blocked but this position is judged not quiescent
	public void test_depthSearchedUpdates() throws InvalidPieceException, IllegalNotationException {
		initialisePositionAndSearch("7K/7P/8/6Q1/3k4/8/8/8 w - - 1 69", (byte)4);
		doReturn(new TranspositionEvaluation()).when(mock_hashMap).getTransposition(anyByte(), anyInt());

		assertEquals(2*MaterialEvaluator.MATERIAL_VALUE_QUEEN-40 /* relative pos of Kings, endgame */, classUnderTest.searchPly().getScore());		
	}
	
	@Test
	@Ignore
	public void test_singleLineOfPlay_depthSearchedUpdates() throws InvalidPieceException, IllegalNotationException {
		initialisePositionAndSearch("8/8/1P6/8/5p2/8/8/8 w - - 0 1", (byte)4);
		
		doReturn(new TranspositionEvaluation()).when(mock_hashMap).getTransposition(anyByte(), anyInt());
		
		doReturn(new Transposition((byte)1, (short)0, (byte) 1, null)).when(mock_hashMap).setTransposition(anyByte(), (Transposition)isNull(), anyByte(), anyShort(), any(byte.class), anyInt());
		
		assertEquals(650, classUnderTest.searchPly());
		
		verify(mock_hashMap, times(8)).setTransposition(anyByte(), (Transposition)isNull(), anyByte(), anyShort(), any(byte.class), anyInt());
		
		ArgumentCaptor<Integer> captorNew = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<Transposition> captorOld = ArgumentCaptor.forClass(Transposition.class);
		ArgumentCaptor<Byte> captorPly = ArgumentCaptor.forClass(Byte.class);
		verify(mock_hashMap, times(8)).setTransposition(captorPly.capture(), captorOld.capture(), anyByte(), anyShort(), any(byte.class), captorNew.capture());
		List<Integer> new_trans_args = captorNew.getAllValues();
		List<Transposition> trans_args = captorOld.getAllValues();
		List<Byte> plies = captorPly.getAllValues();
		
		assertEquals(new GenericMove("f3f2"),Move.toGenericMove(new_trans_args.get(0)));
		assertNull(trans_args.get(0));
		assertEquals(3, plies.get(0).byteValue());
		
		assertEquals(new GenericMove("b7b8q"),Move.toGenericMove(new_trans_args.get(1)));
		assertNull(trans_args.get(1));
		assertEquals(2, plies.get(1).byteValue());
		
		assertEquals(new GenericMove("f3f2"),Move.toGenericMove(new_trans_args.get(2)));
		assertNull(trans_args.get(2));
		assertEquals(3, plies.get(2).byteValue());
		
		assertEquals(new GenericMove("f3f2"),Move.toGenericMove(new_trans_args.get(3)));
		assertNull(trans_args.get(3));
		assertEquals(3, plies.get(3).byteValue());
		
		assertEquals(new GenericMove("f3f2"),Move.toGenericMove(new_trans_args.get(4)));
		assertNull(trans_args.get(4));
		assertEquals(3, plies.get(4).byteValue());
		
		assertEquals(new GenericMove("b7b8q"),Move.toGenericMove(new_trans_args.get(5)));
		assertNotNull(trans_args.get(5));
		assertEquals(2, plies.get(5).byteValue());
		
		assertEquals(new GenericMove("f4f3"),Move.toGenericMove(new_trans_args.get(6)));
		assertNull(trans_args.get(6));
		assertEquals(1, plies.get(6).byteValue());
		
		assertEquals(new GenericMove("b6b7"),Move.toGenericMove(new_trans_args.get(7)));
		assertNull(trans_args.get(7));
		assertEquals(0, plies.get(7).byteValue());
	}	 
	
	@Test
	@Ignore
	public void test_singleLineOfPlay_exactHashHit() throws InvalidPieceException, IllegalNotationException {
		initialisePositionAndSearch("8/8/1P6/8/5p2/8/8/8 w - - 0 1", (byte)1);
		
		TranspositionEvaluation eval = new TranspositionEvaluation();
		eval.status = TranspositionTableStatus.sufficientTerminalNode;
		eval.trans = new Transposition((byte)1, (short)50, Score.exact, new GenericMove("b6b7"));
		
		when(mock_hashMap.getTransposition((byte)0, 1)).thenReturn(eval);
		
		assertEquals(50, classUnderTest.searchPly().getScore());
	}
	
	@Test
	@Ignore
	public void test_refutation() throws InvalidPieceException, IllegalNotationException {
		initialisePositionAndSearch("6k1/5pb1/6p1/r2R4/8/2q5/1B3PP1/5RK1 w - - 0 1", (byte)2);
		
		TranspositionEvaluation eval0 = new TranspositionEvaluation();
		eval0.status = TranspositionTableStatus.sufficientSeedMoveList;
		eval0.trans = new Transposition((byte)1, (short)-5, Score.lowerBound, new GenericMove("b2c3"));
		
		TranspositionEvaluation eval1_0 = new TranspositionEvaluation();
		eval1_0.status = TranspositionTableStatus.sufficientTerminalNode;
		eval1_0.trans = new Transposition((byte)1, (short)0, Score.exact, new GenericMove("a5d5"));

		TranspositionEvaluation eval1_1 = new TranspositionEvaluation();
		eval1_1.status = TranspositionTableStatus.sufficientSeedMoveList;
		eval1_1.trans = new Transposition((byte)1, (short)-400, Score.exact, new GenericMove("c3a5"));
		
		when(mock_hashMap.getTransposition((byte)0, 2)).thenReturn(eval0);
		when(mock_hashMap.getTransposition((byte)1, 1)).thenReturn(eval1_0).thenReturn(eval1_1);
		
		assertEquals(0, classUnderTest.searchPly());
	}
	
	@Test
	@Ignore
	public void test_when_aborted_doesnt_update_transposition_table() throws InvalidPieceException, IllegalNotationException {
		initialisePositionAndSearch("6k1/5pb1/6p1/r2R4/8/2q5/1B3PP1/5RK1 w - - 0 1", (byte)2);
		
	    //setupBackUpToRootNodeTerminatesTest();
		doReturn(new TranspositionEvaluation()).when(mock_hashMap).getTransposition(anyByte(), anyInt());
		verify(mock_hashMap, never()).setTransposition(anyByte(), (Transposition)isNull(), anyByte(), anyShort(), any(byte.class), anyInt());
		classUnderTest.searchPly();
	}

	/*private void setupBackUpToRootNodeTerminatesTest() throws InvalidPieceException {
		doAnswer(new Answer<Void>(){
            public Void answer(InvocationOnMock invocation) throws Throwable {
            	classUnderTest.terminateFindMove();
                return null;
            }}).when(mock_hashMap).createPrincipalContinuation(any(PrincipalContinuation.class), anyByte(), any(PositionManager.class));
	}*/
}
