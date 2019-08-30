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
import eubos.position.Transposition;
import eubos.position.Transposition.ScoreType;
import eubos.search.TranspositionTableAccessor.TranspositionEval;
import eubos.search.TranspositionTableAccessor.TranspositionTableStatus;

public class TranspositionTableAccessorTest {

	FixedSizeTranspositionTable transTable;
	PositionManager pm;
	ScoreTracker st;
	PrincipalContinuation pc;
	List<GenericMove> lastPc;
	
	private  static final int SEARCH_DEPTH_IN_PLY = 4;
	
	byte currPly; 
	
	TranspositionTableAccessor sut;
	TranspositionEval eval;
	
	@Before
	public void setUp() throws Exception {
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
		eval = sut.evaluateTranspositionData(currPly, SEARCH_DEPTH_IN_PLY);
		assertEquals(TranspositionTableStatus.insufficientNoData, eval.status);
	}
	
	@Test
	public void testEval_StoreRetrieve_sufficientTerminalNode() throws InvalidPieceException, IllegalNotationException {
		List<GenericMove> ml = new LinkedList<GenericMove>();
		ml.add(new GenericMove("e2e4"));
		ml.add(new GenericMove("d2d4"));
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(new GenericMove("e2e4"));
		Transposition new_trans = new Transposition((byte)1, (short)105, ScoreType.exact, ml, pc);
		
		sut.getTransCreateIfNew(currPly, new_trans);
		
		eval = sut.evaluateTranspositionData(currPly, 1);
		
		assertEquals(TranspositionTableStatus.sufficientTerminalNode, eval.status);
	}
	
	@Test
	public void testEval_StoreRetrieve_sufficientSeedMoveList() throws InvalidPieceException, IllegalNotationException {
		List<GenericMove> ml = new LinkedList<GenericMove>();
		ml.add(new GenericMove("e2e4"));
		ml.add(new GenericMove("d2d4"));
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(new GenericMove("e2e4"));
		Transposition new_trans = new Transposition((byte)1, (short)105, ScoreType.exact, ml, pc);
		
		sut.getTransCreateIfNew(currPly, new_trans);
		
		eval = sut.evaluateTranspositionData(currPly, 2);
		
		assertEquals(TranspositionTableStatus.sufficientSeedMoveList, eval.status);
	}
	
	@Test
	@Ignore
	public void testEval_StoreRetrieve_whenNoMoveList_insufficientNoData() throws InvalidPieceException, IllegalNotationException {
		List<GenericMove> pc = new ArrayList<GenericMove>();
		Transposition new_trans = new Transposition((byte)1, (short)105, ScoreType.exact, null, pc);
		
		sut.getTransCreateIfNew(currPly, new_trans);
		
		eval = sut.evaluateTranspositionData(currPly, 2);
		
		assertEquals(TranspositionTableStatus.insufficientNoData, eval.status);
	}
	
	@Test
	public void testEval_StoreRetrieve_whenUpperBound_AndScoreIsLower_sufficientRefutation() throws InvalidPieceException, IllegalNotationException {
		List<GenericMove> ml = new LinkedList<GenericMove>();
		ml.add(new GenericMove("e2e4"));
		ml.add(new GenericMove("d2d4"));
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(new GenericMove("e2e4"));
		Transposition new_trans = new Transposition((byte)1, (short)105, ScoreType.upperBound, ml, pc);

		currPly = 2;
		sut.getTransCreateIfNew(currPly, new_trans);
		
		st.setBackedUpScoreAtPly(currPly, (short)100);
		eval = sut.evaluateTranspositionData(currPly, 1);
		
		assertEquals(TranspositionTableStatus.sufficientRefutation, eval.status);
	}
	
	@Test
	public void testEval_StoreRetrieve_whenLowerBound_AndScoreIsHigher_sufficientRefutation() throws InvalidPieceException, IllegalNotationException {
		List<GenericMove> ml = new LinkedList<GenericMove>();
		ml.add(new GenericMove("e2e4"));
		ml.add(new GenericMove("d2d4"));
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(new GenericMove("e2e4"));
		Transposition new_trans = new Transposition((byte)1, (short)105, ScoreType.lowerBound, ml, pc);

		currPly = 2;
		sut.getTransCreateIfNew(currPly, new_trans);
		
		st.setBackedUpScoreAtPly(currPly, (short)110);
		eval = sut.evaluateTranspositionData(currPly, 1);
		
		assertEquals(TranspositionTableStatus.sufficientRefutation, eval.status);
	}
}
