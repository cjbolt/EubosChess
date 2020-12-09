package eubos.search.searchers;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.search.DrawChecker;
import eubos.search.NoLegalMoveException;
import eubos.search.SearchMetrics;
import eubos.search.SearchMetricsReporter;
import eubos.search.SearchResult;
import eubos.search.generators.MiniMaxMoveGenerator;
import eubos.search.transposition.FixedSizeTranspositionTable;

public abstract class AbstractMoveSearcher extends Thread {

	protected EubosEngineMain eubosEngine;
	protected MiniMaxMoveGenerator mg;
	
	protected boolean sendInfo = false;
	protected SearchMetrics sm;
	protected SearchMetricsReporter sr;
	
	protected short initialScore;

	public AbstractMoveSearcher(EubosEngineMain eng, String fen, DrawChecker dc, FixedSizeTranspositionTable hashMap) {
		super();
		this.eubosEngine = eng;
		this.mg = new MiniMaxMoveGenerator(hashMap, fen, dc);
		
		initialScore = mg.pos.getPositionEvaluator().evaluatePosition().getScore();
		if (Colour.isBlack(mg.pos.getOnMove())) {
			initialScore = (short)-initialScore;
		}
		
		sm = new SearchMetrics(mg.pos);
		if (EubosEngineMain.UCI_INFO_ENABLED) {
			sendInfo = true;
			sr = new SearchMetricsReporter(eubosEngine, sm);	
			sr.setSendInfo(true);
			sr.start();
		}
	}

	public AbstractMoveSearcher(Runnable target) {
		super(target);
	}

	public AbstractMoveSearcher(String name) {
		super(name);
	}

	public AbstractMoveSearcher(ThreadGroup group, Runnable target) {
		super(group, target);
	}

	public abstract void run();

	public abstract void halt();

	protected SearchResult doFindMove(GenericMove selectedMove, List<Integer> pc, byte depth) {
		SearchResult res = null;
		try {
			res = mg.findMove(depth, pc);
		} catch( NoLegalMoveException e ) {
			EubosEngineMain.logger.info(
					String.format("AbstractMoveSearcher out of legal moves"));
		} catch(InvalidPieceException e ) {
			EubosEngineMain.logger.info(
					String.format("AbstractMoveSearcher can't find piece at %s", e.getAtPosition()));
		}
		return res;
	}

	public AbstractMoveSearcher(ThreadGroup group, String name) {
		super(group, name);
	}

	public AbstractMoveSearcher(Runnable target, String name) {
		super(target, name);
	}

	public AbstractMoveSearcher(ThreadGroup group, Runnable target, String name) {
		super(group, target, name);
	}

	public AbstractMoveSearcher(ThreadGroup group, Runnable target, String name,
			long stackSize) {
		super(group, target, name, stackSize);
	}
	
	public void terminateSearchMetricsReporter() {
		if (EubosEngineMain.UCI_INFO_ENABLED && sendInfo)
			sr.end();
	}

}