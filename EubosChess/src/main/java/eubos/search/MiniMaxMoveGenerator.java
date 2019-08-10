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
	MiniMaxMoveGenerator( FixedSizeTranspositionTable hashMap, IChangePosition pm, IGenerateMoveList mlgen, IPositionAccessors pos) {
		this.pm = pm;
		this.pos = pos;
		this.mlgen = mlgen;
		this.hashMap = hashMap;
		sm = new SearchMetrics();
		pe = new PositionEvaluator();
	}

	// Used with Arena, Lichess
	MiniMaxMoveGenerator( EubosEngineMain eubos, FixedSizeTranspositionTable hashMap, IChangePosition pm, IGenerateMoveList mlgen, IPositionAccessors pos ) {
		this(hashMap, pm, mlgen, pos);
		sm = new SearchMetrics();
		callback = eubos;
		sendInfo = true;
	}	
	
	private void initialiseSearchDepthDependentObjects(int searchDepth) {
		sg = new MateScoreGenerator(pos, searchDepth);
		pc = new PrincipalContinuation(searchDepth);
		sm.setDepth(searchDepth);
		sm.clearCurrentMoveNumber();
		sm.setPrincipalVariation(pc.toPvList());
		sr = new SearchMetricsReporter(callback,sm);	
		if (sendInfo)
			sr.setSendInfo(true);
	}
	
	@Override
	public SearchResult findMove() throws NoLegalMoveException, InvalidPieceException {
		return this.findMove((byte)1, null);
	}
	
	@Override
	public SearchResult findMove(byte searchDepth) throws NoLegalMoveException, InvalidPieceException {
		return this.findMove(searchDepth, null);
	}
	
	@Override
	public SearchResult findMove(byte searchDepth, LinkedList<GenericMove> lastPc) throws NoLegalMoveException, InvalidPieceException {
		boolean foundMate = false;
		int eval_score = 0;
		initialiseSearchDepthDependentObjects(searchDepth);
		ps = new PlySearcher(hashMap,pe,sg,pc,sm,sr,searchDepth,pm,mlgen,pos,lastPc);
		// Start the search reporter task
		if (sendInfo)
			sr.start();
		// Descend the plies in the search tree, to full depth, updating board and scoring positions
		eval_score = ps.searchPly();
		if (Math.abs(eval_score) >= eubos.board.pieces.King.MATERIAL_VALUE) {
			foundMate = true;
		}
		if (sendInfo) {
			sr.end();
			sr.reportNodeData();
		}
		// Select the best move
		GenericMove bestMove = pc.getBestMove();
		if (bestMove==null) {
			throw new NoLegalMoveException();
		}
		return new SearchResult(bestMove,foundMate);
	}
	
	synchronized void terminateFindMove() {
		if (ps != null)
			ps.terminateFindMove();
		}
}
