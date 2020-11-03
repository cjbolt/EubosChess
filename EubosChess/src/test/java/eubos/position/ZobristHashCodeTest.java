package eubos.position;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Ignore;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.Piece;

public class ZobristHashCodeTest {
	
	ZobristHashCode sut;

	@Test
	public void test_update_PerformUnperformMove_GivesSameHashCode() throws Exception {
		PositionManager pm = new PositionManager("8/8/8/8/8/8/4P3/8 w - - 0 1");
		long initialHashCode = pm.getHash();
		
		pm.performMove(Move.valueOf(Position.e2, Piece.WHITE_PAWN, Position.e4, Piece.NONE));
		pm.unperformMove();
		
		assertEquals(initialHashCode, pm.getHash());
	}
	
	@Test
	public void test_update_PerformCapture_GivesExpectedHashCode() throws Exception {
		GenericMove move = new GenericMove("e2f3");
		PositionManager pm = new PositionManager("8/8/8/8/8/5p2/4P3/8 w - - 0 1");
		PositionManager pm_after_capture = new PositionManager("8/8/8/8/8/5P2/8/8 b - - 0 2");
		
		pm.performMove(Move.toMove(move, pm.getTheBoard(), Move.TYPE_REGULAR_NONE));
		
		assertEquals(pm_after_capture.getHash(), pm.getHash());
	}
	
	@Test
	public void test_update_PerformCaptureUnperform_GivesSameHashCode() throws Exception {
		PositionManager pm = new PositionManager("8/8/8/8/8/5p2/4P3/8 w - - 0 1");
		long initialHashCode = pm.getHash();
		
		pm.performMove(Move.valueOf(Position.e2, Piece.WHITE_PAWN, Position.f3, Piece.BLACK_PAWN));
		pm.unperformMove();

		assertEquals(initialHashCode, pm.getHash());	
	}
	
	@Test
	public void test_update_PerformEnPassantCapture_GivesExpectedHashCode() throws Exception {
		PositionManager pm = new PositionManager("8/8/8/8/4Pp2/8/8/8 b - e3 0 1");
		PositionManager pm_after_capture = new PositionManager("8/8/8/8/8/4p3/8/8 w - - 0 2");
		
		pm.performMove(Move.valueOf(Position.f4, Piece.BLACK_PAWN, Position.e3, Piece.WHITE_PAWN));

		assertEquals(pm_after_capture.getHash(), pm.getHash());	
	}	
	
	@Test
	public void test_update_PerformEnPassantCaptureUnperform_GivesSameHashCode() throws Exception {
		GenericMove move = new GenericMove("f4e3");
		PositionManager pm = new PositionManager("8/8/8/8/4Pp2/8/8/8 b - e3 0 1");
		long initialHashCode = pm.getHash();
		
		pm.performMove(Move.toMove(move, pm.getTheBoard()));
		pm.unperformMove();

		assertEquals(initialHashCode, pm.getHash());	
	}
	
	@Test
	public void test_update_PerformSetEnPassant_GivesExpectedHashCode() throws Exception {
		GenericMove move = new GenericMove("e2e4");
		PositionManager pm = new PositionManager("8/8/8/8/8/8/4P3/8 w - - 0 1");
		PositionManager pm_after_enP = new PositionManager("8/8/8/8/4P3/8/8/8 b - e3 0 2");
		
		pm.performMove(Move.toMove(move, pm.getTheBoard()));

		assertEquals(pm_after_enP.getHash(), pm.getHash());	
	}
	
	@Test
	public void test_update_PerformCastlingWks_GivesExpectedHashCode() throws Exception {
		PositionManager pm = new PositionManager("8/8/8/8/8/8/8/R3K2R w KQ - 0 1");
		PositionManager pm_after_castle = new PositionManager("8/8/8/8/8/8/8/R4RK1 b - - 0 1");
		
		pm.performMove(CastlingManager.wksc);

		assertEquals(pm_after_castle.getHash(), pm.getHash());	
	}
	
	@Test
	public void test_update_PerformCastlingWqs_GivesExpectedHashCode() throws Exception {
		PositionManager pm = new PositionManager("8/8/8/8/8/8/8/R3K2R w KQ - 0 1");
		PositionManager pm_after_castle = new PositionManager("8/8/8/8/8/8/8/2KR3R b - - 0 1");
		
		pm.performMove(CastlingManager.wqsc);

		assertEquals(pm_after_castle.getHash(), pm.getHash());	
	}
	
	@Test
	public void test_update_PerformCastlingBqs_GivesExpectedHashCode() throws Exception {
		PositionManager pm = new PositionManager("r3k2r/8/8/8/8/8/8/8 b kq - 0 1");
		PositionManager pm_after_castle = new PositionManager("2kr3r/8/8/8/8/8/8/8 w - - 0 1");
		
		pm.performMove(CastlingManager.bqsc);

		assertEquals(pm_after_castle.getHash(), pm.getHash());	
	}
	
