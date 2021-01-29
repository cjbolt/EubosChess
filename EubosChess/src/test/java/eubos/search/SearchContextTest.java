package eubos.search;

import static org.junit.Assert.*;

import java.util.logging.Level;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.position.PositionManager;
import eubos.score.PiecewiseEvaluation;

@Ignore // As we now return positional PST factors and mobility score as part of SearchContext bonus (to prevent faffing when mating bare king)
public class SearchContextTest {
	
	private SearchContext sut;
	private PositionManager pm;
	private DrawChecker dc;

	@Before
	public void setUp() throws Exception {
		EubosEngineMain.logger.setLevel(Level.OFF);
	}
	
	private void setupPosition(String fen) {
		dc = new DrawChecker();
		pm = new PositionManager(fen, dc);
		sut = new SearchContext(pm, pm.getTheBoard().evaluateMaterial());
		/* This next line emulates the position being received in EubosEngineMain from the analyse
		 * UCI command, then we set the position reached count for the received FEN string, as the
		 * move is not applied in performMove. */
		dc.incrementPositionReachedCount(pm.getHash());
	}
	
	private void applyMoveList(GenericMove[] moveList)
			throws InvalidPieceException {
		for (GenericMove curr : moveList ) {
			pm.performMove(Move.toMove(curr, pm.getTheBoard()));
		}
	}

	@Test
	public void test_detectSimplification() throws InvalidPieceException, IllegalNotationException {
		setupPosition("5r1k/pp5p/6p1/1N2q3/2P1P1n1/1P6/P2Q2PP/3R2K1 w - - 0 1");
		pm.performMove(Move.toMove(new GenericMove("d2d4"), pm.getTheBoard())); // forces exchange of queens on d4. simplifying
		pm.performMove(Move.toMove(new GenericMove("e5d4"), pm.getTheBoard()));
		pm.performMove(Move.toMove(new GenericMove("d1d4"), pm.getTheBoard()));
		PiecewiseEvaluation current = pm.getTheBoard().evaluateMaterial();
		assertEquals(SearchContext.SIMPLIFICATION_BONUS, sut.computeSearchGoalBonus(current).score);
	}
	
	@Test
	public void test_notSimplified_materialNotRecaptured() throws InvalidPieceException, IllegalNotationException {
		setupPosition("5r1k/pp5p/6p1/1N2q3/2P1P1n1/1P6/P2Q2PP/3R2K1 w - - 0 1");
		pm.performMove(Move.toMove(new GenericMove("d2d4"), pm.getTheBoard())); // forces exchange of queens on d4. simplifying
		pm.performMove(Move.toMove(new GenericMove("e5d4"), pm.getTheBoard()));
		PiecewiseEvaluation current = pm.getTheBoard().evaluateMaterial();
		assertEquals(0, sut.computeSearchGoalBonus(current).score);
	}

	@Test
	public void test_detectSimplification_black() throws InvalidPieceException, IllegalNotationException {
		setupPosition("3r2k1/p2q2pp/1p6/2p1p1N1/1n2Q3/6P1/PP5P/5R1K b - - 0 1");
		pm.performMove(Move.toMove(new GenericMove("d7d5"), pm.getTheBoard())); // forces exchange of queens on d4. simplifying
		pm.performMove(Move.toMove(new GenericMove("e4d5"), pm.getTheBoard()));
		pm.performMove(Move.toMove(new GenericMove("d8d5"), pm.getTheBoard()));
		PiecewiseEvaluation current = pm.getTheBoard().evaluateMaterial();
		assertEquals(-SearchContext.SIMPLIFICATION_BONUS, sut.computeSearchGoalBonus(current).score);
	}
	
	@Test
	public void test_notSimplified_materialNotRecaptured_black() throws InvalidPieceException, IllegalNotationException {
		setupPosition("3r2k1/p2q2pp/1p6/2p1p1N1/1n2Q3/6P1/PP5P/5R1K b - - 0 1");
		pm.performMove(Move.toMove(new GenericMove("d7d5"), pm.getTheBoard())); // forces exchange of queens on d4. simplifying
		pm.performMove(Move.toMove(new GenericMove("e4d5"), pm.getTheBoard()));
		// At this point queen recapture not completed
		PiecewiseEvaluation current = pm.getTheBoard().evaluateMaterial();
		assertEquals(0, sut.computeSearchGoalBonus(current).score);
	}
	
	@Test
	public void test_lichess_pos() throws InvalidPieceException, IllegalNotationException {
		setupPosition("4r1k1/2p2pb1/4Q3/8/3pPB2/1p1P3p/1P3P2/R5K1 b - - 0 42");
		PiecewiseEvaluation current = pm.getTheBoard().evaluateMaterial();
		assertEquals(0, sut.computeSearchGoalBonus(current).score);
	}
	 
	@Test
	@Ignore
	public void test_draw_black() throws InvalidPieceException, IllegalNotationException {
		setupPosition("7q/1P6/8/8/8/8/2k3PQ/7K b - - 0 42");
		// set up a draw by repeated check
		GenericMove [] moveList = new GenericMove[]{
												new GenericMove("h8a1"),
						new GenericMove("h2g1"),new GenericMove("a1h8"),
						new GenericMove("g1h2"),new GenericMove("h8a1"),
						new GenericMove("h2g1"),new GenericMove("a1h8"),
						new GenericMove("g1h2")};
		applyMoveList(moveList);
		PiecewiseEvaluation current = pm.getTheBoard().evaluateMaterial();
		// Good for black as black is trying to draw
		assertEquals(SearchContext.AVOID_DRAW_HANDICAP, sut.computeSearchGoalBonus(current).score);
	}
	
