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
//
//	@Test
//	void testReadbackDepthSearchedInPly() {
//		long reference = Transposition.valueOf((byte)-3, (char)15, (byte)2, (char)0, (char)5);
//		long trans = Transposition.valueOf((byte)-3, (char)15, (byte)2, (char)0, (char)5, (char)Short.MAX_VALUE);
//		assertEquals(reference, trans);
//		assertEquals(-3, Transposition.getDepthSearchedInPly(trans));
//	}
//	
//	@Test
//	void testReadbackNegativeDepthSearchedInPly() {
//		long trans = Transposition.setDepthSearchedInPly(0L, (byte)10);
//		assertEquals(10, Transposition.getDepthSearchedInPly(trans));
//	}
//	
//	@Test
//	void testReadbackDepthSearchedInPly_AdjacentValue() {
//		long trans = Transposition.setDepthSearchedInPly(0L, (byte)10);
//		trans = Transposition.setScore(trans, (char) 65535);
//		assertEquals(10, Transposition.getDepthSearchedInPly(trans));
//		// This means an overflow occurred, i.e. a large positive number became -1 as a signed short
//		// but at least it is guarded and doesn't impinge on adjacent bit field.
//		assertEquals(-1, Transposition.getScore(trans));
//	}
//
//	@Test
//	void testReadbackScore() {
//		long trans = Transposition.setScore(0L, (char)100);
//		assertEquals(100, Transposition.getScore(trans));
//	}
//
//	@Test
//	void testModifyReadbackScore() {
//		long trans = Transposition.setScore(0xFFL, (char)100);
//		assertEquals(100, Transposition.getScore(trans));
//		assertEquals((100L<<32)+0xFFL, trans);
//	}
//	
//	@Test
//	void testReadbackAge() {
//		long trans = Transposition.setAge(0L, (char)15);
//		assertEquals(15, Transposition.getAge(trans));
//	}
//
//	@Test
//	void testModifyReadbackAge() {
//		long trans = Transposition.setScore(0x30L << 58, (char)15);
//		assertEquals(15, Transposition.getScore(trans));
//	}
//	
//	@Test
//	void testReadbackStatic() {
//		long trans = Transposition.setStaticEval(0xFFFFFFFF_FFFFFFFFL, (char)-9734);
//		assertEquals(-9734, Transposition.getStaticEval(trans));
//	}
//	
//	@Test
//	void testReadbackStaticAlt() {
//		long trans = Transposition.setStaticEval(0x0L, (char)-9734);
//		assertEquals(-9734, Transposition.getStaticEval(trans));
//		assertEquals(Move.NULL_MOVE, Transposition.getBestMove(trans));
//		assertEquals(0, Transposition.getDepthSearchedInPly(trans));
//		assertEquals(0, Transposition.getAge(trans));
//		trans = Transposition.setDepthSearchedInPly(trans, (byte)-3);
//		assertEquals(0, Transposition.getAge(trans));
//		assertEquals(Move.NULL_MOVE, Transposition.getBestMove(trans));
//		assertEquals(0, Transposition.getScore(trans));
//		assertEquals(-9734, Transposition.getStaticEval(trans));
//		assertEquals(-3, Transposition.getDepthSearchedInPly(trans));
//	}
//
//	@Test
//	void testModifyReadbackStatic() {
//		long trans = Transposition.setScore(0x30L << 58, (char)15);
//		assertEquals(15, Transposition.getScore(trans));
//	}
//	
//	PositionManager pm;
//	
//	protected void setUpPosition(String fen) {
//		pm = new PositionManager(fen, new DrawChecker(), new PawnEvalHashTable());
//	}
//	
//	@Test
//	void test_canRebuildBestMoveFromBoard() {
//		setUpPosition("k7/8/8/8/8/8/8/4K2R w K - - 1");
//		int move = Move.NULL_MOVE;
//		try {
//			move = Move.toMove(new GenericMove("h1h2"), pm.getTheBoard());
//		} catch (IllegalNotationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		move = Move.setBest(move);
//		long trans = 0L;
//		trans = Transposition.setBestMove(trans, (char)move);
//		
//		assertEquals(move, Move.valueOfFromTransposition(trans, pm.getTheBoard()));
//	}
//	
//	@Test
//	void test_canRebuildBestMoveFromBoardWhenPromotion() {
//		setUpPosition("k7/7P/8/8/8/8/8/4K2R w K - - 1");
//		int move = Move.NULL_MOVE;
//		try {
//			move = Move.toMove(new GenericMove("h7h8=Q"), pm.getTheBoard());
//		} catch (IllegalNotationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		move = Move.setBest(move);
//		long trans = 0L;
//		trans = Transposition.setBestMove(trans, (char)move);
//		
//		assertEquals(move, Move.valueOfFromTransposition(trans, pm.getTheBoard()));
//	}
//	
//	@Test
//	void test_optimisedValueOf() {
//		long reference = Transposition.valueOf((byte)1, (char)15, (byte)2, (char)0, (char)5);
//		assertEquals(reference, Transposition.valueOf((byte)1, (char)15, (byte)2, (char)0, (char)5, (char)Short.MAX_VALUE));
//		
//		long reference1 = Transposition.valueOf((byte)-1, (char)-15, (byte)2, (char)0, (char)5);
//		assertEquals(reference1, Transposition.valueOf((byte)-1, (char)-15, (byte)2, (char)0, (char)5, (char)Short.MAX_VALUE));
//	}
}