	@Test
	public void test_update_PerformCastlingBks_GivesExpectedHashCode() throws Exception {
		PositionManager pm = new PositionManager("r3k2r/8/8/8/8/8/8/8 b kq - 0 1");
		PositionManager pm_after_castle = new PositionManager("r4rk1/8/8/8/8/8/8/8 w - - 0 1");
		
		pm.performMove(CastlingManager.bksc);

		assertEquals(pm_after_castle.getHash(), pm.getHash());	
	}
	
	@Test
	public void test_update_PerformCastlingUnperformWks_GivesExpectedHashCode() throws Exception {
		PositionManager pm = new PositionManager("8/8/8/8/8/8/8/R3K2R w KQ - 0 1");
		long originalHashCode = pm.getHash();
		
		pm.performMove(CastlingManager.wksc);
		pm.unperformMove();

		assertEquals(originalHashCode, pm.getHash());	
	}
	
	@Test
	public void test_update_PerformCastlingUnperformWqs_GivesExpectedHashCode() throws Exception {
		PositionManager pm = new PositionManager("8/8/8/8/8/8/8/R3K2R w KQ - 0 1");
		long originalHashCode = pm.getHash();
		
		pm.performMove(CastlingManager.wksc);
		pm.unperformMove();

		assertEquals(originalHashCode, pm.getHash());	
	}
	
	@Test
	public void test_update_PerformCastlingUnperformBqs_GivesExpectedHashCode() throws Exception {
		PositionManager pm = new PositionManager("r3k2r/8/8/8/8/8/8/8 b kq - 0 1");
		long originalHashCode = pm.getHash();
		
		pm.performMove(CastlingManager.bqsc);
		pm.unperformMove();

		assertEquals(originalHashCode, pm.getHash());
	}
	
	@Test
	public void test_update_PerformCastlingUnperformBks_GivesExpectedHashCode() throws Exception {
		PositionManager pm = new PositionManager("r3k2r/8/8/8/8/8/8/8 b kq - 0 1");
		long originalHashCode = pm.getHash();
		
		pm.performMove(CastlingManager.bksc);
		pm.unperformMove();

		assertEquals(originalHashCode, pm.getHash());	
	}
	
	@Test
	public void test_EndGame_RegenerateSamePositionAlternate() throws Exception {
		//GenericMove move = new GenericMove("g3g2");
		PositionManager pm = new PositionManager("8/8/p6p/1p3kp1/1P6/P4PKP/5P2/8 w - - 0 1");
		long originalHashCode = pm.getHash();
		
		pm.performMove(Move.toMove(new GenericMove("g3g2"), pm.getTheBoard()));
		pm.performMove(Move.toMove(new GenericMove("f5f6"), pm.getTheBoard()));
		pm.performMove(Move.toMove(new GenericMove("g2g3"), pm.getTheBoard()));
		pm.performMove(Move.toMove(new GenericMove("f6f5"), pm.getTheBoard()));

		assertEquals(originalHashCode, pm.getHash());	
	}
	
	@Test
	public void test_EndGame_RegenerateSamePosition() throws Exception {
		//GenericMove move = new GenericMove("g3g2");
		PositionManager pm = new PositionManager("8/8/p6p/1p3kp1/1P6/P4PKP/5P2/8 w - - 0 1");
		long originalHashCode = pm.getHash();
		
		pm.performMove(Move.toMove(new GenericMove("g3g2"), pm.getTheBoard()));
		pm.performMove(Move.toMove(new GenericMove("f5f6"), pm.getTheBoard()));
		pm.performMove(Move.toMove(new GenericMove("g2g3"), pm.getTheBoard()));
		pm.performMove(Move.toMove(new GenericMove("f6f5"), pm.getTheBoard()));

		assertEquals(originalHashCode, pm.getHash());	
	}
	
	@Test
	@Ignore
	public void test_EndGamePromotion_RegenerateSamePosition() throws Exception {
		//GenericMove move = new GenericMove("g3g2");
		PositionManager pm = new PositionManager("8/8/p6p/1p3kp1/1P6/P4PKP/5P2/8 w - - 0 1");
		long originalHashCode = pm.getHash();
		
		pm.performMove(Move.toMove(new GenericMove("g3g2")));
		pm.performMove(Move.toMove(new GenericMove("f5f6")));
		pm.performMove(Move.toMove(new GenericMove("g2g3")));
		pm.performMove(Move.toMove(new GenericMove("f6f5")));
		pm.performMove(Move.toMove(new GenericMove("a3a4")));
		pm.performMove(Move.toMove(new GenericMove("b5a4")));
		pm.performMove(Move.toMove(new GenericMove("g3g2")));
		pm.performMove(Move.toMove(new GenericMove("a4a3")));
		pm.performMove(Move.toMove(new GenericMove("g2g3")));
		pm.performMove(Move.toMove(new GenericMove("a3a2")));
		pm.performMove(Move.toMove(new GenericMove("g3g2")));
		pm.performMove(Move.toMove(new GenericMove("a2a1Q")));
		pm.performMove(Move.toMove(new GenericMove("g2h2")));
		for (int i=0; i<13; i++)
			pm.unperformMove();

		assertEquals(originalHashCode, pm.getHash());	
	}
	
