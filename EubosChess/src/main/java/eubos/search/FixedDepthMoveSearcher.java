package eubos.search;

import java.util.LinkedList;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.models.GenericMove;

import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IGenerateMoveList;
import eubos.position.IPositionAccessors;

public class FixedDepthMoveSearcher extends AbstractMoveSearcher {
	
	private byte searchDepth = 1;
	boolean searchStopped = false;
	
	public FixedDepthMoveSearcher( EubosEngineMain eubos, FixedSizeTranspositionTable hashMap, IChangePosition inputPm, IGenerateMoveList mlgen, IPositionAccessors pos, byte searchDepth ) {
		super(eubos,inputPm,pos, new MiniMaxMoveGenerator( eubos, hashMap, inputPm, mlgen, pos ));
		this.searchDepth = searchDepth;
	}
	
	@Override
	public void halt() {
		searchStopped = true;
		mg.terminateFindMove();
	}
	
	@Override
	public void run() {
		SearchResult res = new SearchResult(null, false);
		LinkedList<GenericMove> pc = null;
		for (byte depth=1; depth<searchDepth && !searchStopped; depth++) {
			res = doFindMove(res.bestMove, pc, depth);
			pc = mg.pc.toPvList();
		}
		eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( res.bestMove, null ));
	}
}
