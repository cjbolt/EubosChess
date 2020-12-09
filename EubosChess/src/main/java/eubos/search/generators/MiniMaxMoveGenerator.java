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
import eubos.position.PositionManager;
import eubos.score.IEvaluate;
import eubos.search.DrawChecker;
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
	public IPositionAccessors pos;
	public PrincipalContinuation pc;

	private PlySearcher ps;
	private IEvaluate pe;
	private FixedSizeTranspositionTable tt;
	private TranspositionTableAccessor tta;
	private ScoreTracker st;
	private SearchMetrics sm;
	private short score;
	
	private KillerList killers;
	
	public static final int EXTENDED_SEARCH_PLY_LIMIT = 8;

	// Used for unit tests
	MiniMaxMoveGenerator( FixedSizeTranspositionTable hashMap,
			IChangePosition pm,
			IPositionAccessors pos) {
		this.pm = pm;
		this.pos = pos;
		this.pe = pos.getPositionEvaluator();
		sm = new SearchMetrics(pos);
		
		tt = hashMap;
		score = 0;
		killers = new KillerList(EXTENDED_SEARCH_PLY_LIMIT);
	}

	// Used with Arena, Lichess
	public MiniMaxMoveGenerator(FixedSizeTranspositionTable hashMap,
			String fen,
			DrawChecker dc) {
		PositionManager pm = new PositionManager(fen, dc);
		this.pm = pm;
		this.pos = pm;
		this.pe = pos.getPositionEvaluator();
		
		tt = hashMap;
		score = 0;
		killers = new KillerList(EubosEngineMain.SEARCH_DEPTH_IN_PLY);

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
		this.sm = new SearchMetrics(pos);
		return this.findMove(searchDepth, null, sm, new SearchMetricsReporter(null, sm));
	}
	
	public SearchResult findMove(
			byte searchDepth, 
			List<Integer> lastPc) throws NoLegalMoveException, InvalidPieceException {
		this.sm = new SearchMetrics(pos);
		return this.findMove(searchDepth, lastPc, sm, new SearchMetricsReporter(null, sm));
	}
	
	@Override
	public SearchResult findMove(
			byte searchDepth, 
			List<Integer> lastPc,
			SearchMetrics sm,
			SearchMetricsReporter sr) throws NoLegalMoveException, InvalidPieceException {
		boolean foundMate = false;
		this.sm = sm;
		initialiseSearchDepthDependentObjects(searchDepth, pm);
		ps = new PlySearcher(tta, st, pc, sm, sr, searchDepth, pm, pos, lastPc, pe, killers);
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
}
