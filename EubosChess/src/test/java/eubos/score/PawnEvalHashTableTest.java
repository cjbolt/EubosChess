package eubos.score;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.Before;
import org.junit.jupiter.api.Test;

import eubos.position.PositionManager;
import eubos.search.DrawChecker;

class PawnEvalHashTableTest {
	
	PawnEvalHashTable SUT;
	PositionManager pm;
	long whitePawns;
	long blackPawns;
	
	@Before
	public void setUp() throws Exception {
	}

	protected void setUpPosition(String fen) {
		SUT = new PawnEvalHashTable();
		pm = new PositionManager(fen, new DrawChecker(), SUT);
		whitePawns = pm.getTheBoard().getWhitePawns();
		blackPawns = pm.getTheBoard().getBlackPawns();
	}
	
	@Test
	void test_readback() {
		setUpPosition("8/pppppppp/8/8/P7/P7/P3PPPP/8 w - - 0 1");
		SUT.put(pm.getPawnHash(), -256, pm.getTheBoard().getPawns(), true);
		assertEquals(-256 , SUT.get(pm.getPawnHash(), pm.getTheBoard().getPawns(), true));
	}
	
	@Test
	void test_always_replace_previous_scores_are_overwritten() {
		setUpPosition("8/pppppppp/8/8/P7/P7/P3PPPP/8 w - - 0 1");
		SUT.put(pm.getPawnHash(), -256, pm.getTheBoard().getPawns(), true);
		SUT.put(pm.getPawnHash(), -25, pm.getTheBoard().getPawns(), true);
		assertEquals(-25 , SUT.get(pm.getPawnHash(), pm.getTheBoard().getPawns(), true));
	}
}
