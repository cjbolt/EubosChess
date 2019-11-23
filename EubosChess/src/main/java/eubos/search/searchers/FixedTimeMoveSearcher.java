package eubos.search.searchers;

import java.sql.Timestamp;
import java.util.List;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.models.GenericMove;

import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IEvaluate;
import eubos.position.IPositionAccessors;
import eubos.search.FixedSizeTranspositionTable;
import eubos.search.SearchResult;
import eubos.search.generators.MiniMaxMoveGenerator;


public class FixedTimeMoveSearcher extends AbstractMoveSearcher {

	long moveTime;
	
	private static final int MAX_SEARCH_DEPTH = 18;

	public FixedTimeMoveSearcher(EubosEngineMain eubos, FixedSizeTranspositionTable hashMap, IChangePosition inputPm, 
			IPositionAccessors pos, long time, IEvaluate pe ) {
		super(eubos, inputPm, pos, new MiniMaxMoveGenerator( eubos, hashMap, inputPm, pos, pe ));
		moveTime = time;
		this.setName("FixedTimeMoveSearcher");
	}
	
	@Override
	public void halt() {
		mg.terminateFindMove();
	}
	
	@Override
	public void run() {
		SearchResult res = new SearchResult(null, false);
		List<GenericMove> pc = null;
		Timestamp msTargetEndTime = new Timestamp(System.currentTimeMillis() + moveTime);
		for (byte depth=1; depth<MAX_SEARCH_DEPTH; depth++) {
			res = doFindMove(res.bestMove, pc, depth);
			Timestamp msCurrTime = new Timestamp(System.currentTimeMillis());
			if (msCurrTime.after(msTargetEndTime))
				break;
			if (res != null && res.foundMate)
				break;
			pc = mg.pc.toPvList();
		}
		eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( res.bestMove, null ));
	}
}

