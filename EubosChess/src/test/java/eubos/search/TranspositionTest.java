package eubos.search;

import org.junit.*;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.position.Position;
import eubos.search.transposition.PrincipalVariationTransposition;

public class TranspositionTest {

	private PrincipalVariationTransposition sut;
	
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test_truncationOfPv() throws IllegalNotationException {
		if (PrincipalVariationTransposition.TRUNCATION_OF_PV_ENABLED) {
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) {
				byte depth = 1;
				short score = 0;
				byte scoreType = Score.lowerBound;
				GenericMove bestMove = new GenericMove("e2e4");
				sut = new PrincipalVariationTransposition(depth, score, scoreType, bestMove);
				
				int[] pvArray = { Move.NULL_MOVE, Move.NULL_MOVE, Move.NULL_MOVE };
				List<Integer> pv = Arrays.stream(pvArray).boxed().collect(Collectors.toList());
				
				sut.setPv(pv);
				List<Integer> truncated = sut.getPv();
				assertEquals(depth, truncated.size());
			}
		}
	}
	
	@Test
	public void test_PvDoesntNeedTruncation() throws IllegalNotationException {
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) {
			byte depth = 1;
			short score = 0;
			byte scoreType = Score.lowerBound;
			GenericMove bestMove = new GenericMove("e2e4");
			sut = new PrincipalVariationTransposition(depth, score, scoreType, bestMove);
			
			int[] pvArray = { Move.toMove(bestMove) };
			List<Integer> pv = Arrays.stream(pvArray).boxed().collect(Collectors.toList());
			
			sut.setPv(pv);
			List<Integer> truncated = sut.getPv();
			
			assertEquals(depth, truncated.size());
			assertEquals(bestMove, Move.toGenericMove(truncated.get(0)));
		}
	}
	
	@Test
	public void test_PvValuesExpected() throws IllegalNotationException {
		if (PrincipalVariationTransposition.TRUNCATION_OF_PV_ENABLED) {
			if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) {
				byte depth = 1;
				short score = 0;
				byte scoreType = Score.lowerBound;
				GenericMove bestMove = new GenericMove("e2e4");
				sut = new PrincipalVariationTransposition(depth, score, scoreType, bestMove);
				
				int[] pvArray = { Move.toMove(bestMove), Move.toMove(new GenericMove("d7d5")), Move.toMove(new GenericMove("d2d4")) };
				List<Integer> pv = Arrays.stream(pvArray).boxed().collect(Collectors.toList());
				
				sut.setPv(pv);
				List<Integer> truncated = sut.getPv();
				
				assertEquals(depth, truncated.size());
				assertEquals(bestMove, Move.toGenericMove(truncated.get(0)));
			}
		}
	}
	
	@Test
	public void test_bitfield() {
		byte depth = 1;
		short score = 0;
		byte scoreType = Score.exact;
		sut = new PrincipalVariationTransposition(depth, score, scoreType, Move.valueOfRegular(Position.d5, Piece.WHITE_PAWN, Position.d6), null);
		int move = sut.getBestMove(null);
		assertEquals(Move.valueOfPromotion(Position.d5, Position.d6, Piece.NONE), move);
	}
}
