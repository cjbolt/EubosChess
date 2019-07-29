package eubos.position;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.board.pieces.King;
import eubos.board.pieces.Rook;

public class PositionEvaluatorTest {

	PositionEvaluator SUT;
	
	@Before
	public void setUp() throws Exception {
		SUT = new PositionEvaluator();
	}

	@Test
	public void testEvaluatePosition_notYetCastled() {
		// 8 ........
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ....K..R
		//   abcdefgh
		int score = SUT.evaluatePosition(new PositionManager("8/8/8/8/8/8/8/4K2R w K - - -"));
		assertTrue(score == (King.MATERIAL_VALUE + Rook.MATERIAL_VALUE));
	}
	
	@Test
	public void testEvaluatePosition_castled() {
		// 8 k.......
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ....K..R
		//   abcdefgh
		PositionManager pm = new PositionManager("k7/8/8/8/8/8/8/4K2R w K - - -");
		
		try {
			pm.performMove(new GenericMove("e1g1"));
		} catch (InvalidPieceException e) {
			e.printStackTrace();
		} catch (IllegalNotationException e) {
			e.printStackTrace();
		}
		int score = SUT.evaluatePosition(pm);
		assertTrue(score == (Rook.MATERIAL_VALUE + PositionEvaluator.HAS_CASTLED_BOOST_CENTIPAWNS));
	}	

	@Test
	public void testEvaluatePosition_castled_fewMoveLater() {
		// 8 k.......
		// 7 ........
		// 6 ........
		// 5 ........
		// 4 ........
		// 3 ........
		// 2 ........
		// 1 ....K..R
		//   abcdefgh
		PositionManager pm = new PositionManager("k7/8/8/8/8/8/8/4K2R w K - - -");
		
		try {
			pm.performMove(new GenericMove("e1g1"));
			pm.performMove(new GenericMove("a8b8"));
			pm.performMove(new GenericMove("f1d1"));
			pm.performMove(new GenericMove("b8a8"));
			pm.performMove(new GenericMove("d1d8"));
		} catch (InvalidPieceException e) {
			e.printStackTrace();
		} catch (IllegalNotationException e) {
			e.printStackTrace();
		}
		int score = SUT.evaluatePosition(pm);
		assertTrue(score == (Rook.MATERIAL_VALUE + PositionEvaluator.HAS_CASTLED_BOOST_CENTIPAWNS));
	}	
	
	@Test
	public void testDiscourageDoubledPawns() {
		PositionManager pm = new PositionManager("8/pppppp2/8/8/8/1P2P3/1P1P2PP/8 b - - 0 1");
		int score = SUT.evaluatePosition(pm);
		assertTrue(score == -PositionEvaluator.DOUBLED_PAWN_HANDICAP+5 /* position of e3 pawn gives +5 boost */);
	}
}
