package eubos.search.generators;

import java.util.List;

import com.fluxchess.jcpi.models.GenericMove;

import eubos.board.Board;
import eubos.board.InvalidPieceException;
import eubos.board.Piece;
import eubos.main.EubosEngineMain;
import eubos.position.IChangePosition;
import eubos.position.IPositionAccessors;
import eubos.position.Move;
import eubos.score.IEvaluate;
import eubos.search.KillerList;
import eubos.search.NoLegalMoveException;
import eubos.search.PlySearcher;
import eubos.search.PrincipalContinuation;
import eubos.search.ScoreTracker;
import eubos.search.SearchDebugAgent;
import eubos.search.SearchMetrics;
import eubos.search.SearchMetricsReporter;
import eubos.search.SearchResult;
import eubos.search.transposition.FixedSizeTranspositionTable;
import eubos.search.transposition.TranspositionTableAccessor;

public class MiniMaxMoveGenerator implements
		IMoveGenerator {

	private IChangePosition pm;
	private IPositionAccessors pos;
	public PrincipalContinuation pc;
	public SearchMetrics sm;
	private SearchMetricsReporter sr;
	private EubosEngineMain callback;
	private PlySearcher ps;
	private IEvaluate pe;
	private FixedSizeTranspositionTable tt;
	private TranspositionTableAccessor tta;
	private ScoreTracker st;
	private short score;
	private boolean sendInfo = false;
	private KillerList killers;
	
	public static final int EXTENDED_SEARCH_PLY_LIMIT = 8;

	// Used for unit tests
	MiniMaxMoveGenerator( FixedSizeTranspositionTable hashMap,
			IChangePosition pm,
			IPositionAccessors pos) {
		this.pm = pm;
		this.pos = pos;
		this.pe = pos.getPositionEvaluator();
		tt = hashMap;
		killers = new KillerList(EXTENDED_SEARCH_PLY_LIMIT);
		score = 0;
		sm = new SearchMetrics(pos);
	}

	// Used with Arena, Lichess
	public MiniMaxMoveGenerator( EubosEngineMain eubos,
			FixedSizeTranspositionTable hashMap,
			IChangePosition pm,
			IPositionAccessors pos,
			KillerList killers) {
		callback = eubos;
		this.pm = pm;
		this.pos = pos;
		this.pe = pos.getPositionEvaluator();
		tt = hashMap;
		this.killers = killers;
		score = 0;
		sm = new SearchMetrics(pos);
		if (EubosEngineMain.UCI_INFO_ENABLED) {
			sendInfo = true;
			sr = new SearchMetricsReporter(callback, sm);	
			sr.setSendInfo(true);
			sr.start();
		}
		SearchDebugAgent.open(pos.getMoveNumber(), pos.getOnMove() == Piece.Colour.white);
	}
	
	public short getScore() { return score; }
	
	private void initialiseSearchDepthDependentObjects(int searchDepth, IChangePosition pm) {
		pc = new PrincipalContinuation(searchDepth+EXTENDED_SEARCH_PLY_LIMIT);
		sm.setDepth(searchDepth);
		sm.setPrincipalVariation(pc.toPvList(0));
		st = new ScoreTracker(searchDepth+EXTENDED_SEARCH_PLY_LIMIT, pos.onMoveIsWhite());
		tta = new TranspositionTableAccessor(tt, pos, st);
	}
	
	@Override
	public SearchResult findMove(byte searchDepth) throws NoLegalMoveException, InvalidPieceException {
		return this.findMove(searchDepth, null);
	}
	
	@Override
	public SearchResult findMove(byte searchDepth, List<Integer> lastPc) throws NoLegalMoveException, InvalidPieceException {
		boolean foundMate = false;
		initialiseSearchDepthDependentObjects(searchDepth, pm);
		ps = new PlySearcher(tta, st, pc, sm, sr, searchDepth, pm, pos, lastPc, pe, killers);
		if (EubosEngineMain.UCI_INFO_ENABLED && sendInfo) {
			sr.setSendInfo(true);
		}
		// Descend the plies in the search tree, to full depth, updating board and scoring positions
		try {
			score = ps.searchPly().getScore();
		} catch (AssertionError e) {
			e.printStackTrace();
			System.exit(0);
		}
		if (score != Short.MIN_VALUE && score != Short.MAX_VALUE &&
			Math.abs(score) >= (Board.MATERIAL_VALUE_KING*2)) {
			foundMate = true;
		}
		if (EubosEngineMain.UCI_INFO_ENABLED && sendInfo) {
			sr.setSendInfo(false);
		}
		// Select the best move
		GenericMove bestMove = Move.toGenericMove(pc.getBestMove((byte)0));
		if (bestMove==null) {
			throw new NoLegalMoveException();
		}
		return new SearchResult(bestMove,foundMate);
	}
	
	public synchronized void terminateFindMove() {
		if (ps != null)
			ps.terminateFindMove();
	}
	
	public void terminateSearchMetricsReporter() {
		if (EubosEngineMain.UCI_INFO_ENABLED && sendInfo)
			sr.end();
	}
}
