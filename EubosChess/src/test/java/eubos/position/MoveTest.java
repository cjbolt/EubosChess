package eubos.position;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.IllegalNotationException;
import com.fluxchess.jcpi.models.IntChessman;
import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.MoveList.MoveClassification;

public class MoveTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void test_good() throws IllegalNotationException {
		int move1 = Move.valueOf(MoveClassification.CASTLE.ordinal(), Position.e1, Position.g1, IntChessman.NOCHESSMAN);
		int move2 = Move.toMove(new GenericMove("e1g1"));
		assertTrue(Move.areEqual(move1, move2));
	}
	
	@Test
	public void test_good1() throws IllegalNotationException {
		int move1 = Move.valueOf(MoveClassification.NONE.ordinal(), Position.e1, Position.g1, IntChessman.NOCHESSMAN);
		int move2 = Move.toMove(new GenericMove("e1g1"));
		assertTrue(Move.areEqual(move1, move2));
	}
	
	@Test
	public void testbad() throws IllegalNotationException {
		int move1 = Move.valueOf(MoveClassification.CASTLE.ordinal(), Position.e1, Position.g1, IntChessman.KNIGHT);
		int move2 = Move.toMove(new GenericMove("e1g1"));
		assertFalse(Move.areEqual(move1, move2));
	}
	
	@Test
	public void testbad1() throws IllegalNotationException {
		int move1 = Move.valueOf(MoveClassification.CASTLE.ordinal(), Position.g1, Position.e1, IntChessman.NOCHESSMAN);
		int move2 = Move.toMove(new GenericMove("e1g1"));
		assertFalse(Move.areEqual(move1, move2));
	}
}
