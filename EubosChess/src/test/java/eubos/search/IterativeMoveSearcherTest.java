package eubos.search;

import static org.junit.Assert.*;

import java.util.logging.Level;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.commands.ProtocolInformationCommand;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;


import eubos.main.EubosEngineMain;
import eubos.position.PositionManager;
import eubos.score.ReferenceScore;
import eubos.search.searchers.IterativeMoveSearcher;
import eubos.search.transposition.FixedSizeTranspositionTable;

public class IterativeMoveSearcherTest {
	
	protected IterativeMoveSearcher sut;
	protected GenericMove expectedMove;
	protected FixedSizeTranspositionTable hashMap;
	protected KillerList killers;
	PositionManager pm;
	
	private class EubosMock extends EubosEngineMain {
		boolean bestMoveCommandReceived = false;
		ProtocolBestMoveCommand last_bestMove;
		
		@Override
		public void sendInfoCommand(ProtocolInformationCommand command) {
			// Debug the principal continuations returned during the search
			if (command.getMoveList() != null)
				System.out.println(String.format("Info depth: %d moves: %s", command.getDepth(), command.getMoveList()));
		}
		
		@Override
		public void sendBestMoveCommand(ProtocolBestMoveCommand command) {
			bestMoveCommandReceived = true;
			last_bestMove = command;
		}
	}
	private EubosMock eubos;
	
	protected void setupPosition(String fen, long time) {
		sut = new IterativeMoveSearcher(eubos, hashMap, fen, new DrawChecker(), time, 0, new ReferenceScore(hashMap));
	}
	
	@Before
	public void setUp() throws Exception {
		eubos = new EubosMock();
		killers = new KillerList(EubosEngineMain.SEARCH_DEPTH_IN_PLY);
		hashMap = new FixedSizeTranspositionTable();
		EubosEngineMain.logger.setLevel(Level.OFF);
		EubosEngineMain.logger.setUseParentHandlers(false);
	}
	
	@After
	public void tearDown() {
		sut.closeSearchDebugAgent();
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
	public void test_endgame_a()throws IllegalNotationException, NoLegalMoveException {
		setupPosition("8/8/2pp3k/8/1P1P3K/8/8/8 w - - 0 1", 7000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("d4d5"); //Levy
		runSearcherAndTestBestMoveReturned();
	}
	
	@Test
	public void test_endgame_b()throws IllegalNotationException, NoLegalMoveException {
		setupPosition("8/ppp5/8/PPP5/6kp/8/6KP/8 w - - 0 1", 4000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("b5b6"); // Levy
		runSearcherAndTestBestMoveReturned();		
	}
	 
	@Test
	@Ignore // Eubos v1.1.0 needs 4mins to reliably find a reasonable continuation!
	public void test_endgame_d()throws IllegalNotationException, NoLegalMoveException {
		setupPosition("8/pp5p/8/PP2k3/2P2pp1/3K4/6PP/8 w - - 1 10", 4000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("c4c5"); // Levy
		runSearcherAndTestBestMoveReturned();		
	}
	
	@Test
	public void test_endgame_e()throws IllegalNotationException, NoLegalMoveException {
		setupPosition("6k1/7p/5P1K/8/8/8/7P/8 w - - 0 1", 4000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("h6g5"); // Stockfish
		runSearcherAndTestBestMoveReturned();		
	}
	
	@Test
	// Eubos finds capture at about 19ply search, finds mate in 13 after 15 to 20 minutes
	public void test_endgame_i()throws IllegalNotationException, NoLegalMoveException {
		setupPosition("8/k7/3p4/p2P1p2/P2P1P2/8/8/K7 w - - 0 1", 1000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("a1b2");
		runSearcherAndTestBestMoveReturned();
	}
	
	@Test
	public void test_endgame_k()throws IllegalNotationException, NoLegalMoveException {
		setupPosition("8/2k5/p1P5/P1K5/8/8/8/8 w - - 0 1", 100);
		expectedMove = new GenericMove("c5d5");
		runSearcherAndTestBestMoveReturned();
	}
	
	@Test
	public void test_endgame_o()throws IllegalNotationException, NoLegalMoveException {
		setupPosition("4k3/4Pp2/5P2/4K3/8/8/8/8 w - - 0 1",  1000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("e5e4"); // not in accordance with Stockfish (e5f5), but fine
		runSearcherAndTestBestMoveReturned();
	}
	
	@Test
	public void test_mateInFour()throws IllegalNotationException, NoLegalMoveException {
		// chess.com Problem ID: 0102832
		setupPosition( "r1r3k1/pb1p1p2/1p2p1p1/2pPP1B1/1nP4Q/1Pq2NP1/P4PBP/b2R2K1 w - - - -", 13000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
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
		setupPosition("2kr3r/ppp2ppp/8/8/1P5P/1K1b1P1N/P3P1P1/4qB1R b - - 3 24", 4500*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("e1b1");
		runSearcherAndTestBestMoveReturned();
	}
}
