package eubos.position;

import static org.junit.Assert.*;

import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.PositionManager;
import eubos.position.ZobristHashCode;

public class ZobristHashCodeTest {
	
	ZobristHashCode sut;

	@Test
	public void test_generate_SamePosition_GivesSameHashCode() throws Exception {
		GenericMove move = new GenericMove("e2e4");
		PositionManager pm = new PositionManager("8/8/8/8/8/8/4P3/8 w - - 0 1");
		sut = new ZobristHashCode(pm);
		long initialHashCode = sut.hashCode;
		pm.performMove(sut, move);
		pm.unperformMove(sut);
		assertEquals(initialHashCode, sut.hashCode);
	}
	
	@Test
	public void test_generate_DiffPosition_GivesDiffHashCode() throws Exception {
		GenericMove move = new GenericMove("e2e4");
		PositionManager pm = new PositionManager("8/8/8/8/8/8/4P3/8 w - - 0 1");
		sut = new ZobristHashCode(pm);
		long initialHashCode = sut.hashCode;
		pm.performMove(null, move);
		long hashCode2 = sut.generate(pm);
		assertNotEquals(initialHashCode, hashCode2);
	}
	
	@Test
	public void test_update_PerformUnperformMove_GivesSameHashCode() throws Exception {
		GenericMove move = new GenericMove("e2e4");
		PositionManager pm = new PositionManager("8/8/8/8/8/8/4P3/8 w - - 0 1");
		sut = new ZobristHashCode(pm);
		
		pm.performMove(null, move);
		sut.update(pm, move, null);
		pm.unperformMove(null);
		
		long testHashCode = sut.update(pm, new GenericMove(move.to,move.from), null);
		assertEquals(testHashCode, sut.hashCode);
	}
	
	@Test
	public void test_update_PerformCapture_GivesSameHashCodeAsGenerate() throws Exception {
		GenericMove move = new GenericMove("e2f3");
		PositionManager pm = new PositionManager("8/8/8/8/8/5p2/4P3/8 w - - 0 1");
		sut = new ZobristHashCode(pm);
		PositionManager pm_after_capture = new PositionManager("8/8/8/8/8/5P2/8/8 b - - 0 2");
		ZobristHashCode afterCaptureHashCode = new ZobristHashCode(pm_after_capture);
		
		pm.performMove(sut, move);
		
		assertEquals(afterCaptureHashCode.hashCode, sut.hashCode);
	}
	
	@Test
	public void test_update_PerformCaptureUnperform_GivesSameHashCode() throws Exception {
		GenericMove move = new GenericMove("e2f3");
		PositionManager pm = new PositionManager("8/8/8/8/8/5p2/4P3/8 w - - 0 1");
		sut = new ZobristHashCode(pm);
		long initialHashCode = sut.hashCode;
		
		pm.performMove(sut, move);
		pm.unperformMove(sut);

		assertEquals(initialHashCode, sut.hashCode);	
	}
}
