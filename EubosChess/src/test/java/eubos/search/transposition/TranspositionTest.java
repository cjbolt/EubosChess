package eubos.search.transposition;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.position.Move;
import eubos.position.PositionManager;
import eubos.score.PawnEvalHashTable;
import eubos.search.DrawChecker;

class TranspositionTest {

	@Test
	void testReadbackDepthSearchedInPly() {
		long trans = Transposition.setDepthSearchedInPly(0L, (byte)10);
		assertEquals(10, Transposition.getDepthSearchedInPly(trans));
	}
	
	@Test
	void testReadbackDepthSearchedInPly_AdjacentValue() {
		long trans = Transposition.setDepthSearchedInPly(0L, (byte)10);
		trans = Transposition.setScore(trans, (short) 65535);
		assertEquals(10, Transposition.getDepthSearchedInPly(trans));
		// This means an overflow occurred, i.e. a large positive number became -1 as a signed short
		// but at least it is guarded and doesn't impinge on adjacent bit field.
		assertEquals(-1, Transposition.getScore(trans));
	}

	@Test
	void testReadbackScore() {
		long trans = Transposition.setScore(0L, (short)100);
		assertEquals(100, Transposition.getScore(trans));
	}

	@Test
	void testModifyReadbackScore() {
		long trans = Transposition.setScore(0xFFL, (short)100);
		assertEquals(100, Transposition.getScore(trans));
		assertEquals((100L<<32)+0xFFL, trans);
	}
	
	PositionManager pm;
	
	protected void setUpPosition(String fen) {
		pm = new PositionManager(fen, new DrawChecker(), new PawnEvalHashTable());
	}
	
	@Test
	void test_canRebuildBestMoveFromBoard() {
		setUpPosition("k7/8/8/8/8/8/8/4K2R w K - - 1");
		int move = Move.NULL_MOVE;
		try {
			move = Move.toMove(new GenericMove("h1h2"), pm.getTheBoard());
		} catch (IllegalNotationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		move = Move.setBest(move);
		long trans = 0L;
		trans = Transposition.setBestMove(trans, move);
		
		assertEquals(move, Move.valueOfFromTransposition(trans, pm.getTheBoard()));
	}
	
	@Test
	void test_canRebuildBestMoveFromBoardWhenPromotion() {
		setUpPosition("k7/7P/8/8/8/8/8/4K2R w K - - 1");
		int move = Move.NULL_MOVE;
		try {
			move = Move.toMove(new GenericMove("h7h8=Q"), pm.getTheBoard());
		} catch (IllegalNotationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		move = Move.setBest(move);
		long trans = 0L;
		trans = Transposition.setBestMove(trans, move);
		
		assertEquals(move, Move.valueOfFromTransposition(trans, pm.getTheBoard()));
	}
}
