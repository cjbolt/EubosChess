package eubos.search;

import java.util.LinkedList;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.InvalidPieceException;
import eubos.main.EubosEngineMain;
import eubos.position.MateScoreGenerator;
import eubos.position.IChangePosition;
import eubos.position.IGenerateMoveList;
import eubos.position.IPositionAccessors;
import eubos.position.PositionEvaluator;

class MiniMaxMoveGenerator implements
		IMoveGenerator {

	private IChangePosition pm;
	private IGenerateMoveList mlgen;
	private IPositionAccessors pos;
	private PositionEvaluator pe;
	private MateScoreGenerator sg;
	PrincipalContinuation pc;
	private SearchMetrics sm;
	private SearchMetricsReporter sr;
	private boolean sendInfo = false;
	private EubosEngineMain callback;
	private PlySearcher ps;
	private FixedSizeTranspositionTable hashMap;

	// Used for unit tests
	MiniMaxMoveGenerator( IChangePosition pm, IGenerateMoveList mlgen, IPositionAccessors pos) {
		this.pm = pm;
		this.pos = pos;
		this.mlgen = mlgen;
		pe = new PositionEvaluator();
	}

	// Used with Arena, Lichess
	MiniMaxMoveGenerator( EubosEngineMain eubos, FixedSizeTranspositionTable hashMap, IChangePosition pm, IGenerateMoveList mlgen, IPositionAccessors pos ) {
		this(pm, mlgen, pos);
		callback = eubos;
		sendInfo = true;
		this.hashMap = hashMap;
	}	
	
	private void initialiseSearchDepthDependentObjects(int searchDepth) {
		sg = new MateScoreGenerator(pos, searchDepth);
		pc = new PrincipalContinuation(searchDepth);
		sm = new SearchMetrics(searchDepth);
		sm.setPrincipalVariation(pc.toPvList());
		sr = new SearchMetricsReporter(callback,sm);	
		if (sendInfo)
			sr.setSendInfo(true);
	}
	
	@Override
	public GenericMove findMove() throws NoLegalMoveException, InvalidPieceException {
		return this.findMove(1, null);
	}
	
	@Override
	public GenericMove findMove(int searchDepth) throws NoLegalMoveException, InvalidPieceException {
		return this.findMove(searchDepth, null);
	}
	
	@Override
	public GenericMove findMove(int searchDepth, LinkedList<GenericMove> lastPc) throws NoLegalMoveException, InvalidPieceException {
		initialiseSearchDepthDependentObjects(searchDepth);
		ps = new PlySearcher(hashMap,pe,sg,pc,sm,sr,searchDepth,pm,mlgen,pos,lastPc);
		// Start the search reporter task
		if (sendInfo)
			sr.start();
		// Descend the plies in the search tree, to full depth, updating board and scoring positions
		ps.searchPly();
		if (sendInfo) {
			sr.end();
			sr.reportNodeData();
		}
		// Select the best move
		GenericMove bestMove = pc.getBestMove();
		if (bestMove==null) {
			throw new NoLegalMoveException();
		}
		return bestMove;
	}
	
	synchronized void terminateFindMove() {
		if (ps != null)
			ps.terminateFindMove();
		}
}