	@Test
	@Ignore
	public void test_EndGameNotPromotion_RegenerateSamePosition() throws Exception {
		//GenericMove move = new GenericMove("g3g2");
		PositionManager pm = new PositionManager("8/8/p6p/1p3kp1/1P6/P4PKP/5P2/8 w - - 0 1");
		long originalHashCode = pm.getHash();
		
		pm.performMove(Move.toMove(new GenericMove("g3g2")));
		pm.performMove(Move.toMove(new GenericMove("f5f6")));
		pm.performMove(Move.toMove(new GenericMove("g2g3")));
		pm.performMove(Move.toMove(new GenericMove("f6f5")));
		pm.performMove(Move.toMove(new GenericMove("a3a4")));
		pm.performMove(Move.toMove(new GenericMove("b5a4")));
		pm.performMove(Move.toMove(new GenericMove("g3g2")));
		pm.performMove(Move.toMove(new GenericMove("a4a3")));
		pm.performMove(Move.toMove(new GenericMove("g2g3")));
		pm.performMove(Move.toMove(new GenericMove("a3a2")));
		pm.performMove(Move.toMove(new GenericMove("g3g2")));
		for (int i=0; i<11; i++)
			pm.unperformMove();

		assertEquals(originalHashCode, pm.getHash());	
	}
	
	@Test
	public void test_EndGameJustPromotionMove_RegenerateSamePosition() throws Exception {
		PositionManager pm = new PositionManager("8/8/p6p/5kp1/1P6/5P1P/p4PK1/8 b - - 1 6 ");
		PositionManager after_pm = new PositionManager("8/8/p6p/5kp1/1P6/5P1P/5PK1/q7 w - - 0 7 ");
		long afterHashCode = after_pm.getHash();
		
		pm.performMove(Move.toMove(new GenericMove("a2a1Q"), pm.getTheBoard(), Move.TYPE_PROMOTION_QUEEN_MASK));

		assertEquals(afterHashCode, pm.getHash());	
	}
	
	@Test
	@Ignore
	public void test_EndGameJustUndoPromotionMove_RegenerateSamePosition() throws Exception {
		PositionManager after_pm = new PositionManager("8/8/p6p/5kp1/1P6/5P1P/p4PK1/8 b - - 1 6 ");
		PositionManager pm = new PositionManager("8/8/p6p/5kp1/1P6/5P1P/5PK1/q7 w - - 0 7 ");
		long afterHashCode = after_pm.getHash();
		// There is no easy way to undo a promotion without first performing a promotion
		pm.performMove(Move.toMove(new GenericMove("a1a2")));

		assertEquals(afterHashCode, pm.getHash());	
	}
	
	@Test
	public void test_EndGameJustPromotion_RegenerateSamePosition() throws Exception {
		PositionManager pm = new PositionManager("8/8/p6p/5kp1/1P6/5P1P/p4PK1/8 b - - 1 6 ");
		long originalHashCode = pm.getHash();
		
		pm.performMove(Move.valueOf(Move.TYPE_PROMOTION_QUEEN_MASK, Position.a2, Piece.BLACK_PAWN, Position.a1, Piece.NONE, Piece.QUEEN));
		pm.unperformMove();

		assertEquals(originalHashCode, pm.getHash());	
	}
	
	@Test
	public void test_BadHashFound_enPassantWasPossible() throws Exception {
		PositionManager pm = new PositionManager("1r1k1r2/6Q1/2p3p1/p7/1q1p2n1/3P2P1/P3RPP1/4RK2 w - a6 - 2");
		long originalHashCode = pm.getHash();
		
		pm.performMove(Move.valueOf(Position.g7, Piece.WHITE_QUEEN, Position.g6, Piece.BLACK_PAWN));
		pm.unperformMove();

		assertEquals(originalHashCode, pm.getHash());	
	}
	
	@Test
	public void test_BadHashFound_enPassantWasPossible_andAnotherenPassantSettingMoveMade() throws Exception {
		PositionManager pm = new PositionManager("1r1k1r2/6Q1/2p3p1/p7/1q1p2n1/3P2P1/P3RPP1/4RK2 w - a6 - 2");
		long originalHashCode = pm.getHash();
		
		pm.performMove(Move.valueOf(Position.f2, Piece.WHITE_PAWN, Position.f4, Piece.NONE));
		pm.unperformMove();

		assertEquals(originalHashCode, pm.getHash());	
	}
}
