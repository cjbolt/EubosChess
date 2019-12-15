package eubos.search;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.commands.ProtocolInformationCommand;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;

import eubos.board.InvalidPieceException;
import eubos.main.EubosEngineMain;
import eubos.position.PositionManager;
import eubos.search.searchers.IterativeMoveSearcher;
import eubos.search.transposition.FixedSizeTranspositionTable;

public class IterativeMoveSearcherTest {
	
	protected IterativeMoveSearcher sut;
	protected GenericMove expectedMove;
	protected FixedSizeTranspositionTable hashMap;
	PositionManager pm;
	
	private class EubosMock extends EubosEngineMain {
		boolean bestMoveCommandReceived = false;
		ProtocolBestMoveCommand last_bestMove;
		
		@Override
		public void sendInfoCommand(ProtocolInformationCommand command) {
			// Debug the principal continuations returned during the search
			if (command.getMoveList() != null)
				System.out.println(command.getMoveList());
		}
		
		@Override
		public void sendBestMoveCommand(ProtocolBestMoveCommand command) {
			bestMoveCommandReceived = true;
			last_bestMove = command;
		}
	}
	private EubosMock eubos;
	
	protected void setupPosition(String fen, long time) {
		pm = new PositionManager( fen );
		sut = new IterativeMoveSearcher(eubos, hashMap, pm, pm, time, pm.getPositionEvaluator());
	}
	
	@Before
	public void setUp() throws Exception {
		eubos = new EubosMock();
		SearchDebugAgent.open(0);
		hashMap = new FixedSizeTranspositionTable();
	}
	
	@After
	public void tearDown() {
		SearchDebugAgent.close();
	}

	private void runSearcherAndTestBestMoveReturned() {
		eubos.bestMoveCommandReceived = false;
		eubos.last_bestMove = null;
		sut.start(); // need to wait for result
		while (!eubos.bestMoveCommandReceived) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}
		assertEquals(expectedMove, eubos.last_bestMove.bestMove);
	}
	
	@Test
	@Ignore // The problem with this test is that the best move is rejected after ply 10.
	public void test_endgame_a() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		setupPosition("8/8/2pp3k/8/1P1P3K/8/8/8 w - - 0 1", 1000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("d4d5"); //Levy
		runSearcherAndTestBestMoveReturned();
	}
	
	@Test
	public void test_endgame_b() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		setupPosition("8/ppp5/8/PPP5/6kp/8/6KP/8 w - - 0 1", 4000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("b5b6"); // Levy
		runSearcherAndTestBestMoveReturned();		
	}
	
	@Test
	@Ignore // Eubos doesn't get close :(
	// According to Stockfish this position is a dead draw, so I guess Levy is wrong.
	public void test_endgame_c() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		setupPosition("8/p7/1p1k1p2/1P2pp1p/1P1P3P/4KPP1/8/8 w - - 1 10", 6000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("g3g4"); // Levy
		runSearcherAndTestBestMoveReturned();		
	}
	 
	@Test
	@Ignore // Eubos needs 2mins to reliably find the correct move
	public void test_endgame_d() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		setupPosition("8/pp5p/8/PP2k3/2P2pp1/3K4/6PP/8 w - - 1 10", 6000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("c4c5"); // Levy
		runSearcherAndTestBestMoveReturned();		
	}
	
	@Test
	@Ignore // needs to search to 20 odd plies to see a win (when mate should be seen in 19 - this is a bug!)
	// Doesn't reliably pass
	public void test_endgame_e() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		setupPosition("6k1/7p/5P1K/8/8/8/7P/8 w - - 0 1", 950*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("h6g5"); // Stockfish
		runSearcherAndTestBestMoveReturned();		
	}
	
	@Test
	@Ignore //Eubos doesn't have a clue, even at ply==24; probably indicating a bug.
	public void test_endgame_i() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		setupPosition("8/k7/3p4/p2P1p2/P2P1P2/8/8/K7 w - - 0 1", 100);
		expectedMove = new GenericMove("c5d5");
		runSearcherAndTestBestMoveReturned();
	}
	
	@Test
	public void test_endgame_k() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		setupPosition("8/2k5/p1P5/P1K5/8/8/8/8 w - - 0 1", 100);
		expectedMove = new GenericMove("c5d5");
		runSearcherAndTestBestMoveReturned();
	}
	
	@Test
	public void test_endgame_o() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		setupPosition("4k3/4Pp2/5P2/4K3/8/8/8/8 w - - 0 1", 100);
		expectedMove = new GenericMove("e5f5"); // Stockfish
		runSearcherAndTestBestMoveReturned();
	}
	
	@Test
	public void test_mateInFour() throws InvalidPieceException, IllegalNotationException, NoLegalMoveException {
		// chess.com Problem ID: 0102832
		setupPosition( "r1r3k1/pb1p1p2/1p2p1p1/2pPP1B1/1nP4Q/1Pq2NP1/P4PBP/b2R2K1 w - - - -", 6000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("g5f6");
		runSearcherAndTestBestMoveReturned();
	}
	
	@Test
	public void test_findMove_mateInTwo() throws NoLegalMoveException, IllegalNotationException {
		// chess.com Problem ID: 0551140
		setupPosition("rnbq1rk1/p4ppN/4p2n/1pbp4/8/2PQP2P/PPB2PP1/RNB1K2R w - - - -", 1000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("h7f6");
		runSearcherAndTestBestMoveReturned();
	}
	
	@Test
	public void test_findMove_mateInThree() throws NoLegalMoveException, IllegalNotationException {
		setupPosition("2kr3r/ppp2ppp/8/8/1P5P/1K1b1P1N/P3P1P1/4qB1R b - - 3 24", 1000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("e1b1");
		runSearcherAndTestBestMoveReturned();
	}
}
