package eubos.search;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
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
	
	private  static final int SEARCH_DEPTH_IN_PLY = 2;
	
	byte currPly; 
	
	TranspositionTableAccessor sut;
	TranspositionEval eval;
	
	@Before
	public void setUp() throws Exception {
		transTable = new FixedSizeTranspositionTable();
		pc = new PrincipalContinuation(SEARCH_DEPTH_IN_PLY);
		st = new ScoreTracker(SEARCH_DEPTH_IN_PLY, true);
		pm = new PositionManager();
		lastPc = null;
		sut = new TranspositionTableAccessor(transTable, pm, st, pc, lastPc);
		currPly = 0;
	}

	@Test
	public void testEval_WhenEmpty_insufficientNoData() {
		eval = sut.evaluateTranspositionData(currPly, SEARCH_DEPTH_IN_PLY);
		assertEquals(TranspositionTableStatus.insufficientNoData, eval.status);
	}
	
	@Test
	public void testEval_StoreRetrieve_sufficientTerminalNode() throws InvalidPieceException, IllegalNotationException {
		GenericMove move = new GenericMove("e2e4");		
		byte depthPositionSearchedPly = 1;
		GenericMove bestMove = move;
		short score = 105;
		ScoreType bound = ScoreType.exact;
		List<GenericMove> ml = null;
		Transposition trans = null;
		
		sut.storeTranspositionScore(currPly, depthPositionSearchedPly, bestMove, score, bound, ml, trans);
		
		int search_depth_needed = depthPositionSearchedPly;
		eval = sut.evaluateTranspositionData(currPly, search_depth_needed);
		
		assertEquals(TranspositionTableStatus.sufficientTerminalNode, eval.status);
	}
}
