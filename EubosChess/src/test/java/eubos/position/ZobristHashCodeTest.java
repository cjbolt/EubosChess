package eubos.position;

import static org.junit.Assert.*;

import org.junit.Test;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.position.PositionManager;
import eubos.position.ZobristHashCode;

public class ZobristHashCodeTest {
	
	ZobristHashCode sut;

	@Test
	public void test_update_PerformUnperformMove_GivesSameHashCode() throws Exception {
		GenericMove move = new GenericMove("e2e4");
		PositionManager pm = new PositionManager("8/8/8/8/8/8/4P3/8 w - - 0 1");
		long initialHashCode = pm.getHash().hashCode;
		
		pm.performMove(move);
		pm.unperformMove();
		
		assertEquals(initialHashCode, pm.getHash().hashCode);
	}
	
	@Test
	public void test_update_PerformCapture_GivesExpectedHashCode() throws Exception {
		GenericMove move = new GenericMove("e2f3");
		PositionManager pm = new PositionManager("8/8/8/8/8/5p2/4P3/8 w - - 0 1");
		PositionManager pm_after_capture = new PositionManager("8/8/8/8/8/5P2/8/8 b - - 0 2");
		
		pm.performMove(move);
		
		assertEquals(pm_after_capture.getHash().hashCode, pm.getHash().hashCode);
	}
	
	@Test
	public void test_update_PerformCaptureUnperform_GivesSameHashCode() throws Exception {
		GenericMove move = new GenericMove("e2f3");
		PositionManager pm = new PositionManager("8/8/8/8/8/5p2/4P3/8 w - - 0 1");
		long initialHashCode = pm.getHash().hashCode;
		
		pm.performMove(move);
		pm.unperformMove();

		assertEquals(initialHashCode, pm.getHash().hashCode);	
	}
	
	@Test
	public void test_update_PerformEnPassantCapture_GivesExpectedHashCode() throws Exception {
		GenericMove move = new GenericMove("f4e3");
		PositionManager pm = new PositionManager("8/8/8/8/4Pp2/8/8/8 b - e3 0 1");
		PositionManager pm_after_capture = new PositionManager("8/8/8/8/8/4p3/8/8 w - - 0 2");
		
		pm.performMove(move);

		assertEquals(pm_after_capture.getHash().hashCode, pm.getHash().hashCode);	
	}	
	
	@Test
	public void test_update_PerformEnPassantCaptureUnperform_GivesSameHashCode() throws Exception {
		GenericMove move = new GenericMove("f4e3");
		PositionManager pm = new PositionManager("8/8/8/8/4Pp2/8/8/8 b - e3 0 1");
		long initialHashCode = pm.getHash().hashCode;
		
		pm.performMove(move);
		pm.unperformMove();

		assertEquals(initialHashCode, pm.getHash().hashCode);	
	}
	
	@Test
	public void test_update_PerformSetEnPassant_GivesExpectedHashCode() throws Exception {
		GenericMove move = new GenericMove("e2e4");
		PositionManager pm = new PositionManager("8/8/8/8/8/8/4P3/8 w - - 0 1");
		PositionManager pm_after_enP = new PositionManager("8/8/8/8/4P3/8/8/8 b - e3 0 2");
		
		pm.performMove(move);

		assertEquals(pm_after_enP.getHash().hashCode, pm.getHash().hashCode);	
	}
	
	@Test
	public void test_update_PerformCastlingWks_GivesExpectedHashCode() throws Exception {
		GenericMove move = new GenericMove("e1g1");
		PositionManager pm = new PositionManager("8/8/8/8/8/8/8/R3K2R w KQ - 0 1");
		PositionManager pm_after_castle = new PositionManager("8/8/8/8/8/8/8/R4RK1 b - - 0 1");
		
		pm.performMove(move);

		assertEquals(pm_after_castle.getHash().hashCode, pm.getHash().hashCode);	
	}
	
	@Test
	public void test_update_PerformCastlingWqs_GivesExpectedHashCode() throws Exception {
		GenericMove move = new GenericMove("e1c1");
		PositionManager pm = new PositionManager("8/8/8/8/8/8/8/R3K2R w KQ - 0 1");
		PositionManager pm_after_castle = new PositionManager("8/8/8/8/8/8/8/2KR3R b - - 0 1");
		
		pm.performMove(move);

		assertEquals(pm_after_castle.getHash().hashCode, pm.getHash().hashCode);	
	}
	
	@Test
	public void test_update_PerformCastlingBqs_GivesExpectedHashCode() throws Exception {
		GenericMove move = new GenericMove("e8c8");
		PositionManager pm = new PositionManager("r3k2r/8/8/8/8/8/8/8 b kq - 0 1");
		PositionManager pm_after_castle = new PositionManager("2kr3r/8/8/8/8/8/8/8 w - - 0 1");
		
		pm.performMove(move);

		assertEquals(pm_after_castle.getHash().hashCode, pm.getHash().hashCode);	
	}
	
	@Test
	public void test_update_PerformCastlingBks_GivesExpectedHashCode() throws Exception {
		GenericMove move = new GenericMove("e8g8");
		PositionManager pm = new PositionManager("r3k2r/8/8/8/8/8/8/8 b kq - 0 1");
		PositionManager pm_after_castle = new PositionManager("r4rk1/8/8/8/8/8/8/8 w - - 0 1");
		
		pm.performMove(move);

		assertEquals(pm_after_castle.getHash().hashCode, pm.getHash().hashCode);	
	}
	
	@Test
	public void test_update_PerformCastlingUnperformWks_GivesExpectedHashCode() throws Exception {
		GenericMove move = new GenericMove("e1g1");
		PositionManager pm = new PositionManager("8/8/8/8/8/8/8/R3K2R w KQ - 0 1");
		long originalHashCode = pm.getHash().hashCode;
		
		pm.performMove(move);
		pm.unperformMove();

		assertEquals(originalHashCode, pm.getHash().hashCode);	
	}
	
	@Test
	public void test_update_PerformCastlingUnperformWqs_GivesExpectedHashCode() throws Exception {
		GenericMove move = new GenericMove("e1c1");
		PositionManager pm = new PositionManager("8/8/8/8/8/8/8/R3K2R w KQ - 0 1");
		long originalHashCode = pm.getHash().hashCode;
		
		pm.performMove(move);
		pm.unperformMove();

		assertEquals(originalHashCode, pm.getHash().hashCode);	
	}
	
	@Test
	public void test_update_PerformCastlingUnperformBqs_GivesExpectedHashCode() throws Exception {
		GenericMove move = new GenericMove("e8c8");
		PositionManager pm = new PositionManager("r3k2r/8/8/8/8/8/8/8 b kq - 0 1");
		long originalHashCode = pm.getHash().hashCode;
		
		pm.performMove(move);
		pm.unperformMove();

		assertEquals(originalHashCode, pm.getHash().hashCode);
	}
	
	@Test
	public void test_update_PerformCastlingUnperformBks_GivesExpectedHashCode() throws Exception {
		GenericMove move = new GenericMove("e8g8");
		PositionManager pm = new PositionManager("r3k2r/8/8/8/8/8/8/8 b kq - 0 1");
		long originalHashCode = pm.getHash().hashCode;
		
		pm.performMove(move);
		pm.unperformMove();

		assertEquals(originalHashCode, pm.getHash().hashCode);	
	}
}
