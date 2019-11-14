package eubos.search;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.position.MaterialEvaluation;
import eubos.position.MaterialEvaluator;
import eubos.position.PositionManager;

public class SearchContextTest {
	
	private SearchContext sut;
	private PositionManager pm;

	@Before
	public void setUp() throws Exception {
	}
	
	public void setupPosition(String fen) {
		pm = new PositionManager(fen);
		sut = new SearchContext(pm, MaterialEvaluator.evaluate(pm.getTheBoard()));
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
		assertEquals(SearchContext.SIMPLIFICATION_BONUS, sut.computeSearchGoalBonus(current));
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
}
