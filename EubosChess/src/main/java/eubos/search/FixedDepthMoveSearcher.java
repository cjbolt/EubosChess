package eubos.search;

import java.util.LinkedList;

import com.fluxchess.jcpi.commands.ProtocolBestMoveCommand;
import com.fluxchess.jcpi.models.GenericMove;

import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IGenerateMoveList;
import eubos.position.IPositionAccessors;

public class FixedDepthMoveSearcher extends AbstractMoveSearcher {
	
	private int searchDepth = 1;
	
	public FixedDepthMoveSearcher( EubosEngineMain eubos, FixedSizeTranspositionTable hashMap, IChangePosition inputPm, IGenerateMoveList mlgen, IPositionAccessors pos, int searchDepth ) {
		super(eubos,inputPm,pos, new MiniMaxMoveGenerator( eubos, hashMap, inputPm, mlgen, pos ));
		this.searchDepth = searchDepth;
	}
	
	@Override
	public void halt() {
		mg.terminateFindMove();
	}
	
	@Override
	public void run() {
		GenericMove selectedMove = null;
		LinkedList<GenericMove> pc = null;
		for (int depth=1; depth<searchDepth; depth++) {
			selectedMove = doFindMove(selectedMove, pc, depth);
			pc = mg.pc.toPvList();
		}
		eubosEngine.sendBestMoveCommand(new ProtocolBestMoveCommand( selectedMove, null ));
	}
}
