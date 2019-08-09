package eubos.search;

import java.sql.Timestamp;
import java.util.LinkedList;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.models.GenericMove;

import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IGenerateMoveList;
import eubos.position.IPositionAccessors;


public class FixedTimeMoveSearcher extends AbstractMoveSearcher {

	long moveTime;
	
	private static final int MAX_SEARCH_DEPTH = 12;

	public FixedTimeMoveSearcher(EubosEngineMain eubos, FixedSizeTranspositionTable hashMap, IChangePosition inputPm, 
			IGenerateMoveList mlgen, IPositionAccessors pos, long time ) {
		super(eubos, inputPm, pos, new MiniMaxMoveGenerator( eubos, hashMap, inputPm, mlgen, pos ));
		moveTime = time;
	}
	
	@Override
	public void halt() {
		mg.terminateFindMove();
	}
	
	@Override
	public void run() {
		SearchResult res = new SearchResult(null, false);
		LinkedList<GenericMove> pc = null;
		Timestamp msTargetEndTime = new Timestamp(System.currentTimeMillis() + moveTime);
		for (int depth=1; depth<MAX_SEARCH_DEPTH; depth++) {
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

