package eubos.search.searchers;

import java.sql.Timestamp;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;

import eubos.main.EubosEngineMain;
import eubos.position.Move;
import eubos.score.ReferenceScore;
import eubos.search.DrawChecker;
import eubos.search.SearchResult;
import eubos.search.transposition.FixedSizeTranspositionTable;


public class FixedTimeMoveSearcher extends AbstractMoveSearcher {

	long moveTime;
	
	private static final int MAX_SEARCH_DEPTH = 18;

	public FixedTimeMoveSearcher(EubosEngineMain eubos, FixedSizeTranspositionTable hashMap, String fen, DrawChecker dc, long time) {
		super(eubos, fen, dc, hashMap, new ReferenceScore(hashMap));
		moveTime = time;
		this.setName("FixedTimeMoveSearcher");
	}
	
	@Override
	public void halt() {
		mg.terminateFindMove();
	}
	
	@Override
	public void run() {
		SearchResult res = new SearchResult(Move.NULL_MOVE, false);
		enableSearchMetricsReporter(true);
		Timestamp msTargetEndTime = new Timestamp(System.currentTimeMillis() + moveTime);
		for (byte depth=1; depth<MAX_SEARCH_DEPTH; depth++) {
			res = doFindMove(Move.toGenericMove(res.bestMove), depth);
			Timestamp msCurrTime = new Timestamp(System.currentTimeMillis());
			if (msCurrTime.after(msTargetEndTime))
				break;
			if (res != null && res.foundMate)
				break;
		}
		enableSearchMetricsReporter(false);
		eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( Move.toGenericMove(res.bestMove), null ));
		terminateSearchMetricsReporter();
		mg.sda.close();
	}
}

