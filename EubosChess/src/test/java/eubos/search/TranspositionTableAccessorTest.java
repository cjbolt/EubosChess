package eubos.search;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;


import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.position.PositionManager;
import eubos.search.transposition.FixedSizeTranspositionTable;
import eubos.search.transposition.Transposition;
import eubos.search.transposition.TranspositionTableAccessor;

public class TranspositionTableAccessorTest {

	FixedSizeTranspositionTable transTable;
	PositionManager pm;
	PrincipalContinuation pc;
	List<GenericMove> lastPc;
	
	//private static final int SEARCH_DEPTH_IN_PLY = 4;
	
	byte currPly; 
	
	TranspositionTableAccessor sut;
	long trans;
	
	@Before
	public void setUp() throws Exception {
		transTable = new FixedSizeTranspositionTable();
		SearchDebugAgent sda = new SearchDebugAgent(0, true);
		pm = new PositionManager();
		sut = new TranspositionTableAccessor(transTable, pm, sda);
		currPly = 0;
	}
	
	@After
	public void tearDown() {
	}

	@Test
	public void testtrans_WhenEmpty_insufficientNoData() {
		trans = sut.getTransposition();
		assertEquals(0L, trans);
	}
	
	@Test
	public void testtrans_StoreRetrieve_sufficientTerminalNode()throws IllegalNotationException {
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(new GenericMove("e2e4"));
		
		sut.setTransposition(0L, (byte)1, (short)105, Score.exact, Move.toMove(pc.get(0), pm.getTheBoard()));
		
		trans = sut.getTransposition();
		
		assertEquals(Score.exact, Transposition.getType(trans));
		}
	}
	
	@Test
	public void testtrans_StoreRetrieve_sufficientSeedMoveList()throws IllegalNotationException {
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(new GenericMove("e2e4"));
		
		sut.setTransposition(0L, (byte)1, (short)105, Score.exact, Move.toMove(pc.get(0)));
		
		trans = sut.getTransposition();
		
		//assertEquals(Status.sufficientSeedMoveList, trans.getBestMove());
		}
	}
	
	@Test
	@Ignore
	public void testtrans_StoreRetrieve_whenNoMoveList_insufficientNoData()throws IllegalNotationException {
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
		List<GenericMove> pc = new ArrayList<GenericMove>();
		
		sut.setTransposition(0L, (byte)1, (short)105, Score.exact, Move.toMove(pc.get(0)));
		
		trans = sut.getTransposition();
		
		//assertEquals(Status.insufficientNoData, trans.status);
		}
	}
	
	@Test
	public void testtrans_StoreRetrieve_whenUpperBound_AndScoreIsLower_inSufficientRefutation()throws IllegalNotationException {
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(new GenericMove("e2e4"));
		
		sut.setTransposition(0L, (byte)1, (short)18, Score.upperBound, Move.toMove(pc.get(0)));
		
		// Set up score tracker according to diagram
		trans = sut.getTransposition();
		
		//assertEquals(Status.sufficientSeedMoveList, trans.status);
		}
	}
	
	@Test
	@Ignore
	public void testtrans_StoreRetrieve_whenLowerBound_AndScoreIsHigher_sufficientRefutation()throws IllegalNotationException {
		/* Example from second limb of search tree, fig9.15, pg.178, How Computers Play Chess, Newborn and Levy */
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(new GenericMove("e2e4"));

		currPly = 3;
		sut.setTransposition(0L, (byte)1, (short)18, Score.upperBound, Move.toMove(pc.get(0), pm.getTheBoard()));
		
		// Set up score tracker according to diagram
		trans = sut.getTransposition();
		
		//assertEquals(Status.sufficientRefutation, trans.status);
	}
	
	@Test
	public void testUpdateWorks_whenNew() throws IllegalNotationException {
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(new GenericMove("e2e4"));

		currPly = 2;
		sut.setTransposition(0L, (byte)1, (short)105, Score.lowerBound, Move.toMove(pc.get(0)));
		}
	}
	
	@Test
	public void testUpdateWorks_whenExistingUpdated() throws IllegalNotationException {
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
		GenericMove move1 = new GenericMove("e2e4");
		GenericMove move2 = new GenericMove("d2d4");
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(move1);

		currPly = 2;
		long stored_trans = sut.setTransposition(0L, (byte)1, (short)105, Score.lowerBound, Move.toMove(move1, pm.getTheBoard()));
		
		stored_trans = sut.setTransposition(stored_trans, (byte)1, (short)110, Score.exact, Move.toMove(move2, pm.getTheBoard()));
		
		assertEquals(Score.exact, Transposition.getType(stored_trans));
		assertEquals(110, Transposition.getScore(stored_trans));
		
		// check move list order is updated
		//assertTrue(Move.areEqual(Move.toMove(move2), stored_trans.getBestMove(null)));
		
		// Check trans returns expected hash data
		trans = sut.getTransposition();
		//assertEquals(stored_trans, trans.trans);
		//assertTrue(Move.areEqual(Move.toMove(move2), trans.trans.getBestMove()));
		}
	}
	
	@Test
	public void testUpdateWorks_whenExistingUpdated_ArenaError() throws IllegalNotationException {
		if (EubosEngineMain.ENABLE_TRANSPOSITION_TABLE) {
		pm = new PositionManager("8/8/p6p/1p3kp1/1P6/P4PKP/5P2/8 w - - 0 1"); //Endgame pos
		sut = new TranspositionTableAccessor(transTable, pm, new SearchDebugAgent(0, true));
		GenericMove move1 = new GenericMove("h3h4");
		GenericMove move2 = new GenericMove("f3f4");
		
		List<GenericMove> pc = new ArrayList<GenericMove>();
		pc.add(move1);
		
		currPly = 0;
		long stored_trans = sut.setTransposition(0L, (byte)9, (short)25, Score.lowerBound, Move.toMove(move1));
		
		stored_trans = sut.setTransposition(stored_trans, (byte)9, (short)72, Score.lowerBound, Move.toMove(move2));
		
		assertEquals(Score.lowerBound, Transposition.getType(stored_trans));
		assertEquals(72, Transposition.getScore(stored_trans));
		
		// check move list order is updated
		//assertTrue(Move.areEqual(Move.toMove(move2), stored_trans.getBestMove(null)));
		
		// Check trans returns expected hash data
		trans = sut.getTransposition();
		//assertEquals(stored_trans, trans.trans);
		//assertTrue(Move.areEqual(Move.toMove(move2), trans.trans.getBestMove()));
		}
	}
}
