package eubos.search;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.position.PositionManager;
import eubos.search.Transposition.ScoreType;
import eubos.search.TranspositionTableAccessor.TranspositionEval;
import eubos.search.TranspositionTableAccessor.TranspositionTableStatus;

public class TranspositionTableAccessorTest {

	FixedSizeTranspositionTable transTable;
	PositionManager pm;
	ScoreTracker st;
	PrincipalContinuation pc;
	List<GenericMove> lastPc;
	private SearchMetrics sm;
	
	private  static final int SEARCH_DEPTH_IN_PLY = 4;
	
	byte currPly; 
	
	TranspositionTableAccessor sut;
	TranspositionEval eval;
	
	@Before
	public void setUp() throws Exception {
		sm = new SearchMetrics();
		transTable = new FixedSizeTranspositionTable();
		st = new ScoreTracker(SEARCH_DEPTH_IN_PLY, true);
		st.setProvisionalScoreAtPly((byte) 0);
		pm = new PositionManager();
		lastPc = null;
		sut = new TranspositionTableAccessor(transTable, pm, st, lastPc);
		currPly = 0;
	}

	@Test
	public void testEval_WhenEmpty_insufficientNoData() {
		eval = sut.getTransposition(currPly, SEARCH_DEPTH_IN_PLY);
		assertEquals(TranspositionTableStatus.insufficientNoData, eval.status);
	}
	
	@Test
	public void testEval_StoreRetrieve_sufficientTerminalNode() throws InvalidPieceException, IllegalNotationException {
		List<GenericMove> ml = new LinkedList<GenericMove>();
		ml.add(new GenericMove("e2e4"));
		ml.add(new GenericMove("d2d4"));
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(new GenericMove("e2e4"));
		Transposition new_trans = new Transposition((byte)1, (short)105, ScoreType.exact, ml, pc.get(0));
		
		sut.setTransposition(sm, currPly, null, new_trans);
		
		eval = sut.getTransposition(currPly, 1);
		
		assertEquals(TranspositionTableStatus.sufficientTerminalNode, eval.status);
	}
	
	@Test
	public void testEval_StoreRetrieve_sufficientSeedMoveList() throws InvalidPieceException, IllegalNotationException {
		List<GenericMove> ml = new LinkedList<GenericMove>();
		ml.add(new GenericMove("e2e4"));
		ml.add(new GenericMove("d2d4"));
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(new GenericMove("e2e4"));
		Transposition new_trans = new Transposition((byte)1, (short)105, ScoreType.exact, ml, pc.get(0));
		
		sut.setTransposition(sm, currPly, null, new_trans);
		
		eval = sut.getTransposition(currPly, 2);
		
		assertEquals(TranspositionTableStatus.sufficientSeedMoveList, eval.status);
	}
	
	@Test
	@Ignore
	public void testEval_StoreRetrieve_whenNoMoveList_insufficientNoData() throws InvalidPieceException, IllegalNotationException {
		List<GenericMove> pc = new ArrayList<GenericMove>();
		Transposition new_trans = new Transposition((byte)1, (short)105, ScoreType.exact, null, pc.get(0));
		
		sut.setTransposition(sm, currPly, null, new_trans);
		
		eval = sut.getTransposition(currPly, 2);
		
		assertEquals(TranspositionTableStatus.insufficientNoData, eval.status);
	}
	
	@Test
	public void testEval_StoreRetrieve_whenUpperBound_AndScoreIsLower_sufficientRefutation() throws InvalidPieceException, IllegalNotationException {
		List<GenericMove> ml = new LinkedList<GenericMove>();
		ml.add(new GenericMove("e2e4"));
		ml.add(new GenericMove("d2d4"));
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(new GenericMove("e2e4"));
		Transposition new_trans = new Transposition((byte)1, (short)105, ScoreType.upperBound, ml, pc.get(0));

		currPly = 2;
		sut.setTransposition(sm, currPly, null, new_trans);
		
		st.setBackedUpScoreAtPly(currPly, (short)100);
		eval = sut.getTransposition(currPly, 1);
		
		assertEquals(TranspositionTableStatus.sufficientRefutation, eval.status);
	}
	
	@Test
	public void testEval_StoreRetrieve_whenLowerBound_AndScoreIsHigher_sufficientRefutation() throws InvalidPieceException, IllegalNotationException {
		List<GenericMove> ml = new LinkedList<GenericMove>();
		ml.add(new GenericMove("e2e4"));
		ml.add(new GenericMove("d2d4"));
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(new GenericMove("e2e4"));
		Transposition new_trans = new Transposition((byte)1, (short)105, ScoreType.lowerBound, ml, pc.get(0));

		currPly = 2;
		sut.setTransposition(sm, currPly, null, new_trans);
		
		st.setBackedUpScoreAtPly(currPly, (short)110);
		eval = sut.getTransposition(currPly, 1);
		
		assertEquals(TranspositionTableStatus.sufficientRefutation, eval.status);
	}
	
	@Test
	public void testUpdateWorks_whenNew() throws IllegalNotationException {
		List<GenericMove> ml = new LinkedList<GenericMove>();
		ml.add(new GenericMove("e2e4"));
		ml.add(new GenericMove("d2d4"));
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(new GenericMove("e2e4"));
		Transposition new_trans = new Transposition((byte)1, (short)105, ScoreType.lowerBound, ml, pc.get(0));

		currPly = 2;
		Transposition stored_trans = sut.setTransposition(sm, currPly, null, new_trans);
		
		assertEquals(stored_trans, new_trans);
	}
	
	@Test
	public void testUpdateWorks_whenExistingUpdated() throws IllegalNotationException {
		GenericMove move1 = new GenericMove("e2e4");
		GenericMove move2 = new GenericMove("d2d4");
		List<GenericMove> ml = new LinkedList<GenericMove>();
		ml.add(move1);
		ml.add(move2);
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(move1);
		Transposition upper_trans = new Transposition((byte)1, (short)105, ScoreType.lowerBound, ml, move1);

		currPly = 2;
		Transposition stored_trans = sut.setTransposition(sm, currPly, null, upper_trans);
		
		Transposition exact_trans = new Transposition((byte)1, (short)110, ScoreType.exact, ml, move2);
		stored_trans = sut.setTransposition(sm, currPly, stored_trans, exact_trans);
		
		// check hash data is updated, not replaced
		assertEquals(stored_trans, upper_trans);
		assertEquals(ScoreType.exact, stored_trans.getScoreType());
		assertEquals(110, stored_trans.getScore());
		
		// check move list order is updated
		assertEquals(move2, stored_trans.getBestMove());
		assertEquals(move2, stored_trans.getMoveList().get(0));
		assertEquals(move1, stored_trans.getMoveList().get(1));
		
		// Check eval returns expected hash data
		eval = sut.getTransposition(currPly, 1);
		assertEquals(stored_trans, eval.trans);
		assertEquals(move2, eval.trans.getBestMove());
		
	}
}
