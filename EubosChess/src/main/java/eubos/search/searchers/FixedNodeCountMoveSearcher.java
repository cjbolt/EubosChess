package eubos.search.searchers;

import eubos.main.EubosEngineMain;
import eubos.search.DrawChecker;
import eubos.search.ReferenceScore;
import eubos.search.SearchResult;
import eubos.search.transposition.ITranspositionAccessor;

public class FixedNodeCountMoveSearcher extends AbstractMoveSearcher {
	
	private int targetNodeCount = 1;
	boolean searchStopped = false;
	
	public FixedNodeCountMoveSearcher( EubosEngineMain eubos, 
			ITranspositionAccessor hashMap, 
			String fen,  
			DrawChecker dc, 
			int nodeCount,
			ReferenceScore refScore) {
		super(eubos, fen, dc, hashMap, refScore);
		this.targetNodeCount = nodeCount;
		this.setName("FixedNodeCountMoveSearcher");
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
		byte depth = 1;
		while (mg.sm.getNodesSearched() < targetNodeCount) {
			res = doFindMove(depth++);
		}
		enableSearchMetricsReporter(false);
		eubosEngine.sendBestMoveCommand(res);
		terminateSearchMetricsReporter();
		mg.sda.close();
	}
}