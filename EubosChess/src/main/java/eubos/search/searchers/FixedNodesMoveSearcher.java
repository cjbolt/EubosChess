package eubos.search.searchers;

import eubos.main.EubosEngineMain;
import eubos.score.PawnEvalHashTable;
import eubos.score.ReferenceScore;
import eubos.search.DrawChecker;
import eubos.search.SearchResult;
import eubos.search.transposition.FixedSizeTranspositionTable;

public class FixedNodesMoveSearcher extends AbstractMoveSearcher {
	
	private long searchNodes = 0;
	private long nodeCount = 0;
	boolean searchStopped = false;
	
	public FixedNodesMoveSearcher( EubosEngineMain eubos, 
			FixedSizeTranspositionTable hashMap, 
			String fen,  
			DrawChecker dc, 
			long nodes,
			ReferenceScore refScore) {
		super(eubos, fen, dc, hashMap, refScore, new PawnEvalHashTable());
		this.searchNodes = nodes;
		this.setName("FixedNodesMoveSearcher");
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
		while (nodeCount<=searchNodes) {
			res = doFindMove(depth);
			nodeCount = sr.getNodesSearched();
			depth++;
		}
		enableSearchMetricsReporter(false);
		eubosEngine.sendBestMoveCommand(res);
		terminateSearchMetricsReporter();
		mg.sda.close();
	}
}
