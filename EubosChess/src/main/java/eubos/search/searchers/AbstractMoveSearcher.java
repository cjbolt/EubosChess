package eubos.search.searchers;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.main.EubosEngineMain;
import eubos.position.PositionManager;
import eubos.score.PawnEvalHashTable;
import eubos.score.ReferenceScore;
import eubos.score.ReferenceScore.Reference;
import eubos.search.DrawChecker;

import eubos.search.Score;
import eubos.search.SearchMetricsReporter;
import eubos.search.SearchResult;
import eubos.search.generators.MiniMaxMoveGenerator;
import eubos.search.transposition.FixedSizeTranspositionTable;

public abstract class AbstractMoveSearcher extends Thread {

	protected EubosEngineMain eubosEngine;
	protected MiniMaxMoveGenerator mg;
	
	protected boolean sendInfo = false;
	protected SearchMetricsReporter sr;
	protected ReferenceScore refScore;

	public AbstractMoveSearcher(EubosEngineMain eng, String fen, DrawChecker dc, FixedSizeTranspositionTable hashMap, ReferenceScore refScore, PawnEvalHashTable pawnHash) {
		super();
		PositionManager pm = new PositionManager(fen, dc, pawnHash);
		refScore.updateReference(pm); // Setup the reference score that shall be used by any IterativeSearchStopper
		this.refScore = refScore;				
		this.eubosEngine = eng;
		
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) {
			sendInfo = true;
			sr = new SearchMetricsReporter(eubosEngine, hashMap, refScore);
		}
		this.mg = new MiniMaxMoveGenerator(hashMap, pm, sr, refScore.getReference());
		
		
		Reference ref = refScore.getReference();
		if (EubosEngineMain.ENABLE_LOGGING) {
			EubosEngineMain.logger.info(String.format("refScore %s, depth %d %s phase=%d",
					Score.toString(ref.score), ref.depth, ref.origin, mg.pos.getTheBoard().me.phase));
		}
		
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING) sr.start();
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

	protected SearchResult doFindMove(GenericMove selectedMove, byte depth) {
		return mg.findMove(depth, sr);
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
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING && sendInfo) {
			sr.setSendInfo(enable);
		}
	}
	
	public void terminateSearchMetricsReporter() {
		if (EubosEngineMain.ENABLE_UCI_INFO_SENDING && sendInfo)
			sr.end();
	}

}