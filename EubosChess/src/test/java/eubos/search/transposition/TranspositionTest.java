package eubos.search.transposition;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.Piece;
import eubos.position.Move;
import eubos.position.Position;
import eubos.position.PositionManager;
import eubos.score.PawnEvalHashTable;
import eubos.search.DrawChecker;
import eubos.search.Score;

class TranspositionTest {

//	@Test
//	void test_ValueOf_UnitTests_Depth_Score_Bound_GenericMove() throws IllegalNotationException {
//		long trans = Transposition.valueOf((byte) 4, (short)-10, Score.upperBound, new GenericMove("a1b1"));
//		assertEquals(4, Transposition.getDepthSearchedInPly(trans));
//		assertEquals(-10, Transposition.getScore(trans));
//		assertEquals(Score.upperBound, Transposition.getType(trans));
//		// Sets the best move bit in the type field, automatically on writing to transposition table.
//		assertEquals(Move.valueOf(Move.TYPE_BEST_MASK, Position.a1, Piece.NONE, Position.b1, Piece.NONE, Piece.NONE), Transposition.getBestMove(trans));
//	}
//
//	@Test
//	void test_ValueOf_Depth_Score_Bound_BestMove() {
//		int bestMove = Move.valueOf(Position.a1, Piece.WHITE_ROOK, Position.b1, Piece.NONE);
//		long trans = Transposition.valueOf((byte) 4, (short)-10, Score.upperBound, bestMove, 0);
//		assertEquals(4, Transposition.getDepthSearchedInPly(trans));
//		assertEquals(-10, Transposition.getScore(trans));
//		assertEquals(Score.upperBound, Transposition.getType(trans));
//		// Sets the best move bit in the type field, automatically on writing to transposition table.
//		assertEquals(Move.setBest(bestMove), Transposition.getBestMove(trans));
//	}

	@Test
	void testReadbackDepthSearchedInPly() {
		long trans = Transposition.setDepthSearchedInPly(0L, (byte)10);
		assertEquals(10, Transposition.getDepthSearchedInPly(trans));
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
		
		assertEquals(move, Transposition.getBestMove(trans, pm.getTheBoard()));
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
		
		assertEquals(move, Transposition.getBestMove(trans, pm.getTheBoard()));
	}
}