	@Test
	@Ignore
	public void test_draw_white() throws InvalidPieceException, IllegalNotationException {
		setupPosition("7k/2K3pq/8/8/8/8/1p6/7Q w - - 0 1");
		// set up a draw by repeated check
		GenericMove [] moveList = new GenericMove[]{
						new GenericMove("h1a8"),new GenericMove("h7g8"),
		                new GenericMove("a8h1"),new GenericMove("g8h7"),
		                new GenericMove("h1a8"),new GenericMove("h7g8"),
		                new GenericMove("a8h1"),new GenericMove("g8h7")};
		applyMoveList(moveList);
		PiecewiseEvaluation current = pm.getTheBoard().evaluateMaterial();
		// Good for white as white is trying to draw
		assertEquals(-SearchContext.AVOID_DRAW_HANDICAP, sut.computeSearchGoalBonus(current).score);
	}
	
	@Test
	public void test_is_not_a_draw() throws InvalidPieceException, IllegalNotationException {
		setupPosition("7q/1P6/8/8/8/8/2k3PQ/7K b - - 0 42");
		// insufficient moves for draw
		GenericMove [] moveList = new GenericMove[]{new GenericMove("h8a1"),new GenericMove("h2g1"),
                									new GenericMove("a1h8")};
		applyMoveList(moveList);
		PiecewiseEvaluation current = pm.getTheBoard().evaluateMaterial();
		assertEquals(0, sut.computeSearchGoalBonus(current).score);
	}
	
	@Test
	@Ignore
	public void test_lichess_draw() throws InvalidPieceException, IllegalNotationException {
		setupPosition("8/2R3p1/7p/5k1P/P7/2BP4/1P3P2/1K6 w - - 0 46");
		// Draw from a test lichess game
		GenericMove [] moveList = new GenericMove[]{
						new GenericMove("c7g7"),
						new GenericMove("f5f4"),new GenericMove("g7g3"),
						new GenericMove("f4f5"),new GenericMove("g3g7"),
						new GenericMove("f5f4"),new GenericMove("g7g3"),
						new GenericMove("f4f5"),new GenericMove("g3g7")};
		applyMoveList(moveList);
		assertTrue(dc.isPositionOpponentCouldClaimDraw(pm.getHash()));
		dc.incrementPositionReachedCount(pm.getHash());
		PiecewiseEvaluation current = pm.getTheBoard().evaluateMaterial();
		assertEquals(SearchContext.AVOID_DRAW_HANDICAP, sut.computeSearchGoalBonus(current).score);
	}
	
	@Test
	@Ignore
	public void test_eubos_main_white_achieves_draw() throws InvalidPieceException, IllegalNotationException {
		setupPosition("8/8/2K5/8/7k/8/8/6q1 b - - 0 60");
		GenericMove [] moveList = new GenericMove[] {
												new GenericMove("g1g2"),
						new GenericMove("c6c5"),new GenericMove("g2g1"),
						new GenericMove("c5c6"),new GenericMove("g1g2"),
						new GenericMove("c6c5"),new GenericMove("g2g1"),
						new GenericMove("c5c6")};
		
		applyMoveList(moveList);
		assertTrue(dc.isPositionOpponentCouldClaimDraw(pm.getHash()));
		PiecewiseEvaluation current = pm.getTheBoard().evaluateMaterial();
		assertEquals(SearchContext.ACHIEVES_DRAW_BONUS, sut.computeSearchGoalBonus(current).score);
	}
	
	@Test
	public void test_eubos_takes_draw_instead_of_mate() throws InvalidPieceException, IllegalNotationException {
		/* In this position, Eubos choose to under promote to a bishop in order to secure the bonus for
		 * getting a draw (by insufficient material) when he considered himself behind. A naive material eval
		 * puts white at -130, even though the pawn can promote to a queen. */
		setupPosition("8/1P6/2K2k2/8/4n3/8/8/8 w - - - 75"); 
		GenericMove [] moveList = new GenericMove[] { new GenericMove("b7b8b") };

		applyMoveList(moveList);
		PiecewiseEvaluation current = pm.getTheBoard().evaluateMaterial();
		assertEquals(SearchContext.ACHIEVES_DRAW_BONUS, sut.computeSearchGoalBonus(current).score);
	}
	
	
//	@Test
//	public void test_eubos_takes_draw_instead_of_mate_one_move_earlier() throws InvalidPieceException, IllegalNotationException {
//		/* In this position, Eubos choose to under promote to a bishop in order to secure the bonus for
//		 * getting a draw (by insufficient material) when he considered himself behind. A naive material eval
//		 * puts white at -130, even though the pawn can promote to a queen. */
//		setupPosition("8/1P6/2q2k2/2K5/8/8/5n2/8 w - - - 74"); 
//		GenericMove [] moveList = new GenericMove[] { 
//				new GenericMove("c5c6"), new GenericMove("b7b8b")
//				new GenericMove("b7b8b") };
//
//		applyMoveList(moveList);
//		PiecewiseEvaluation current = pm.getTheBoard().evaluateMaterial();
//		assertEquals(SearchContext.ACHIEVES_DRAW_BONUS, sut.computeSearchGoalBonus(current).score);
//	}
}
