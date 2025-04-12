package eubos.search.searchers;

import eubos.main.EubosEngineMain;
import eubos.search.DrawChecker;
import eubos.search.ReferenceScore;
import eubos.search.SearchResult;
import eubos.search.transposition.ITranspositionAccessor;

public class FixedDepthMoveSearcher extends AbstractMoveSearcher {
	
	private byte searchDepth = 1;
	boolean searchStopped = false;
	
	public FixedDepthMoveSearcher( EubosEngineMain eubos, 
			ITranspositionAccessor hashMap, 
			String fen,  
			DrawChecker dc, 
			byte searchDepth,
			ReferenceScore refScore) {
		super(eubos, fen, dc, hashMap, refScore);
		this.searchDepth = searchDepth;
		this.setName("FixedDepthMoveSearcher");
	}
	
	@Override
	public void halt() {
		searchStopped = true;
		mg.terminateFindMove();
	}
	
	@Override
	public void run() {
		SearchResult res = new SearchResult();
		enableSearchMetricsReporter(true);
		for (byte depth=1; depth<=searchDepth && !searchStopped; depth++) {
			res = doFindMove(depth);
		}
		enableSearchMetricsReporter(false);
		eubosEngine.sendBestMoveCommand(res);
		terminateSearchMetricsReporter();
		mg.sda.close();
	}
}
