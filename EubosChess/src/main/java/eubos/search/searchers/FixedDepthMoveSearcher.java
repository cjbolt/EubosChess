package eubos.search.searchers;

import java.util.List;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;

import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.score.ReferenceScore;
import eubos.search.DrawChecker;
import eubos.search.SearchResult;
import eubos.search.transposition.FixedSizeTranspositionTable;

public class FixedDepthMoveSearcher extends AbstractMoveSearcher {
	
	private byte searchDepth = 1;
	boolean searchStopped = false;
	
	public FixedDepthMoveSearcher( EubosEngineMain eubos, 
			FixedSizeTranspositionTable hashMap, 
			String fen,  
			DrawChecker dc, 
			byte searchDepth) {
		super(eubos, fen, dc, hashMap, new ReferenceScore(hashMap));
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
		SearchResult res = new SearchResult(Move.NULL_MOVE, false);
		enableSearchMetricsReporter(true);
		List<Integer> pc = null;
		for (byte depth=1; depth<=searchDepth && !searchStopped; depth++) {
			res = doFindMove(Move.toGenericMove(res.bestMove), pc, depth);
			pc = mg.pc.toPvList(0);
		}
		enableSearchMetricsReporter(false);
		eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( Move.toGenericMove(res.bestMove), null ));
		terminateSearchMetricsReporter();
		mg.sda.close();
	}
}
