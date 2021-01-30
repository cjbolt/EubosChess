package eubos.search.searchers;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.board.Piece.Colour;
import eubos.main.EubosEngineMain;
import eubos.search.DrawChecker;
import eubos.search.NoLegalMoveException;
import eubos.search.Score;
import eubos.search.SearchMetricsReporter;
import eubos.search.SearchResult;
import eubos.search.generators.MiniMaxMoveGenerator;
import eubos.search.transposition.FixedSizeTranspositionTable;
import eubos.search.transposition.ITransposition;

public abstract class AbstractMoveSearcher extends Thread {

	protected EubosEngineMain eubosEngine;
	protected MiniMaxMoveGenerator mg;
	
	protected boolean sendInfo = false;
	protected SearchMetricsReporter sr;
	
	protected short initialScore;

	public AbstractMoveSearcher(EubosEngineMain eng, String fen, DrawChecker dc, FixedSizeTranspositionTable hashMap) {
		super();
		this.eubosEngine = eng;
		if (EubosEngineMain.UCI_INFO_ENABLED) {
			sendInfo = true;
			sr = new SearchMetricsReporter(eubosEngine, hashMap);
		}
		this.mg = new MiniMaxMoveGenerator(hashMap, fen, dc, sr);
		
		// Set initial score from previous Transposition table, if an entry exists 
		ITransposition trans = hashMap.getTransposition(mg.pos.getHash());
		String transReport = "None";
		if (trans != null) {
			transReport = trans.report();
			if (trans.getType() == Score.exact) {
				initialScore = trans.getScore();
			} else {
				// can't use a bound score!
				trans = null;
			}
		}
		if (trans == null) {
			// if that wasn't possible use a static evaluation of the root position
			initialScore = Score.getScore(mg.pos.getPositionEvaluator().evaluatePosition());
		}
		// convert to a UCI score, as we will sniff the UCI metric in the search stopper
		if (Colour.isBlack(mg.pos.getOnMove())) {
			initialScore = (short)-initialScore;
		}
		EubosEngineMain.logger.info(String.format("initialScore %d, SearchContext %s, isEndgame %s root %s",
				initialScore, mg.pos.getPositionEvaluator().getGoal(), mg.pos.getTheBoard().isEndgame, transReport));
		
		if (EubosEngineMain.UCI_INFO_ENABLED) {
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
			res = mg.findMove(depth, pc, sr);
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
	
	public void enableSearchMetricsReporter(boolean enable) {
		if (EubosEngineMain.UCI_INFO_ENABLED && sendInfo) {
			sr.setSendInfo(enable);
		}
	}
	
	public void terminateSearchMetricsReporter() {
		if (EubosEngineMain.UCI_INFO_ENABLED && sendInfo)
			sr.end();
	}

}