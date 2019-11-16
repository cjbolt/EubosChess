package eubos.search;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.board.pieces.King;
import eubos.position.MaterialEvaluation;
import eubos.position.MaterialEvaluator;
import eubos.position.PositionManager;

public class SearchContextTest {
	
	private SearchContext sut;
	private PositionManager pm;
	private DrawChecker dc;

	@Before
	public void setUp() throws Exception {
	}
	
	public void setupPosition(String fen) {
		dc = new DrawChecker();
		pm = new PositionManager(fen, dc);
		sut = new SearchContext(pm, MaterialEvaluator.evaluate(pm.getTheBoard()), dc);
	}

	@Test
	public void test_detectSimplification() throws InvalidPieceException, IllegalNotationException {
		setupPosition("5r1k/pp5p/6p1/1N2q3/2P1P1n1/1P6/P2Q2PP/3R2K1 w - - 0 1");
		pm.performMove(new GenericMove("d2d4")); // forces exchange of queens on d4. simplifying
		pm.performMove(new GenericMove("e5d4"));
		pm.performMove(new GenericMove("d1d4"));
		MaterialEvaluation current = MaterialEvaluator.evaluate(pm.getTheBoard());
		assertEquals(SearchContext.SIMPLIFICATION_BONUS, sut.computeSearchGoalBonus(current));
	}
	
	@Test
	public void test_notSimplified_materialNotRecaptured() throws InvalidPieceException, IllegalNotationException {
		setupPosition("5r1k/pp5p/6p1/1N2q3/2P1P1n1/1P6/P2Q2PP/3R2K1 w - - 0 1");
		pm.performMove(new GenericMove("d2d4")); // forces exchange of queens on d4. simplifying
		pm.performMove(new GenericMove("e5d4"));
		MaterialEvaluation current = MaterialEvaluator.evaluate(pm.getTheBoard());
		assertEquals(0, sut.computeSearchGoalBonus(current));
	}

	@Test
	public void test_detectSimplification_black() throws InvalidPieceException, IllegalNotationException {
		setupPosition("3r2k1/p2q2pp/1p6/2p1p1N1/1n2Q3/6P1/PP5P/5R1K b - - 0 1");
		pm.performMove(new GenericMove("d7d5")); // forces exchange of queens on d4. simplifying
		pm.performMove(new GenericMove("e4d5"));
		pm.performMove(new GenericMove("d8d5"));
		MaterialEvaluation current = MaterialEvaluator.evaluate(pm.getTheBoard());
		assertEquals(-SearchContext.SIMPLIFICATION_BONUS, sut.computeSearchGoalBonus(current));
	}
	
	@Test
	public void test_notSimplified_materialNotRecaptured_black() throws InvalidPieceException, IllegalNotationException {
		setupPosition("3r2k1/p2q2pp/1p6/2p1p1N1/1n2Q3/6P1/PP5P/5R1K b - - 0 1");
		pm.performMove(new GenericMove("d7d5")); // forces exchange of queens on d4. simplifying
		pm.performMove(new GenericMove("e4d5"));
		MaterialEvaluation current = MaterialEvaluator.evaluate(pm.getTheBoard());
		assertEquals(0, sut.computeSearchGoalBonus(current));
	}
	
	@Test
	public void test_lichess_pos() throws InvalidPieceException, IllegalNotationException {
		setupPosition("4r1k1/2p2pb1/4Q3/8/3pPB2/1p1P3p/1P3P2/R5K1 b - - 0 42");
		MaterialEvaluation current = MaterialEvaluator.evaluate(pm.getTheBoard());
		assertEquals(0, sut.computeSearchGoalBonus(current));
	}
	
	 
	@Test
	public void test_draw_black() throws InvalidPieceException, IllegalNotationException {
		setupPosition("7q/1P6/8/8/8/8/2k3PQ/7K b - - 0 42");
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("h8a1"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("h2g1"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("a1h8"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("g1h2"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("h8a1"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("h2g1"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("a1h8"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("g1h2"));
		dc.incrementPositionReachedCount(pm.getHash());
		MaterialEvaluation current = MaterialEvaluator.evaluate(pm.getTheBoard());
		// Good for black as black is trying to draw
		assertEquals(-King.MATERIAL_VALUE/2, sut.computeSearchGoalBonus(current));
	}
	
	@Test
	public void test_draw_white() throws InvalidPieceException, IllegalNotationException {
		setupPosition("7k/2K3pq/8/8/8/8/1p6/7Q w - - 0 1");
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("h1a8"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("h7g8"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("a8h1"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("g8h7"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("h1a8"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("h7g8"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("a8h1"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("g8h7"));
		dc.incrementPositionReachedCount(pm.getHash());
		MaterialEvaluation current = MaterialEvaluator.evaluate(pm.getTheBoard());
		// Good for white as white is trying to draw
		assertEquals(King.MATERIAL_VALUE/2, sut.computeSearchGoalBonus(current));
	}
	
	@Test
	public void test_is_not_a_draw() throws InvalidPieceException, IllegalNotationException {
		setupPosition("7q/1P6/8/8/8/8/2k3PQ/7K b - - 0 42");
		pm.performMove(new GenericMove("h8a1"));
		pm.performMove(new GenericMove("h2g1"));
		pm.performMove(new GenericMove("a1h8"));
		pm.performMove(new GenericMove("g1h2"));
		MaterialEvaluation current = MaterialEvaluator.evaluate(pm.getTheBoard());
		assertEquals(0, sut.computeSearchGoalBonus(current));
	}
	
	@Test
	public void test_wrongly_makes_a_draw() throws InvalidPieceException, IllegalNotationException {
		setupPosition("8/2R3p1/7p/5k1P/P7/2BP4/1P3P2/1K6 w - - 0 46");
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("c7g7"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("f5f4"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("g7g3"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("f4f5"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("g3g7"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("f5f4"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("g7g3"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("f4f5"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("g3g7"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("f5f4"));
		dc.incrementPositionReachedCount(pm.getHash());
		pm.performMove(new GenericMove("g7g3"));
		dc.incrementPositionReachedCount(pm.getHash());
		MaterialEvaluation current = MaterialEvaluator.evaluate(pm.getTheBoard());
		assertEquals(-400, sut.computeSearchGoalBonus(current));
	}
}
