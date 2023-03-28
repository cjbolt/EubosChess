package eubos.search;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.commands.ProtocolInformationCommand;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.IllegalNotationException;


import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.position.PositionManager;
import eubos.score.PawnEvalHashTable;
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
		ProtocolBestMoveCommand last_bestMove = null;
		final AtomicBoolean finished = new AtomicBoolean(false);
		IterativeMoveSearcherTest testObject;
		
		public EubosMock(IterativeMoveSearcherTest test) {
			super();
			testObject = test;
			this.rootPosition = new PositionManager();
		}
		
		@Override
		public void sendInfoCommand(ProtocolInformationCommand command) {
			// Debug the principal continuations returned during the search
			if (command.getMoveList() != null)
				System.out.println(String.format("Info depth: %d moves: %s", command.getDepth(), command.getMoveList()));
		}
		
		@Override
		public void sendBestMoveCommand(SearchResult result) {
			last_bestMove = new ProtocolBestMoveCommand(Move.toGenericMove(result.pv[0]), null);
			synchronized(testObject) {
				finished.set(true);
				testObject.notify();
			}
		}
	}
	private EubosMock eubos;
	
	protected void setupPosition(String fen, long time) {
		sut = new IterativeMoveSearcher(eubos, hashMap, new PawnEvalHashTable(), fen, new DrawChecker(), time, 0, new ReferenceScore(hashMap), 0);
	}
	
	@Before
	public void setUp() throws Exception {
		eubos = new EubosMock(this);
		killers = new KillerList();
		hashMap = new FixedSizeTranspositionTable();
		EubosEngineMain.logger.setLevel(Level.OFF);
		EubosEngineMain.logger.setUseParentHandlers(false);
	}
	
	@After
	public void tearDown() {
		sut.closeSearchDebugAgent();
	}

	private void runSearcherAndTestBestMoveReturned() {
		sut.start();
		// need to wait for result
		synchronized(this) {
			try {
				while (!eubos.finished.get()) {
					wait();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		assertEquals(expectedMove, eubos.last_bestMove.bestMove);
	}
	
	@Test
	public void test_findMove_mateInTwo() throws IllegalNotationException {
		// chess.com Problem ID: 0551140
		setupPosition("rnbq1rk1/p4ppN/4p2n/1pbp4/8/2PQP2P/PPB2PP1/RNB1K2R w - - - 1", 1000*IterativeMoveSearcher.AVG_MOVES_PER_GAME);
		expectedMove = new GenericMove("h7f6");
		runSearcherAndTestBestMoveReturned();
	}
}
