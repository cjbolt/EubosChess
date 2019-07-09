package eubos.position;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.PositionManager;
import eubos.position.ZobristHashCode;

public class ZobristHashCodeTest {
	
	ZobristHashCode sut;

	@Before
	public void setUp() throws Exception {
		sut = new ZobristHashCode();
	}

	@Test
	public void test_generate_SamePositionGivesSameHashCode() throws Exception {
		GenericMove move = new GenericMove("e2e4");
		PositionManager pm = new PositionManager("8/8/8/8/8/8/4P3/8 w - - 0 1");
		long hashCode1 = sut.generate(pm);
		pm.performMove(move);
		pm.unperformMove();
		long sameHashCode = sut.generate(pm);
		assertEquals(sameHashCode, hashCode1);
	}
	
	@Test
	public void test_generate_DiffPositionGivesDiffHashCode() throws Exception {
		GenericMove move = new GenericMove("e2e4");
		PositionManager pm = new PositionManager("8/8/8/8/8/8/4P3/8 w - - 0 1");
		long hashCode1 = sut.generate(pm);
		pm.performMove(move);
		long hashCode2 = sut.generate(pm);
		assertNotEquals(hashCode2, hashCode1);
	}
	
	@Test
	public void test_update_PerformUnperformMoveGivesSameHashCode() throws Exception {
		GenericMove move = new GenericMove("e2e4");
		PositionManager pm = new PositionManager("8/8/8/8/8/8/4P3/8 w - - 0 1");
		long hashCode1 = sut.generate(pm);
		pm.performMove(move);
		long updatedHashCode = sut.update(hashCode1, pm, move);
		pm.unperformMove();
		long sameHashCode = sut.update(updatedHashCode, pm, new GenericMove("e4e2"));
		assertEquals(sameHashCode, hashCode1);
	}
}
